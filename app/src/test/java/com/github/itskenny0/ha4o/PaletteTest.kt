package com.github.itskenny0.ha4o

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
