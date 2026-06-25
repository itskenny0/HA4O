package com.github.itskenny0.ha4o

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Settings, built in code with framework widgets. Each setting is a row of selectable
 * buttons with the current choice highlighted in accent. Changes are written to [Prefs]
 * immediately; MainActivity re-applies them in onResume. Spec 1 ships card layout and
 * wheel step; later batches add theme, density, and text size rows here.
 */
class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(BG)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        val p = dp(16)
        col.setPadding(p, p, p, p)

        col.addView(heading("Settings"))

        col.addView(subheading("CARD LAYOUT"))
        col.addView(
            choiceRow(
                listOf("List" to "list", "Expanded" to "expanded", "Peek deck" to "peek"),
                current = { prefs.cardLayout },
                onPick = { prefs.cardLayout = it },
            ),
        )

        col.addView(subheading("WHEEL STEP (%)"))
        col.addView(
            choiceRow(
                listOf("1" to "1", "2" to "2", "5" to "5", "10" to "10"),
                current = { prefs.wheelStep.toString() },
                onPick = { prefs.wheelStep = it.toInt() },
            ),
        )

        scroll.addView(col, MATCH, WRAP)
        setContentView(scroll)
    }

    /** A row of options; the one whose value equals [current] is highlighted. */
    private fun choiceRow(
        options: List<Pair<String, String>>,
        current: () -> String,
        onPick: (String) -> Unit,
    ): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val buttons = ArrayList<Pair<Button, String>>()
        fun restyle() {
            for ((b, value) in buttons) {
                b.setBackgroundColor(if (value == current()) ACCENT else PANEL)
                b.setTextColor(Color.WHITE)
            }
        }
        for ((label, value) in options) {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { onPick(value); restyle() }
            row.addView(b, LinearLayout.LayoutParams(0, WRAP, 1f))
            buttons.add(b to value)
        }
        restyle()
        return row
    }

    private fun heading(text: String): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = 22f
        t.setTextColor(Color.WHITE)
        t.setPadding(0, 0, 0, dp(8))
        return t
    }

    private fun subheading(text: String): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = 12f
        t.setTextColor(ACCENT)
        t.setPadding(0, dp(16), 0, dp(4))
        return t
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val BG = 0xFF121212.toInt()
        private val PANEL = 0xFF2A2A2A.toInt()
        private val ACCENT = 0xFFFF6F00.toInt()
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
