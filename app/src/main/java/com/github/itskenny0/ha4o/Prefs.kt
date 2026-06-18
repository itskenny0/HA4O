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

    fun clear() {
        sp.edit().clear().commit()
    }

    companion object {
        private const val KEY_URL = "base_url"
        private const val KEY_TOKEN = "token"
    }
}
