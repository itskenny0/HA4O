package com.github.itskenny0.ha4o

/**
 * The app's accent colour, used for highlights (selected tab, favourite stars, headings,
 * the selected option in Settings). Four roles mirroring R1HA's WARM / COOL / GREEN /
 * NEUTRAL. Plain ARGB ints so it stays Android-free and unit-tests on the JVM.
 */
object Accent {

    fun color(role: String): Int = when (role) {
        "cool" -> 0xFF2196F3.toInt()
        "green" -> 0xFF4CAF50.toInt()
        "neutral" -> 0xFF9E9E9E.toInt()
        else -> 0xFFFF6F00.toInt() // warm (default)
    }
}
