// ARMOR-ANDROID-CONTROL - the two addresses of an installation (at home and away from home), which one to use now, and what to tell the person when the house cannot be reached.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

/** Where the phone is, as far as the choice of address goes. */
enum class NetworkPlace { Home, Away, Unknown;
    companion object {
        /**
         * From the kinds of network the phone is on. A VPN means "away" (the address of the VPN is the one to use: the phone is not on the house's network even if it is on some Wi-Fi), a Wi-Fi or an
         * Ethernet without a VPN means "at home" (the private address is tried first; if it does not answer, the other one is), mobile data alone means "away".
         */
        fun of(wifi: Boolean, ethernet: Boolean, cellular: Boolean, vpn: Boolean): NetworkPlace = when {
            vpn -> Away
            wifi || ethernet -> Home
            cellular -> Away
            else -> Unknown
        }
    }
}

/** The two addresses typed by the person. Either may be missing (a house with no way in from outside has only the first). */
data class ServerAddresses(val home: ServerEndpoint?, val away: ServerEndpoint?) {
    /** The address to use now: at home the private one, away the one for outside; with only one, that one; while the place is unknown, the private one if there is one. */
    fun chosen(place: NetworkPlace): ServerEndpoint? = when (place) {
        NetworkPlace.Home -> home ?: away
        NetworkPlace.Away -> away ?: home
        NetworkPlace.Unknown -> home ?: away
    }

    /** The other address, tried when the one in use does not answer; null when there is no other. */
    fun alternativeTo(used: ServerEndpoint): ServerEndpoint? = when (used) {
        home -> away?.takeIf { it != used }
        away -> home?.takeIf { it != used }
        else -> null
    }

    /** True when [endpoint] is the one for away from home (and not also the one for home). */
    fun isAway(endpoint: ServerEndpoint): Boolean = endpoint == away && endpoint != home

    val isEmpty: Boolean get() = home == null && away == null

    fun save(store: KeyValueStore) {
        store.put(HOME, home?.origin.orEmpty())
        store.put(AWAY, away?.origin.orEmpty())
    }

    companion object {
        private const val HOME = "home_origin"
        private const val AWAY = "away_origin"

        /** What was saved; the address of a version that kept only one (the key `origin`) is taken as the one for home. */
        fun load(store: KeyValueStore): ServerAddresses = ServerAddresses(
            home = (store.get(HOME)?.takeIf { it.isNotBlank() } ?: store.get("origin"))?.let { ServerEndpoint.parse(it) },
            away = store.get(AWAY)?.takeIf { it.isNotBlank() }?.let { ServerEndpoint.parse(it) },
        )
    }
}

/** Why the house could not be reached, in words. */
enum class ReachProblem {
    NoAddress, Offline, HomeUnreachableNoRemote, RemoteUnreachable, BothUnreachable, CertificateChanged;

    val message: String get() = when (this) {
        NoAddress -> tr("La dirección del servidor no es válida.")
        Offline -> tr("El teléfono no tiene conexión a Internet ni a la red de casa.")
        HomeUnreachableNoRemote -> tr("No se puede llegar a casa. Si no estás en tu wifi, añade la dirección de fuera de casa (un nombre DNS o la VPN) en los ajustes del servidor.")
        RemoteUnreachable -> tr("No se puede llegar a casa desde fuera. Comprueba que el servidor tiene el acceso remoto activo y que la dirección de fuera de casa es correcta.")
        BothUnreachable -> tr("No se puede llegar a casa ni por la red de casa ni desde fuera. Comprueba la wifi o los datos del teléfono, y que el servidor está encendido.")
        CertificateChanged -> tr("El certificado del servidor ha cambiado y no coincide con el que se guardó. Si lo has renovado, vuelve a emparejar el servidor desde los ajustes; si no, alguien podría estar interceptando la conexión.")
    }

    companion object {
        /** What to say when the attempts failed: which addresses were tried, whether the phone has a network at all, and whether a certificate was refused. */
        fun of(addresses: ServerAddresses, triedHome: Boolean, triedAway: Boolean, networkAvailable: Boolean, pinMismatch: Boolean): ReachProblem = when {
            pinMismatch -> CertificateChanged
            addresses.isEmpty -> NoAddress
            !networkAvailable -> Offline
            triedHome && triedAway -> BothUnreachable
            triedAway -> RemoteUnreachable
            else -> HomeUnreachableNoRemote
        }
    }
}

sealed interface Selection {
    data class Reached(val endpoint: ServerEndpoint) : Selection
    data class Failed(val problem: ReachProblem) : Selection
}

/** Chooses the address: the one for this place, and the other when it does not answer. Any answer from the server, even a refusal, means the address is the right one; only silence moves on. */
object Connector {
    fun select(
        addresses: ServerAddresses, place: NetworkPlace, networkAvailable: Boolean, pinMismatch: () -> Boolean = { false },
        answers: (ServerEndpoint) -> Boolean,
    ): Selection {
        val first = addresses.chosen(place) ?: return Selection.Failed(ReachProblem.of(addresses, false, false, networkAvailable, false))
        var triedHome = first == addresses.home
        var triedAway = first == addresses.away
        if (answers(first)) return Selection.Reached(first)
        if (pinMismatch()) return Selection.Failed(ReachProblem.CertificateChanged)
        addresses.alternativeTo(first)?.let { other ->
            if (other == addresses.home) triedHome = true
            if (other == addresses.away) triedAway = true
            if (answers(other)) return Selection.Reached(other)
            if (pinMismatch()) return Selection.Failed(ReachProblem.CertificateChanged)
        }
        return Selection.Failed(ReachProblem.of(addresses, triedHome, triedAway, networkAvailable, false))
    }
}
