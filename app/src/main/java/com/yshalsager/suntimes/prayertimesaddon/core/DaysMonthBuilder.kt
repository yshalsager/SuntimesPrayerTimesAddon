package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class MonthSkeleton(
    val title: String,
    val start: Long,
    val end: Long,
    val today_pos: Int,
    val days: List<DayMeta>
)

data class DayMeta(
    val day_start: Long,
    val title: String,
    val hijri: String?,
    val is_today: Boolean,
    val is_friday: Boolean
)

data class DayItem(
    val day_start: Long,
    val title: String,
    val hijri: String?,
    val is_today: Boolean,
    val is_friday: Boolean,
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val prohibited_dawn: String?,
    val prohibited_sunrise: String?,
    val prohibited_zawal: String?,
    val prohibited_after_asr: String?,
    val prohibited_sunset: String?,
    val night_midpoint: String?,
    val night_last_third: String?,
    val night_last_sixth: String?
)

fun add_days(day_start_millis: Long, offset_days: Int, tz: TimeZone): Long =
    Calendar.getInstance(tz).run {
        timeInMillis = day_start_millis
        add(Calendar.DAY_OF_YEAR, offset_days)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

fun today_start(tz: TimeZone): Long =
    Calendar.getInstance(tz).run {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

fun format_gregorian_day_title(context: Context, day_start_millis: Long, tz: TimeZone, locale: Locale): String {
    val weekday = SimpleDateFormat("EEE", locale).apply { timeZone = tz }.format(Date(day_start_millis))
    val date =
        when (Prefs.get_gregorian_date_format(context)) {
            Prefs.gregorian_date_format_long -> java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG, locale)
            Prefs.gregorian_date_format_card -> SimpleDateFormat("MMM d", locale)
            else -> java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale)
        }
            .apply { timeZone = tz }
            .format(Date(day_start_millis))
    val sep = if (locale.language == "ar") "،" else ","
    return "$weekday$sep $date"
}

fun build_month_skeleton(
    context: Context,
    host: String,
    month_basis: String,
    show_hijri: Boolean,
    hijri_variant: String,
    hijri_offset: Int,
    month_anchor: Long?
): MonthSkeleton {
    val tz = HostConfigReader.read_config(context, host)?.timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()

    val locale = Locale.getDefault()
    val month_format = SimpleDateFormat("MMMM yyyy", locale).apply { timeZone = tz }

    val today = today_start(tz)
    val anchor = month_anchor ?: today

    val hijri_cache = HashMap<Long, HijriInfo>()
    fun hijri_cached(day_start: Long): HijriInfo =
        hijri_cache.getOrPut(day_start) { hijri_for_day(day_start, tz, Locale.getDefault(), hijri_variant, hijri_offset) }

    val days = if (month_basis == Prefs.days_month_basis_hijri) {
        val anchor_hijri = hijri_cached(anchor)
        val month_key = anchor_hijri.year to anchor_hijri.month

        var start = anchor
        while (true) {
            val h = hijri_cached(start)
            if (h.year to h.month != month_key) break
            if (h.day == 1) break
            start = add_days(start, -1, tz)
        }

        val out = ArrayList<Long>(30)
        var d = start
        while (true) {
            val h = hijri_cached(d)
            if (h.year to h.month != month_key) break
            out.add(d)
            d = add_days(d, 1, tz)
            if (out.size > 35) break
        }
        out
    } else {
        val cal = Calendar.getInstance(tz).apply { timeInMillis = anchor }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        val end = add_days(cal.timeInMillis, -1, tz)

        val out = ArrayList<Long>(31)
        var d = start
        while (d <= end) {
            out.add(d)
            d = add_days(d, 1, tz)
        }
        out
    }

    val start = days.firstOrNull() ?: anchor
    val end = days.lastOrNull() ?: anchor
    val title =
        if (month_basis == Prefs.days_month_basis_hijri) hijri_month_title_for_day(start, tz, Locale.getDefault(), hijri_variant, hijri_offset)
        else month_format.format(Date(start))

    val metas = days.map { day_start ->
        val is_friday = Calendar.getInstance(tz).run {
            timeInMillis = day_start
            get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        }
        DayMeta(
            day_start = day_start,
            title = format_gregorian_day_title(context, day_start, tz, locale),
            hijri = if (!show_hijri) null else hijri_cached(day_start).formatted,
            is_today = day_start == today,
            is_friday = is_friday
        )
    }

    return MonthSkeleton(title = title, start = start, end = end, today_pos = days.indexOf(today), days = metas)
}

