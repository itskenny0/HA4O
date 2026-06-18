package com.github.itskenny0.ha4o

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlsTest {

    @Test fun http_becomes_ws() {
        assertEquals("ws://192.168.1.10:8123/api/websocket", Urls.webSocketUrl("http://192.168.1.10:8123"))
    }

    @Test fun https_becomes_wss() {
        assertEquals("wss://ha.example.com/api/websocket", Urls.webSocketUrl("https://ha.example.com"))
    }

    @Test fun trailing_slash_trimmed() {
        assertEquals("ws://h:8123/api/websocket", Urls.webSocketUrl("http://h:8123/"))
    }

    @Test fun bare_host_assumed_plain() {
        assertEquals("ws://h:8123/api/websocket", Urls.webSocketUrl("h:8123"))
    }

    @Test fun plain_http_detection() {
        assertTrue(Urls.isPlainHttp("http://h:8123"))
        assertTrue(Urls.isPlainHttp("192.168.1.5:8123"))
        assertFalse(Urls.isPlainHttp("https://h"))
    }
}
