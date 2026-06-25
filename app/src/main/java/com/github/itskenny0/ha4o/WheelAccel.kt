package com.github.itskenny0.ha4o

/**
 * Wheel/D-pad acceleration: the faster successive presses arrive, the bigger each step,
 * so a long spin covers a slider quickly while a single press stays fine-grained. Pure,
 * so it unit-tests on the JVM. Mirrors R1HA's wheel acceleration (a fixed medium curve).
 */
object WheelAccel {

    private const val MAX_STEP = 50

    /** Effective step for a press [msSinceLast] after the previous one. */
    fun step(base: Int, msSinceLast: Long, enabled: Boolean): Int {
        if (!enabled) return base
        val scaled = when {
            msSinceLast < 80 -> base * 4
            msSinceLast < 160 -> base * 2
            else -> base
        }
        return scaled.coerceAtMost(MAX_STEP)
    }
}
