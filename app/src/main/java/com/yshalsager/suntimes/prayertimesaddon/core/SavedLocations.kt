package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.abs

data class SavedLocation(
    val id: String,
    val label: String,
    val latitude: String,
    val longitude: String,
    val altitude: String?,
    val timezone_id: String,
    val calc_mode: String = SavedLocations.calc_mode_inherit_global,
    val hijri_variant: String = Prefs.hijri_variant_umalqura,
    val hijri_day_offset: Int = 0,
    val method_preset: String = "egypt",
    val method_fajr_angle: Double = 19.5,
    val method_isha_mode: String = Prefs.isha_mode_angle,
    val method_isha_angle: Double = 17.5,
    val method_isha_fixed_minutes: Int = 90,
    val method_asr_factor: Int = 1,
    val method_maghrib_offset_minutes: Int = 0,
    val method_makruh_angle: Double = 5.0,
    val method_makruh_sunrise_minutes: Int = 15,
    val method_zawal_minutes: Int = 10,
    val extra_fajr_1_enabled: Boolean = false,
    val extra_fajr_1_angle: Double = 18.0,
    val extra_fajr_1_label_raw: String = "",
    val extra_isha_1_enabled: Boolean = false,
    val extra_isha_1_angle: Double = 18.0,
    val extra_isha_1_label_raw: String = ""
) {
    fun display_label(): String {
        val trimmed = label.trim()
        return if (trimmed.isNotEmpty()) trimmed else "$latitude, $longitude"
    }

    fun method_config(): MethodConfig =
        MethodConfig(
            method_preset = method_preset,
            fajr_angle = method_fajr_angle,
            isha_mode = method_isha_mode,
            isha_angle = method_isha_angle,
            isha_fixed_minutes = method_isha_fixed_minutes,
            asr_factor = method_asr_factor,
            maghrib_offset_minutes = method_maghrib_offset_minutes,
            makruh_angle = method_makruh_angle,
            makruh_sunrise_minutes = method_makruh_sunrise_minutes,
            zawal_minutes = method_zawal_minutes
        )
}

object SavedLocations {
    const val max_count = 10

    const val home_source_host = "host"
    const val home_source_saved = "saved"

    const val calc_mode_inherit_global = "inherit_global"
    const val calc_mode_custom = "custom"

    private const val selection_base =
        AlarmEventContract.extra_alarm_now + "=? AND " +
            AlarmEventContract.extra_alarm_offset + "=? AND " +
            AlarmEventContract.extra_alarm_repeat + "=? AND " +
            AlarmEventContract.extra_alarm_repeat_days + "=? AND " +
            "latitude=? AND longitude=?"

    private const val selection_with_alt = "$selection_base AND altitude=?"

    fun load(context: Context): List<SavedLocation> {
        val raw = Prefs.get_saved_locations_json(context)
        return parse_json(raw, method_config_from_prefs(context), addon_runtime_profile_from_prefs(context))
    }

    fun save(context: Context, locations: List<SavedLocation>) {
        val normalized = locations.distinctBy { it.id }.take(max_count)
        Prefs.set_saved_locations_json(context, encode_json(normalized))
    }

    fun parse_json(raw: String): List<SavedLocation> = parse_json(raw, MethodConfig.defaults(), AddonRuntimeProfile.defaults())

