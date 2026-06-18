package com.github.itskenny0.ha4o

import org.json.JSONObject

/**
 * Hand-rolled parsing of the Home Assistant WebSocket messages HA4O cares about, using
 * the built-in org.json (present since API 1, so bulletproof on Gingerbread and with no
 * runtime dependency). Kept free of Android types so it unit-tests on the JVM.
 */
object HaJson {

    /** Top-level "type" field of an incoming message ("" if absent/unparseable). */
    fun typeOf(text: String): String =
        try {
            JSONObject(text).optString("type", "")
        } catch (e: Exception) {
            ""
        }

    /** Entities from a `get_states` result message: {type:result, result:[ {..}, .. ]}. */
    fun parseStatesResult(text: String): List<EntityState> {
        val out = ArrayList<EntityState>()
        try {
            val arr = JSONObject(text).optJSONArray("result") ?: return out
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                out.add(entityFromStateObject(obj))
            }
        } catch (e: Exception) {
            // Malformed payload: return whatever we managed to collect.
        }
        return out
    }

    /**
     * The new entity state from a state_changed event, or null when new_state is null
     * (the entity was removed). Shape: {type:event, event:{data:{new_state:{..}}}}.
     */
    fun parseStateChangedEvent(text: String): EntityState? =
        try {
            val data = JSONObject(text)
                .optJSONObject("event")
                ?.optJSONObject("data")
            val newState = data?.optJSONObject("new_state")
            if (newState == null) null else entityFromStateObject(newState)
        } catch (e: Exception) {
            null
        }

    private fun entityFromStateObject(obj: JSONObject): EntityState {
        val entityId = obj.optString("entity_id", "")
        val state = obj.optString("state", "")
        val attrs = obj.optJSONObject("attributes")
        val friendly = attrs?.optString("friendly_name", "") ?: ""
        val sb = StringBuilder()
        if (attrs != null) {
            val keys = attrs.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                sb.append(k).append(": ").append(attrs.opt(k)?.toString() ?: "").append('\n')
            }
        }
        return EntityState(entityId, state, friendly, sb.toString().trim())
    }
}
