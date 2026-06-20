package com.github.itskenny0.ha4o

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * First-run screen: collect the local HA URL and a long-lived access token. Built in
 * code (no layout XML) with framework widgets only. OAuth isn't offered: Gingerbread's
 * WebView can't render HA's modern login page, and a long-lived token is the supported
 * headless path anyway.
 */
class OnboardingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(BG)
        val pad = dp(16)
        root.setPadding(pad, pad, pad, pad)

        val title = TextView(this)
        title.text = "HA4O"
        title.textSize = 28f
        title.setTextColor(ACCENT)
        root.addView(title)

        val hint = TextView(this)
        hint.text = "Home Assistant for old phones. Local network only (plain http://); " +
            "paste a long-lived access token from your HA profile."
        hint.setTextColor(0xFF9E9E9E.toInt())
        root.addView(hint)

        val urlField = EditText(this)
        urlField.hint = "http://192.168.1.10:8123"
        urlField.setTextColor(0xFFFFFFFF.toInt())
        urlField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        urlField.setText(prefs.baseUrl ?: "http://")
        root.addView(urlField, fullWidth())

        val tokenField = EditText(this)
        tokenField.hint = "Long-lived access token"
        tokenField.setTextColor(0xFFFFFFFF.toInt())
        tokenField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        prefs.token?.let { tokenField.setText(it) }
        root.addView(tokenField, fullWidth())

        val connect = Button(this)
        connect.text = "Connect"
        connect.gravity = Gravity.CENTER
        root.addView(connect, fullWidth())

        connect.setOnClickListener {
            val url = urlField.text.toString().trim()
            val token = tokenField.text.toString().trim()
            if (url.isEmpty() || url == "http://" || token.isEmpty()) {
                toast("Enter both a URL and a token")
                return@setOnClickListener
            }
            if (!Urls.isPlainHttp(url)) {
                // wss/https can't be negotiated on 2.3; warn but let them try.
                toast("Warning: only http:// works on Android 2.3")
            }
            prefs.baseUrl = url
            prefs.token = token
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setContentView(root)
    }

    private fun fullWidth(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    companion object {
        private val BG = 0xFF121212.toInt()
        private val ACCENT = 0xFFFF6F00.toInt()
    }
}
