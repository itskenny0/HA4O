package com.github.itskenny0.ha4o

import com.github.itskenny0.ha4o.Pages.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagesTest {

    @Test fun null_or_empty_decodes_to_a_single_home_page() {
        assertEquals(Pages.default(), Pages.decode(null))
        assertEquals(Pages.default(), Pages.decode(""))
        assertEquals(listOf(Page("home", "Home")), Pages.default())
    }

    @Test fun round_trips_pages_including_an_empty_one() {
        val pages = listOf(
            Page("home", "Home", listOf("light.kitchen", "switch.fan")),
            Page("p1", "Bedroom", emptyList()),
        )
        assertEquals(pages, Pages.decode(Pages.encode(pages)))
    }

    @Test fun add_page_uses_fresh_p_ids() {
        var pages = Pages.default()
        pages = Pages.addPage(pages, "Bedroom")
        pages = Pages.addPage(pages, "Office")
        assertEquals(listOf("home", "p1", "p2"), pages.map { it.id })
        assertEquals("Bedroom", pages[1].name)
    }

    @Test fun delete_refuses_the_last_page_but_removes_others() {
        val two = listOf(Page("home", "Home"), Page("p1", "B"))
        assertEquals(listOf("home"), Pages.deletePage(two, "p1").map { it.id })
        val one = listOf(Page("home", "Home"))
        assertEquals(one, Pages.deletePage(one, "home"))
    }

    @Test fun rename_changes_only_the_target() {
        val pages = listOf(Page("home", "Home"), Page("p1", "B"))
        assertEquals(listOf("Home", "Lounge"), Pages.renamePage(pages, "p1", "Lounge").map { it.name })
    }

    @Test fun add_entity_appends_without_duplicates() {
        var pages = listOf(Page("home", "Home"))
        pages = Pages.addEntity(pages, "home", "light.k")
        pages = Pages.addEntity(pages, "home", "light.k") // dup ignored
        pages = Pages.addEntity(pages, "home", "fan.o")
        assertEquals(listOf("light.k", "fan.o"), pages[0].ids)
    }

    @Test fun remove_entity_drops_it() {
        val pages = listOf(Page("home", "Home", listOf("light.k", "fan.o")))
        assertEquals(listOf("fan.o"), Pages.removeEntity(pages, "home", "light.k")[0].ids)
    }

    @Test fun malformed_lines_are_ignored() {
        assertTrue(Pages.decode("\n\t\n").let { it == Pages.default() || it.isNotEmpty() })
        assertEquals(listOf("home"), Pages.decode("home\tHome\t").map { it.id })
    }
}
