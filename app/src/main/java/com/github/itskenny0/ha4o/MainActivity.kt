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
 * The whole app, basically: connect to HA, show entities in a scrolling list with their
 * live state, tap to toggle (or view attributes), long-press to favourite. A menu toggle
 * filters the list to favourites only. Framework widgets only, built in code; state
 * updates arrive over the WebSocket and patch the list in place.
 */
class MainActivity : Activity(), HaSocket.Listener {

    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: EntityAdapter

    private val allEntities = ArrayList<EntityState>()
    private val displayed = ArrayList<EntityState>()
    private val favourites = HashSet<String>()
    private var showFavouritesOnly = false
    private var socket: HaSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        if (!prefs.isConfigured) {
            goToOnboarding()
            return
        }
        favourites.addAll(prefs.favourites)
        showFavouritesOnly = prefs.showFavouritesOnly

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        status = TextView(this)
        val pad = dp(8)
        status.setPadding(pad, pad, pad, pad)
        status.text = "Connecting…"
        root.addView(status, wrap())

        listView = ListView(this)
        adapter = EntityAdapter(this, displayed, favourites)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ -> onEntityTapped(displayed[position]) }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            toggleFavourite(displayed[position]); true
        }
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

    private fun toggleFavourite(entity: EntityState) {
        val id = entity.entityId
        val nowFav = if (favourites.contains(id)) {
            favourites.remove(id); false
        } else {
            favourites.add(id); true
        }
        prefs.favourites = favourites
        rebuildDisplayed()
        toast(if (nowFav) "★ Favourited ${entity.displayName}" else "Removed ${entity.displayName}")
    }

    /** Recompute the visible list from [allEntities] given the current filter. */
    private fun rebuildDisplayed() {
        displayed.clear()
        if (showFavouritesOnly) {
            displayed.addAll(allEntities.filter { favourites.contains(it.entityId) })
        } else {
            displayed.addAll(allEntities)
        }
        adapter.notifyDataSetChanged()
        updateStatus()
    }

    private fun updateStatus() {
        status.text = when {
            showFavouritesOnly && displayed.isEmpty() ->
                "No favourites yet — long-press an entity to add one"
            showFavouritesOnly -> "${displayed.size} favourites"
            else -> "${displayed.size} entities — long-press to favourite"
        }
    }

    // --- HaSocket.Listener (all on the main thread) ---

    override fun onConnected() {
        status.text = "Connected to ${prefs.baseUrl}"
    }

    override fun onStates(states: List<EntityState>) {
        allEntities.clear()
        allEntities.addAll(states)
        sortEntities()
        rebuildDisplayed()
    }

    override fun onStateUpdate(entity: EntityState) {
        val idx = allEntities.indexOfFirst { it.entityId == entity.entityId }
        if (idx >= 0) {
            allEntities[idx] = entity
        } else {
            allEntities.add(entity)
            sortEntities()
        }
        rebuildDisplayed()
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
        menu.add(0, MENU_FAV_TOGGLE, 0, "Show favourites only")
        menu.add(0, MENU_RECONNECT, 1, "Reconnect")
        menu.add(0, MENU_SIGN_OUT, 2, "Sign out")
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // invalidateOptionsMenu is API 11; onPrepareOptionsMenu (API 1) relabels instead.
        menu.findItem(MENU_FAV_TOGGLE).title =
            if (showFavouritesOnly) "Show all entities" else "Show favourites only"
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_FAV_TOGGLE -> {
                showFavouritesOnly = !showFavouritesOnly
                prefs.showFavouritesOnly = showFavouritesOnly
                rebuildDisplayed()
                true
            }
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
        allEntities.sortBy { it.displayName.lowercase() }

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
        private const val MENU_FAV_TOGGLE = 1
        private const val MENU_RECONNECT = 2
        private const val MENU_SIGN_OUT = 3
    }
}
