package com.github.itskenny0.ha4o

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Home Assistant WebSocket client for HA4O, built on okhttp 3.12 (the last Gingerbread-
 * compatible release). Speaks the minimum of the HA WS API: authenticate with a
 * long-lived token, pull all states once, subscribe to state_changed, and call a
 * service. Network runs on okhttp's threads; every [Listener] callback is posted to the
 * main thread so the UI can touch views directly.
 */
class HaSocket(private val listener: Listener) {

    interface Listener {
        fun onConnected()
        fun onStates(states: List<EntityState>)
        fun onStateUpdate(entity: EntityState)
        fun onAuthFailed()
        fun onDisconnected(reason: String)
    }

    private val main = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private val nextId = AtomicInteger(1)

    private var ws: WebSocket? = null
    private var token: String = ""
    private var statesRequestId: Int = -1

    fun connect(baseUrl: String, token: String) {
        this.token = token
        val request = Request.Builder().url(Urls.webSocketUrl(baseUrl)).build()
        ws = client.newWebSocket(request, SocketListener())
    }

    /** Send a service call, optionally with extra service data (e.g. brightness_pct). */
    fun callService(domain: String, service: String, entityId: String, data: JSONObject? = null) {
        val msg = JSONObject()
            .put("id", nextId.getAndIncrement())
            .put("type", "call_service")
            .put("domain", domain)
            .put("service", service)
            .put("target", JSONObject().put("entity_id", entityId))
        if (data != null) msg.put("service_data", data)
        ws?.send(msg.toString())
    }

    /** Convenience: send a [Controls.ServiceCall], converting its data map to JSON. */
    fun callService(call: Controls.ServiceCall) {
        val data = if (call.data.isEmpty()) {
            null
        } else {
            JSONObject().also { for ((k, v) in call.data) it.put(k, v) }
        }
        callService(call.domain, call.service, call.entityId, data)
    }

    fun close() {
        ws?.close(1000, "bye")
        ws = null
    }

    private fun post(r: () -> Unit) = main.post(r)

    private inner class SocketListener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            when (HaJson.typeOf(text)) {
                "auth_required" -> {
                    val auth = JSONObject().put("type", "auth").put("access_token", token)
                    webSocket.send(auth.toString())
                }
                "auth_ok" -> {
                    statesRequestId = nextId.getAndIncrement()
                    webSocket.send(
                        JSONObject().put("id", statesRequestId).put("type", "get_states").toString(),
                    )
                    webSocket.send(
                        JSONObject()
                            .put("id", nextId.getAndIncrement())
                            .put("type", "subscribe_events")
                            .put("event_type", "state_changed")
                            .toString(),
                    )
                    post { listener.onConnected() }
                }
                "auth_invalid" -> post { listener.onAuthFailed() }
                "result" -> {
                    // Only the get_states result carries the full state array; ignore
                    // call_service acknowledgements.
                    if (JSONObject(text).optInt("id", -1) == statesRequestId) {
                        val states = HaJson.parseStatesResult(text)
                        post { listener.onStates(states) }
                    }
                }
                "event" -> {
                    val entity = HaJson.parseStateChangedEvent(text)
                    if (entity != null) post { listener.onStateUpdate(entity) }
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            post { listener.onDisconnected(t.message ?: "connection failed") }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            post { listener.onDisconnected(if (reason.isEmpty()) "disconnected" else reason) }
        }
    }
}
