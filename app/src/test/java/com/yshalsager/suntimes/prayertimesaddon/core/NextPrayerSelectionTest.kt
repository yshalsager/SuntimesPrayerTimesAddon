package com.yshalsager.suntimes.prayertimesaddon.core

import org.junit.Assert.assertEquals
import org.junit.Test

class NextPrayerSelectionTest {
    @Test
    fun select_next_and_prev_obligatory_prayer_before_fajr_uses_previous_day_isha() {
        val result =
            select_next_and_prev_obligatory_prayer(
                now = 500L,
                input =
                    ObligatoryPrayerWindowInput(
                        fajr = 600L,
                        dhuhr = 1200L,
                        asr = 1500L,
                        maghrib = 1800L,
                        isha = 2000L,
                        prev_day_isha = -100L
                    )
            )

        assertEquals(AddonEvent.prayer_fajr to 600L, result.next)
        assertEquals(-100L, result.prev_time)
    }

    @Test
    fun select_next_and_prev_obligatory_prayer_between_asr_and_maghrib() {
        val result =
            select_next_and_prev_obligatory_prayer(
                now = 1600L,
                input =
                    ObligatoryPrayerWindowInput(
                        fajr = 600L,
                        dhuhr = 1200L,
                        asr = 1500L,
                        maghrib = 1800L,
                        isha = 2000L,
                        prev_day_isha = -100L
                    )
            )

        assertEquals(AddonEvent.prayer_maghrib to 1800L, result.next)
        assertEquals(1500L, result.prev_time)
    }

    @Test
    fun select_next_and_prev_obligatory_prayer_after_isha_uses_tomorrow_fajr_when_available() {
        val result =
            select_next_and_prev_obligatory_prayer(
                now = 2100L,
                input =
                    ObligatoryPrayerWindowInput(
                        fajr = 600L,
                        dhuhr = 1200L,
                        asr = 1500L,
                        maghrib = 1800L,
                        isha = 2000L,
                        prev_day_isha = -100L,
                        next_day_fajr = 2600L
                    )
            )

        assertEquals(AddonEvent.prayer_fajr to 2600L, result.next)
        assertEquals(2000L, result.prev_time)
    }

    @Test
    fun select_next_and_prev_obligatory_prayer_after_isha_without_tomorrow_fajr_has_no_next() {
        val result =
            select_next_and_prev_obligatory_prayer(
                now = 2100L,
                input =
                    ObligatoryPrayerWindowInput(
                        fajr = 600L,
                        dhuhr = 1200L,
                        asr = 1500L,
                        maghrib = 1800L,
                        isha = 2000L,
                        prev_day_isha = -100L
                    )
            )

        assertEquals(null, result.next)
        assertEquals(2000L, result.prev_time)
    }
}
