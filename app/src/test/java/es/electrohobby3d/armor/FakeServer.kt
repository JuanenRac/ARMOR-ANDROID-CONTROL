// ARMOR-ANDROID-CONTROL - a server on the loopback for the tests of the client and of the screens' state.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

internal class Seen(val method: String, val path: String, val rawPath: String, val query: String?, val headers: Map<String, String>, val body: String)

/** A server on the loopback with one handler for every path; each request it gets is written down. */
internal class FakeServer {
    val seen: MutableList<Seen> = java.util.Collections.synchronizedList(mutableListOf<Seen>())
    var respond: (Seen) -> Triple<Int, String, Map<String, String>> = { Triple(200, "{}", emptyMap()) }
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val origin: String get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/") { exchange: HttpExchange ->
            val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            val headers = exchange.requestHeaders.entries.associate { it.key.lowercase() to it.value.joinToString(",") }
            val request = Seen(exchange.requestMethod, exchange.requestURI.path, exchange.requestURI.rawPath, exchange.requestURI.rawQuery, headers, body)
            seen += request
            val (status, text, extra) = respond(request)
            val bytes = text.toByteArray(Charsets.UTF_8)
            extra.forEach { (key, value) -> exchange.responseHeaders.add(key, value) }
            if (status == 204) exchange.sendResponseHeaders(204, -1) else { exchange.sendResponseHeaders(status, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) } }
            exchange.close()
        }
        server.start()
    }
    fun stop() = server.stop(0)
    fun reply(status: Int, body: String = "{}", vararg headers: Pair<String, String>) { respond = { Triple(status, body, headers.toMap()) } }
}
