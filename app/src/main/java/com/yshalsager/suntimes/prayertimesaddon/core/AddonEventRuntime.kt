package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context

private fun effective_addon_runtime_profile(context: Context, runtime_profile_override: AddonRuntimeProfile?): AddonRuntimeProfile =
    (runtime_profile_override ?: addon_runtime_profile_from_prefs(context)).normalized()

fun is_addon_event_enabled(
    context: Context,
    event: AddonEvent,
    runtime_profile_override: AddonRuntimeProfile? = null
): Boolean {
    val runtime = effective_addon_runtime_profile(context, runtime_profile_override)
    return when (event) {
        AddonEvent.prayer_fajr_extra_1 -> runtime.extra_fajr_1_enabled
        AddonEvent.prayer_isha_extra_1 -> runtime.extra_isha_1_enabled
        else -> true
    }
}

fun visible_addon_events(
    context: Context,
    runtime_profile_override: AddonRuntimeProfile? = null
): List<AddonEvent> =
    AddonEvent.entries.filter { is_addon_event_enabled(context, it, runtime_profile_override) }

fun addon_event_title(
    context: Context,
    event: AddonEvent,
    runtime_profile_override: AddonRuntimeProfile? = null
): String {
    val runtime = effective_addon_runtime_profile(context, runtime_profile_override)
    return when (event) {
        AddonEvent.prayer_fajr_extra_1 -> runtime.extra_fajr_1_label(context)
        AddonEvent.prayer_isha_extra_1 -> runtime.extra_isha_1_label(context)
        else -> context.getString(event.title_res)
    }
}
