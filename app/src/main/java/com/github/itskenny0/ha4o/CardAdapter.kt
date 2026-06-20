package com.github.itskenny0.ha4o

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * Builds one tall gradient card per entity, with the on-card controls its domain supports
 * (brightness/temp sliders, cover position, fan speed, volume, toggle, or a fire-once
 * button). Cards are built fresh every getView call rather than recycled: a per-domain
 * list is short, and rebuilding sidesteps the well-known pain of rebinding SeekBar
 * listeners across recycled ListView rows on Gingerbread. All control wiring goes through
 * [Controls] to the [Listener].
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
    }

    private val density = context.resources.displayMetrics.density

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): EntityState = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    /** The entity-id whose primary slider the hardware wheel currently drives, or null. */
    fun primaryEntityAt(position: Int): EntityState? = items.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val entity = items[position]
        val d = Controls.describe(entity)
        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(pad(16), pad(16), pad(16), pad(16))
        card.setBackgroundDrawable(cardBackground(d))

        // Spacing between stacked cards so the next one peeks below the current.
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.setMargins(pad(8), pad(8), pad(8), 0)
        card.layoutParams = lp

        card.addView(header(entity, d))
        card.addView(bigState(d))
        addControls(card, entity, d)

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

    private fun addControls(card: LinearLayout, entity: EntityState, d: Controls.Descriptor) {
        when (d.kind) {
            Controls.Kind.LightBrightnessTemp -> {
                card.addView(slider("BRIGHT", d.primary ?: 0) { v ->
                    listener.onServiceCall(Controls.setBrightnessPct(entity.entityId, v))
                })
                card.addView(slider("TEMP", d.secondary ?: 0) { v ->
                    listener.onServiceCall(Controls.setColorTempPct(entity, v))
                })
                card.addView(buttonRow(
                    "25%" to { listener.onServiceCall(Controls.setBrightnessPct(entity.entityId, 25)) },
                    "50%" to { listener.onServiceCall(Controls.setBrightnessPct(entity.entityId, 50)) },
                    "100%" to { listener.onServiceCall(Controls.setBrightnessPct(entity.entityId, 100)) },
                    "OFF" to { listener.onServiceCall(Controls.turnOff(entity.entityId)) },
                ))
            }
            Controls.Kind.CoverPosition -> {
                card.addView(slider("POSITION", d.primary ?: 0) { v ->
                    listener.onServiceCall(Controls.setCoverPosition(entity.entityId, v))
                })
                card.addView(buttonRow(
                    "OPEN" to { listener.onServiceCall(Controls.openCover(entity.entityId)) },
                    "STOP" to { listener.onServiceCall(Controls.stopCover(entity.entityId)) },
                    "CLOSE" to { listener.onServiceCall(Controls.closeCover(entity.entityId)) },
                ))
            }
            Controls.Kind.FanPercent -> {
                card.addView(slider("SPEED", d.primary ?: 0) { v ->
                    listener.onServiceCall(Controls.setFanPercentage(entity.entityId, v))
                })
                card.addView(buttonRow(
                    "ON/OFF" to { listener.onServiceCall(Controls.toggle(entity.entityId)) },
                ))
            }
            Controls.Kind.Volume -> {
                card.addView(slider("VOLUME", d.primary ?: 0) { v ->
                    listener.onServiceCall(Controls.setVolume(entity.entityId, v))
                })
                card.addView(buttonRow(
                    "PLAY / PAUSE" to { listener.onServiceCall(Controls.playPause(entity.entityId)) },
                ))
            }
            Controls.Kind.Toggle -> card.addView(buttonRow(
                (if (d.isOn) "ON" else "OFF") to { listener.onServiceCall(Controls.toggle(entity.entityId)) },
            ))
            Controls.Kind.FireOnce -> card.addView(buttonRow(
                (if (d.domain == "script") "RUN" else "ACTIVATE") to {
                    listener.onServiceCall(Controls.fireOnce(entity.entityId))
                },
            ))
            Controls.Kind.ReadOnly -> Unit // big state only
        }
    }

    private fun slider(name: String, value: Int, onChange: (Int) -> Unit): View {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(0, pad(4), 0, pad(4))

        val l = label(name)
        l.textSize = 12f
        l.setTextColor(0xCCFFFFFF.toInt())
        box.addView(l)

        val bar = SeekBar(context)
        bar.max = 100
        bar.progress = value.coerceIn(0, 100) // set before attaching the listener
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) = onChange(sb.progress)
        })
        box.addView(bar, MATCH, WRAP)
        return box
    }

    private fun buttonRow(vararg buttons: Pair<String, () -> Unit>): View {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, pad(8), 0, 0)
        for ((text, action) in buttons) {
            val b = Button(context)
            b.text = text
            b.setOnClickListener { action() }
            row.addView(b, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        return row
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
        "scene" -> "✦"
        "script" -> "▷"
        "sensor", "binary_sensor" -> "◷"
        else -> "●"
    }

    companion object {
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
