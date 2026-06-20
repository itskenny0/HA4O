package com.github.itskenny0.ha4o

import kotlin.math.roundToInt

/**
 * Pure mapping from an entity to its on-card control descriptor and the service calls its
 * controls fire. Supersedes the old Domains helper: it folds in domain extraction and the
 * controllable? check, and adds the scalar read/write conversions the card UI needs. Kept
 * free of Android types so it unit-tests on the JVM; service-call data is a plain Map that
 * the socket layer turns into JSON.
 */
object Controls {

    enum class Kind {
        Toggle, LightBrightnessTemp, CoverPosition, FanPercent, Volume, FireOnce, ReadOnly,
    }

    data class Descriptor(
        val entityId: String,
        val domain: String,
        val kind: Kind,
        val isOn: Boolean,
        /** Big state label for the card, e.g. "21.5 °C" or "on". */
        val displayState: String,
        /** Primary slider value 0..100, or null when the card has no primary slider. */
        val primary: Int?,
        /** Secondary slider value 0..100 (colour temperature for lights), or null. */
        val secondary: Int?,
    )

    /** Default mired range when a light doesn't report its own min/max. */
    private const val DEFAULT_MIN_MIREDS = 153.0
    private const val DEFAULT_MAX_MIREDS = 500.0

    private val OFF_STATES = setOf(
        "off", "closed", "idle", "standby", "unavailable", "unknown", "none", "",
    )

    fun domainOf(entityId: String): String = entityId.substringBefore('.', "")

    fun describe(entity: EntityState): Descriptor {
        val domain = domainOf(entity.entityId)
        val kind = kindOf(domain)
        return Descriptor(
            entityId = entity.entityId,
            domain = domain,
            kind = kind,
            isOn = entity.state.lowercase() !in OFF_STATES,
            displayState = displayState(entity),
            primary = primaryOf(kind, entity.attributes),
            secondary = secondaryOf(kind, entity.attributes),
        )
    }

    fun isControllable(entityId: String): Boolean = kindOf(domainOf(entityId)) != Kind.ReadOnly

    /** A service call ready to send: target entity plus any extra service data fields. */
    data class ServiceCall(
        val domain: String,
        val service: String,
        val entityId: String,
        val data: Map<String, Any> = emptyMap(),
    )

    /** `<domain>.toggle` — for switches and other simple on/off entities. */
    fun toggle(entityId: String) = ServiceCall(domainOf(entityId), "toggle", entityId)

    /** `<domain>.turn_on` with no data — for scenes and scripts. */
    fun fireOnce(entityId: String) = ServiceCall(domainOf(entityId), "turn_on", entityId)

    /** `<domain>.turn_off` — e.g. a light's OFF button. */
    fun turnOff(entityId: String) = ServiceCall(domainOf(entityId), "turn_off", entityId)

    fun setBrightnessPct(entityId: String, pct: Int) =
        ServiceCall("light", "turn_on", entityId, mapOf("brightness_pct" to pct.coerceIn(0, 100)))

    fun setColorTempPct(entity: EntityState, pct: Int): ServiceCall {
        val min = num(entity.attributes["min_mireds"]) ?: DEFAULT_MIN_MIREDS
        val max = num(entity.attributes["max_mireds"]) ?: DEFAULT_MAX_MIREDS
        val mireds = (min + pct.coerceIn(0, 100) / 100.0 * (max - min)).roundToInt()
        return ServiceCall("light", "turn_on", entity.entityId, mapOf("color_temp" to mireds))
    }

    fun setCoverPosition(entityId: String, pct: Int) =
        ServiceCall("cover", "set_cover_position", entityId, mapOf("position" to pct.coerceIn(0, 100)))

    fun openCover(entityId: String) = ServiceCall("cover", "open_cover", entityId)
    fun closeCover(entityId: String) = ServiceCall("cover", "close_cover", entityId)
    fun stopCover(entityId: String) = ServiceCall("cover", "stop_cover", entityId)

    fun setFanPercentage(entityId: String, pct: Int) =
        ServiceCall("fan", "set_percentage", entityId, mapOf("percentage" to pct.coerceIn(0, 100)))

    fun setVolume(entityId: String, pct: Int) =
        ServiceCall("media_player", "volume_set", entityId, mapOf("volume_level" to pct.coerceIn(0, 100) / 100.0))

    fun playPause(entityId: String) = ServiceCall("media_player", "media_play_pause", entityId)

    /** Set the card's primary slider to [pct], dispatching to the right service by kind. */
    fun setPrimary(entity: EntityState, pct: Int): ServiceCall? = when (kindOf(domainOf(entity.entityId))) {
        Kind.LightBrightnessTemp -> setBrightnessPct(entity.entityId, pct)
        Kind.CoverPosition -> setCoverPosition(entity.entityId, pct)
        Kind.FanPercent -> setFanPercentage(entity.entityId, pct)
        Kind.Volume -> setVolume(entity.entityId, pct)
        else -> null
    }

    private fun displayState(entity: EntityState): String {
        val unit = entity.attributes["unit_of_measurement"]
        return if (!unit.isNullOrEmpty()) "${entity.state} $unit" else entity.state
    }

    private fun primaryOf(kind: Kind, attrs: Map<String, String>): Int? = when (kind) {
        Kind.LightBrightnessTemp -> num(attrs["brightness"])?.let { (it * 100.0 / 255.0).roundToInt() }
        Kind.CoverPosition -> num(attrs["current_position"])?.roundToInt()
        Kind.FanPercent -> num(attrs["percentage"])?.roundToInt()
        Kind.Volume -> num(attrs["volume_level"])?.let { (it * 100.0).roundToInt() }
        else -> null
    }

    private fun secondaryOf(kind: Kind, attrs: Map<String, String>): Int? {
        if (kind != Kind.LightBrightnessTemp) return null
        val ct = num(attrs["color_temp"]) ?: return null
        val min = num(attrs["min_mireds"]) ?: DEFAULT_MIN_MIREDS
        val max = num(attrs["max_mireds"]) ?: DEFAULT_MAX_MIREDS
        if (max <= min) return null
        return (((ct - min) / (max - min)) * 100.0).roundToInt().coerceIn(0, 100)
    }

    private fun num(value: String?): Double? = value?.toDoubleOrNull()

    private fun kindOf(domain: String): Kind = when (domain) {
        "light" -> Kind.LightBrightnessTemp
        "cover" -> Kind.CoverPosition
        "fan" -> Kind.FanPercent
        "media_player" -> Kind.Volume
        "switch", "input_boolean", "automation", "siren", "humidifier" -> Kind.Toggle
        "scene", "script" -> Kind.FireOnce
        else -> Kind.ReadOnly
    }
}
