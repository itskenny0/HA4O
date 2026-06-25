package com.github.itskenny0.ha4o

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {

    @Test fun light_is_amber_when_on() {
        val g = Palette.forDomain("light", on = true)
        assertEquals(0xFFFFCA28.toInt(), g.top)
        assertEquals(0xFFFF8F00.toInt(), g.bottom)
    }

    @Test fun off_variant_differs_from_on_variant() {
        for (domain in listOf("light", "cover", "fan", "switch", "sensor")) {
            assertNotEquals("$domain on/off should differ", Palette.forDomain(domain, true), Palette.forDomain(domain, false))
        }
    }

    @Test fun known_domains_have_distinct_palettes() {
        val light = Palette.forDomain("light", true)
        val cover = Palette.forDomain("cover", true)
        val fan = Palette.forDomain("fan", true)
        assertNotEquals(light, cover)
        assertNotEquals(cover, fan)
        assertNotEquals(light, fan)
    }

    @Test fun unknown_domain_falls_back_to_default() {
        assertEquals(Palette.forDomain("default", true), Palette.forDomain("nonsense_domain", true))
        assertEquals(Palette.forDomain("default", false), Palette.forDomain("nonsense_domain", false))
    }

    @Test fun palette_sets_restyle_the_gradient() {
        val vivid = Palette.forDomain("light", true, "vivid")
        val pastel = Palette.forDomain("light", true, "pastel")
        val neon = Palette.forDomain("light", true, "neon")
        assertNotEquals(vivid, pastel)
        assertNotEquals(vivid, neon)
        // Pastel is lighter: its top stop has a higher channel sum than vivid's.
        assertTrue(channelSum(pastel.top) > channelSum(vivid.top))
    }

    @Test fun default_set_matches_the_two_arg_overload() {
        assertEquals(Palette.forDomain("light", true), Palette.forDomain("light", true, "vivid"))
        assertEquals(Palette.forDomain("cover", false), Palette.forDomain("cover", false, "vivid"))
    }

    private fun channelSum(c: Int): Int =
        ((c ushr 16) and 0xFF) + ((c ushr 8) and 0xFF) + (c and 0xFF)
}
