"""The checks of the unsigned APK of Actions and of the signed APK made from it, with small zip files made for the test (no key and no Android tools)."""
import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

import apk_artifact_check as tool


def make_apk(path: Path, entries: dict[str, bytes]) -> Path:
    with zipfile.ZipFile(path, "w") as archive:
        for name, data in entries.items():
            archive.writestr(name, data)
    return path


BASE = {"AndroidManifest.xml": b"manifest", "classes.dex": b"dex", "res/a.xml": b"res"}


def artifact(folder: Path, version: str = "1.2.3", commit: str = "a" * 40, signed: bool = False, extra: dict[str, bytes] | None = None) -> None:
    apk = make_apk(folder / tool.APK_NAME, {**BASE, **(extra or {})})
    (folder / "SHA256SUMS").write_text(f"{hashlib.sha256(apk.read_bytes()).hexdigest()}  {tool.APK_NAME}\n", encoding="utf-8")
    (folder / "build-manifest.json").write_text(json.dumps({"version": version, "commit": commit, "signed": signed}), encoding="utf-8")


class UnsignedTests(unittest.TestCase):
    def test_an_artifact_of_this_commit_and_version_passes(self):
        with tempfile.TemporaryDirectory() as holder:
            artifact(Path(holder))
            self.assertEqual(tool.unsigned_problems(Path(holder), "1.2.3", "a" * 40, False), [])

    def test_another_version_or_commit_or_a_dirty_tree_is_refused(self):
        with tempfile.TemporaryDirectory() as holder:
            artifact(Path(holder))
            found = tool.unsigned_problems(Path(holder), "1.2.4", "b" * 40, True)
        text = " ".join(found)
        self.assertIn("uncommitted", text)
        self.assertIn("version 1.2.3", text)
        self.assertIn("built from aaaaaaaaaaaa", text)

    def test_a_changed_apk_does_not_match_its_hashes(self):
        with tempfile.TemporaryDirectory() as holder:
            artifact(Path(holder))
            make_apk(Path(holder) / tool.APK_NAME, {**BASE, "classes.dex": b"other"})
            self.assertIn("does not match SHA256SUMS", " ".join(tool.unsigned_problems(Path(holder), "1.2.3", "a" * 40, False)))

    def test_an_apk_that_already_carries_a_signature_is_refused(self):
        with tempfile.TemporaryDirectory() as holder:
            artifact(Path(holder), extra={"META-INF/CERT.RSA": b"x"})
            self.assertIn("already carries a signature", " ".join(tool.unsigned_problems(Path(holder), "1.2.3", "a" * 40, False)))

    def test_a_missing_file_is_named(self):
        with tempfile.TemporaryDirectory() as holder:
            found = tool.unsigned_problems(Path(holder), "1.2.3", "a" * 40, False)
        self.assertEqual(len(found), 3)


class SignedTests(unittest.TestCase):
    def test_the_same_contents_plus_a_signature_pass(self):
        with tempfile.TemporaryDirectory() as holder:
            unsigned = make_apk(Path(holder) / "u.apk", BASE)
            signed = make_apk(Path(holder) / "s.apk", {**BASE, "META-INF/CERT.RSA": b"s", "META-INF/CERT.SF": b"s"})
            self.assertEqual(tool.signed_problems(unsigned, signed), [])

    def test_a_changed_added_or_removed_entry_is_named(self):
        with tempfile.TemporaryDirectory() as holder:
            unsigned = make_apk(Path(holder) / "u.apk", BASE)
            signed = make_apk(Path(holder) / "s.apk", {"AndroidManifest.xml": b"manifest", "classes.dex": b"changed", "lib/x.so": b"n", "META-INF/CERT.RSA": b"s"})
            text = " ".join(tool.signed_problems(unsigned, signed))
        self.assertIn("classes.dex differs", text)
        self.assertIn("res/a.xml is not in the signed APK", text)
        self.assertIn("lib/x.so was added", text)

    def test_a_file_without_a_signature_is_not_a_signed_apk(self):
        with tempfile.TemporaryDirectory() as holder:
            unsigned = make_apk(Path(holder) / "u.apk", BASE)
            copy = make_apk(Path(holder) / "c.apk", BASE)
            self.assertIn("carries no signature", " ".join(tool.signed_problems(unsigned, copy)))


if __name__ == "__main__":
    unittest.main()
