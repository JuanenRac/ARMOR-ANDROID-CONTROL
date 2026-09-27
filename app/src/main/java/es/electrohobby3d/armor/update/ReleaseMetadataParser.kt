// ARMOR-ANDROID-CONTROL - Safe GitHub Release metadata parser.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.update

import org.json.JSONObject

/** A stable GitHub release and its explicitly named installable APK asset. */
data class AvailableUpdate(
    val version: SemanticVersion,
    val releaseName: String,
    val notes: String,
    val assetUrl: String,
)

/** Result of checking the trusted public release endpoint. */
sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val update: AvailableUpdate) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

/**
 * Pure release-metadata gate used before any APK is downloaded. Keeping it
 * free of Context, HTTP and package-manager APIs makes the update channel's
 * trust decisions directly testable on the JVM.
 */
object ReleaseMetadataParser {
    const val REQUIRED_ASSET_NAME = "ARMOR-ANDROID-CONTROL-release.apk"

    fun parseLatestStable(payload: String, installedVersionName: String): UpdateCheckResult {
        val release = JSONObject(payload)
        if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
            return UpdateCheckResult.UpToDate
        }
        val remoteVersion = SemanticVersion.parseStable(release.optString("tag_name"))
            ?: return UpdateCheckResult.Failed("La última versión publicada en GitHub no tiene una etiqueta estable MAYOR.MENOR.PARCHE.")
        val localVersion = SemanticVersion.parseStable(installedVersionName)
            ?: return UpdateCheckResult.Failed("La versión instalada no es un número de versión estable.")
        if (remoteVersion <= localVersion) return UpdateCheckResult.UpToDate

        val assets = release.optJSONArray("assets")
            ?: return UpdateCheckResult.Failed("La versión v$remoteVersion no tiene el archivo $REQUIRED_ASSET_NAME.")
        val asset = (0 until assets.length())
            .asSequence()
            .map { assets.optJSONObject(it) }
            .firstOrNull { it?.optString("name") == REQUIRED_ASSET_NAME }
            ?: return UpdateCheckResult.Failed("La versión v$remoteVersion no tiene el archivo $REQUIRED_ASSET_NAME.")
        val assetUrl = asset.optString("browser_download_url")
        if (!assetUrl.startsWith("https://")) {
            return UpdateCheckResult.Failed("El enlace del APK de la versión no usa HTTPS.")
        }
        return UpdateCheckResult.Available(
            AvailableUpdate(
                version = remoteVersion,
                releaseName = release.optString("name", "A.R.M.O.R. v$remoteVersion"),
                notes = release.optString("body").trim(),
                assetUrl = assetUrl,
            ),
        )
    }
}
