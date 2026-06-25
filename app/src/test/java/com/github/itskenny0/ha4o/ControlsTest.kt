package com.github.itskenny0.ha4o

import com.github.itskenny0.ha4o.Controls.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsTest {

    private fun entity(id: String, state: String = "on", attrs: Map<String, String> = emptyMap()) =
        EntityState(id, state, attrs["friendly_name"] ?: "", attrs, "")

    // --- classification -----------------------------------------------------

    @Test fun domainOf_splits_on_first_dot() {
        assertEquals("light", Controls.domainOf("light.kitchen_ceiling"))
        assertEquals("", Controls.domainOf("no_dot_here"))
    }

    @Test fun maps_each_domain_to_its_control_kind() {
        assertEquals(Kind.LightBrightnessTemp, Controls.describe(entity("light.k")).kind)
        assertEquals(Kind.CoverPosition, Controls.describe(entity("cover.garage")).kind)
        assertEquals(Kind.FanPercent, Controls.describe(entity("fan.office")).kind)
        assertEquals(Kind.Media, Controls.describe(entity("media_player.tv")).kind)
        assertEquals(Kind.Toggle, Controls.describe(entity("switch.fan")).kind)
        assertEquals(Kind.Toggle, Controls.describe(entity("input_boolean.guest")).kind)
        assertEquals(Kind.Toggle, Controls.describe(entity("automation.night")).kind)
        assertEquals(Kind.FireOnce, Controls.describe(entity("scene.movie")).kind)
        assertEquals(Kind.FireOnce, Controls.describe(entity("script.bedtime")).kind)
        assertEquals(Kind.ReadOnly, Controls.describe(entity("sensor.temp")).kind)
        assertEquals(Kind.ReadOnly, Controls.describe(entity("binary_sensor.door")).kind)
    }

    @Test fun maps_the_richer_domains() {
        assertEquals(Kind.Climate, Controls.describe(entity("climate.house")).kind)
        assertEquals(Kind.Lock, Controls.describe(entity("lock.front")).kind)
        assertEquals(Kind.Vacuum, Controls.describe(entity("vacuum.robi")).kind)
        assertEquals(Kind.Select, Controls.describe(entity("input_select.mode")).kind)
        assertEquals(Kind.Select, Controls.describe(entity("select.source")).kind)
        assertEquals(Kind.NumberStepper, Controls.describe(entity("input_number.temp")).kind)
        assertEquals(Kind.NumberStepper, Controls.describe(entity("number.bri")).kind)
        assertEquals(Kind.ButtonPress, Controls.describe(entity("input_button.go")).kind)
        assertEquals(Kind.ButtonPress, Controls.describe(entity("button.restart")).kind)
        assertEquals(Kind.TextInput, Controls.describe(entity("input_text.note")).kind)
        assertEquals(Kind.TextInput, Controls.describe(entity("text.msg")).kind)
    }

    @Test fun controllable_means_not_read_only() {
        assertTrue(Controls.isControllable("light.kitchen"))
        assertTrue(Controls.isControllable("scene.movie"))
        assertFalse(Controls.isControllable("sensor.temperature"))
        assertFalse(Controls.isControllable("weather.home"))
    }

    // --- read side: on/off, display state, slider positions -----------------

    @Test fun on_off_derivation() {
        assertTrue(Controls.describe(entity("light.k", "on")).isOn)
        assertTrue(Controls.describe(entity("cover.g", "open")).isOn)
        assertTrue(Controls.describe(entity("media_player.tv", "playing")).isOn)
        assertFalse(Controls.describe(entity("switch.f", "off")).isOn)
        assertFalse(Controls.describe(entity("cover.g", "closed")).isOn)
        assertFalse(Controls.describe(entity("light.k", "unavailable")).isOn)
    }

    @Test fun display_state_appends_unit_for_sensors() {
        val d = Controls.describe(entity("sensor.temp", "21.5", mapOf("unit_of_measurement" to "°C")))
        assertEquals("21.5 °C", d.displayState)
        assertEquals("on", Controls.describe(entity("light.k", "on")).displayState)
    }

    @Test fun light_primary_reads_brightness_as_percent() {
        assertEquals(100, Controls.describe(entity("light.k", "on", mapOf("brightness" to "255"))).primary)
        assertEquals(50, Controls.describe(entity("light.k", "on", mapOf("brightness" to "128"))).primary)
        assertNull(Controls.describe(entity("light.k", "off")).primary)
    }

    @Test fun light_secondary_reads_color_temp_within_mired_range() {
        val attrs = mapOf("color_temp" to "370", "min_mireds" to "153", "max_mireds" to "500")
        assertEquals(63, Controls.describe(entity("light.k", "on", attrs)).secondary)
        assertNull(Controls.describe(entity("light.k", "on")).secondary)
    }

    @Test fun cover_fan_media_primary_sliders() {
        assertEquals(40, Controls.describe(entity("cover.g", "open", mapOf("current_position" to "40"))).primary)
        assertEquals(66, Controls.describe(entity("fan.o", "on", mapOf("percentage" to "66"))).primary)
        assertEquals(50, Controls.describe(entity("media_player.tv", "playing", mapOf("volume_level" to "0.5"))).primary)
    }

    // --- write side: service-call builders ----------------------------------

    @Test fun toggle_fire_and_turn_off_builders() {
        assertEquals(Controls.ServiceCall("switch", "toggle", "switch.fan"), Controls.toggle("switch.fan"))
        assertEquals(Controls.ServiceCall("scene", "turn_on", "scene.movie"), Controls.fireOnce("scene.movie"))
        assertEquals(Controls.ServiceCall("light", "turn_off", "light.k"), Controls.turnOff("light.k"))
    }

    @Test fun light_brightness_pct_passes_through() {
        val call = Controls.setBrightnessPct("light.k", 50)
        assertEquals("light", call.domain)
        assertEquals("turn_on", call.service)
        assertEquals(50, call.data["brightness_pct"])
    }

    @Test fun light_color_temp_pct_converts_to_mireds_within_range() {
        val e = entity("light.k", "on", mapOf("min_mireds" to "153", "max_mireds" to "500"))
        val call = Controls.setColorTempPct(e, 63)
        assertEquals("light", call.domain)
        assertEquals("turn_on", call.service)
        assertEquals(372, call.data["color_temp"])
    }

    @Test fun cover_position_and_commands() {
        assertEquals(40, Controls.setCoverPosition("cover.g", 40).data["position"])
        assertEquals("set_cover_position", Controls.setCoverPosition("cover.g", 40).service)
        assertEquals(Controls.ServiceCall("cover", "open_cover", "cover.g"), Controls.openCover("cover.g"))
        assertEquals(Controls.ServiceCall("cover", "close_cover", "cover.g"), Controls.closeCover("cover.g"))
        assertEquals(Controls.ServiceCall("cover", "stop_cover", "cover.g"), Controls.stopCover("cover.g"))
    }

    @Test fun fan_percentage_builder() {
        val call = Controls.setFanPercentage("fan.o", 66)
        assertEquals(Controls.ServiceCall("fan", "set_percentage", "fan.o", mapOf("percentage" to 66)), call)
    }

    @Test fun set_primary_dispatches_by_kind() {
        assertEquals(Controls.setBrightnessPct("light.k", 40), Controls.setPrimary(entity("light.k"), 40))
        assertEquals(Controls.setCoverPosition("cover.g", 40), Controls.setPrimary(entity("cover.g"), 40))
        assertEquals(Controls.setFanPercentage("fan.o", 40), Controls.setPrimary(entity("fan.o"), 40))
        assertEquals(Controls.setVolume("media_player.tv", 40), Controls.setPrimary(entity("media_player.tv"), 40))
    }

    @Test fun set_primary_is_null_for_non_scalar_kinds() {
        assertNull(Controls.setPrimary(entity("switch.f"), 40))
        assertNull(Controls.setPrimary(entity("scene.movie"), 40))
        assertNull(Controls.setPrimary(entity("sensor.temp"), 40))
    }

    @Test fun media_volume_and_play_pause() {
        val call = Controls.setVolume("media_player.tv", 50)
        assertEquals("media_player", call.domain)
        assertEquals("volume_set", call.service)
        assertEquals(0.5, call.data["volume_level"])
        assertEquals(
            Controls.ServiceCall("media_player", "media_play_pause", "media_player.tv"),
            Controls.playPause("media_player.tv"),
        )
    }

    @Test fun media_transport_and_mute() {
        val id = "media_player.tv"
        assertEquals(Controls.ServiceCall("media_player", "media_play", id), Controls.mediaPlay(id))
        assertEquals(Controls.ServiceCall("media_player", "media_pause", id), Controls.mediaPause(id))
        assertEquals(Controls.ServiceCall("media_player", "media_next_track", id), Controls.mediaNext(id))
        assertEquals(Controls.ServiceCall("media_player", "media_previous_track", id), Controls.mediaPrevious(id))
        assertEquals(true, Controls.setMuted(id, true).data["is_volume_muted"])
        assertEquals("volume_mute", Controls.setMuted(id, true).service)
    }

    @Test fun light_rgb_colour() {
        val call = Controls.setRgb("light.k", 255, 128, 0)
        assertEquals("light", call.domain)
        assertEquals("turn_on", call.service)
        assertEquals(listOf(255, 128, 0), call.data["rgb_color"])
    }

    @Test fun cover_tilt() {
        assertEquals(30, Controls.setCoverTilt("cover.blind", 30).data["tilt_position"])
        assertEquals("set_cover_tilt_position", Controls.setCoverTilt("cover.blind", 30).service)
        assertEquals(Controls.ServiceCall("cover", "open_cover_tilt", "cover.blind"), Controls.openCoverTilt("cover.blind"))
        assertEquals(Controls.ServiceCall("cover", "close_cover_tilt", "cover.blind"), Controls.closeCoverTilt("cover.blind"))
    }

    @Test fun climate_temperature_and_mode() {
        val call = Controls.setClimateTemperature("climate.house", 21.5)
        assertEquals(Controls.ServiceCall("climate", "set_temperature", "climate.house", mapOf("temperature" to 21.5)), call)
        assertEquals(
            Controls.ServiceCall("climate", "set_hvac_mode", "climate.house", mapOf("hvac_mode" to "heat")),
            Controls.setHvacMode("climate.house", "heat"),
        )
    }

    @Test fun lock_and_vacuum() {
        assertEquals(Controls.ServiceCall("lock", "lock", "lock.front"), Controls.lock("lock.front"))
        assertEquals(Controls.ServiceCall("lock", "unlock", "lock.front"), Controls.unlock("lock.front"))
        assertEquals(Controls.ServiceCall("vacuum", "start", "vacuum.r"), Controls.vacuumStart("vacuum.r"))
        assertEquals(Controls.ServiceCall("vacuum", "pause", "vacuum.r"), Controls.vacuumPause("vacuum.r"))
        assertEquals(Controls.ServiceCall("vacuum", "return_to_base", "vacuum.r"), Controls.vacuumReturn("vacuum.r"))
    }

    @Test fun helper_builders_use_the_entitys_own_domain() {
        assertEquals(Controls.ServiceCall("input_number", "set_value", "input_number.t", mapOf("value" to 4.0)),
            Controls.setNumberValue("input_number.t", 4.0))
        assertEquals(Controls.ServiceCall("number", "set_value", "number.t", mapOf("value" to 4.0)),
            Controls.setNumberValue("number.t", 4.0))
        assertEquals(Controls.ServiceCall("input_select", "select_option", "input_select.m", mapOf("option" to "Day")),
            Controls.selectOption("input_select.m", "Day"))
        assertEquals(Controls.ServiceCall("input_button", "press", "input_button.g"), Controls.press("input_button.g"))
        assertEquals(Controls.ServiceCall("input_text", "set_value", "input_text.n", mapOf("value" to "hi")),
            Controls.setText("input_text.n", "hi"))
    }

    @Test fun stepped_number_rounds_to_step_and_clamps() {
        assertEquals(5.0, Controls.steppedNumber(4.0, +1, min = 0.0, max = 10.0, step = 1.0), 1e-9)
        assertEquals(3.0, Controls.steppedNumber(4.0, -1, min = 0.0, max = 10.0, step = 1.0), 1e-9)
        // Snaps an off-grid current value onto the step grid before stepping.
        assertEquals(21.5, Controls.steppedNumber(21.3, +1, min = 7.0, max = 35.0, step = 0.5), 1e-9)
        // Clamps at the bounds.
        assertEquals(0.0, Controls.steppedNumber(0.0, -1, min = 0.0, max = 10.0, step = 1.0), 1e-9)
        assertEquals(10.0, Controls.steppedNumber(10.0, +1, min = 0.0, max = 10.0, step = 1.0), 1e-9)
    }
}
