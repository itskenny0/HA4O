package com.github.itskenny0.ha4o

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainsTest {

    @Test fun toggleable_domains_use_toggle() {
        assertEquals(Domains.Call("light", "toggle"), Domains.serviceFor("light.kitchen"))
        assertEquals(Domains.Call("switch", "toggle"), Domains.serviceFor("switch.fan"))
        assertEquals(Domains.Call("input_boolean", "toggle"), Domains.serviceFor("input_boolean.guest"))
    }

    @Test fun scene_and_script_turn_on() {
        assertEquals(Domains.Call("scene", "turn_on"), Domains.serviceFor("scene.movie"))
        assertEquals(Domains.Call("script", "turn_on"), Domains.serviceFor("script.bedtime"))
    }

    @Test fun read_only_domains_are_not_controllable() {
        assertNull(Domains.serviceFor("sensor.temperature"))
        assertNull(Domains.serviceFor("weather.home"))
        assertFalse(Domains.isControllable("binary_sensor.door"))
        assertTrue(Domains.isControllable("light.kitchen"))
    }

    @Test fun domainOf_splits_on_first_dot() {
        assertEquals("light", Domains.domainOf("light.kitchen_ceiling"))
        assertEquals("", Domains.domainOf("no_dot_here"))
    }
}
