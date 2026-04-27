package com.yshalsager.suntimes.prayertimesaddon.core

data class ObligatoryPrayerWindowInput(
    val fajr: Long?,
    val dhuhr: Long?,
    val asr: Long?,
    val maghrib: Long?,
    val isha: Long?,
    val prev_day_isha: Long? = null,
    val next_day_fajr: Long? = null
)

data class ObligatoryPrayerSelection(
    val next: Pair<AddonEvent, Long>?,
    val prev_time: Long?
)

fun select_next_and_prev_obligatory_prayer(
    now: Long,
    input: ObligatoryPrayerWindowInput
): ObligatoryPrayerSelection {
    val today =
        listOf(
            AddonEvent.prayer_fajr to input.fajr,
            AddonEvent.prayer_dhuhr to input.dhuhr,
            AddonEvent.prayer_asr to input.asr,
            AddonEvent.prayer_maghrib to input.maghrib,
            AddonEvent.prayer_isha to input.isha
        ).mapNotNull { (event, t) -> t?.let { event to it } }
            .sortedBy { it.second }

    val next_today = today.firstOrNull { it.second >= now }
    val prev_today = today.lastOrNull { it.second < now }?.second

    if (next_today != null) {
        return ObligatoryPrayerSelection(next = next_today, prev_time = prev_today ?: input.prev_day_isha)
    }

    val next_fallback = input.next_day_fajr?.let { AddonEvent.prayer_fajr to it }
    val prev_fallback = input.isha ?: prev_today ?: input.prev_day_isha
    return ObligatoryPrayerSelection(next = next_fallback, prev_time = prev_fallback)
}
