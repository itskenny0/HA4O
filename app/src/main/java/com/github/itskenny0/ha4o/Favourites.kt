package com.github.itskenny0.ha4o

/**
 * Codec for the favourite entity-id set. SharedPreferences.getStringSet is API 11, but
 * HA4O's floor is API 9, so favourites are persisted as a single newline-joined string
 * instead. Kept pure for unit testing.
 */
object Favourites {

    fun encode(ids: Set<String>): String =
        ids.filter { it.isNotBlank() }.sorted().joinToString("\n")

    fun decode(raw: String?): Set<String> =
        raw?.split('\n')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
}
