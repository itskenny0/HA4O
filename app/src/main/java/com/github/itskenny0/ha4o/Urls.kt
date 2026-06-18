package com.github.itskenny0.ha4o

/** URL helpers, kept pure for unit testing. */
object Urls {

    /**
     * Derive the HA WebSocket URL from a base URL. http -> ws, https -> wss, trailing
     * slashes trimmed, `/api/websocket` appended. HA4O realistically only ever uses the
     * ws:// (plain) form, since Gingerbread can't negotiate modern TLS for wss://.
     */
    fun webSocketUrl(baseUrl: String): String {
        var b = baseUrl.trim().trimEnd('/')
        b = when {
            b.startsWith("https://") -> "wss://" + b.removePrefix("https://")
            b.startsWith("http://") -> "ws://" + b.removePrefix("http://")
            else -> "ws://$b" // assume plain http host:port if no scheme given
        }
        return "$b/api/websocket"
    }

    /** True for a base URL HA4O can actually reach on Gingerbread (plain http/ws). */
    fun isPlainHttp(baseUrl: String): Boolean =
        baseUrl.trim().startsWith("http://") || !baseUrl.trim().contains("://")
}
