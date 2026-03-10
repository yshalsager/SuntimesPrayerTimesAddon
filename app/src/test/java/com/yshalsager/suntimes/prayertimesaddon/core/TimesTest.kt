package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.Context
import android.content.pm.ProviderInfo
import com.yshalsager.suntimes.prayertimesaddon.FakeHostCalcProvider
import com.yshalsager.suntimes.prayertimesaddon.FakeHostEventProvider
import com.yshalsager.suntimes.prayertimesaddon.host_calc_authority
import com.yshalsager.suntimes.prayertimesaddon.host_event_authority
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TimesTest {
    private lateinit var context: Context

    @Before
    fun set_up() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE).edit().clear().apply()
        HostConfigReader.clear_cache()
        Prefs.set_asr_factor(context, 1)

        Robolectric.setupContentProvider(FakeHostEventProvider::class.java, host_event_authority)
        Robolectric.setupContentProvider(FakeHostCalcProvider::class.java, host_calc_authority)

        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = host_event_authority
                packageName = context.packageName
                name = FakeHostEventProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
        shadowOf(context.packageManager).addOrUpdateProvider(
            ProviderInfo().apply {
                authority = host_calc_authority
                packageName = context.packageName
                name = FakeHostCalcProvider::class.java.name
                applicationInfo = context.applicationInfo
            }
        )
    }

    @Test
    fun query_host_addon_time_uses_host_shadow_ratio_event_for_asr() {
        val day_start = 0L

        val asr = query_host_addon_time(context, host_event_authority, AddonEvent.prayer_asr, day_start)

        assertEquals(15 * 60 * 60 * 1000L + 30 * 60 * 1000L, asr)
    }
}
