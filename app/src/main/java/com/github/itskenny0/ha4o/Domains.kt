package com.github.itskenny0.ha4o

/**
 * Pure mapping from an entity_id to the HA service that a tap should call, or null when
 * the domain isn't controllable from HA4O's one-tap model (those entities just show a
 * read-only attribute dialog). Kept free of Android types so it unit-tests on the JVM.
 */
object Domains {

    /** Domains that support a `<domain>.toggle` service — one tap flips them. */
    private val TOGGLEABLE = setOf(
        "light", "switch", "fan", "input_boolean",
        "automation", "humidifier", "siren",
    )

    /** Domains with no toggle but a sensible one-tap `turn_on` (fire-and-forget). */
    private val TURN_ON_ONLY = setOf("scene", "script")

    data class Call(val domain: String, val service: String)

    fun domainOf(entityId: String): String = entityId.substringBefore('.', "")

    /** The service to call when the user taps [entityId], or null if not controllable. */
    fun serviceFor(entityId: String): Call? {
        val domain = domainOf(entityId)
        return when {
            domain in TOGGLEABLE -> Call(domain, "toggle")
            domain in TURN_ON_ONLY -> Call(domain, "turn_on")
            else -> null
        }
    }

    fun isControllable(entityId: String): Boolean = serviceFor(entityId) != null
}
