package com.pitlanehq.android.data

import com.pitlanehq.android.model.LIVE_VARS
import com.pitlanehq.android.model.LinkState
import com.pitlanehq.android.model.LiveState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Live telemetry from your PC through your Pitlane HQ account's live room (/live), the same
 * link the web app uses: works on any network, no PC address needed. Everything is sealed
 * with the account's data key (AES-256-GCM): the server only passes it along. The PC only
 * streams while a screen of yours is watching.
 */
class PitWallRepository(private val account: AccountRepository) {
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: WebSocket? = null
    private var loop: Job? = null
    private var fields: List<String> = emptyList()
    private val _state = MutableStateFlow(LiveState())
    val state = _state.asStateFlow()

    fun start() {
        if (loop?.isActive == true) return
        loop = scope.launch {
            var wait = 2_000L
            while (true) {
                val closed = kotlinx.coroutines.CompletableDeferred<Unit>()
                val ok = runCatching { open(closed) }.onFailure { e ->
                    _state.update { it.copy(link = LinkState.OFF, pcOnline = false, message = e.message) }
                }.isSuccess
                if (ok) {
                    // keepalive: answered by the server without waking the room
                    val ping = launch { while (true) { delay(30_000); socket?.send("ping") } }
                    closed.await()
                    ping.cancel()
                    if (_state.value.message == null) wait = 2_000L
                }
                if (_state.value.message?.startsWith("Signed out") == true) break
                delay(wait)
                wait = (wait * 2).coerceAtMost(30_000L)
            }
        }
    }

    fun stop() {
        loop?.cancel()
        loop = null
        socket?.close(1000, "bye")
        socket = null
        _state.value = LiveState()
    }

    private fun open(closed: kotlinx.coroutines.CompletableDeferred<Unit>) {
        val key = account.dataKey()
        val req = Request.Builder()
            .url(SERVER.replaceFirst("http", "ws") + "/live?role=view")
            .header("Authorization", "Bearer " + account.token())
            .build()
        _state.update { it.copy(link = LinkState.CONNECTING, message = null) }
        fields = emptyList()
        socket = null
        socket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _state.update { it.copy(link = LinkState.OPEN, message = null) }
                want(webSocket, key)
                // the PC sends the list of variables once: if it got lost, ask again
                scope.launch {
                    delay(3_000)
                    if (socket === webSocket && _state.value.pcOnline && fields.isEmpty()) want(webSocket, key)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { handle(webSocket, key, text) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (socket === webSocket) _state.update { it.copy(link = LinkState.OFF, pcOnline = false) }
                closed.complete(Unit)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val msg = when (response?.code) {
                    401 -> "Signed out: sign in again"
                    429 -> "Too many screens are watching"
                    else -> t.message ?: "Connection lost"
                }
                if (socket === webSocket) _state.update { it.copy(link = LinkState.OFF, pcOnline = false, message = msg) }
                closed.complete(Unit)
            }
        })
    }

    private fun want(ws: WebSocket, key: ByteArray) {
        val msg = JSONArray().put("want").put(JSONObject().put("vars", JSONArray(LIVE_VARS)).put("all", false))
        ws.send("e:" + Crypto.seal(key, Crypto.gzip(msg.toString().toByteArray()), Crypto.LIVE_AAD))
    }

    private fun handle(ws: WebSocket, key: ByteArray, text: String) {
        if (text == "pong") return
        if (text.startsWith("{")) {
            val o = JSONObject(text)
            if (o.optString("ctl") == "pc") {
                val on = o.optBoolean("on")
                _state.update { if (on) it.copy(pcOnline = true) else it.copy(pcOnline = false, simConnected = false, values = emptyMap()) }
                if (on) want(ws, key) // a PC that just started needs to be told what to send
            }
            return
        }
        if (!text.startsWith("e:")) return
        val a = JSONArray(String(Crypto.gunzip(Crypto.open(key, text.substring(2), Crypto.LIVE_AAD))))
        when (a.getString(0)) {
            "status" -> {
                val s = a.getJSONObject(1)
                _state.update { it.copy(pcOnline = true, simConnected = s.optBoolean("connected")) }
            }
            "fields" -> {
                val f = a.getJSONArray(1)
                fields = (0 until f.length()).map { f.getString(it) }
            }
            "t" -> {
                val v = a.getJSONObject(1).getJSONArray("v")
                val names = fields
                val m = HashMap<String, Any?>(names.size)
                for (i in names.indices) if (i < v.length()) m[names[i]] = v.opt(i).takeUnless { it == JSONObject.NULL }
                _state.update { it.copy(pcOnline = true, values = m) }
            }
        }
    }
}
