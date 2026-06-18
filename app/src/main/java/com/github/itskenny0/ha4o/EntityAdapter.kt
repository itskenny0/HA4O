package com.github.itskenny0.ha4o

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

/**
 * Plain BaseAdapter over the entity list, reusing the framework two-line row
 * (android.R.layout.simple_list_item_2) so HA4O ships no row layout of its own. The
 * adapter reads the same list instance the Activity mutates; call notifyDataSetChanged
 * after edits.
 */
class EntityAdapter(
    context: Context,
    private val items: List<EntityState>,
) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): EntityState = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val row = convertView
            ?: inflater.inflate(android.R.layout.simple_list_item_2, parent, false)
        val entity = items[position]
        row.findViewById<TextView>(android.R.id.text1).text = entity.displayName
        row.findViewById<TextView>(android.R.id.text2).text = entity.state
        return row
    }
}
