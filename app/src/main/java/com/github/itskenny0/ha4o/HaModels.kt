package com.github.itskenny0.ha4o

/** One Home Assistant entity, reduced to what the list and the attribute dialog need. */
data class EntityState(
    val entityId: String,
    val state: String,
    val friendlyName: String,
    /** Pre-rendered "key: value" lines for the read-only attribute dialog. */
    val attributesText: String,
) {
    val displayName: String
        get() = if (friendlyName.isNotEmpty()) friendlyName else entityId
}
