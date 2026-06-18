package com.github.itskenny0.ha4o

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavouritesTest {

    @Test fun round_trips() {
        val ids = setOf("light.kitchen", "switch.fan", "scene.movie")
        assertEquals(ids, Favourites.decode(Favourites.encode(ids)))
    }

    @Test fun empty_and_null_decode_to_empty() {
        assertTrue(Favourites.decode(null).isEmpty())
        assertTrue(Favourites.decode("").isEmpty())
        assertEquals(emptySet<String>(), Favourites.decode("\n\n  \n"))
    }

    @Test fun encode_is_stable_and_sorted() {
        // Deterministic ordering so the persisted string doesn't churn between writes.
        assertEquals("a.one\nb.two", Favourites.encode(setOf("b.two", "a.one")))
    }
}
