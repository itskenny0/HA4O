package com.github.itskenny0.ha4o

/** One Home Assistant entity, reduced to what the cards and the attribute dialog need. */
data class EntityState(
    val entityId: String,
    val state: String,
    val friendlyName: String,
    /** Structured attributes, stringified, in the order Home Assistant sent them. */
    val attributes: Map<String, String> = emptyMap(),
    /** ISO-8601 `last_changed` timestamp, or "" when absent. */
    val lastChanged: String = "",
) {
    val displayName: String
        get() = if (friendlyName.isNotEmpty()) friendlyName else entityId

    /** Pre-rendered "key: value" lines for the read-only attribute dialog. */
    val attributesText: String
        get() = attributes.entries.joinToString("\n") { "${it.key}: ${it.value}" }
}
