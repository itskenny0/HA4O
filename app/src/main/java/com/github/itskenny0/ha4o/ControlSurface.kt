package com.github.itskenny0.ha4o

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import org.json.JSONArray

/**
 * Builds the on-card / more-info control views for an entity, one builder per control kind,
 * all wired through [Controls] to a single service-call callback. Shared by [CardAdapter]
 * (controls on a card) and [MoreInfoView] (the full detail surface) so there is one place
 * that knows how each domain is driven. Framework widgets only, built in code.
 */
class ControlSurface(
    private val context: Context,
    private val onCall: (Controls.ServiceCall) -> Unit,
) {

    private val density = context.resources.displayMetrics.density

    /** The control rows for [entity], in display order. Empty for read-only entities. */
    fun build(entity: EntityState): List<View> {
        val id = entity.entityId
        val a = entity.attributes
        return when (Controls.describe(entity).kind) {
            Controls.Kind.LightBrightnessTemp -> listOf(
                slider("BRIGHT", pct(a["brightness"], 255.0) ?: 0) { onCall(Controls.setBrightnessPct(id, it)) },
                slider("TEMP", colorTempPct(a) ?: 0) { onCall(Controls.setColorTempPct(entity, it)) },
                colourRow(id),
                buttonRow(
                    "25%" to { onCall(Controls.setBrightnessPct(id, 25)) },
                    "50%" to { onCall(Controls.setBrightnessPct(id, 50)) },
                    "100%" to { onCall(Controls.setBrightnessPct(id, 100)) },
                    "OFF" to { onCall(Controls.turnOff(id)) },
                ),
            )

            Controls.Kind.CoverPosition -> buildList {
                add(slider("POSITION", intAttr(a["current_position"]) ?: 0) { onCall(Controls.setCoverPosition(id, it)) })
                add(buttonRow(
                    "OPEN" to { onCall(Controls.openCover(id)) },
                    "STOP" to { onCall(Controls.stopCover(id)) },
                    "CLOSE" to { onCall(Controls.closeCover(id)) },
                ))
                if (a.containsKey("current_tilt_position")) {
                    add(slider("TILT", intAttr(a["current_tilt_position"]) ?: 0) { onCall(Controls.setCoverTilt(id, it)) })
                }
            }

            Controls.Kind.FanPercent -> listOf(
                slider("SPEED", intAttr(a["percentage"]) ?: 0) { onCall(Controls.setFanPercentage(id, it)) },
                buttonRow("ON / OFF" to { onCall(Controls.toggle(id)) }),
            )

            Controls.Kind.Media -> listOf(
                slider("VOLUME", pct(a["volume_level"], 1.0) ?: 0) { onCall(Controls.setVolume(id, it)) },
                buttonRow(
                    "⏮" to { onCall(Controls.mediaPrevious(id)) },
                    "⏯" to { onCall(Controls.playPause(id)) },
                    "⏭" to { onCall(Controls.mediaNext(id)) },
                    "🔇" to { onCall(Controls.setMuted(id, a["is_volume_muted"] != "true")) },
                ),
            )

            Controls.Kind.Climate -> climateControls(entity)

            Controls.Kind.Lock -> listOf(buttonRow(
                "LOCK" to { onCall(Controls.lock(id)) },
                "UNLOCK" to { onCall(Controls.unlock(id)) },
            ))

            Controls.Kind.Vacuum -> listOf(buttonRow(
                "START" to { onCall(Controls.vacuumStart(id)) },
                "PAUSE" to { onCall(Controls.vacuumPause(id)) },
                "DOCK" to { onCall(Controls.vacuumReturn(id)) },
            ))

            Controls.Kind.Select -> selectControls(entity)

            Controls.Kind.NumberStepper -> numberControls(entity)

            Controls.Kind.TextInput -> textControls(entity)

            Controls.Kind.ButtonPress -> listOf(buttonRow("PRESS" to { onCall(Controls.press(id)) }))

            Controls.Kind.Toggle -> listOf(buttonRow(
                (if (Controls.describe(entity).isOn) "ON" else "OFF") to { onCall(Controls.toggle(id)) },
            ))

            Controls.Kind.FireOnce -> listOf(buttonRow(
                (if (Controls.domainOf(id) == "script") "RUN" else "ACTIVATE") to { onCall(Controls.fireOnce(id)) },
            ))

            Controls.Kind.ReadOnly -> emptyList()
        }
    }

    private fun climateControls(entity: EntityState): List<View> {
        val id = entity.entityId
        val a = entity.attributes
        val current = num(a["temperature"]) ?: num(entity.state) ?: 20.0
        val min = num(a["min_temp"]) ?: 7.0
        val max = num(a["max_temp"]) ?: 35.0
        val step = num(a["target_temp_step"]) ?: 0.5
        val rows = ArrayList<View>()
        rows.add(buttonRow(
            "−" to { onCall(Controls.setClimateTemperature(id, Controls.steppedNumber(current, -1, min, max, step))) },
            "${trim(current)}°" to { },
            "+" to { onCall(Controls.setClimateTemperature(id, Controls.steppedNumber(current, +1, min, max, step))) },
        ))
        val modes = jsonList(a["hvac_modes"])
        if (modes.isNotEmpty()) rows.add(wrapButtons(modes.map { m -> m to { onCall(Controls.setHvacMode(id, m)) } }))
        return rows
    }

    private fun selectControls(entity: EntityState): List<View> {
        val options = jsonList(entity.attributes["options"])
        if (options.isEmpty()) return listOf(label("(no options)"))
        return listOf(wrapButtons(options.map { o -> o to { onCall(Controls.selectOption(entity.entityId, o)) } }))
    }

    private fun numberControls(entity: EntityState): List<View> {
        val id = entity.entityId
        val a = entity.attributes
        val current = num(entity.state) ?: 0.0
        val min = num(a["min"]) ?: 0.0
        val max = num(a["max"]) ?: 100.0
        val step = num(a["step"]) ?: 1.0
        return listOf(buttonRow(
            "−" to { onCall(Controls.setNumberValue(id, Controls.steppedNumber(current, -1, min, max, step))) },
            trim(current) to { },
            "+" to { onCall(Controls.setNumberValue(id, Controls.steppedNumber(current, +1, min, max, step))) },
        ))
    }

    private fun textControls(entity: EntityState): List<View> {
        val field = EditText(context)
        field.setText(entity.state)
        field.setTextColor(Color.WHITE)
        field.setSingleLine(true)
        field.inputType = InputType.TYPE_CLASS_TEXT
        val send = Button(context)
        send.text = "SET"
        send.setOnClickListener { onCall(Controls.setText(entity.entityId, field.text.toString())) }
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(field, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(send, WRAP, WRAP)
        return listOf(row)
    }

    // --- shared widgets ---

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
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) = onChange(sb.progress)
        })
        box.addView(bar, MATCH, WRAP)
        return box
    }

    private fun colourRow(id: String): View {
        val presets = listOf(
            Triple(255, 80, 80), Triple(255, 180, 60), Triple(255, 240, 150),
            Triple(120, 220, 120), Triple(120, 180, 255), Triple(200, 130, 255), Triple(255, 255, 255),
        )
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, pad(4), 0, pad(4))
        for ((r, g, b) in presets) {
            val sw = Button(context)
            sw.setBackgroundColor((0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b)
            sw.setOnClickListener { onCall(Controls.setRgb(id, r, g, b)) }
            row.addView(sw, LinearLayout.LayoutParams(0, pad(32), 1f))
        }
        return row
    }

    private fun buttonRow(vararg buttons: Pair<String, () -> Unit>): View =
        wrapButtons(buttons.toList())

    private fun wrapButtons(buttons: List<Pair<String, () -> Unit>>): View {
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
        t.gravity = Gravity.CENTER_VERTICAL
        return t
    }

    // --- attribute helpers ---

    private fun num(s: String?): Double? = s?.toDoubleOrNull()
    private fun intAttr(s: String?): Int? = num(s)?.toInt()
    private fun pct(s: String?, full: Double): Int? = num(s)?.let { ((it / full) * 100.0).toInt().coerceIn(0, 100) }
    private fun trim(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

    private fun colorTempPct(a: Map<String, String>): Int? {
        val ct = num(a["color_temp"]) ?: return null
        val min = num(a["min_mireds"]) ?: 153.0
        val max = num(a["max_mireds"]) ?: 500.0
        if (max <= min) return null
        return (((ct - min) / (max - min)) * 100.0).toInt().coerceIn(0, 100)
    }

    private fun jsonList(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.optString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun pad(v: Int): Int = (v * density).toInt()

    companion object {
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
