package com.yshalsager.suntimes.prayertimesaddon.core

import net.time4j.Moment
import net.time4j.PlainDate
import net.time4j.TemporalType
import net.time4j.calendar.HijriCalendar
import net.time4j.format.expert.ChronoFormatter
import net.time4j.format.expert.PatternType
import net.time4j.tz.ZonalOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class HijriInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val formatted: String
)

private fun time4j_variant(variant: String): String =
    if (variant == Prefs.hijri_variant_diyanet) HijriCalendar.VARIANT_DIYANET else HijriCalendar.VARIANT_UMALQURA

private fun adjusted_midday_millis(day_start_millis: Long, tz: TimeZone, offset_days: Int): Long =
    Calendar.getInstance(tz).run {
        timeInMillis = day_start_millis
        add(Calendar.HOUR_OF_DAY, 12)
        add(Calendar.DAY_OF_YEAR, offset_days.coerceIn(-2, 2))
        timeInMillis
    }

private fun plain_date_at(millis: Long, tz: TimeZone): PlainDate {
    val moment: Moment = TemporalType.JAVA_UTIL_DATE.translate(Date(millis))
    val offset = ZonalOffset.ofTotalSeconds(tz.getOffset(millis) / 1000)
    return moment.toZonalTimestamp(offset).toDate()
}

private data class HijriFmtKey(val locale: String, val variant: String, val pattern: String)
private val hijri_formatters = ConcurrentHashMap<HijriFmtKey, ChronoFormatter<HijriCalendar>>()

private fun hijri_formatter(locale: Locale, variant: String, pattern: String): ChronoFormatter<HijriCalendar> {
    val key = HijriFmtKey(locale.toLanguageTag(), variant, pattern)
    return hijri_formatters.getOrPut(key) {
        ChronoFormatter.setUp(HijriCalendar::class.java, locale)
            .addPattern(pattern, PatternType.CLDR_DATE)
            .build()
            .withCalendarVariant(variant)
    }
}

fun hijri_for_day(
    day_start_millis: Long,
    tz: TimeZone,
    locale: Locale,
    variant: String,
    offset_days: Int
): HijriInfo {
    val millis = adjusted_midday_millis(day_start_millis, tz, offset_days)
    val time4j_variant = time4j_variant(variant)
    val date = plain_date_at(millis, tz)
    val hijri = date.transform(HijriCalendar::class.java, time4j_variant)
    val fmt = hijri_formatter(locale, time4j_variant, "d MMMM y")

    return HijriInfo(
        year = hijri.year,
        month = hijri.month.value,
        day = hijri.dayOfMonth,
        formatted = fmt.format(hijri)
    )
}

fun hijri_month_title_for_day(
    day_start_millis: Long,
    tz: TimeZone,
    locale: Locale,
    variant: String,
    offset_days: Int
): String {
    val millis = adjusted_midday_millis(day_start_millis, tz, offset_days)
    val time4j_variant = time4j_variant(variant)
    val date = plain_date_at(millis, tz)
    val hijri = date.transform(HijriCalendar::class.java, time4j_variant)
    return hijri_formatter(locale, time4j_variant, "MMMM y").format(hijri)
}
