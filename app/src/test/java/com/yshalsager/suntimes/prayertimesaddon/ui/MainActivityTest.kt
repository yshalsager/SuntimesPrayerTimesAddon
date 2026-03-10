package com.yshalsager.suntimes.prayertimesaddon.ui

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityTest {
    @Test
    fun shortcut_days_uri_resolves_to_main_activity() {
        val context = RuntimeEnvironment.getApplication()
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("prayertimes://shortcuts/days")).addCategory(Intent.CATEGORY_DEFAULT)

        val resolve_info = context.packageManager.resolveActivity(intent, 0)!!

        assertNotNull(resolve_info)
        assertEquals(MainActivity::class.java.name, resolve_info.activityInfo.name)
    }

    @Test
    fun shortcut_settings_uri_resolves_to_main_activity() {
        val context = RuntimeEnvironment.getApplication()
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("prayertimes://shortcuts/settings")).addCategory(Intent.CATEGORY_DEFAULT)

        val resolve_info = context.packageManager.resolveActivity(intent, 0)!!

        assertNotNull(resolve_info)
        assertEquals(MainActivity::class.java.name, resolve_info.activityInfo.name)
    }
}
