package com.yshalsager.suntimes.prayertimesaddon.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.yshalsager.suntimes.prayertimesaddon.core.open_url
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
                DaysScreen(
                    vm,
                    on_back = { finish() },
                    on_install_host = { open_url(this@DaysActivity, "https://github.com/forrestguice/SuntimesWidget") },
                    on_reinstall_addon = { open_url(this@DaysActivity, "https://github.com/yshalsager/SuntimesPrayerTimesAddon/releases/latest") }
                )
            }
        }
        vm.load(force = true)
    }

    override fun onResume() {
        super.onResume()
        vm.load()
    }
}
