package com.yshalsager.suntimes.prayertimesaddon.core

object CalculatorConfigContract {
    const val query_config = "config"
    const val query_sun = "sun"
    const val query_sunpos = "sunpos"

    const val column_location = "location"
    const val column_latitude = "latitude"
    const val column_longitude = "longitude"
    const val column_timezone = "timezone"

    const val column_sun_noon = "solarnoon"
    const val column_sunrise = "sunrise"
    const val column_sunset = "sunset"

    const val column_sunpos_dec = "sunpos_dec"

    val projection_basic = arrayOf(column_location, column_latitude, column_longitude, column_timezone)
    val projection_sun_basic = arrayOf(column_sun_noon, column_sunrise, column_sunset)
    val projection_sunpos_dec = arrayOf(column_sunpos_dec)
}
