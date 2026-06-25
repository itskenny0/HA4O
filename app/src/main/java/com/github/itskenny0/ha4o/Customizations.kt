package com.github.itskenny0.ha4o

/**
 * Per-entity display overrides — a custom name, glyph, and/or card colour — keyed by
 * entity id. Persisted as one tab-separated record per line (entity ids, names, and
 * glyphs never contain a tab or newline). Fully-empty overrides are dropped so the store
 * stays minimal. Pure, so it unit-tests on the JVM; see [Prefs] for storage.
 */
object Customizations {

    /** An override; empty strings / 0 colour mean "not set, use the default". */
    data class Custom(val name: String = "", val glyph: String = "", val color: Int = 0) {
        val isEmpty: Boolean get() = name.isEmpty() && glyph.isEmpty() && color == 0
    }

    fun encode(map: Map<String, Custom>): String =
        map.entries
            .filter { it.key.isNotEmpty() && !it.value.isEmpty }
            .sortedBy { it.key }
            .joinToString("\n") { (id, c) -> "$id\t${c.name}\t${c.glyph}\t${c.color}" }

    fun decode(raw: String?): Map<String, Custom> {
        if (raw.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, Custom>()
        for (line in raw.split('\n')) {
            val f = line.split('\t')
            if (f.size < 4) continue
            val id = f[0]
            val custom = Custom(f[1], f[2], f[3].toIntOrNull() ?: 0)
            if (id.isNotEmpty() && !custom.isEmpty) out[id] = custom
        }
        return out
    }
}
