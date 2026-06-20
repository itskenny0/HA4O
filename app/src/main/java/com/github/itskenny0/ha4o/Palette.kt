package com.github.itskenny0.ha4o

/**
 * Per-domain card gradient colours. Each domain has a saturated "on" pair; the "off"
 * variant is the same pair dimmed. Colours are plain ARGB ints (not android.graphics.Color)
 * so this stays free of Android types and unit-tests on the JVM.
 */
object Palette {

    data class Gradient(val top: Int, val bottom: Int)

    private val ON = mapOf(
        "light" to Gradient(0xFFFFCA28.toInt(), 0xFFFF8F00.toInt()),       // amber
        "cover" to Gradient(0xFF7986CB.toInt(), 0xFF303F9F.toInt()),       // indigo
        "fan" to Gradient(0xFF4DB6AC.toInt(), 0xFF00796B.toInt()),         // teal
        "media_player" to Gradient(0xFFBA68C8.toInt(), 0xFF7B1FA2.toInt()), // purple
        "switch" to Gradient(0xFF4FC3F7.toInt(), 0xFF0277BD.toInt()),      // blue
        "input_boolean" to Gradient(0xFF4FC3F7.toInt(), 0xFF0277BD.toInt()),
        "scene" to Gradient(0xFFFFD54F.toInt(), 0xFFF9A825.toInt()),       // gold
        "script" to Gradient(0xFFFFD54F.toInt(), 0xFFF9A825.toInt()),
        "default" to Gradient(0xFF90A4AE.toInt(), 0xFF455A64.toInt()),     // slate
    )

    fun forDomain(domain: String, on: Boolean): Gradient {
        val base = ON[domain] ?: ON.getValue("default")
        return if (on) base else Gradient(dim(base.top), dim(base.bottom))
    }

    /** Darken a colour to ~35% brightness, preserving alpha. */
    private fun dim(c: Int): Int {
        val a = (c ushr 24) and 0xFF
        val r = ((c ushr 16) and 0xFF) * 35 / 100
        val g = ((c ushr 8) and 0xFF) * 35 / 100
        val b = (c and 0xFF) * 35 / 100
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
