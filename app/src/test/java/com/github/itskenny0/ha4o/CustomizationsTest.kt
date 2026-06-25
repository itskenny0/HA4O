package com.github.itskenny0.ha4o

import com.github.itskenny0.ha4o.Customizations.Custom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomizationsTest {

    @Test fun round_trips() {
        val map = mapOf(
            "light.kitchen" to Custom(name = "Kitchen Lamp", glyph = "☀", color = 0xFFAABBCC.toInt()),
            "fan.office" to Custom(name = "", glyph = "❋", color = 0),
        )
        assertEquals(map, Customizations.decode(Customizations.encode(map)))
    }

    @Test fun empty_and_null_decode_to_empty() {
        assertTrue(Customizations.decode(null).isEmpty())
        assertTrue(Customizations.decode("").isEmpty())
    }

    @Test fun fully_empty_overrides_are_dropped() {
        val map = mapOf("light.k" to Custom("", "", 0))
        assertTrue(Customizations.encode(map).isEmpty())
        assertTrue(Customizations.decode(Customizations.encode(map)).isEmpty())
    }

    @Test fun malformed_lines_are_ignored() {
        assertTrue(Customizations.decode("garbage-without-tabs").isEmpty())
    }
}
