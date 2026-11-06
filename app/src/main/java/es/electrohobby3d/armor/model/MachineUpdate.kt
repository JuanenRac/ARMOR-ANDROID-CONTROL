// ARMOR-ANDROID-CONTROL - updating the programs of the machine that runs the server, from the phone, as the server reports it: whether the switch is on (here and on the maintenance helper), the
// programs the helper can update, the preview of one of them (what is installed against the newest release, and a plan that is good once for a few minutes) and the job that the apply starts.
// Nothing here runs a command: the phone only asks, and the server and the helper decide. Only a signed release is ever put in place. See ARMOR-SERVER src/routes/machine_update.ts.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor.model

import org.json.JSONArray
import org.json.JSONObject

/** The switch and the programs. [enabled] is true only when the server and the helper both have it on; [reason] says why not otherwise (switched_off, not_configured, agent_switched_off, not_answering, refused). */
data class MachineUpdateState(val enabled: Boolean, val available: Boolean, val requireSigned: Boolean, val projects: List<String>, val reason: String)

/** What a program has installed against the newest release; [planId] is present only when an update is available and is good once, for [expiresInS] seconds. */
data class MachineUpdatePreview(
    val project: String, val installed: Boolean, val localVersion: String, val githubVersion: String, val state: String, val updateAvailable: Boolean, val planId: String, val expiresInS: Int,
)

data class MachineJobStep(val id: String, val state: String, val percent: Double)

/** The job an apply started, as the maintenance helper reports it: steps with a bar each, a total, the log from a point on and the error when it failed. */
data class MachineJobView(val id: String, val state: String, val percent: Double, val steps: List<MachineJobStep>, val error: String, val log: List<String>, val logTotal: Int) {
    val running: Boolean get() = state == "running"
    val failed: Boolean get() = state == "failed"
}

object MachineUpdateParser {
    /** The names the server accepts for a program: the repository name, for example ARMOR-SERVER. */
    private val projectName = Regex("^ARMOR-[A-Z0-9][A-Z0-9-]{0,39}$")
    /** The plan the preview hands out. */
    private val planName = Regex("^[0-9a-f]{24}$")
    private val oneTimeCode = Regex("^[0-9]{6}$")
    /** The lines of the log shown on the screen, so that a long update does not fill it. */
    const val LOG_LINES = 400

    fun isProject(name: String): Boolean = projectName.matches(name)
    fun isPlan(plan: String): Boolean = planName.matches(plan)
    /** True for an empty code (the account may not have the second factor) or for six digits. */
    fun codeOk(code: String): Boolean = code.isEmpty() || oneTimeCode.matches(code)
    /** The name of the program typed again is the confirmation of the apply; it must be exactly the program's. */
    fun confirmationOk(project: String, typed: String): Boolean = isProject(project) && typed == project

    private fun JSONArray?.strings(): List<String> = buildList { if (this@strings != null) for (i in 0 until length()) optStr(i).takeIf { it.isNotBlank() }?.let(::add) }
    private fun JSONArray?.objects(): List<JSONObject> = buildList { if (this@objects != null) for (i in 0 until length()) optJSONObject(i)?.let(::add) }

    fun state(root: JSONObject): MachineUpdateState = MachineUpdateState(
        enabled = root.optBoolean("enabled"), available = root.optBoolean("available"), requireSigned = root.optBoolean("require_signed", true),
        projects = root.optJSONArray("projects").strings().filter(::isProject), reason = root.optStr("reason"),
    )

    fun preview(root: JSONObject): MachineUpdatePreview = MachineUpdatePreview(
        project = root.optStr("project"), installed = root.optBoolean("installed", true), localVersion = root.optStr("local_version"), githubVersion = root.optStr("github_version"),
        state = root.optStr("state"), updateAvailable = root.optBoolean("update_available"),
        planId = root.optStr("plan_id").takeIf { isPlan(it) }.orEmpty(), expiresInS = root.optInt("expires_in_s", 0),
    )

    /** The id of the job an apply started ("202 { id }"); empty when the answer has none. */
    fun startedId(root: JSONObject): String = root.optStr("id")

    fun job(root: JSONObject): MachineJobView = MachineJobView(
        id = root.optStr("id"), state = root.optStr("state", "running"), percent = root.optDouble("percent", 0.0).let { if (it.isNaN()) 0.0 else it.coerceIn(0.0, 100.0) },
        steps = root.optJSONArray("steps").objects().map { MachineJobStep(it.optStr("id"), it.optStr("state"), it.optDouble("percent", 0.0).let { value -> if (value.isNaN()) 0.0 else value.coerceIn(0.0, 100.0) }) }.filter { it.id.isNotBlank() },
        error = root.optStr("error"), log = root.optJSONArray("log").strings(), logTotal = root.optInt("log_total", 0),
    )

    /** The log with the lines of an answer that starts at [since]: whatever was after that point is replaced and never added twice. The screen shows the last [LOG_LINES]. */
    fun mergeLog(lines: List<String>, since: Int, fresh: List<String>): List<String> =
        if (fresh.isEmpty()) lines else lines.take(since.coerceIn(0, lines.size)) + fresh

    /** The part of a step id after the program ("server.download" is "download"). */
    fun stepPart(id: String): String = id.substringAfterLast('.')

    /** The seconds left of a plan that was handed out [elapsedMs] ago, never below zero. */
    fun secondsLeft(expiresInS: Int, elapsedMs: Long): Int = (expiresInS - (elapsedMs.coerceAtLeast(0L) / 1000L).toInt()).coerceAtLeast(0)
}
