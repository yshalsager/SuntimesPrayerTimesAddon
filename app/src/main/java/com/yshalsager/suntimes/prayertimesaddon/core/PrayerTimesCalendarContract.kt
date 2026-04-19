package com.yshalsager.suntimes.prayertimesaddon.core

import android.provider.CalendarContract
import com.yshalsager.suntimes.prayertimesaddon.R

enum class PrayerTimesCalendarSource(
    val id: String,
    val title_res: Int,
    val color_res: Int
) {
    prayers("prayers", R.string.calendar_prayers_title, R.color.calendar_prayers),
    makruh("makruh", R.string.calendar_makruh_title, R.color.calendar_makruh),
    night("night", R.string.calendar_night_title, R.color.calendar_night);

    companion object {
        fun from_id(id: String): PrayerTimesCalendarSource? = entries.firstOrNull { it.id == id }
    }
}

object PrayerTimesCalendarContract {
    val authority = AppIds.calendar_provider_authority
    const val param_saved_location_id = query_param_saved_location_id

    const val action_add_calendar = "suntimes.action.ADD_CALENDAR"
    const val category_suntimes_calendar = "suntimes.SUNTIMES_CALENDAR"
    const val metadata_reference = "SuntimesCalendarReference"

    const val column_calendar_name = "calendar_name"
    const val column_calendar_title = "calendar_title"
    const val column_calendar_summary = "calendar_summary"
    const val column_calendar_color = "calendar_color"
    const val column_calendar_template_title = "template_title"
    const val column_calendar_template_description = "template_description"
    const val column_calendar_template_location = "template_location"
    const val column_calendar_template_strings = "template_strings"
    const val column_calendar_template_flags = "template_flags"
    const val column_calendar_template_flag_labels = "template_flag_labels"

    const val query_calendar_info = "calendarInfo"
    const val query_calendar_template_strings = "calendarTemplateStrings"
    const val query_calendar_template_flags = "calendarTemplateFlags"
    const val query_calendar_content = "calendarContent"

    val query_calendar_info_projection =
        arrayOf(
            column_calendar_name,
            column_calendar_title,
            column_calendar_summary,
            column_calendar_color,
            column_calendar_template_title,
            column_calendar_template_description,
            column_calendar_template_location
        )

    val query_calendar_template_strings_projection = arrayOf(column_calendar_template_strings)
    val query_calendar_template_flags_projection = arrayOf(column_calendar_template_flags, column_calendar_template_flag_labels)

    val query_calendar_content_projection =
        arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_TIMEZONE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )

    fun reference_for(source: PrayerTimesCalendarSource): String = "content://$authority/${source.id}/"

    val discovery_references = PrayerTimesCalendarSource.entries.joinToString(" | ") { reference_for(it) }
}
