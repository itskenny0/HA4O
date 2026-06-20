package com.github.itskenny0.ha4o

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    // 2021-01-01T00:00:00Z in epoch millis.
    private val base = 1609459200000L
    private val ts = "2021-01-01T00:00:00+00:00"

    @Test fun under_a_minute_is_just_now() {
        assertEquals("just now", RelativeTime.format(ts, base + 30_000))
    }

    @Test fun minutes_bucket() {
        assertEquals("5 min ago", RelativeTime.format(ts, base + 5 * 60_000))
    }

    @Test fun hours_bucket() {
        assertEquals("2 h ago", RelativeTime.format(ts, base + 2 * 3_600_000))
    }

    @Test fun days_bucket() {
        assertEquals("3 d ago", RelativeTime.format(ts, base + 3 * 86_400_000L))
    }

    @Test fun accepts_microseconds_and_z_suffix() {
        assertEquals("1 min ago", RelativeTime.format("2021-01-01T00:00:00.123456Z", base + 60_000))
    }

    @Test fun future_timestamps_read_as_just_now() {
        assertEquals("just now", RelativeTime.format(ts, base - 5_000))
    }

    @Test fun malformed_input_is_empty() {
        assertEquals("", RelativeTime.format("not a date", base))
        assertEquals("", RelativeTime.format("", base))
    }
}
