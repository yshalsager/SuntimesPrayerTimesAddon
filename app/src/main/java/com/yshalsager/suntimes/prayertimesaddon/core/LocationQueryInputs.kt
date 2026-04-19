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
    return build_location_query_inputs(
        alarm_now = alarm_now,
        saved_location = saved_location,
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
    return LocationQueryInputs(
        selection = selection_pair?.first,
        selection_args = selection_pair?.second,
        timezone_override = timezone_override,
        latitude_override = latitude_override,
        method_config_override = method_config_override,
        addon_runtime_profile_override = addon_runtime_profile_override
    )
}
