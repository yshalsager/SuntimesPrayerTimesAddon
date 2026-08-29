package com.yshalsager.suntimes.prayertimesaddon.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HostSelectionTest {
    @Test
    fun parses_reordered_named_fields_and_extra_fields() {
        val parsed = parse_host_selection(
            "timezone=? AND extra=? OR longitude=? AND ${AlarmEventContract.extra_alarm_now}=? AND altitude=? AND latitude=?",
            arrayOf("Africa/Cairo", "ignored", "31.0", "123", "50", "30.0")
        )

        assertEquals("123", parsed[AlarmEventContract.extra_alarm_now])
        assertEquals("30.0", parsed[CalculatorConfigContract.column_latitude])
        assertEquals("31.0", parsed[CalculatorConfigContract.column_longitude])
        assertEquals("50", parsed[CalculatorConfigContract.column_altitude])
        assertEquals("Africa/Cairo", parsed[CalculatorConfigContract.column_timezone])
    }

    @Test
    fun missing_named_fields_do_not_use_positional_values() {
        val parsed = parse_host_selection("extra=?", arrayOf("30.0", "31.0"))

        assertNull(parsed[CalculatorConfigContract.column_latitude])
        assertNull(parsed[CalculatorConfigContract.column_longitude])
    }

    @Test
    fun null_arguments_are_skipped_like_host_parser() {
        @Suppress("UNCHECKED_CAST")
        val args = arrayOf<String?>("30.0", null) as Array<String>

        val parsed = parse_host_selection("latitude=? AND longitude=?", args)

        assertEquals("30.0", parsed[CalculatorConfigContract.column_latitude])
        assertEquals("?", parsed[CalculatorConfigContract.column_longitude])
    }

    @Test
    fun selection_absent_uses_legacy_positions() {
        val parsed = parse_host_selection(null, arrayOf("123", "5", "false", "[]", "30.0", "31.0", "UTC", "50"))

        assertEquals("123", parsed[AlarmEventContract.extra_alarm_now])
        assertEquals("30.0", parsed[CalculatorConfigContract.column_latitude])
        assertEquals("31.0", parsed[CalculatorConfigContract.column_longitude])
        assertEquals("UTC", parsed[CalculatorConfigContract.column_timezone])
        assertEquals("50", parsed[CalculatorConfigContract.column_altitude])
    }
}
