package com.yshalsager.suntimes.prayertimesaddon.core

object AlarmEventContract {
    const val required_permission = "suntimes.permission.READ_CALCULATOR"

    const val column_config_provider = "provider"

    const val column_event_name = "event_name"
    const val column_event_title = "event_title"
    const val column_event_summary = "event_summary"
    const val column_event_type = "event_type"
    const val column_event_type_label = "event_type_label"

    const val column_event_phrase = "event_phrase"
    const val column_event_phrase_gender = "event_phrase_gender"
    const val column_event_phrase_quantity = "event_phrase_quantity"

    const val column_event_supports_repeating = "event_supports_repeat"
    const val column_event_supports_offsetdays = "event_supports_offsetdays"
    const val column_event_requires_location = "event_requires_location"

    const val repeat_support_daily = 0

    const val column_event_timemillis = "event_time"

    const val query_event_info = "eventInfo"
    val query_event_info_projection = arrayOf(
        column_event_name,
        column_event_title,
        column_event_summary,
        column_event_phrase,
        column_event_phrase_gender,
        column_event_phrase_quantity,
        column_event_supports_repeating,
        column_event_supports_offsetdays,
        column_event_requires_location,
        column_event_type
    )

    const val query_event_types = "eventTypes"
    val query_event_types_projection = arrayOf(column_event_type, column_event_type_label)

    const val query_event_calc = "eventCalc"
    val query_event_calc_projection = arrayOf(column_event_name, column_event_timemillis)

    const val extra_alarm_now = "alarm_now"
    const val extra_alarm_offset = "alarm_offset"
    const val extra_alarm_repeat = "alarm_repeat"
    const val extra_alarm_repeat_days = "alarm_repeat_days"
}
