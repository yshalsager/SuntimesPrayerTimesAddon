package com.yshalsager.suntimes.prayertimesaddon

import com.yshalsager.suntimes.prayertimesaddon.core.HostEventIds
import org.junit.Assert.assertEquals
import org.junit.Test

class EventIdsTest {
    @Test
    fun sun_elevation_event_ids_match_host_format() {
        assertEquals("SUN_-18.0r", HostEventIds.sun_elevation(-18.0, rising = true))
        assertEquals("SUN_-17.5s", HostEventIds.sun_elevation(-17.5, rising = false))
        assertEquals("SUN_5.0r", HostEventIds.sun_elevation(5.0, rising = true))
    }

    @Test
    fun shadow_ratio_event_ids_match_host_format() {
        assertEquals("SHADOWRATIO_X:1.0", HostEventIds.shadow_ratio(1))
        assertEquals("SHADOWRATIO_X:2.0", HostEventIds.shadow_ratio(2))
    }
}
