package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import androidx.core.net.toUri

data class HostConfig(
    val location: String?,
    val latitude: String?,
    val longitude: String?,
    val timezone: String?
) {
    fun display_label(): String? {
        val loc = location?.trim()
        if (!loc.isNullOrEmpty()) return loc

        val lat = latitude?.trim()
        val lon = longitude?.trim()
        return if (!lat.isNullOrEmpty() && !lon.isNullOrEmpty()) "$lat, $lon" else null
    }
}

object HostConfigReader {
    private const val event_suffix = ".event.provider"
    private const val calc_suffix = ".calculator.provider"

    private var cached_host: String? = null
    private var cached_config: HostConfig? = null

    fun clear_cache(host_event_authority: String? = null) {
        if (host_event_authority == null || host_event_authority == cached_host) {
            cached_host = null
            cached_config = null
        }
    }

    fun calc_authority_from_event_authority(event_authority: String): String? =
        event_authority.takeIf { it.endsWith(event_suffix) }?.removeSuffix(event_suffix)?.plus(calc_suffix)

    fun read_config(context: Context, host_event_authority: String): HostConfig? {
        if (host_event_authority == cached_host && cached_config != null) return cached_config

        val calc_authority = calc_authority_from_event_authority(host_event_authority) ?: return null
        val uri = "content://$calc_authority/${CalculatorConfigContract.query_config}".toUri()
        val c = try {
            context.contentResolver.query(
                uri,
                CalculatorConfigContract.projection_basic,
                null,
                null,
                null
            )
        } catch (_: SecurityException) {
            null
        } ?: return null

        c.use { cur ->
            if (!cur.moveToFirst()) return null

            fun col(name: String): String? = cur.getColumnIndex(name).takeIf { it >= 0 && !cur.isNull(it) }?.let(cur::getString)

            val config = HostConfig(
                location = col(CalculatorConfigContract.column_location),
                latitude = col(CalculatorConfigContract.column_latitude),
                longitude = col(CalculatorConfigContract.column_longitude),
                timezone = col(CalculatorConfigContract.column_timezone)
            )
            cached_host = host_event_authority
            cached_config = config
            return config
        }
    }
}