fun build_day_item(
    context: Context,
    host: String,
    meta: DayMeta,
    show_prohibited: Boolean,
    show_night: Boolean
): DayItem {
    val tz = HostConfigReader.read_config(context, host)?.timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()

    val time_format = DateFormat.getTimeFormat(context).apply { timeZone = tz }
    val time_only_format =
        SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", Locale.getDefault()).apply { timeZone = tz }

    fun time_short(v: Long?): String = v?.let { time_only_format.format(Date(it)) } ?: "--"
    fun time_str(v: Long?): String = v?.let { time_format.format(Date(it)) } ?: "--"
    fun range(a: Long?, b: Long?): String = "${time_short(a)}-${time_short(b)}"

    fun q(event: AddonEvent) = query_host_addon_time(context, host, event, meta.day_start)

    val sun = query_host_sun(context, host, meta.day_start)
    val maghrib_offset_ms = Prefs.get_maghrib_offset_minutes(context) * 60_000L

    val fajr = q(AddonEvent.prayer_fajr)
    val asr = q(AddonEvent.prayer_asr)
    val isha = q(AddonEvent.prayer_isha)

    val dhuhr = sun?.noon ?: q(AddonEvent.prayer_dhuhr)
    val maghrib = sun?.sunset?.plus(maghrib_offset_ms) ?: q(AddonEvent.prayer_maghrib)

    val sunrise = if (!show_prohibited) null else sun?.sunrise ?: q(AddonEvent.makruh_sunrise_start)
    val sunrise_end = if (!show_prohibited) null else q(AddonEvent.makruh_sunrise_end)
    val zawal_start =
        if (!show_prohibited) null
        else if (sun?.noon != null) sun.noon - Prefs.get_zawal_minutes(context) * 60_000L
        else q(AddonEvent.makruh_zawal_start)
    val zawal_end = dhuhr
    val sunset_start = if (!show_prohibited) null else q(AddonEvent.makruh_sunset_start)
    val sunset = if (!show_prohibited) null else sun?.sunset ?: q(AddonEvent.makruh_sunset_end)

    val prohibited_dawn = if (!show_prohibited) null else range(fajr, sunrise)
    val prohibited_sunrise = if (!show_prohibited) null else range(sunrise, sunrise_end)
    val prohibited_zawal = if (!show_prohibited) null else range(zawal_start, zawal_end)
    val prohibited_after_asr = if (!show_prohibited) null else range(asr, sunset_start)
    val prohibited_sunset = if (!show_prohibited) null else range(sunset_start, sunset)

    val fajr_next =
        if (!show_night) null
        else query_host_addon_time(context, host, AddonEvent.prayer_fajr, add_days(meta.day_start, 1, tz))
    val night = if (!show_night) null else calc_night(maghrib, fajr_next)

    val night_midpoint = night?.midpoint?.let(::time_short)
    val night_last_third = night?.last_third?.let(::time_short)
    val night_last_sixth = night?.last_sixth?.let(::time_short)

    return DayItem(
        day_start = meta.day_start,
        title = meta.title,
        hijri = meta.hijri,
        is_today = meta.is_today,
        is_friday = meta.is_friday,
        fajr = time_str(fajr),
        dhuhr = time_str(dhuhr),
        asr = time_str(asr),
        maghrib = time_str(maghrib),
        isha = time_str(isha),
        prohibited_dawn = prohibited_dawn,
        prohibited_sunrise = prohibited_sunrise,
        prohibited_zawal = prohibited_zawal,
        prohibited_after_asr = prohibited_after_asr,
        prohibited_sunset = prohibited_sunset,
        night_midpoint = night_midpoint,
        night_last_third = night_last_third,
        night_last_sixth = night_last_sixth
    )
}
