package com.github.itskenny0.ha4o

import android.content.Context

/**
 * Tiny SharedPreferences wrapper. SharedPreferences is API 1, so nothing here needs a
 * newer Android. Stores the local HA base URL (http://host:8123) and a long-lived
 * access token; OAuth isn't an option on Gingerbread's ancient WebView.
 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("ha4o", Context.MODE_PRIVATE)

    var baseUrl: String?
        get() = sp.getString(KEY_URL, null)
        set(value) {
            sp.edit().putString(KEY_URL, value).commit()
        }

    var token: String?
        get() = sp.getString(KEY_TOKEN, null)
        set(value) {
            sp.edit().putString(KEY_TOKEN, value).commit()
        }

    val isConfigured: Boolean
        get() = !baseUrl.isNullOrEmpty() && !token.isNullOrEmpty()

    /** Favourite entity ids. Stored as a newline-joined string (getStringSet is API 11). */
    var favourites: Set<String>
        get() = Favourites.decode(sp.getString(KEY_FAVS, null))
        set(value) {
            sp.edit().putString(KEY_FAVS, Favourites.encode(value)).commit()
        }

    /** Whether the list is currently filtered to favourites only. */
    var showFavouritesOnly: Boolean
        get() = sp.getBoolean(KEY_FAV_ONLY, false)
        set(value) {
            sp.edit().putBoolean(KEY_FAV_ONLY, value).commit()
        }

    /** Card layout: "list" (compact), "expanded" (full controls inline), or "peek" (deck). */
    var cardLayout: String
        get() = sp.getString(KEY_LAYOUT, "expanded") ?: "expanded"
        set(value) {
            sp.edit().putString(KEY_LAYOUT, value).commit()
        }

    /** How many percent the hardware wheel / D-pad nudges a slider per press. */
    var wheelStep: Int
        get() = sp.getInt(KEY_WHEEL_STEP, 5)
        set(value) {
            sp.edit().putInt(KEY_WHEEL_STEP, value).commit()
        }

    /** Whether rapid wheel presses accelerate the step. */
    var wheelAccel: Boolean
        get() = sp.getBoolean(KEY_WHEEL_ACCEL, true)
        set(value) {
            sp.edit().putBoolean(KEY_WHEEL_ACCEL, value).commit()
        }

    /** Accent role: "warm" (default), "cool", "green", "neutral". */
    var accent: String
        get() = sp.getString(KEY_ACCENT, "warm") ?: "warm"
        set(value) {
            sp.edit().putString(KEY_ACCENT, value).commit()
        }

    /** Text size: "compact", "default", "large", "xlarge". */
    var textSize: String
        get() = sp.getString(KEY_TEXT_SIZE, "default") ?: "default"
        set(value) {
            sp.edit().putString(KEY_TEXT_SIZE, value).commit()
        }

    /** Card density: "comfortable" (default) or "compact". */
    var density: String
        get() = sp.getString(KEY_DENSITY, "comfortable") ?: "comfortable"
        set(value) {
            sp.edit().putString(KEY_DENSITY, value).commit()
        }

    /** Card palette set: "vivid" (default), "pastel", "neon". */
    var paletteSet: String
        get() = sp.getString(KEY_PALETTE, "vivid") ?: "vivid"
        set(value) {
            sp.edit().putString(KEY_PALETTE, value).commit()
        }

    /** Per-entity display overrides (name / glyph / colour). */
    var customizations: Map<String, Customizations.Custom>
        get() = Customizations.decode(sp.getString(KEY_CUSTOM, null))
        set(value) {
            sp.edit().putString(KEY_CUSTOM, Customizations.encode(value)).commit()
        }

    fun clear() {
        sp.edit().clear().commit()
    }

    companion object {
        private const val KEY_URL = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_FAVS = "favourites"
        private const val KEY_FAV_ONLY = "fav_only"
        private const val KEY_LAYOUT = "card_layout"
        private const val KEY_WHEEL_STEP = "wheel_step"
        private const val KEY_WHEEL_ACCEL = "wheel_accel"
        private const val KEY_ACCENT = "accent"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_DENSITY = "density"
        private const val KEY_PALETTE = "palette_set"
        private const val KEY_CUSTOM = "customizations"
    }
}
