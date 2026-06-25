package com.github.itskenny0.ha4o

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

/**
 * The card-stack home screen: a top bar, a horizontal domain tab strip, and a vertically
 * scrolling list of gradient cards for the selected domain. Swipe horizontally (or tap a
 * tab) to switch domains; the hardware volume keys and D-pad nudge the top card's primary
 * slider. Long-press a card to favourite it. A menu opens the flat "All entities" finder.
 * Framework widgets only, built in code; live state_changed updates patch the cards.
 */
class MainActivity : Activity(), HaSocket.Listener, TabStrip.Listener, CardAdapter.Listener {

    private lateinit var prefs: Prefs
    private lateinit var title: TextView
    private lateinit var position: TextView
    private lateinit var tabStrip: TabStrip
    private lateinit var content: FrameLayout
    private lateinit var cardList: ListView
    private lateinit var emptyState: TextView
    private lateinit var finderPanel: LinearLayout
    private lateinit var finderSearch: EditText
    private lateinit var finderList: ListView
    private lateinit var cardAdapter: CardAdapter
    private lateinit var finderAdapter: EntityAdapter
    private lateinit var gestures: GestureDetector

    private val allEntities = ArrayList<EntityState>()
    private val displayed = ArrayList<EntityState>()
    private val finderItems = ArrayList<EntityState>()
    private val favourites = HashSet<String>()
    private var selectedTab = TabStrip.FAVOURITES_KEY
    private var finderMode = false
    private var moreInfoId: String? = null
    private var moreInfoView: View? = null
    private var socket: HaSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        if (!prefs.isConfigured) {
            goToOnboarding()
            return
        }
        favourites.addAll(prefs.favourites)
        setContentView(buildUi())
        connect()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(BG)

        root.addView(topBar(), wrap())

        tabStrip = TabStrip(this)
        tabStrip.listener = this
        root.addView(tabStrip, wrap())

        content = FrameLayout(this)

