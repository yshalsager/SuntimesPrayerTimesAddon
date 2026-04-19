package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context

fun is_addon_event_enabled(context: Context, event: AddonEvent): Boolean =
    when (event) {
        AddonEvent.prayer_fajr_extra_1 -> Prefs.get_extra_fajr_1_enabled(context)
        AddonEvent.prayer_isha_extra_1 -> Prefs.get_extra_isha_1_enabled(context)
        else -> true
    }

fun visible_addon_events(context: Context): List<AddonEvent> =
    AddonEvent.entries.filter { is_addon_event_enabled(context, it) }

fun addon_event_title(context: Context, event: AddonEvent): String =
    when (event) {
        AddonEvent.prayer_fajr_extra_1 -> Prefs.get_extra_fajr_1_label(context)
        AddonEvent.prayer_isha_extra_1 -> Prefs.get_extra_isha_1_label(context)
        else -> context.getString(event.title_res)
    }
