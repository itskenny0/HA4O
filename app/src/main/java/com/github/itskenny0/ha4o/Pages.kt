package com.github.itskenny0.ha4o

/**
 * The card-stack's tabs: an ordered list of user-defined pages, each a curated list of
 * entity ids (mirroring R1HA's FavoritePage). There is always at least one page ("Home").
 * Pure operations return new lists so they unit-test on the JVM; persistence lives in
 * [Prefs]. Persisted as one tab-separated record per line: `id \t name \t id1,id2,...`.
 */
object Pages {

    data class Page(val id: String, val name: String, val ids: List<String> = emptyList())

    fun default(): List<Page> = listOf(Page("home", "Home"))

    fun encode(pages: List<Page>): String =
        pages.joinToString("\n") { "${it.id}\t${it.name}\t${it.ids.joinToString(",")}" }

    fun decode(raw: String?): List<Page> {
        if (raw.isNullOrEmpty()) return default()
        val out = ArrayList<Page>()
        for (line in raw.split('\n')) {
            val f = line.split('\t')
            if (f.size < 2 || f[0].isEmpty()) continue
            val ids = if (f.size >= 3) f[2].split(',').filter { it.isNotEmpty() } else emptyList()
            out.add(Page(f[0], f[1], ids))
        }
        return if (out.isEmpty()) default() else out
    }

    /** Append a new page with a fresh id; returns the updated list. */
    fun addPage(pages: List<Page>, name: String): List<Page> = pages + Page(freshId(pages), name)

    fun renamePage(pages: List<Page>, id: String, name: String): List<Page> =
        pages.map { if (it.id == id) it.copy(name = name) else it }

    /** Delete [id], unless it's the only page (every install keeps at least one). */
    fun deletePage(pages: List<Page>, id: String): List<Page> =
        if (pages.size <= 1) pages else pages.filterNot { it.id == id }

    /** Add [entityId] to [pageId] if not already present (kept in insertion order). */
    fun addEntity(pages: List<Page>, pageId: String, entityId: String): List<Page> =
        pages.map { if (it.id == pageId && entityId !in it.ids) it.copy(ids = it.ids + entityId) else it }

    fun removeEntity(pages: List<Page>, pageId: String, entityId: String): List<Page> =
        pages.map { if (it.id == pageId) it.copy(ids = it.ids - entityId) else it }

    private fun freshId(pages: List<Page>): String {
        val ids = pages.map { it.id }.toSet()
        var n = 1
        while ("p$n" in ids) n++
        return "p$n"
    }
}
