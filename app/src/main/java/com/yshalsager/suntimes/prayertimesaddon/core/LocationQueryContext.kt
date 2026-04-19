package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import java.util.TimeZone

const val query_param_saved_location_id = "saved_location_id"

data class LocationQueryContext(
    val source: String,
    val requested_saved_location_id: String?,
    val saved_location: SavedLocation?,
    val latitude: String?,
    val longitude: String?,
    val altitude: String?,
    val timezone_override: TimeZone?,
    val method_config_override: MethodConfig?,
    val addon_runtime_profile_override: AddonRuntimeProfile?,
    val latitude_override: Double?
) {
    val resolved_saved_location_id: String? = saved_location?.id

    fun selection_for_alarm_now(
        alarm_now: Long,
        base_selection: String?,
        base_selection_args: Array<String>?
    ): Pair<String?, Array<String>?> {
        val saved = saved_location ?: return base_selection to base_selection_args
        val resolved = SavedLocations.build_selection(alarm_now, saved, base_selection_args)
        return resolved.first to resolved.second
    }
}

fun resolve_location_query_context(
    context: Context,
    saved_location_id: String?,
    latitude: String?,
    longitude: String?,
    altitude: String?,
    saved_locations: List<SavedLocation> = SavedLocations.load(context)
): LocationQueryContext {
    val requested_id = saved_location_id?.trim().orEmpty().ifBlank { null }
    val from_id = SavedLocations.find_by_id(requested_id, saved_locations)
    val from_coords =
        if (from_id == null) {
            SavedLocations.find_matching_location(saved_locations, latitude, longitude, altitude)
        } else {
            null
        }
    val saved = from_id ?: from_coords
    val source = if (saved != null) SavedLocations.home_source_saved else SavedLocations.home_source_host
    val timezone_override = valid_timezone_id(saved?.timezone_id)?.let(TimeZone::getTimeZone)
    val method_override = SavedLocations.method_config_for_location(context, saved)
    val addon_runtime_profile_override = SavedLocations.addon_runtime_profile_for_location(context, saved)

    return LocationQueryContext(
        source = source,
        requested_saved_location_id = requested_id,
        saved_location = saved,
        latitude = saved?.latitude ?: latitude?.trim().orEmpty().ifBlank { null },
        longitude = saved?.longitude ?: longitude?.trim().orEmpty().ifBlank { null },
        altitude = saved?.altitude ?: altitude?.trim().orEmpty().ifBlank { null },
        timezone_override = timezone_override,
        method_config_override = method_override,
        addon_runtime_profile_override = addon_runtime_profile_override,
        latitude_override = saved?.latitude?.toDoubleOrNull() ?: latitude?.trim()?.toDoubleOrNull()
    )
}
