#!/usr/bin/env python3
"""ARMOR-ANDROID-CONTROL - check the unsigned APK that Actions built, and the signed APK made from it, on the maintainer's machine.

Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.

    python tools/apk_artifact_check.py unsigned DIR       # DIR = the downloaded artifact (APK, SHA256SUMS, build-manifest.json): hashes, manifest, version and commit
    python tools/apk_artifact_check.py signed UNSIGNED.apk SIGNED.apk   # the signed APK carries exactly the contents of the unsigned one (only META-INF differs)

The release is built by GitHub Actions without any key and signed here: the key never goes to Actions. These two checks close the gap between them. `unsigned` refuses an artifact whose
hashes, version or commit do not match this checkout (a dirty or unpushed tree is refused too), so that what is signed is the commit that is released. `signed` refuses a signed file
that is not the build that was checked: a different class file, resource or native library, or an entry added or removed outside META-INF. Nothing here reads a key; it prints the
SHA-256 of the signed file so that it can be written in the release notes.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APK_NAME = "ARMOR-ANDROID-CONTROL-release-unsigned.apk"
SIGNATURE_ENTRY = re.compile(r"META-INF/.*\.(RSA|DSA|EC|SF)$")


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git_text(*args: str, cwd: Path = ROOT) -> str:
    done = subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True, timeout=60)
    return done.stdout.strip() if done.returncode == 0 else ""


def unsigned_problems(folder: Path, version: str, commit: str, dirty: bool) -> list[str]:
    """Why the downloaded artifact must not be signed, or an empty list."""
    problems: list[str] = []
    if dirty:
        problems.append("the working tree has uncommitted changes: sign only a clean checkout of the released commit")
    sums, manifest_file, apk = folder / "SHA256SUMS", folder / "build-manifest.json", folder / APK_NAME
    missing = [f"{path.name} is missing from the artifact" for path in (sums, manifest_file, apk) if not path.is_file()]
    if missing:
        return problems + missing
    listed = {}
    for line in sums.read_text(encoding="utf-8").splitlines():
        if "  " in line:
            digest, name = line.strip().split("  ", 1)
            listed[name] = digest
    if listed.get(APK_NAME) != sha256_of(apk):
        problems.append("the APK does not match SHA256SUMS")
    try:
        manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
    except ValueError:
        return problems + ["build-manifest.json is not JSON"]
    if manifest.get("signed") is not False:
        problems.append("the manifest does not say the APK is unsigned")
    if manifest.get("version") != version:
        problems.append(f"the artifact is version {manifest.get('version')}, this checkout is {version}")
    if manifest.get("commit") != commit:
        problems.append(f"the artifact was built from {str(manifest.get('commit'))[:12]}, this checkout is at {commit[:12]}")
    if any(SIGNATURE_ENTRY.match(name) for name in zipfile.ZipFile(apk).namelist() if name.startswith("META-INF/")):
        problems.append("the unsigned APK already carries a signature")
    return problems


def payload(path: Path) -> dict[str, str]:
    """The SHA-256 of every entry outside META-INF."""
    with zipfile.ZipFile(path) as archive:
        return {name: hashlib.sha256(archive.read(name)).hexdigest() for name in archive.namelist() if not name.startswith("META-INF/") and not name.endswith("/")}


def signed_problems(unsigned: Path, signed: Path) -> list[str]:
    """Why a signed APK is not the checked build, or an empty list."""
    before, after = payload(unsigned), payload(signed)
    problems = [f"{name} is not in the signed APK" for name in sorted(set(before) - set(after))]
    problems += [f"{name} was added to the signed APK" for name in sorted(set(after) - set(before))]
    problems += [f"{name} differs in the signed APK" for name in sorted(name for name in set(before) & set(after) if before[name] != after[name])]
    if not any(name.startswith("META-INF/") and SIGNATURE_ENTRY.match(name) for name in zipfile.ZipFile(signed).namelist()):
        problems.append("the signed APK carries no signature")
    return problems


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Check the unsigned APK of Actions and the signed APK made from it.")
    sub = parser.add_subparsers(dest="command", required=True)
    unsigned = sub.add_parser("unsigned")
    unsigned.add_argument("folder", type=Path)
    signed = sub.add_parser("signed")
    signed.add_argument("unsigned_apk", type=Path)
    signed.add_argument("signed_apk", type=Path)
    args = parser.parse_args(argv)
    if args.command == "unsigned":
        version = json.loads((ROOT / "armor.project.json").read_text(encoding="utf-8"))["version"]
        found = unsigned_problems(args.folder, version, git_text("rev-parse", "HEAD"), bool(git_text("status", "--porcelain")))
    else:
        found = signed_problems(args.unsigned_apk, args.signed_apk)
    for line in found:
        print(line, file=sys.stderr)
    if args.command == "signed" and not found:
        print(f"signed sha256 {sha256_of(args.signed_apk)}")
    print(f"APK_{args.command.upper()}={'FAIL' if found else 'PASS'}")
    return 1 if found else 0


if __name__ == "__main__":
    raise SystemExit(main())
