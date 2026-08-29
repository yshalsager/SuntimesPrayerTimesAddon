package com.yshalsager.suntimes.prayertimesaddon.core

import java.util.TimeZone

data class LocationQueryInputs(
    val selection: String?,
    val selection_args: Array<String>?,
    val timezone_override: TimeZone?,
    val latitude_override: Double?,
    val method_config_override: MethodConfig?,
    val addon_runtime_profile_override: AddonRuntimeProfile?
)

fun HomeSelectedLocation.query_inputs(alarm_now: Long): LocationQueryInputs {
    return build_location_query_inputs(
        alarm_now = alarm_now,
        saved_location = saved_location,
        timezone_override = timezone,
        latitude_override = saved_location?.latitude?.toDoubleOrNull(),
        method_config_override = method_config_override,
        addon_runtime_profile_override = addon_runtime_profile_override
    )
}

fun LocationQueryContext.query_inputs(alarm_now: Long): LocationQueryInputs {
    val base = (host_selection ?: parse_host_selection(null, null)).with_values(
        mapOf(
            AlarmEventContract.extra_alarm_now to alarm_now.toString(),
            AlarmEventContract.extra_alarm_offset to "0",
            AlarmEventContract.extra_alarm_repeat to "false",
            AlarmEventContract.extra_alarm_repeat_days to "[]"
        )
    )
    val selection_pair = selection_for_alarm_now(alarm_now, base.selection, base.selection_args)
    return LocationQueryInputs(
        selection = selection_pair.first,
        selection_args = selection_pair.second,
        timezone_override = timezone_override,
        latitude_override = latitude_override,
        method_config_override = method_config_override,
        addon_runtime_profile_override = addon_runtime_profile_override
    )
}

private fun build_location_query_inputs(
    alarm_now: Long,
    saved_location: SavedLocation?,
    timezone_override: TimeZone?,
    latitude_override: Double?,
    method_config_override: MethodConfig?,
    addon_runtime_profile_override: AddonRuntimeProfile?
): LocationQueryInputs {
    val selection_pair = saved_location?.let { SavedLocations.build_selection(alarm_now, it) }
        ?: (event_calc_selection to event_calc_args(alarm_now))
    return LocationQueryInputs(
        selection = selection_pair.first,
        selection_args = selection_pair.second,
        timezone_override = timezone_override,
        latitude_override = latitude_override,
        method_config_override = method_config_override,
        addon_runtime_profile_override = addon_runtime_profile_override
    )
}
