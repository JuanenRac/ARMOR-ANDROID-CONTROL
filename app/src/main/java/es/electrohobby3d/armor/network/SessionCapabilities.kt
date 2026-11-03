// ARMOR-ANDROID-CONTROL - what the signed-in person may do, from the `capabilities` field of the session (the same reading as ARMOR-STUDIO, so both show the same things).
package es.electrohobby3d.armor.network

import org.json.JSONObject

/** The actions the server names in `capabilities.actions`. */
object CapabilityAction {
    const val ARM = "arm"
    const val DISARM = "disarm"
    const val ACKNOWLEDGE = "acknowledge"
    const val OPERATE = "operate"
    const val CAMERA_CONTROL = "camera_control"
    const val ADMINISTER = "administer"
    const val MANAGE_USERS = "manage_users"
    const val SECURITY_SETTINGS = "security_settings"
    val ALL = listOf(ARM, DISARM, ACKNOWLEDGE, OPERATE, CAMERA_CONTROL, ADMINISTER, MANAGE_USERS, SECURITY_SETTINGS)
}

/**
 * What a person may do and what the server offers. A server that announces its capabilities is believed (an action it does not name is not allowed, so a newer action never appears on
 * its own); an older server sends no field and then the role decides, as it always did: administrators everything, operators everything but the administration, anyone else nothing
 * that changes. The routes check the role again: this only decides what the screens show or enable.
 */
class SessionCapabilities private constructor(
    /** True when the server announced them; false when they were worked out from the role. */
    val announced: Boolean,
    private val actions: Set<String>,
    val features: Set<String>,
    /** The security options in force, as the server sends them (null when not announced). */
    val security: JSONObject?,
) {
    fun can(action: String): Boolean = action in actions
    fun has(feature: String): Boolean = feature in features
    override fun equals(other: Any?): Boolean = other is SessionCapabilities && announced == other.announced && actions == other.actions && features == other.features && security?.toString() == other.security?.toString()
    override fun hashCode(): Int = actions.hashCode() * 31 + features.hashCode()
    override fun toString(): String = "SessionCapabilities(announced=$announced, actions=${actions.sorted()}, features=${features.sorted()})"

    companion object {
        private val OPERATOR = setOf(CapabilityAction.ARM, CapabilityAction.DISARM, CapabilityAction.ACKNOWLEDGE, CapabilityAction.OPERATE, CapabilityAction.CAMERA_CONTROL)
        private val ADMIN = OPERATOR + setOf(CapabilityAction.ADMINISTER, CapabilityAction.MANAGE_USERS)   // security_settings needs a route only the new server has: never worked out from the role

        /** An older server: the role decides. */
        fun fromRole(role: String): SessionCapabilities =
            SessionCapabilities(false, when (role) { "admin" -> ADMIN; "operator" -> OPERATOR; else -> emptySet() }, emptySet(), null)

        /** The announced field, or null when it is missing or not the shape of version 1. */
        fun parse(json: JSONObject?): SessionCapabilities? {
            if (json == null || json.optInt("version", 0) != 1) return null
            val raw = json.optJSONObject("actions") ?: return null
            val allowed = raw.keys().asSequence().filter { raw.opt(it) == true }.toSet()
            val list = json.optJSONArray("features")
            val features = if (list == null) emptySet() else (0 until list.length()).mapNotNull { list.opt(it) as? String }.toSet()
            return SessionCapabilities(true, allowed, features, json.optJSONObject("security"))
        }

        /** The answer of `GET /api/v1/studio/session` in one place: announced when the server sent them, else from the role. */
        fun of(session: JSONObject, role: String): SessionCapabilities = parse(session.optJSONObject("capabilities")) ?: fromRole(role)
    }
}
