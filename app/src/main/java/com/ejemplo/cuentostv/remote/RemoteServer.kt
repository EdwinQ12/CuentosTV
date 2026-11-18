package com.ejemplo.cuentostv.remote

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

data class RemoteInfo(
    val ip: String? = null,
    val port: Int = 8765,
    val code: String = "----",
    val running: Boolean = false,
    val paired: Boolean = false,
    val error: String? = null
) {
    val url: String?
        get() = ip?.let { "http://$it:$port" }
}

class RemoteServer(
    private val context: Context,
    private val stateProvider: () -> RemoteState,
    private val actionHandler: (RemoteRequest) -> Unit
) {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val _info = MutableStateFlow(RemoteInfo())
    val info: StateFlow<RemoteInfo> = _info.asStateFlow()

    private var server: NanoHTTPD? = null
    private var token: String? = null
    private var page: String = ""

    fun start() {
        if (server != null) return

        page = runCatching {
            context.assets.open("remote/index.html").bufferedReader().use { it.readText() }
        }.getOrDefault("<h1>CuentosTV</h1><p>No se encontró la página del mando.</p>")

        val ip = findLocalIp()
        var lastError: String? = null

        for (port in 8765..8769) {
            val http = object : NanoHTTPD(port) {
                override fun serve(session: IHTTPSession): Response = handle(session)
            }
            val started = runCatching { http.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) }
            if (started.isSuccess) {
                server = http
                _info.value = RemoteInfo(
                    ip = ip,
                    port = port,
                    code = newCode(),
                    running = true,
                    paired = false
                )
                return
            }
            lastError = started.exceptionOrNull()?.message
        }

        _info.value = RemoteInfo(
            ip = ip,
            code = newCode(),
            running = false,
            paired = false,
            error = lastError ?: "No se pudo abrir el puerto del mando."
        )
    }

    fun stop() {
        server?.stop()
        server = null
        token = null
        _info.update { it.copy(running = false, paired = false) }
    }

    private fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val path = session.uri
        return when {
            path == "/" || path == "/index.html" -> html(session)
            path == "/api/pair" -> pair(session)
            path == "/api/state" -> state(session)
            path == "/api/action" -> action(session)
            path == "/api/info" -> infoResponse()
            else -> text(NanoHTTPD.Response.Status.NOT_FOUND, "No encontrado")
        }
    }

    private fun html(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        if (session.method != NanoHTTPD.Method.GET) {
            return text(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "Método no permitido")
        }
        val response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "text/html; charset=utf-8",
            page
        )
        response.addHeader("Cache-Control", "no-store")
        return response
    }

    private fun pair(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        if (session.method != NanoHTTPD.Method.POST) {
            return text(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "Método no permitido")
        }

        val request = parse(session) ?: return json(
            NanoHTTPD.Response.Status.BAD_REQUEST,
            ErrorResponse("Solicitud inválida")
        )

        if (request.code != _info.value.code) {
            return json(NanoHTTPD.Response.Status.FORBIDDEN, ErrorResponse("Código incorrecto"))
        }

        val newToken = UUID.randomUUID().toString()
        token = newToken
        _info.update { it.copy(paired = true) }
        return json(NanoHTTPD.Response.Status.OK, PairResponse(newToken))
    }

    private fun state(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        if (session.method != NanoHTTPD.Method.GET) {
            return text(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "Método no permitido")
        }
        val provided = session.parameters["token"]?.firstOrNull()
        if (provided == null || provided != token) {
            return json(NanoHTTPD.Response.Status.UNAUTHORIZED, ErrorResponse("Sin vincular"))
        }
        return json(NanoHTTPD.Response.Status.OK, stateProvider())
    }

    private fun action(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        if (session.method != NanoHTTPD.Method.POST) {
            return text(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "Método no permitido")
        }
        val request = parse(session) ?: return json(
            NanoHTTPD.Response.Status.BAD_REQUEST,
            ErrorResponse("Solicitud inválida")
        )
        if (request.token != token) {
            return json(NanoHTTPD.Response.Status.UNAUTHORIZED, ErrorResponse("Sin vincular"))
        }
        actionHandler(request)
        return json(NanoHTTPD.Response.Status.OK, OkResponse())
    }

    private fun infoResponse(): NanoHTTPD.Response {
        val current = _info.value
        return json(
            NanoHTTPD.Response.Status.OK,
            RemoteInfoResponse(
                name = "CuentosTV",
                paired = current.paired,
                running = current.running
            )
        )
    }

    private fun parse(session: NanoHTTPD.IHTTPSession): RemoteRequest? {
        val body = runCatching {
            val files = HashMap<String, String>()
            session.parseBody(files)
            files["postData"].orEmpty()
        }.getOrNull() ?: return null

        return runCatching {
            json.decodeFromString(RemoteRequest.serializer(), body)
        }.getOrNull()
    }

    private fun json(status: NanoHTTPD.Response.IStatus, value: Any): NanoHTTPD.Response {
        val payload = when (value) {
            is RemoteState -> json.encodeToString(RemoteState.serializer(), value)
            is RemoteRequest -> json.encodeToString(RemoteRequest.serializer(), value)
            is PairResponse -> json.encodeToString(PairResponse.serializer(), value)
            is OkResponse -> json.encodeToString(OkResponse.serializer(), value)
            is ErrorResponse -> json.encodeToString(ErrorResponse.serializer(), value)
            is RemoteInfoResponse -> json.encodeToString(RemoteInfoResponse.serializer(), value)
            else -> "{}"
        }
        val response = NanoHTTPD.newFixedLengthResponse(status, "application/json; charset=utf-8", payload)
        response.addHeader("Cache-Control", "no-store")
        return response
    }

    private fun text(status: NanoHTTPD.Response.IStatus, message: String): NanoHTTPD.Response {
        val response = NanoHTTPD.newFixedLengthResponse(status, "text/plain; charset=utf-8", message)
        response.addHeader("Cache-Control", "no-store")
        return response
    }

    private fun newCode(): String = Random.nextInt(1000, 10000).toString()

    private fun findLocalIp(): String? {
        val interfaces = runCatching { NetworkInterface.getNetworkInterfaces().toList() }
            .getOrDefault(emptyList())

        return interfaces
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .sortedByDescending {
                when {
                    it.name.startsWith("wlan") -> 2
                    it.name.startsWith("eth") -> 1
                    else -> 0
                }
            }
            .flatMap { runCatching { it.inetAddresses.toList() }.getOrDefault(emptyList()) }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }
}

@kotlinx.serialization.Serializable
private data class RemoteInfoResponse(
    val name: String,
    val paired: Boolean,
    val running: Boolean
)
