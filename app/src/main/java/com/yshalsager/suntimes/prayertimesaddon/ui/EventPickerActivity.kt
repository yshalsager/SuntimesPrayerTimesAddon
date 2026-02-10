package com.yshalsager.suntimes.prayertimesaddon.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.remember
import androidx.activity.compose.setContent
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.AlarmEventContract
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.AppScaffold
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.EventPickerScreen
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.PrayerTimesTheme
import com.yshalsager.suntimes.prayertimesaddon.R

class EventPickerActivity : ThemedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PrayerTimesTheme {
                val items = remember { AddonEvent.values().toList() }
                AppScaffold(
                    title = getString(R.string.picker_title),
                    nav_content_description = getString(android.R.string.cancel),
                    on_nav = { finish() }
                ) { padding ->
                    EventPickerScreen(
                        items = items,
                        on_pick = { e ->
                            val uri =
                                Uri.parse("content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_info}/${e.event_id}")
                            setResult(
                                Activity.RESULT_OK,
                                Intent()
                                    .setData(uri)
                                    .putExtra(AlarmEventContract.column_config_provider, PrayerTimesProvider.authority)
                                    .putExtra(AlarmEventContract.column_event_name, e.event_id)
                            )
                            finish()
                        },
                        padding = padding
                    )
                }
            }
        }
    }
}
