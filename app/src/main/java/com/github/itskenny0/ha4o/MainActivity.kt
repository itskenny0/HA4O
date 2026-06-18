package com.github.itskenny0.ha4o

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

/**
 * The whole app, basically: connect to HA, show every entity in a scrolling list with
 * its live state, tap to toggle (or view attributes). Framework widgets only, built in
 * code. State updates arrive over the WebSocket and patch the list in place.
 */
class MainActivity : Activity(), HaSocket.Listener {

    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: EntityAdapter
    private val entities = ArrayList<EntityState>()
    private var socket: HaSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        if (!prefs.isConfigured) {
            goToOnboarding()
            return
        }

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        status = TextView(this)
        val pad = dp(8)
        status.setPadding(pad, pad, pad, pad)
        status.text = "Connecting…"
        root.addView(status, wrap())

        listView = ListView(this)
        adapter = EntityAdapter(this, entities)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ -> onEntityTapped(entities[position]) }
        root.addView(listView, fill())

        setContentView(root)
        connect()
    }

    private fun connect() {
        socket?.close()
        val s = HaSocket(this)
        socket = s
        status.text = "Connecting to ${prefs.baseUrl}…"
        s.connect(prefs.baseUrl!!, prefs.token!!)
    }

    private fun onEntityTapped(entity: EntityState) {
        if (Domains.isControllable(entity.entityId)) {
            socket?.callService(entity.entityId)
        } else {
            AlertDialog.Builder(this)
                .setTitle(entity.displayName)
                .setMessage(
                    "State: ${entity.state}\n\n" +
                        (if (entity.attributesText.isEmpty()) "(no attributes)" else entity.attributesText),
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // --- HaSocket.Listener (all on the main thread) ---

    override fun onConnected() {
        status.text = "Connected to ${prefs.baseUrl}"
    }

    override fun onStates(states: List<EntityState>) {
        entities.clear()
        entities.addAll(states)
        sortEntities()
        adapter.notifyDataSetChanged()
        status.text = "${entities.size} entities"
    }

    override fun onStateUpdate(entity: EntityState) {
        val idx = entities.indexOfFirst { it.entityId == entity.entityId }
        if (idx >= 0) {
            entities[idx] = entity
        } else {
            entities.add(entity)
            sortEntities()
        }
        adapter.notifyDataSetChanged()
    }

    override fun onAuthFailed() {
        toast("Invalid token")
        prefs.token = null
        goToOnboarding()
    }

    override fun onDisconnected(reason: String) {
        status.text = "Disconnected: $reason — menu ▸ Reconnect"
    }

    // --- menu ---

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_RECONNECT, 0, "Reconnect")
        menu.add(0, MENU_SIGN_OUT, 1, "Sign out")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_RECONNECT -> { connect(); true }
            MENU_SIGN_OUT -> { prefs.clear(); goToOnboarding(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        socket?.close()
        socket = null
        super.onDestroy()
    }

    private fun sortEntities() =
        entities.sortBy { it.displayName.lowercase() }

    private fun goToOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    private fun wrap() =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun fill() =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    companion object {
        private const val MENU_RECONNECT = 1
        private const val MENU_SIGN_OUT = 2
    }
}
