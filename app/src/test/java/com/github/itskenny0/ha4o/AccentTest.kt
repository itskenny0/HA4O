package com.github.itskenny0.ha4o

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccentTest {

    @Test fun known_roles_map_to_distinct_colours() {
        val warm = Accent.color("warm")
        val cool = Accent.color("cool")
        val green = Accent.color("green")
        val neutral = Accent.color("neutral")
        assertNotEquals(warm, cool)
        assertNotEquals(cool, green)
        assertNotEquals(green, neutral)
    }

    @Test fun unknown_falls_back_to_warm() {
        assertEquals(Accent.color("warm"), Accent.color("nonsense"))
        assertEquals(Accent.color("warm"), Accent.color(""))
    }
}
