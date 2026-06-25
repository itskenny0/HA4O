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

    fun forDomain(domain: String, on: Boolean): Gradient = forDomain(domain, on, "vivid")

    /**
     * The card gradient for [domain], restyled by palette [set]: "vivid" (the saturated
     * default), "pastel" (lightened toward white), or "neon" (brightened/punchier).
     */
    fun forDomain(domain: String, on: Boolean, set: String): Gradient {
        val base = ON[domain] ?: ON.getValue("default")
        val vivid = if (on) base else Gradient(dim(base.top), dim(base.bottom))
        return when (set) {
            "pastel" -> Gradient(lighten(vivid.top), lighten(vivid.bottom))
            "neon" -> Gradient(boost(vivid.top), boost(vivid.bottom))
            else -> vivid
        }
    }

    /** Darken a colour to ~35% brightness, preserving alpha. */
    private fun dim(c: Int): Int {
        val a = (c ushr 24) and 0xFF
        val r = ((c ushr 16) and 0xFF) * 35 / 100
        val g = ((c ushr 8) and 0xFF) * 35 / 100
        val b = (c and 0xFF) * 35 / 100
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Blend ~45% toward white for a softer, pastel look. */
    private fun lighten(c: Int): Int = blendToward(c, 255, 45)

    /** Push channels up ~30% (clamped) for a punchier, neon look. */
    private fun boost(c: Int): Int {
        val a = (c ushr 24) and 0xFF
        val r = (((c ushr 16) and 0xFF) * 130 / 100).coerceAtMost(255)
        val g = (((c ushr 8) and 0xFF) * 130 / 100).coerceAtMost(255)
        val b = ((c and 0xFF) * 130 / 100).coerceAtMost(255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun blendToward(c: Int, target: Int, pct: Int): Int {
        val a = (c ushr 24) and 0xFF
        val r = mix((c ushr 16) and 0xFF, target, pct)
        val g = mix((c ushr 8) and 0xFF, target, pct)
        val b = mix(c and 0xFF, target, pct)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun mix(from: Int, to: Int, pct: Int): Int = from + (to - from) * pct / 100
}
