package com.yshalsager.suntimes.prayertimesaddon.core

import com.yshalsager.suntimes.prayertimesaddon.BuildConfig

object AppIds {
    val event_provider_authority = "${BuildConfig.APPLICATION_ID}.event.provider"
    val calendar_provider_authority = "${BuildConfig.APPLICATION_ID}.calendar.provider"
    val action_widget_alarm = "${BuildConfig.APPLICATION_ID}.action.WIDGET_ALARM"
}
