package com.yshalsager.suntimes.prayertimesaddon.ui

import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DaysViewModelTest {
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun set_up() {
        context.getSharedPreferences("${context.packageName}_preferences", android.content.Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun build_days_sig_changes_when_gregorian_date_format_changes() {
        val host = "com.test.host.event.provider"
        val month_anchor = 123L
        val show_prohibited = true
        val show_night = true
        val show_hijri_effective = true
        val month_basis = Prefs.days_month_basis_gregorian
        val hijri_variant = Prefs.hijri_variant_umalqura
        val hijri_offset = 0

        val first = build_days_sig(context, host, month_anchor, show_prohibited, show_night, show_hijri_effective, month_basis, hijri_variant, hijri_offset)
        Prefs.set_gregorian_date_format(context, Prefs.gregorian_date_format_long)
        val second = build_days_sig(context, host, month_anchor, show_prohibited, show_night, show_hijri_effective, month_basis, hijri_variant, hijri_offset)

        assertNotEquals(first, second)
    }

    @Test
    fun build_days_sig_changes_when_makruh_sunrise_minutes_changes() {
        val host = "com.test.host.event.provider"
        val month_anchor = 123L
        val show_prohibited = true
        val show_night = true
        val show_hijri_effective = true
        val month_basis = Prefs.days_month_basis_gregorian
        val hijri_variant = Prefs.hijri_variant_umalqura
        val hijri_offset = 0

        val first = build_days_sig(context, host, month_anchor, show_prohibited, show_night, show_hijri_effective, month_basis, hijri_variant, hijri_offset)
        Prefs.set_makruh_sunrise_minutes(context, 20)
        val second = build_days_sig(context, host, month_anchor, show_prohibited, show_night, show_hijri_effective, month_basis, hijri_variant, hijri_offset)

        assertNotEquals(first, second)
    }
}
