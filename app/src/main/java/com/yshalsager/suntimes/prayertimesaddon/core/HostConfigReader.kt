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

    fun calc_authority_from_event_authority(event_authority: String): String? =
        event_authority.takeIf { it.endsWith(event_suffix) }?.removeSuffix(event_suffix)?.plus(calc_suffix)

    fun read_config(context: Context, host_event_authority: String): HostConfig? {
        val calc_authority = calc_authority_from_event_authority(host_event_authority) ?: return null
        val uri = "content://$calc_authority/${CalculatorConfigContract.query_config}".toUri()
        return query_host_provider(
            authority = calc_authority,
            operation = CalculatorConfigContract.query_config,
            query = {
                context.contentResolver.query(
                    uri,
                    CalculatorConfigContract.projection_basic,
                    null,
                    null,
                    null
                )
            }
        ) { cur ->
            if (!cur.moveToFirst()) return@query_host_provider null
            HostConfig(
                location = cur.host_string(CalculatorConfigContract.column_location),
                latitude = cur.host_string(CalculatorConfigContract.column_latitude),
                longitude = cur.host_string(CalculatorConfigContract.column_longitude),
                timezone = cur.host_string(CalculatorConfigContract.column_timezone)
            )
        }.value_or_null
    }
}