        cardAdapter = CardAdapter(this, displayed, favourites, this)
        cardList = ListView(this)
        // Uniform gaps via a transparent divider + list padding. ListView strips margins
        // from item views, so spacing has to live here, not on the cards.
        cardList.divider = ColorDrawable(Color.TRANSPARENT)
        cardList.dividerHeight = dp(10)
        cardList.setPadding(dp(8), dp(8), dp(8), dp(8))
        cardList.clipToPadding = false
        cardList.adapter = cardAdapter
        cardList.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, state: Int) {}
            override fun onScroll(view: AbsListView, first: Int, visible: Int, total: Int) = updatePosition()
        })
        content.addView(cardList, fill())

        emptyState = TextView(this)
        emptyState.gravity = Gravity.CENTER
        emptyState.setTextColor(0xFF9E9E9E.toInt())
        emptyState.text = "NO FAVOURITES YET\n\nbrowse domains to add some"
        emptyState.visibility = View.GONE
        content.addView(emptyState, fill())

        content.addView(buildFinderPanel(), fill())

        root.addView(content, fill())

        // Horizontal flings over the card area switch domains; vertical scrolling is left
        // to the ListView (we never consume the event here).
        gestures = GestureDetector(this, SwipeListener())
        cardList.setOnTouchListener { _, ev -> gestures.onTouchEvent(ev); false }
        return root
    }

    /** The "All entities" finder: a search box above a filtered flat list. */
    private fun buildFinderPanel(): View {
        finderSearch = EditText(this)
        finderSearch.hint = "Search entities"
        finderSearch.setSingleLine(true)
        finderSearch.setTextColor(Color.WHITE)
        finderSearch.setHintTextColor(0xFF777777.toInt())
        val p = dp(8)
        finderSearch.setPadding(p, p, p, p)
        finderSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = applyFinderFilter()
        })

        finderAdapter = EntityAdapter(this, finderItems, favourites)
        finderList = ListView(this)
        finderList.adapter = finderAdapter
        finderList.setOnItemClickListener { _, _, pos, _ -> onFinderTapped(finderItems[pos]) }
        finderList.setOnItemLongClickListener { _, _, pos, _ -> toggleFavourite(finderItems[pos]); true }

        finderPanel = LinearLayout(this)
        finderPanel.orientation = LinearLayout.VERTICAL
        finderPanel.visibility = View.GONE
        finderPanel.addView(finderSearch, wrap())
        finderPanel.addView(finderList, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return finderPanel
    }

    /** Refilter the finder list from the current search text. */
    private fun applyFinderFilter() {
        val q = finderSearch.text.toString().trim().lowercase()
        finderItems.clear()
        finderItems.addAll(
            if (q.isEmpty()) {
                allEntities
            } else {
                allEntities.filter {
                    it.displayName.lowercase().contains(q) || it.entityId.lowercase().contains(q)
                }
            },
        )
        finderAdapter.notifyDataSetChanged()
    }

    private fun topBar(): View {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setBackgroundColor(0xFF1E1E1E.toInt())
        val p = dp(8)
        bar.setPadding(p, p, p, p)

        bar.addView(barButton("☰") { showMenu() }, wrap())

        title = TextView(this)
        title.setTextColor(Color.WHITE)
        title.text = "Connecting…"
        bar.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        position = TextView(this)
        position.setTextColor(0xFF9E9E9E.toInt())
        bar.addView(position, wrap())

        bar.addView(barButton("★") { showFavouritesTab() }, wrap())
        return bar
    }

    private fun barButton(text: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = text
        b.setOnClickListener { onClick() }
        return b
    }

    private fun connect() {
        socket?.close()
        val s = HaSocket(this)
        socket = s
        title.text = "Connecting…"
        s.connect(prefs.baseUrl!!, prefs.token!!)
    }

    // --- tab + content state -------------------------------------------------

    private fun buildTabs(): List<TabStrip.Tab> {
        val counts = LinkedHashMap<String, Int>()
        for (e in allEntities) {
            val dom = Controls.domainOf(e.entityId)
            counts[dom] = (counts[dom] ?: 0) + 1
        }
        val tabs = ArrayList<TabStrip.Tab>()
        val favCount = allEntities.count { favourites.contains(it.entityId) }
        // The Favourites tab appears once there are favourites, or while it's the active
        // tab (so the ★ button can land on it and show the empty state when there are none).
        if (favourites.isNotEmpty() || selectedTab == TabStrip.FAVOURITES_KEY) {
            tabs.add(TabStrip.Tab(TabStrip.FAVOURITES_KEY, "★ FAVOURITES", favCount))
        }
        for (dom in counts.keys.sorted()) {
            tabs.add(TabStrip.Tab(dom, dom.uppercase(), counts[dom] ?: 0))
        }
        return tabs
    }

    private fun rebuild() {
        val tabs = buildTabs()
        if (tabs.none { it.key == selectedTab }) {
            selectedTab = tabs.firstOrNull()?.key ?: ""
        }
        tabStrip.setTabs(tabs, selectedTab)

        displayed.clear()
        displayed.addAll(
            if (selectedTab == TabStrip.FAVOURITES_KEY) {
                allEntities.filter { favourites.contains(it.entityId) }
            } else {
                allEntities.filter { Controls.domainOf(it.entityId) == selectedTab }
            },
        )
        cardAdapter.notifyDataSetChanged()
        if (finderMode) applyFinderFilter()
        updateContentVisibility()
        updatePosition()
    }

    private fun updateContentVisibility() {
        val showEmpty = !finderMode && selectedTab == TabStrip.FAVOURITES_KEY && displayed.isEmpty()
        finderPanel.visibility = if (finderMode) View.VISIBLE else View.GONE
        emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
        cardList.visibility = if (!finderMode && !showEmpty) View.VISIBLE else View.GONE
    }

    private fun showMenu() {
        val items = arrayOf(
            if (finderMode) "Card view" else "All entities",
            "Master off…",
            "Clear favourites",
            "Reconnect",
            "Sign out",
        )
        AlertDialog.Builder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> { finderMode = !finderMode; rebuild() }
                    1 -> showMasterOff()
                    2 -> clearFavourites()
                    3 -> connect()
                    4 -> { prefs.clear(); goToOnboarding() }
                }
            }
            .show()
    }

    private fun showMasterOff() {
        val items = arrayOf("All lights off", "All switches off", "Pause all media")
        AlertDialog.Builder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> masterAction("light", "turn_off")
                    1 -> masterAction("switch", "turn_off")
                    2 -> masterAction("media_player", "media_pause")
                }
            }
            .show()
    }

    private fun masterAction(domain: String, service: String) {
        val ids = allEntities.map { it.entityId }.filter { Controls.domainOf(it) == domain }
        if (ids.isEmpty()) {
            toast("No $domain entities")
            return
        }
        for (id in ids) socket?.callService(Controls.ServiceCall(domain, service, id))
        toast("$service → ${ids.size} $domain")
    }

    private fun clearFavourites() {
        if (favourites.isEmpty()) {
            toast("No favourites to clear")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Clear favourites?")
            .setMessage("Remove all ${favourites.size} favourites?")
            .setPositiveButton("Clear") { _, _ ->
                favourites.clear()
                prefs.favourites = favourites
                rebuild()
                toast("Favourites cleared")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePosition() {
        position.text = when {
            finderMode -> "${allEntities.size}"
            displayed.isEmpty() -> ""
            else -> "${cardList.firstVisiblePosition + 1}/${displayed.size}"
        }
    }

    private fun showFavouritesTab() {
        finderMode = false
        selectedTab = TabStrip.FAVOURITES_KEY
        rebuild()
    }

    private fun stepTab(direction: Int) {
        val tabs = buildTabs()
        val idx = tabs.indexOfFirst { it.key == selectedTab }
        if (idx < 0) return
        val next = (idx + direction).coerceIn(0, tabs.size - 1)
        if (next != idx) {
            selectedTab = tabs[next].key
            rebuild()
        }
    }

    // --- TabStrip.Listener ---

    override fun onTabSelected(key: String) {
        finderMode = false
        selectedTab = key
        rebuild()
    }

    // --- CardAdapter.Listener ---

    override fun onServiceCall(call: Controls.ServiceCall) {
        socket?.callService(call)
    }

    override fun onCardLongPress(entity: EntityState) = toggleFavourite(entity)

    override fun onCardTap(entity: EntityState) = showMoreInfo(entity)

    // --- more-info overlay ---

    private fun showMoreInfo(entity: EntityState) {
        closeMoreInfo()
        moreInfoId = entity.entityId
        val view = MoreInfoView(this, entity, { socket?.callService(it) }, { closeMoreInfo() }).root
        moreInfoView = view
        content.addView(view, fill())
    }

    private fun closeMoreInfo() {
        moreInfoView?.let { content.removeView(it) }
        moreInfoView = null
        moreInfoId = null
    }

    /** Rebuild the open more-info overlay from the latest state, preserving scroll-free. */
    private fun refreshMoreInfo() {
        val id = moreInfoId ?: return
        val entity = allEntities.firstOrNull { it.entityId == id } ?: return
        moreInfoView?.let { content.removeView(it) }
        val view = MoreInfoView(this, entity, { socket?.callService(it) }, { closeMoreInfo() }).root
        moreInfoView = view
        content.addView(view, fill())
    }

    override fun onBackPressed() {
        if (moreInfoId != null) closeMoreInfo() else super.onBackPressed()
    }

    // --- finder (flat "All entities") ---

    private fun onFinderTapped(entity: EntityState) {
        val id = entity.entityId
        when (Controls.describe(entity).kind) {
            Controls.Kind.Toggle -> socket?.callService(Controls.toggle(id))
            Controls.Kind.FireOnce -> socket?.callService(Controls.fireOnce(id))
            Controls.Kind.ReadOnly -> showAttributes(entity)
            else -> { // scalar: jump to its domain card
                finderMode = false
                selectedTab = Controls.domainOf(id)
                rebuild()
            }
        }
    }

    private fun showAttributes(entity: EntityState) {
        AlertDialog.Builder(this)
            .setTitle(entity.displayName)
            .setMessage(
                "State: ${entity.state}\n\n" +
                    (if (entity.attributesText.isEmpty()) "(no attributes)" else entity.attributesText),
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleFavourite(entity: EntityState) {
        val id = entity.entityId
        val nowFav = if (favourites.contains(id)) {
            favourites.remove(id); false
        } else {
            favourites.add(id); true
        }
        prefs.favourites = favourites
        rebuild()
        toast(if (nowFav) "★ Favourited ${entity.displayName}" else "Removed ${entity.displayName}")
    }

    // --- hardware wheel: volume keys + D-pad nudge the top card's primary slider ---

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val delta = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_UP -> STEP
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> -STEP
            else -> return super.onKeyDown(keyCode, event)
        }
        if (finderMode) return super.onKeyDown(keyCode, event)
        val entity = displayed.getOrNull(cardList.firstVisiblePosition)
            ?: return super.onKeyDown(keyCode, event)
        val current = Controls.describe(entity).primary ?: return super.onKeyDown(keyCode, event)
        val call = Controls.setPrimary(entity, (current + delta).coerceIn(0, 100))
            ?: return super.onKeyDown(keyCode, event)
        socket?.callService(call)
        return true
    }

    // --- HaSocket.Listener (all on the main thread) ---

    override fun onConnected() {
        title.text = "HA4O"
    }

    override fun onStates(states: List<EntityState>) {
        allEntities.clear()
        allEntities.addAll(states)
        sortEntities()
        rebuild()
        refreshMoreInfo()
    }

    override fun onStateUpdate(entity: EntityState) {
        val idx = allEntities.indexOfFirst { it.entityId == entity.entityId }
        if (idx >= 0) {
            allEntities[idx] = entity
        } else {
            allEntities.add(entity)
            sortEntities()
        }
        rebuild()
        // Only refresh the open detail when its own entity changed, so unrelated traffic
        // doesn't rebuild (and reset the scroll of) the more-info overlay.
        if (entity.entityId == moreInfoId) refreshMoreInfo()
    }

    override fun onAuthFailed() {
        toast("Invalid token")
        prefs.token = null
        goToOnboarding()
    }

    override fun onDisconnected(reason: String) {
        title.text = "Disconnected: $reason"
    }

    // --- menu ---

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_FINDER, 0, "All entities")
        menu.add(0, MENU_MASTER_OFF, 1, "Master off…")
        menu.add(0, MENU_CLEAR_FAVS, 2, "Clear favourites")
        menu.add(0, MENU_RECONNECT, 3, "Reconnect")
        menu.add(0, MENU_SIGN_OUT, 4, "Sign out")
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(MENU_FINDER).title = if (finderMode) "Card view" else "All entities"
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_FINDER -> { finderMode = !finderMode; rebuild(); true }
            MENU_MASTER_OFF -> { showMasterOff(); true }
            MENU_CLEAR_FAVS -> { clearFavourites(); true }
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

    private fun sortEntities() = allEntities.sortBy { it.displayName.lowercase() }

    private fun goToOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    private fun wrap() =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun fill() =
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    /** Detects a horizontal fling and steps to the previous/next domain tab. */
    private inner class SwipeListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (e1 == null) return false
            val dx = e2.x - e1.x
            val dy = e2.y - e1.y
            if (abs(dx) < abs(dy) || abs(dx) < dp(64) || abs(vx) < dp(200)) return false
            stepTab(if (dx > 0) -1 else 1)
            return true
        }
    }

    companion object {
        private const val MENU_FINDER = 1
        private const val MENU_MASTER_OFF = 2
        private const val MENU_CLEAR_FAVS = 3
        private const val MENU_RECONNECT = 4
        private const val MENU_SIGN_OUT = 5
        private const val STEP = 5
        private val BG = 0xFF121212.toInt()
    }
}
