package com.yshalsager.suntimes.prayertimesaddon.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.DaysScreen
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.PrayerTimesTheme

class DaysActivity : ThemedActivity() {
    private lateinit var vm: DaysViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm =
            ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[DaysViewModel::class.java]

        setContent {
            PrayerTimesTheme {
                DaysScreen(vm, on_back = { finish() })
            }
        }
        vm.load(force = true)
    }
}
