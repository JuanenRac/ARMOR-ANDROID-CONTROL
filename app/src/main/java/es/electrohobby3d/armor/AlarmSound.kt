// ARMOR-ANDROID-CONTROL - which sound each alarm makes: one per severity, and one per kind of alarm when the person wants it, from the phone's own sounds or from a file of theirs.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns

/**
 * What plays for a notice: the sound of the channel of its severity as it is ([DEFAULT]: an alarm sound for a critical one, the notification sound for the rest), a sound that comes with
 * the phone ([SYSTEM]), or a file the person chose ([OWN], kept in the sounds of the phone so that the system can play it for a notification).
 */
data class SoundChoice(val kind: String = DEFAULT, val uri: String = "", val name: String = "") {
    val isDefault: Boolean get() = kind == DEFAULT

    companion object {
        const val DEFAULT = "default"
        const val SYSTEM = "system"
        const val OWN = "own"
    }
}

/** The sounds of the person: one per severity and, above it, one per kind of notice (the kinds of [NoticeKinds]). Nothing chosen is the way it was before: the default of each severity. */
data class SoundSettings(val bySeverity: Map<String, SoundChoice> = emptyMap(), val byKind: Map<String, SoundChoice> = emptyMap()) {
    /** The kind comes first (it is the more specific choice), then the severity, then the default. */
    fun choiceFor(severity: String, kind: String): SoundChoice = byKind[kind] ?: bySeverity[severity] ?: SoundChoice()

    fun withSeverity(severity: String, choice: SoundChoice) = copy(bySeverity = update(bySeverity, severity, choice, SEVERITIES))
    fun withKind(kind: String, choice: SoundChoice) = copy(byKind = update(byKind, kind, choice, NoticeKinds.CHOICES))

    private fun update(map: Map<String, SoundChoice>, key: String, choice: SoundChoice, known: List<String>): Map<String, SoundChoice> =
        if (key !in known) map else if (choice.isDefault) map - key else map + (key to choice)

    /** One line per choice (`S` for a severity, `K` for a kind), the fields apart by a tab, so that the store needs no more than text. */
    fun encode(): String = (bySeverity.map { (key, choice) -> line("S", key, choice) } + byKind.map { (key, choice) -> line("K", key, choice) }).sorted().joinToString("\n")

    private fun line(scope: String, key: String, choice: SoundChoice) = listOf(scope, key, choice.kind, clean(choice.uri), clean(choice.name)).joinToString("\t")

    companion object {
        /** The severities that have a sound of their own, from the most to the least grave. */
        val SEVERITIES = listOf("critical", "high", "warning")
        /** The most a sound file may weigh: a sound for a notification is short, and it is copied. */
        const val MAX_OWN_BYTES = 3L * 1024 * 1024

        private fun clean(text: String) = text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim().take(300)

        /** Reads what [encode] wrote; anything else (nothing, a damaged line, a kind that is not known, a sound without its address) is skipped and leaves the default. */
        fun decode(text: String?): SoundSettings {
            if (text.isNullOrBlank()) return SoundSettings()
            val severities = LinkedHashMap<String, SoundChoice>()
            val kinds = LinkedHashMap<String, SoundChoice>()
            for (line in text.split("\n")) {
                val parts = line.split("\t")
                if (parts.size != 5) continue
                val kind = parts[2]
                if (kind != SoundChoice.SYSTEM && kind != SoundChoice.OWN) continue
                if (parts[3].isBlank()) continue
                val choice = SoundChoice(kind, parts[3], parts[4])
                when {
                    parts[0] == "S" && parts[1] in SEVERITIES -> severities[parts[1]] = choice
                    parts[0] == "K" && parts[1] in NoticeKinds.CHOICES -> kinds[parts[1]] = choice
                }
            }
            return SoundSettings(severities, kinds)
        }

        /** The channel a notice goes to. A sound that is not the default needs a channel of its own, because Android does not let an app change the sound of a channel once it exists: the name carries what the sound is. */
        fun channelFor(severity: String, choice: SoundChoice): String {
            val base = baseChannel(severity)
            return if (choice.isDefault || choice.uri.isBlank()) base else base + "_s" + Integer.toHexString(choice.uri.hashCode())
        }

        fun baseChannel(severity: String): String = when (severity) { "critical" -> AlarmNotifier.CRITICAL_CHANNEL; "warning" -> AlarmNotifier.WARNING_CHANNEL; else -> AlarmNotifier.ALARM_CHANNEL }
    }
}

