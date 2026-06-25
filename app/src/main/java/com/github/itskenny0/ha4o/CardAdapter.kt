package com.github.itskenny0.ha4o

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Builds one tall gradient card per entity. The card's controls come from the shared
 * [ControlSurface] (so cards and the more-info screen drive each domain the same way);
 * the card adds the gradient background, header (glyph, name, relative time), and big
 * state. Tapping the header opens more-info; long-pressing favourites. Cards are built
 * fresh every getView rather than recycled: a per-domain list is short, and rebuilding
 * sidesteps the pain of rebinding SeekBar listeners across recycled rows on Gingerbread.
 */
class CardAdapter(
    private val context: Context,
    private val items: List<EntityState>,
    private val favourites: Set<String>,
    private val listener: Listener,
    private val style: Style,
    private val customizations: Map<String, Customizations.Custom>,
    /** List mode: show only the primary control; tap the card for the rest. */
    private val compact: Boolean = false,
    /** Peek-deck mode: minimum card height in px so one card fills the viewport (0 = wrap). */
    private val cardMinHeightPx: Int = 0,
) : BaseAdapter() {

    interface Listener {
        fun onServiceCall(call: Controls.ServiceCall)
        fun onCardLongPress(entity: EntityState)
        fun onCardTap(entity: EntityState)
    }

    private val density = context.resources.displayMetrics.density
    private val controls = ControlSurface(context) { listener.onServiceCall(it) }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): EntityState = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val entity = items[position]
        val d = Controls.describe(entity)
        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        val p = if (style.dense) pad(10) else pad(16)
        card.setPadding(p, p, p, p)
        card.setBackgroundDrawable(cardBackground(entity, d))
        if (cardMinHeightPx > 0) card.minimumHeight = cardMinHeightPx
        // Inter-card spacing is the ListView's transparent divider; item-view margins are
        // stripped by AbsListView, so they're not set here.

        val head = header(entity, d)
        head.setOnClickListener { listener.onCardTap(entity) }
        card.addView(head)
        card.addView(bigState(d))
        // List mode shows just the primary control (build()'s first row); the rest live in
        // more-info. Expanded and peek modes show the full surface.
        val built = controls.build(entity)
        val shown = if (compact) listOfNotNull(built.firstOrNull()) else built
        for (control in shown) card.addView(control)

        card.setOnLongClickListener { listener.onCardLongPress(entity); true }
        return card
    }

    private fun cardBackground(entity: EntityState, d: Controls.Descriptor): GradientDrawable {
        val custom = customizations[entity.entityId]
        val colors = if (custom != null && custom.color != 0) {
            intArrayOf(custom.color, darken(custom.color))
        } else {
            val g = Palette.forDomain(d.domain, d.isOn, style.paletteSet)
            intArrayOf(g.top, g.bottom)
        }
        val bg = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors)
        bg.cornerRadius = pad(12).toFloat()
        return bg
    }

    private fun header(entity: EntityState, d: Controls.Descriptor): View {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL

        val custom = customizations[entity.entityId]
        val name = custom?.name?.ifEmpty { null } ?: entity.displayName
        val glyphChar = custom?.glyph?.ifEmpty { null } ?: glyphFor(d.domain)
        val glyph = label("$glyphChar  ${favStar(entity)}$name")
        glyph.textSize = style.sp(16f)
        row.addView(glyph, LinearLayout.LayoutParams(0, WRAP, 1f))

        val age = RelativeTime.format(entity.lastChanged, System.currentTimeMillis())
        if (age.isNotEmpty()) {
            val t = label(age)
            t.textSize = style.sp(12f)
            t.setTextColor(0xCCFFFFFF.toInt())
            row.addView(t, WRAP, WRAP)
        }
        return row
    }

    private fun bigState(d: Controls.Descriptor): View {
        val t = label(d.displayState)
        t.textSize = style.sp(34f)
        t.setPadding(0, pad(8), 0, pad(8))
        return t
    }

    /** Blend a colour ~40% toward black for the gradient's bottom stop. */
    private fun darken(c: Int): Int {
        val a = (c ushr 24) and 0xFF
        val r = ((c ushr 16) and 0xFF) * 60 / 100
        val g = ((c ushr 8) and 0xFF) * 60 / 100
        val b = (c and 0xFF) * 60 / 100
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun label(text: String): TextView {
        val t = TextView(context)
        t.text = text
        t.setTextColor(Color.WHITE)
        return t
    }

    private fun favStar(entity: EntityState): String =
        if (favourites.contains(entity.entityId)) "★ " else ""

    private fun pad(v: Int): Int = (v * density).toInt()

    private fun glyphFor(domain: String): String = when (domain) {
        "light" -> "☀"
        "fan" -> "❋"
        "cover" -> "▤"
        "switch", "input_boolean" -> "⏻"
        "media_player" -> "♪"
        "climate" -> "❄"
        "lock" -> "⚿"
        "vacuum" -> "⊙"
        "scene" -> "✦"
        "script" -> "▷"
        "sensor", "binary_sensor" -> "◷"
        else -> "●"
    }

    companion object {
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