    private fun parse_json(raw: String, defaults: MethodConfig, runtime_defaults: AddonRuntimeProfile): List<SavedLocation> {
        val arr =
            try {
                JSONArray(raw)
            } catch (_: Exception) {
                return emptyList()
            }

        val out = ArrayList<SavedLocation>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim()
            val label = obj.optString("label").trim()
            val lat = obj.optString("latitude").trim()
            val lon = obj.optString("longitude").trim()
            val tz = valid_timezone_id(obj.optString("timezone_id")) ?: continue
            val alt = obj.optString("altitude").trim().ifBlank { null }?.takeIf(::is_valid_alt)
            if (id.isBlank() || lat.isBlank() || lon.isBlank()) continue
            if (!is_valid_lat(lat) || !is_valid_lon(lon)) continue

            val calc_mode =
                obj.optString("calc_mode").trim().takeIf { it == calc_mode_custom || it == calc_mode_inherit_global }
                    ?: calc_mode_inherit_global
            val hijri_variant =
                obj.optString("hijri_variant").trim().takeIf { it == Prefs.hijri_variant_umalqura || it == Prefs.hijri_variant_diyanet }
                    ?: runtime_defaults.hijri_variant
            val hijri_day_offset =
                obj.takeIf { it.has("hijri_day_offset") }?.optInt("hijri_day_offset", Int.MIN_VALUE)
                    ?.takeIf { it != Int.MIN_VALUE }
                    ?.coerceIn(-2, 2)
                    ?: runtime_defaults.hijri_day_offset
            val method_preset = obj.optString("method_preset").trim().takeIf { it in MethodConfig.supported_presets } ?: defaults.method_preset

            fun d(key: String, fallback: Double): Double =
                obj.takeIf { it.has(key) }?.optDouble(key, Double.NaN)?.takeIf { !it.isNaN() && !it.isInfinite() } ?: fallback

            fun i(key: String, fallback: Int): Int =
                obj.takeIf { it.has(key) }?.optInt(key, Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE } ?: fallback

            fun s(key: String, fallback: String): String =
                obj.optString(key).trim().ifBlank { fallback }

            fun b(key: String, fallback: Boolean): Boolean =
                obj.takeIf { it.has(key) }?.optBoolean(key, fallback) ?: fallback

            out.add(
                SavedLocation(
                    id = id,
                    label = label,
                    latitude = lat,
                    longitude = lon,
                    altitude = alt,
                    timezone_id = tz,
                    calc_mode = calc_mode,
                    hijri_variant = hijri_variant,
                    hijri_day_offset = hijri_day_offset,
                    method_preset = method_preset,
                    method_fajr_angle = d("method_fajr_angle", defaults.fajr_angle),
                    method_isha_mode = s("method_isha_mode", defaults.isha_mode).takeIf { it == Prefs.isha_mode_angle || it == Prefs.isha_mode_fixed } ?: defaults.isha_mode,
                    method_isha_angle = d("method_isha_angle", defaults.isha_angle),
                    method_isha_fixed_minutes = i("method_isha_fixed_minutes", defaults.isha_fixed_minutes),
                    method_asr_factor = i("method_asr_factor", defaults.asr_factor).takeIf { it == 1 || it == 2 } ?: defaults.asr_factor,
                    method_maghrib_offset_minutes = i("method_maghrib_offset_minutes", defaults.maghrib_offset_minutes),
                    method_makruh_angle = d("method_makruh_angle", defaults.makruh_angle),
                    method_makruh_sunrise_minutes = i("method_makruh_sunrise_minutes", defaults.makruh_sunrise_minutes).takeIf { it in listOf(10, 15, 20) }
                        ?: defaults.makruh_sunrise_minutes,
                    method_zawal_minutes = i("method_zawal_minutes", defaults.zawal_minutes),
                    extra_fajr_1_enabled = b("extra_fajr_1_enabled", runtime_defaults.extra_fajr_1_enabled),
                    extra_fajr_1_angle = d("extra_fajr_1_angle", runtime_defaults.extra_fajr_1_angle),
                    extra_fajr_1_label_raw = s("extra_fajr_1_label", runtime_defaults.extra_fajr_1_label_raw),
                    extra_isha_1_enabled = b("extra_isha_1_enabled", runtime_defaults.extra_isha_1_enabled),
                    extra_isha_1_angle = d("extra_isha_1_angle", runtime_defaults.extra_isha_1_angle),
                    extra_isha_1_label_raw = s("extra_isha_1_label", runtime_defaults.extra_isha_1_label_raw)
                )
            )

            if (out.size >= max_count) break
        }
        return out
    }

    fun encode_json(locations: List<SavedLocation>): String {
        val arr = JSONArray()
        locations.distinctBy { it.id }.take(max_count).forEach { l ->
            arr.put(
                JSONObject()
                    .put("id", l.id)
                    .put("label", l.label.trim())
                    .put("latitude", l.latitude.trim())
                    .put("longitude", l.longitude.trim())
                    .put("altitude", l.altitude?.trim())
                    .put("timezone_id", l.timezone_id.trim())
                    .put("calc_mode", l.calc_mode)
                    .put("hijri_variant", l.hijri_variant)
                    .put("hijri_day_offset", l.hijri_day_offset.coerceIn(-2, 2))
                    .put("method_preset", l.method_preset)
                    .put("method_fajr_angle", l.method_fajr_angle)
                    .put("method_isha_mode", l.method_isha_mode)
                    .put("method_isha_angle", l.method_isha_angle)
                    .put("method_isha_fixed_minutes", l.method_isha_fixed_minutes)
                    .put("method_asr_factor", l.method_asr_factor)
                    .put("method_maghrib_offset_minutes", l.method_maghrib_offset_minutes)
                    .put("method_makruh_angle", l.method_makruh_angle)
                    .put("method_makruh_sunrise_minutes", l.method_makruh_sunrise_minutes)
                    .put("method_zawal_minutes", l.method_zawal_minutes)
                    .put("extra_fajr_1_enabled", l.extra_fajr_1_enabled)
                    .put("extra_fajr_1_angle", l.extra_fajr_1_angle)
                    .put("extra_fajr_1_label", l.extra_fajr_1_label_raw.trim())
                    .put("extra_isha_1_enabled", l.extra_isha_1_enabled)
                    .put("extra_isha_1_angle", l.extra_isha_1_angle)
                    .put("extra_isha_1_label", l.extra_isha_1_label_raw.trim())
            )
        }
        return arr.toString()
    }

    fun create_from_host(context: Context, host_event_authority: String): SavedLocation? {
        val cfg = HostConfigReader.read_config(context, host_event_authority) ?: return null
        val lat = cfg.latitude?.trim().orEmpty()
        val lon = cfg.longitude?.trim().orEmpty()
        val tz = cfg.timezone?.trim().orEmpty()
        if (lat.isBlank() || lon.isBlank() || tz.isBlank()) return null
        if (!is_valid_lat(lat) || !is_valid_lon(lon)) return null
        val method = method_config_from_prefs(context)
        val runtime = addon_runtime_profile_from_prefs(context)
        return SavedLocation(
            id = UUID.randomUUID().toString(),
            label = cfg.location?.trim().orEmpty(),
            latitude = lat,
            longitude = lon,
            altitude = null,
            timezone_id = tz,
            calc_mode = calc_mode_inherit_global,
            hijri_variant = runtime.hijri_variant,
            hijri_day_offset = runtime.hijri_day_offset,
            method_preset = method.method_preset,
            method_fajr_angle = method.fajr_angle,
            method_isha_mode = method.isha_mode,
            method_isha_angle = method.isha_angle,
            method_isha_fixed_minutes = method.isha_fixed_minutes,
            method_asr_factor = method.asr_factor,
            method_maghrib_offset_minutes = method.maghrib_offset_minutes,
            method_makruh_angle = method.makruh_angle,
            method_makruh_sunrise_minutes = method.makruh_sunrise_minutes,
            method_zawal_minutes = method.zawal_minutes,
            extra_fajr_1_enabled = runtime.extra_fajr_1_enabled,
            extra_fajr_1_angle = runtime.extra_fajr_1_angle,
            extra_fajr_1_label_raw = runtime.extra_fajr_1_label_raw,
            extra_isha_1_enabled = runtime.extra_isha_1_enabled,
            extra_isha_1_angle = runtime.extra_isha_1_angle,
            extra_isha_1_label_raw = runtime.extra_isha_1_label_raw
        )
    }

    fun is_valid_lat(v: String): Boolean = v.toDoubleOrNull()?.let { it in -90.0..90.0 } == true
    fun is_valid_lon(v: String): Boolean = v.toDoubleOrNull()?.let { it in -180.0..180.0 } == true
    fun is_valid_alt(v: String): Boolean = v.toDoubleOrNull() != null

    fun build_selection(alarm_now: Long, location: SavedLocation): Pair<String, Array<String>> =
        build_selection(alarm_now, location, null)

    fun build_selection(
        alarm_now: Long,
        location: SavedLocation,
        base_selection_args: Array<String>? = null
    ): Pair<String, Array<String>> {
        val base_now = base_selection_args?.getOrNull(0)?.toLongOrNull() ?: alarm_now
        val base_offset = base_selection_args?.getOrNull(1) ?: "0"
        val base_repeat = base_selection_args?.getOrNull(2) ?: "false"
        val base_repeat_days = base_selection_args?.getOrNull(3) ?: "[]"
        val alt = location.altitude?.trim().takeUnless { it.isNullOrBlank() }
        return if (alt != null) {
            selection_with_alt to arrayOf(
                base_now.toString(),
                base_offset,
                base_repeat,
                base_repeat_days,
                location.latitude,
                location.longitude,
                alt
            )
        } else {
            selection_base to arrayOf(
                base_now.toString(),
                base_offset,
                base_repeat,
                base_repeat_days,
                location.latitude,
                location.longitude
            )
        }
    }

    fun find_by_id(context: Context, id: String?): SavedLocation? =
        find_by_id(id, load(context))

    fun find_by_id(id: String?, locations: List<SavedLocation>): SavedLocation? {
        val normalized = id?.trim().orEmpty()
        if (normalized.isBlank()) return null
        return locations.firstOrNull { it.id == normalized }
    }

    fun find_matching_location(context: Context, latitude: String?, longitude: String?, altitude: String?): SavedLocation? {
        return find_matching_location(load(context), latitude, longitude, altitude)
    }

    fun find_matching_location(
        locations: List<SavedLocation>,
        latitude: String?,
        longitude: String?,
        altitude: String?
    ): SavedLocation? {
        val lat = latitude?.trim().orEmpty()
        val lon = longitude?.trim().orEmpty()
        if (lat.isBlank() || lon.isBlank()) return null
        val lat_value = lat.toDoubleOrNull() ?: return null
        val lon_value = lon.toDoubleOrNull() ?: return null
        val alt_value = altitude?.trim()?.toDoubleOrNull()
        return locations.firstOrNull { loc ->
            same_number(loc.latitude.toDoubleOrNull(), lat_value) &&
                same_number(loc.longitude.toDoubleOrNull(), lon_value) &&
                same_optional_number(loc.altitude?.toDoubleOrNull(), alt_value)
        }
    }

    fun method_config_for_location(context: Context, location: SavedLocation?): MethodConfig? {
        if (location == null || location.calc_mode != calc_mode_custom) return null
        return location.method_config()
    }

    fun addon_runtime_profile_for_location(context: Context, location: SavedLocation?): AddonRuntimeProfile? {
        if (location == null || location.calc_mode != calc_mode_custom) return null
        return AddonRuntimeProfile(
            hijri_variant = location.hijri_variant,
            hijri_day_offset = location.hijri_day_offset,
            extra_fajr_1_enabled = location.extra_fajr_1_enabled,
            extra_fajr_1_angle = location.extra_fajr_1_angle,
            extra_fajr_1_label_raw = location.extra_fajr_1_label_raw,
            extra_isha_1_enabled = location.extra_isha_1_enabled,
            extra_isha_1_angle = location.extra_isha_1_angle,
            extra_isha_1_label_raw = location.extra_isha_1_label_raw
        ).normalized()
    }

    private fun same_number(a: Double?, b: Double?): Boolean {
        if (a == null || b == null) return false
        return abs(a - b) < 0.0001
    }

    private fun same_optional_number(a: Double?, b: Double?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return same_number(a, b)
    }
}
