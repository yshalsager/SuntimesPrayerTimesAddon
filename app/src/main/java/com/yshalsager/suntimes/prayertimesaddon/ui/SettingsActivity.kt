package com.yshalsager.suntimes.prayertimesaddon.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.PrayerTimesTheme
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.SettingsRoot
import com.yshalsager.suntimes.prayertimesaddon.widget.WidgetUpdate

class SettingsActivity : ThemedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrayerTimesTheme {
                SettingsRoot(on_back = { finish() })
            }
        }
    }

    override fun onStop() {
        super.onStop()
        WidgetUpdate.request(this)
    }
}
