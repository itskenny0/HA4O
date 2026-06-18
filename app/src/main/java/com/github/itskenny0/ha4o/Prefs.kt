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

    fun clear() {
        sp.edit().clear().commit()
    }

    companion object {
        private const val KEY_URL = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_FAVS = "favourites"
        private const val KEY_FAV_ONLY = "fav_only"
    }
}
