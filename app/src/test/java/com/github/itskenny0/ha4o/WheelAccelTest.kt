package com.github.itskenny0.ha4o

import org.junit.Assert.assertEquals
import org.junit.Test

class WheelAccelTest {

    @Test fun disabled_returns_the_base_step() {
        assertEquals(5, WheelAccel.step(5, 30, enabled = false))
    }

    @Test fun slow_presses_use_the_base_step() {
        assertEquals(5, WheelAccel.step(5, 1000, enabled = true))
    }

    @Test fun faster_presses_accelerate() {
        assertEquals(10, WheelAccel.step(5, 120, enabled = true)) // medium
        assertEquals(20, WheelAccel.step(5, 40, enabled = true))  // fast
    }

    @Test fun the_step_is_capped() {
        assertEquals(50, WheelAccel.step(20, 40, enabled = true)) // 20*4 = 80 -> capped
    }
}
