package com.github.itskenny0.ha4o

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Turns a Home Assistant `last_changed` ISO-8601 string into a short "x min ago" label.
 * Uses SimpleDateFormat because java.time isn't available on API 9. Best-effort: returns
 * "" when the timestamp can't be parsed. Kept free of Android types so it unit-tests on
 * the JVM.
 */
object RelativeTime {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    fun format(isoTimestamp: String, nowMillis: Long): String {
        val whenMillis = parse(isoTimestamp) ?: return ""
        val diff = nowMillis - whenMillis
        return when {
            diff < MINUTE -> "just now"
            diff < HOUR -> "${diff / MINUTE} min ago"
            diff < DAY -> "${diff / HOUR} h ago"
            else -> "${diff / DAY} d ago"
        }
    }

    /** Epoch millis for an ISO-8601 timestamp, or null if it can't be parsed. */
    private fun parse(isoTimestamp: String): Long? {
        if (isoTimestamp.isEmpty()) return null
        // SimpleDateFormat on API 9 understands neither fractional seconds nor the colon
        // in a "+00:00" zone offset, so normalise both away first.
        val normalized = isoTimestamp
            .replace(Regex("\\.\\d+"), "")
            .replace(Regex("Z$"), "+0000")
            .replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(normalized)?.time
        } catch (e: Exception) {
            null
        }
    }
}
