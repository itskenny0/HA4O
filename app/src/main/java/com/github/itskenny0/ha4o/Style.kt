package com.github.itskenny0.ha4o

/**
 * The current look-and-feel, bundled so it can be passed to the cards, finder, tab strip,
 * and more-info in one object instead of many parameters. Built from [Prefs]; rebuilt when
 * the user changes a setting.
 */
data class Style(
    val accent: Int,
    val textScale: Float,
    val dense: Boolean,
    val paletteSet: String,
) {
    /** A text size scaled by the user's choice. */
    fun sp(base: Float): Float = base * textScale

    companion object {
        fun from(prefs: Prefs): Style = Style(
            accent = Accent.color(prefs.accent),
            textScale = textScaleOf(prefs.textSize),
            dense = prefs.density == "compact",
            paletteSet = prefs.paletteSet,
        )

        fun textScaleOf(size: String): Float = when (size) {
            "compact" -> 0.85f
            "large" -> 1.15f
            "xlarge" -> 1.3f
            else -> 1.0f
        }
    }
}
