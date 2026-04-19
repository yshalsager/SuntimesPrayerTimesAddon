package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import java.util.TimeZone

data class HomeSelectedLocation(
    val key: String,
    val label: String,
    val timezone: TimeZone,
    val timezone_id: String,
    val saved_location: SavedLocation?,
    val method_config_override: MethodConfig?,
    val addon_runtime_profile_override: AddonRuntimeProfile?
)

fun home_location_key(saved_id: String?): String =
    if (saved_id.isNullOrBlank()) SavedLocations.home_source_host else "${SavedLocations.home_source_saved}:$saved_id"

fun saved_location_id_from_key(key: String): String? {
    val prefix = "${SavedLocations.home_source_saved}:"
    if (!key.startsWith(prefix)) return null
    return key.removePrefix(prefix).takeIf { it.isNotBlank() }
}

fun resolve_selected_home_location(
    context: Context,
    host_label: String,
    host_timezone_id: String,
    saved_locations: List<SavedLocation> = SavedLocations.load(context)
): HomeSelectedLocation {
    val source_pref = Prefs.get_home_location_source(context)
    val id_pref = Prefs.get_home_location_id(context)
    val requested_saved_id = if (source_pref == SavedLocations.home_source_saved) id_pref else null
    val location_context = resolve_location_query_context(context, requested_saved_id, null, null, null, saved_locations)
    val saved = location_context.saved_location

    if (source_pref == SavedLocations.home_source_saved && saved == null) {
        Prefs.set_home_location_source(context, SavedLocations.home_source_host)
        Prefs.set_home_location_id(context, "")
    }

    if (saved != null) {
        val timezone_id = location_context.timezone_override?.id ?: host_timezone_id
        return HomeSelectedLocation(
            key = home_location_key(saved.id),
            label = saved.display_label(),
            timezone = TimeZone.getTimeZone(timezone_id),
            timezone_id = timezone_id,
            saved_location = saved,
            method_config_override = location_context.method_config_override,
            addon_runtime_profile_override = location_context.addon_runtime_profile_override
        )
    }

    return HomeSelectedLocation(
        key = SavedLocations.home_source_host,
        label = host_label,
        timezone = TimeZone.getTimeZone(host_timezone_id),
        timezone_id = host_timezone_id,
        saved_location = null,
        method_config_override = null,
        addon_runtime_profile_override = null
    )
}

fun select_home_location_by_key(
    context: Context,
    key: String,
    saved_locations: List<SavedLocation> = SavedLocations.load(context)
): Boolean {
    val (source, id) =
        if (key == SavedLocations.home_source_host) {
            SavedLocations.home_source_host to ""
        } else {
            val saved_id = saved_location_id_from_key(key) ?: return false
            if (saved_locations.none { it.id == saved_id }) return false
            SavedLocations.home_source_saved to saved_id
        }

    Prefs.set_home_location_source(context, source)
    Prefs.set_home_location_id(context, id)
    return true
}
