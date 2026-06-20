package com.github.itskenny0.ha4o

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Horizontal strip of domain tabs with counts, e.g. "★ FAVOURITES · 3", "LIGHT · 6". The
 * active tab is highlighted in accent orange. Built from framework views only; the host
 * sets the tab list and is told when a tab is picked.
 */
class TabStrip(context: Context) : HorizontalScrollView(context) {

    interface Listener {
        fun onTabSelected(key: String)
    }

    /** A tab's stable key ("favourites" or a domain) and its display label. */
    data class Tab(val key: String, val label: String, val count: Int)

    var listener: Listener? = null

    private val row = LinearLayout(context)
    private val density = context.resources.displayMetrics.density

    init {
        isHorizontalScrollBarEnabled = false
        row.orientation = LinearLayout.HORIZONTAL
        addView(row, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setTabs(tabs: List<Tab>, selectedKey: String) {
        row.removeAllViews()
        for (tab in tabs) {
            row.addView(tabView(tab, tab.key == selectedKey))
        }
    }

    private fun tabView(tab: Tab, selected: Boolean): TextView {
        val view = TextView(context)
        view.text = "${tab.label} · ${tab.count}"
        view.gravity = Gravity.CENTER
        view.setPadding(pad(14), pad(12), pad(14), pad(12))
        view.setTextColor(if (selected) Color.WHITE else 0xFF9E9E9E.toInt())
        view.setBackgroundColor(if (selected) ACCENT else Color.TRANSPARENT)
        view.setOnClickListener { listener?.onTabSelected(tab.key) }
        return view
    }

    private fun pad(v: Int): Int = (v * density).toInt()

    companion object {
        const val FAVOURITES_KEY = "favourites"
        private val ACCENT = 0xFFFF6F00.toInt()
    }
}
