package com.github.itskenny0.ha4o

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Full-screen more-info detail for one entity: title and close, the big state and relative
 * time, the entity's complete control surface (shared with the cards via [ControlSurface]),
 * and its attributes. Built fresh per entity; the host rebuilds it on live state updates
 * while it's open. Framework widgets only. (History is added in a later batch.)
 */
class MoreInfoView(
    context: Context,
    entity: EntityState,
    onCall: (Controls.ServiceCall) -> Unit,
    onClose: () -> Unit,
) {

    private val ctx = context
    private val density = context.resources.displayMetrics.density
    val root: View

    init {
        val scroll = ScrollView(context)
        scroll.setBackgroundColor(0xFF121212.toInt())
        scroll.isFillViewport = true

        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(pad(16), pad(16), pad(16), pad(16))

        val titleRow = LinearLayout(context)
        titleRow.orientation = LinearLayout.HORIZONTAL
        titleRow.gravity = Gravity.CENTER_VERTICAL
        val title = text(entity.displayName, 20f, Color.WHITE)
        titleRow.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        val close = Button(context)
        close.text = "×"
        close.setOnClickListener { onClose() }
        titleRow.addView(close, WRAP, WRAP)
        col.addView(titleRow, MATCH, WRAP)

        val d = Controls.describe(entity)
        col.addView(text(d.displayState, 34f, Color.WHITE))
        val age = RelativeTime.format(entity.lastChanged, System.currentTimeMillis())
        if (age.isNotEmpty()) col.addView(text(age, 12f, 0xFF9E9E9E.toInt()))

        for (control in ControlSurface(context, onCall).build(entity)) col.addView(control)

        if (entity.attributes.isNotEmpty()) {
            col.addView(spacer())
            col.addView(text("ATTRIBUTES", 12f, 0xFFFF6F00.toInt()))
            col.addView(text(entity.attributesText, 13f, 0xFFBBBBBB.toInt()))
        }

        scroll.addView(col, MATCH, WRAP)
        root = scroll
    }

    private fun text(s: String, size: Float, color: Int): TextView {
        val t = TextView(ctx)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        t.setPadding(0, pad(4), 0, pad(4))
        return t
    }

    private fun spacer(): View {
        val v = View(ctx)
        v.layoutParams = LinearLayout.LayoutParams(MATCH, pad(12))
        return v
    }

    private fun pad(v: Int): Int = (v * density).toInt()

    companion object {
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
