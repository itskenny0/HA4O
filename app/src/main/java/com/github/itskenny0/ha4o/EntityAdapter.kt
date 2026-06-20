package com.github.itskenny0.ha4o

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Dark "All entities" finder row, built in code: a left colour bar (hue = domain, bright
 * when on / dim when off), a two-line name + state label, and a trailing star for
 * favourites. Reads the same list instance the Activity mutates; call notifyDataSetChanged
 * after edits.
 */
class EntityAdapter(
    private val context: Context,
    private val items: List<EntityState>,
    /** Live reference to the favourite ids; favourited rows get a trailing star. */
    private val favourites: Set<String>,
) : BaseAdapter() {

    private val density = context.resources.displayMetrics.density

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): EntityState = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val entity = items[position]
        val d = Controls.describe(entity)

        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setBackgroundColor(BG)
        row.setPadding(0, dp(2), dp(8), dp(2))

        val bar = View(context)
        bar.setBackgroundColor(Palette.forDomain(d.domain, d.isOn).top)
        row.addView(bar, LinearLayout.LayoutParams(dp(6), dp(40)))

        val labels = LinearLayout(context)
        labels.orientation = LinearLayout.VERTICAL
        labels.setPadding(dp(10), dp(6), dp(10), dp(6))

        val name = TextView(context)
        name.text = entity.displayName
        name.setTextColor(Color.WHITE)
        labels.addView(name)

        val state = TextView(context)
        state.text = d.displayState
        state.setTextColor(0xFF9E9E9E.toInt())
        state.textSize = 12f
        labels.addView(state)
        row.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        if (favourites.contains(entity.entityId)) {
            val star = TextView(context)
            star.text = "★"
            star.setTextColor(0xFFFF6F00.toInt())
            row.addView(star, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return row
    }

    private fun dp(v: Int): Int = (v * density).toInt()

    companion object {
        private val BG = 0xFF121212.toInt()
    }
}
