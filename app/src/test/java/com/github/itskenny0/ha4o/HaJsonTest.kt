package com.github.itskenny0.ha4o

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HaJsonTest {

    @Test fun reads_top_level_type() {
        assertEquals("auth_required", HaJson.typeOf("""{"type":"auth_required","ha_version":"2024.1"}"""))
        assertEquals("", HaJson.typeOf("not json"))
    }

    @Test fun parses_get_states_result() {
        val msg = """
            {"id":1,"type":"result","success":true,"result":[
              {"entity_id":"light.kitchen","state":"on","attributes":{"friendly_name":"Kitchen","brightness":200}},
              {"entity_id":"sensor.temp","state":"21.5","attributes":{"friendly_name":"Temp","unit_of_measurement":"°C"}}
            ]}
        """.trimIndent()
        val states = HaJson.parseStatesResult(msg)
        assertEquals(2, states.size)
        assertEquals("light.kitchen", states[0].entityId)
        assertEquals("on", states[0].state)
        assertEquals("Kitchen", states[0].friendlyName)
        assertEquals("Kitchen", states[0].displayName)
        assertTrue(states[0].attributesText.contains("brightness: 200"))
        // No friendly name -> displayName falls back to entity_id
        assertEquals("Temp", states[1].displayName)
    }

    @Test fun parses_state_changed_new_state() {
        val msg = """
            {"type":"event","event":{"event_type":"state_changed","data":{
              "entity_id":"switch.fan","new_state":{"entity_id":"switch.fan","state":"off","attributes":{"friendly_name":"Fan"}}
            }}}
        """.trimIndent()
        val e = HaJson.parseStateChangedEvent(msg)
        assertEquals("switch.fan", e!!.entityId)
        assertEquals("off", e.state)
        assertEquals("Fan", e.friendlyName)
    }

    @Test fun state_changed_with_null_new_state_is_null() {
        val msg = """
            {"type":"event","event":{"event_type":"state_changed","data":{
              "entity_id":"switch.fan","new_state":null}}}
        """.trimIndent()
        assertNull(HaJson.parseStateChangedEvent(msg))
    }

    @Test fun keeps_structured_attributes_and_last_changed() {
        val msg = """
            {"id":1,"type":"result","success":true,"result":[
              {"entity_id":"light.kitchen","state":"on","last_changed":"2026-06-20T10:00:00.000000+00:00",
               "attributes":{"friendly_name":"Kitchen","brightness":200,"color_temp":370}}
            ]}
        """.trimIndent()
        val e = HaJson.parseStatesResult(msg).single()
        assertEquals("200", e.attributes["brightness"])
        assertEquals("370", e.attributes["color_temp"])
        assertEquals("Kitchen", e.attributes["friendly_name"])
        assertEquals("2026-06-20T10:00:00.000000+00:00", e.lastChanged)
    }

    @Test fun attributes_text_is_derived_from_the_map_in_order() {
        val e = EntityState(
            entityId = "fan.office",
            state = "on",
            friendlyName = "Office",
            attributes = linkedMapOf("friendly_name" to "Office", "percentage" to "66"),
            lastChanged = "",
        )
        assertEquals("friendly_name: Office\npercentage: 66", e.attributesText)
    }
}
