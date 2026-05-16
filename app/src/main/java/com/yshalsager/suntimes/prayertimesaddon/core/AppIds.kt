package com.yshalsager.suntimes.prayertimesaddon.core

import com.yshalsager.suntimes.prayertimesaddon.BuildConfig

object AppIds {
    val event_provider_authority = "${BuildConfig.APPLICATION_ID}.event.provider"
    val calendar_provider_authority = "${BuildConfig.APPLICATION_ID}.calendar.provider"
    val action_widget_alarm = "${BuildConfig.APPLICATION_ID}.action.WIDGET_ALARM"
    val action_prayer_status_refresh = "${BuildConfig.APPLICATION_ID}.action.PRAYER_STATUS_REFRESH"
    val action_prayer_status_disable = "${BuildConfig.APPLICATION_ID}.action.PRAYER_STATUS_DISABLE"
}
