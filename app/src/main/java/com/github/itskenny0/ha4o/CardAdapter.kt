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
        card.setPadding(pad(16), pad(16), pad(16), pad(16))
        card.setBackgroundDrawable(cardBackground(d))
        // Inter-card spacing is the ListView's transparent divider; item-view margins are
        // stripped by AbsListView, so they're not set here.

        val head = header(entity, d)
        head.setOnClickListener { listener.onCardTap(entity) }
        card.addView(head)
        card.addView(bigState(d))
        for (control in controls.build(entity)) card.addView(control)

        card.setOnLongClickListener { listener.onCardLongPress(entity); true }
        return card
    }

    private fun cardBackground(d: Controls.Descriptor): GradientDrawable {
        val g = Palette.forDomain(d.domain, d.isOn)
        val bg = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(g.top, g.bottom))
        bg.cornerRadius = pad(12).toFloat()
        return bg
    }

    private fun header(entity: EntityState, d: Controls.Descriptor): View {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL

        val glyph = label(glyphFor(d.domain) + "  " + (favStar(entity) + entity.displayName))
        glyph.textSize = 16f
        row.addView(glyph, LinearLayout.LayoutParams(0, WRAP, 1f))

        val age = RelativeTime.format(entity.lastChanged, System.currentTimeMillis())
        if (age.isNotEmpty()) {
            val t = label(age)
            t.textSize = 12f
            t.setTextColor(0xCCFFFFFF.toInt())
            row.addView(t, WRAP, WRAP)
        }
        return row
    }

    private fun bigState(d: Controls.Descriptor): View {
        val t = label(d.displayState)
        t.textSize = 34f
        t.setPadding(0, pad(8), 0, pad(8))
        return t
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