/** The sounds kept in the preferences of the app, read by the open app, the watch and the push messages alike. */
object SoundPreferences {
    private const val PREFERENCES = "armor-control"
    private const val KEY = "sound_settings"

    fun load(context: Context): SoundSettings = SoundSettings.decode(context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY, null))
    fun save(context: Context, settings: SoundSettings) { context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(KEY, settings.encode()).apply() }
}

/** The sounds of the phone and the files of the person: listing them, playing one to hear it, and keeping a file where the system can read it. */
object AlarmSounds {
    @Volatile private var playing: Ringtone? = null

    /** The sound a severity makes when nothing else was chosen. */
    fun defaultUri(severity: String): Uri? = RingtoneManager.getDefaultUri(if (severity == "critical") RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION)

    fun audioAttributes(severity: String): AudioAttributes = AudioAttributes.Builder()
        .setUsage(if (severity == "critical") AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()

    /** The sounds that come with the phone: the alarm ones first, then the notification ones (title and address). */
    fun systemSounds(context: Context): List<Pair<String, String>> {
        val found = ArrayList<Pair<String, String>>()
        for (type in listOf(RingtoneManager.TYPE_ALARM, RingtoneManager.TYPE_NOTIFICATION)) {
            val manager = RingtoneManager(context).apply { setType(type) }
            runCatching {
                manager.cursor.use { cursor ->
                    while (cursor.moveToNext() && found.size < 80) {
                        val uri = manager.getRingtoneUri(cursor.position)?.toString() ?: continue
                        if (found.none { it.second == uri }) found += cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) to uri
                    }
                }
            }
        }
        return found
    }

    /** Plays the choice (or the default of the severity) so that the person hears it; a second call stops the first. */
    fun play(context: Context, severity: String, choice: SoundChoice) {
        stop()
        val uri = if (choice.isDefault || choice.uri.isBlank()) defaultUri(severity) else Uri.parse(choice.uri)
        val ringtone = runCatching { RingtoneManager.getRingtone(context.applicationContext, uri) }.getOrNull() ?: return
        ringtone.audioAttributes = audioAttributes(severity)
        playing = ringtone
        runCatching { ringtone.play() }
    }

    fun stop() { playing?.let { runCatching { it.stop() } }; playing = null }

    /**
     * Keeps a file the person picked where the system can play it for a notification: in the notification sounds of the phone (no permission is needed from Android 10), under an ARMOR folder.
     * Returns the choice, or null when the file is too big, is not sound or could not be kept.
     */
    fun keepOwn(context: Context, source: Uri): SoundChoice? {
        val resolver = context.contentResolver
        val name = resolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val size = if (cursor.isNull(1)) 0L else cursor.getLong(1)
                if (size > SoundSettings.MAX_OWN_BYTES) return null
                cursor.getString(0)
            } else null
        } ?: "sound"
        val type = resolver.getType(source).orEmpty()
        if (type.isNotEmpty() && !type.startsWith("audio/") && type != "application/ogg") return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Before Android 10 there is no folder an app may write to without a permission: the file is used where it is, if the system lets it be read.
            runCatching { resolver.takePersistableUriPermission(source, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            return SoundChoice(SoundChoice.OWN, source.toString(), name)
        }
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "armor_" + name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60))
            put(MediaStore.Audio.Media.MIME_TYPE, type.ifEmpty { "audio/mpeg" })
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Notifications/ARMOR")
            put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
        }
        val target = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val copied = runCatching {
            resolver.openInputStream(source)?.use { input -> resolver.openOutputStream(target)?.use { output -> input.copyTo(output) } } != null
        }.getOrDefault(false)
        if (!copied) { runCatching { resolver.delete(target, null, null) }; return null }
        return SoundChoice(SoundChoice.OWN, target.toString(), name)
    }
}
