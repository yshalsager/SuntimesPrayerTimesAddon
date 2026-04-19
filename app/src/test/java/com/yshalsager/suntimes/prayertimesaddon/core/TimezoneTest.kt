package com.yshalsager.suntimes.prayertimesaddon.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimezoneTest {
    @Test
    fun timezone_offset_mismatch_hours_returns_null_for_invalid_inputs() {
        assertNull(timezone_offset_mismatch_hours(null, "UTC"))
        assertNull(timezone_offset_mismatch_hours("31.0", "Not/A_Timezone"))
    }

    @Test
    fun timezone_likely_mismatch_detects_large_offset_gap() {
        assertTrue(timezone_likely_mismatch("30.0", "America/Los_Angeles"))
        assertFalse(timezone_likely_mismatch("31.0", "Africa/Cairo"))
    }

    @Test
    fun add_days_handles_spring_forward_boundary() {
        val tz = TimeZone.getTimeZone("America/New_York")
        val day_start = local_day_start(2026, Calendar.MARCH, 8, tz)
        val next_day_start = add_days(day_start, 1, tz)

        assertEquals(23L * 60L * 60L * 1000L, next_day_start - day_start)
        assertEquals(9, day_of_month(next_day_start, tz))
    }

    @Test
    fun add_days_handles_fall_back_boundary() {
        val tz = TimeZone.getTimeZone("America/New_York")
        val day_start = local_day_start(2026, Calendar.NOVEMBER, 1, tz)
        val next_day_start = add_days(day_start, 1, tz)

        assertEquals(25L * 60L * 60L * 1000L, next_day_start - day_start)
        assertEquals(2, day_of_month(next_day_start, tz))
    }

    private fun local_day_start(year: Int, month: Int, day: Int, tz: TimeZone): Long =
        Calendar.getInstance(tz).run {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

    private fun day_of_month(millis: Long, tz: TimeZone): Int =
        Calendar.getInstance(tz).run {
            timeInMillis = millis
            get(Calendar.DAY_OF_MONTH)
        }
}
