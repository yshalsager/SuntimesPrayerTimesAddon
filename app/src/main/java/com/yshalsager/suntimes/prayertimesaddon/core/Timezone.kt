package com.yshalsager.suntimes.prayertimesaddon.core

import java.util.TimeZone
import kotlin.math.abs

fun valid_timezone_id(value: String?): String? {
    val candidate = value?.trim().orEmpty()
    if (candidate.isBlank()) return null
    return if (TimeZone.getAvailableIDs().contains(candidate)) candidate else null
}

fun timezone_offset_mismatch_hours(
    longitude: String?,
    timezone_id: String?,
    at_millis: Long = System.currentTimeMillis()
): Double? {
    val lon = longitude?.trim()?.toDoubleOrNull() ?: return null
    if (lon !in -180.0..180.0) return null
    val normalized_tz = valid_timezone_id(timezone_id) ?: return null
    val tz = TimeZone.getTimeZone(normalized_tz)
    val actual_hours = tz.getOffset(at_millis) / 3_600_000.0
    val expected_hours = lon / 15.0
    return abs(actual_hours - expected_hours)
}

fun timezone_likely_mismatch(
    longitude: String?,
    timezone_id: String?,
    threshold_hours: Double = 2.5
): Boolean {
    val diff = timezone_offset_mismatch_hours(longitude, timezone_id) ?: return false
    return diff >= threshold_hours
}
