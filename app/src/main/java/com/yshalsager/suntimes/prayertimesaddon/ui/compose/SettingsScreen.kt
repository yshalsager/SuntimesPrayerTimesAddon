package com.yshalsager.suntimes.prayertimesaddon.ui.compose

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.material.color.DynamicColors
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingDropdown
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingInlineTextField
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingRow
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingSwitch
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingTextField
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingsSection
import androidx.compose.ui.text.input.KeyboardType
import com.yshalsager.suntimes.prayertimesaddon.widget.WidgetUpdate

@Composable
fun SettingsRoot(on_back: () -> Unit) {
    val ctx = LocalContext.current

    AppScaffold(
        title = ctx.getString(R.string.main_settings),
        nav_content_description = ctx.getString(android.R.string.cancel),
        on_nav = on_back
    ) { padding ->
        SettingsContent(padding = padding)
    }
}

@Composable
private fun SettingsContent(
    padding: PaddingValues
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val lifecycle_owner = LocalLifecycleOwner.current

    val hosts = remember { HostResolver.detect_hosts(ctx) }

    var host_event_authority by rememberSaveable { mutableStateOf(HostResolver.ensure_default_selected(ctx) ?: "") }
    var host_location_label by remember { mutableStateOf(ctx.getString(R.string.unknown_location)) }

    var language by rememberSaveable { mutableStateOf(Prefs.get_language(ctx)) }
    var theme by rememberSaveable { mutableStateOf(Prefs.get_theme(ctx)) }
    var palette by rememberSaveable { mutableStateOf(Prefs.get_palette(ctx)) }
    var gregorian_date_format by rememberSaveable { mutableStateOf(Prefs.get_gregorian_date_format(ctx)) }

    var method_preset by rememberSaveable { mutableStateOf(Prefs.get_method_preset(ctx)) }
    var fajr_angle_text by rememberSaveable { mutableStateOf(Prefs.get_fajr_angle(ctx).toString()) }
    var isha_mode by rememberSaveable { mutableStateOf(Prefs.get_isha_mode(ctx)) }
    var isha_angle_text by rememberSaveable { mutableStateOf(Prefs.get_isha_angle(ctx).toString()) }
    var isha_fixed_minutes_text by rememberSaveable { mutableStateOf(Prefs.get_isha_fixed_minutes(ctx).toString()) }
    var asr_factor by rememberSaveable { mutableStateOf(Prefs.get_asr_factor(ctx).toString()) }
    var maghrib_offset_minutes_text by rememberSaveable { mutableStateOf(Prefs.get_maghrib_offset_minutes(ctx).toString()) }

    var makruh_preset by rememberSaveable { mutableStateOf(Prefs.get_makruh_preset(ctx)) }
    var makruh_angle_text by rememberSaveable { mutableStateOf(Prefs.get_makruh_angle(ctx).toString()) }
    var zawal_minutes_text by rememberSaveable { mutableStateOf(Prefs.get_zawal_minutes(ctx).toString()) }

    var days_month_basis by rememberSaveable { mutableStateOf(Prefs.get_days_month_basis(ctx)) }
    var days_show_hijri by rememberSaveable { mutableStateOf(Prefs.get_days_show_hijri(ctx)) }
    var days_show_prohibited by rememberSaveable { mutableStateOf(Prefs.get_days_show_prohibited(ctx)) }
    var days_show_night by rememberSaveable { mutableStateOf(Prefs.get_days_show_night_portions(ctx)) }

    var widget_show_prohibited by rememberSaveable { mutableStateOf(Prefs.get_widget_show_prohibited(ctx)) }
    var widget_show_night by rememberSaveable { mutableStateOf(Prefs.get_widget_show_night_portions(ctx)) }

    var hijri_variant by rememberSaveable { mutableStateOf(Prefs.get_hijri_variant(ctx)) }
    var hijri_day_offset by rememberSaveable { mutableStateOf(Prefs.get_hijri_day_offset(ctx).toString()) }

    fun sync_method_fields() {
        fajr_angle_text = Prefs.get_fajr_angle(ctx).toString()
        isha_mode = Prefs.get_isha_mode(ctx)
        isha_angle_text = Prefs.get_isha_angle(ctx).toString()
        isha_fixed_minutes_text = Prefs.get_isha_fixed_minutes(ctx).toString()
    }

    fun sync_makruh_fields() {
        makruh_angle_text = Prefs.get_makruh_angle(ctx).toString()
        zawal_minutes_text = Prefs.get_zawal_minutes(ctx).toString()
    }

    fun set_custom_method() {
        if (method_preset != "custom") {
            method_preset = "custom"
            Prefs.set_method_preset(ctx, "custom")
        }
    }

    fun set_custom_makruh() {
        if (makruh_preset != "custom") {
            makruh_preset = "custom"
            Prefs.set_makruh_preset(ctx, "custom")
        }
    }

    fun refresh_host_location() {
        if (host_event_authority.isBlank()) {
            host_location_label = ctx.getString(R.string.unknown_location)
            return
        }
        HostConfigReader.clear_cache(host_event_authority)
        val label = HostConfigReader.read_config(ctx, host_event_authority)?.display_label()
        host_location_label = label ?: ctx.getString(R.string.unknown_location)
    }

    fun open_host_location_picker() {
        if (host_event_authority.isBlank()) return
        val host_package = hosts.firstOrNull { it.event_authority == host_event_authority }?.package_name
            ?: ctx.packageManager.resolveContentProvider(host_event_authority, 0)?.packageName
            ?: return
        if (host_package.isBlank()) return
        val component = ComponentName(host_package, "com.forrestguice.suntimeswidget.SuntimesActivity")
        val explicit_intent = Intent("suntimes.action.CONFIG_LOCATION")
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pm = ctx.packageManager
        try {
            ctx.startActivity(explicit_intent)
        } catch (_: ActivityNotFoundException) {
            pm.getLaunchIntentForPackage(host_package)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(ctx::startActivity)
        }
    }

    LaunchedEffect(host_event_authority) {
        refresh_host_location()
    }

    DisposableEffect(lifecycle_owner, host_event_authority) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh_host_location()
        }
        lifecycle_owner.lifecycle.addObserver(observer)
        onDispose { lifecycle_owner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier.padding(padding),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            SettingsSection(ctx.getString(R.string.general_title)) {
                SettingDropdown(
                    title = ctx.getString(R.string.language_title),
                    value_label = language_label(ctx, language),
                    selected = language,
                    options = language_options(ctx),
                    on_select = { v ->
                        language = v
                        Prefs.set_language(ctx, v)
                        val locales =
                            if (v == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(v)
                        AppCompatDelegate.setApplicationLocales(locales)
                        WidgetUpdate.request(ctx)
                        activity?.recreate()
                    }
                )

                SettingDropdown(
                    title = ctx.getString(R.string.theme_title),
                    value_label = theme_label(ctx, theme),
                    selected = theme,
                    options = theme_options(ctx),
                    on_select = { v ->
                        theme = v
                        Prefs.set_theme(ctx, v)
                        val mode =
                            when (v) {
                                Prefs.theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                                Prefs.theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            }
                        AppCompatDelegate.setDefaultNightMode(mode)
                        WidgetUpdate.request(ctx)
                        activity?.recreate()
                    }
                )

                val dynamic_ok = DynamicColors.isDynamicColorAvailable()
                SettingDropdown(
                    title = ctx.getString(R.string.palette_title),
                    value_label = palette_label(ctx, palette),
                    selected = palette,
                    options = palette_options(ctx, dynamic_ok),
                    on_select = { v ->
                        palette = v
                        Prefs.set_palette(ctx, v)
                        WidgetUpdate.request(ctx)
                        activity?.recreate()
                    }
                )

                SettingDropdown(
                    title = ctx.getString(R.string.days_month_basis_title),
                    value_label = month_basis_label(ctx, days_month_basis),
                    selected = days_month_basis,
                    options = month_basis_options(ctx),
                    on_select = { v ->
                        days_month_basis = v
                        Prefs.set_days_month_basis(ctx, v)
                        WidgetUpdate.request(ctx)
                    }
                )

                SettingDropdown(
                    title = ctx.getString(R.string.gregorian_date_format_title),
                    value_label = gregorian_date_format_label(ctx, gregorian_date_format),
                    selected = gregorian_date_format,
                    options = gregorian_date_format_options(ctx),
                    on_select = { v ->
                        gregorian_date_format = v
                        Prefs.set_gregorian_date_format(ctx, v)
                        WidgetUpdate.request(ctx)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        item {
            SettingsSection(ctx.getString(R.string.host_title)) {
                val host_options = hosts.map { it.event_authority to it.label }
                SettingDropdown(
                    title = ctx.getString(R.string.host_title),
                    subtitle = ctx.getString(R.string.host_summary),
                    value_label = hosts.firstOrNull { it.event_authority == host_event_authority }?.label ?: host_event_authority,
                    selected = host_event_authority,
                    options = host_options.ifEmpty { listOf("" to ctx.getString(R.string.no_host_found)) },
                    on_select = { v ->
                        host_event_authority = v
                        if (v.isNotBlank()) Prefs.set_host_event_authority(ctx, v)
                        WidgetUpdate.request(ctx)
                    }
                )

                SettingRow(
                    title = ctx.getString(R.string.host_location_title),
                    subtitle = host_location_label,
                    on_click = { open_host_location_picker() },
                    trailing = {
                        Text(
                            text = ctx.getString(R.string.open_in_host),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        item {
            SettingsSection(ctx.getString(R.string.method_title)) {
                SettingDropdown(
                    title = ctx.getString(R.string.method_title),
                    subtitle = ctx.getString(R.string.method_summary),
                    value_label = method_label(ctx, method_preset),
                    selected = method_preset,
                    options = method_options(ctx),
                    on_select = { v ->
                        method_preset = v
                        Prefs.set_method_preset(ctx, v)
                        if (v != "custom") Prefs.apply_method_preset(ctx, v)
                        sync_method_fields()
                    }
                )

                SettingInlineTextField(
                    title = ctx.getString(R.string.fajr_angle_title),
                    value = fajr_angle_text,
                    on_value_change = { v ->
                        fajr_angle_text = v
                        v.toDoubleOrNull()?.let { Prefs.set_fajr_angle(ctx, it) }
                        set_custom_method()
                    },
                    keyboard_type = KeyboardType.Decimal
                )

                SettingDropdown(
                    title = ctx.getString(R.string.isha_mode_title),
                    value_label = isha_mode_label(ctx, isha_mode),
                    selected = isha_mode,
                    options = isha_mode_options(ctx),
                    on_select = { v ->
                        isha_mode = v
                        Prefs.set_isha_mode(ctx, v)
                        set_custom_method()
                    }
                )

                if (isha_mode == Prefs.isha_mode_fixed) {
                    SettingInlineTextField(
                        title = ctx.getString(R.string.isha_fixed_minutes_title),
                        value = isha_fixed_minutes_text,
                        on_value_change = { v ->
                            isha_fixed_minutes_text = v
                            v.toIntOrNull()?.let { Prefs.set_isha_fixed_minutes(ctx, it) }
                            set_custom_method()
                        },
                        keyboard_type = KeyboardType.Number
                    )
                } else {
                    SettingInlineTextField(
                        title = ctx.getString(R.string.isha_angle_title),
                        value = isha_angle_text,
                        on_value_change = { v ->
                            isha_angle_text = v
                            v.toDoubleOrNull()?.let { Prefs.set_isha_angle(ctx, it) }
                            set_custom_method()
                        },
                        keyboard_type = KeyboardType.Decimal
                    )
                }

                SettingDropdown(
                    title = ctx.getString(R.string.asr_factor_title),
                    value_label = asr_factor_label(ctx, asr_factor),
                    selected = asr_factor,
                    options = asr_factor_options(ctx),
                    on_select = { v ->
                        asr_factor = v
                        Prefs.set_asr_factor(ctx, v.toIntOrNull() ?: 1)
                    }
                )

                SettingInlineTextField(
                    title = ctx.getString(R.string.maghrib_offset_title),
                    value = maghrib_offset_minutes_text,
                    on_value_change = { v ->
                        maghrib_offset_minutes_text = v
                        v.toIntOrNull()?.let { Prefs.set_maghrib_offset_minutes(ctx, it) }
                        set_custom_method()
                    },
                    keyboard_type = KeyboardType.Number
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        item {
            SettingsSection(ctx.getString(R.string.makruh_preset_title)) {
                SettingDropdown(
                    title = ctx.getString(R.string.makruh_preset_title),
                    value_label = makruh_label(ctx, makruh_preset),
                    selected = makruh_preset,
                    options = makruh_options(ctx),
                    on_select = { v ->
                        makruh_preset = v
                        Prefs.set_makruh_preset(ctx, v)
                        if (v != "custom") Prefs.apply_makruh_preset(ctx, v)
                        sync_makruh_fields()
                    }
                )

                if (makruh_preset == "custom") {
                    SettingTextField(
                        title = ctx.getString(R.string.makruh_angle_title),
                        value = makruh_angle_text,
                        on_value_change = { v ->
                            makruh_angle_text = v
                            v.toDoubleOrNull()?.let { Prefs.set_makruh_angle(ctx, it) }
                            set_custom_makruh()
                        }
                    )

                    SettingTextField(
                        title = ctx.getString(R.string.zawal_minutes_title),
                        value = zawal_minutes_text,
                        on_value_change = { v ->
                            zawal_minutes_text = v
                            v.toIntOrNull()?.let { Prefs.set_zawal_minutes(ctx, it) }
                            set_custom_makruh()
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        item {
            SettingsSection(ctx.getString(R.string.days_view_title)) {
                SettingSwitch(
                    title = ctx.getString(R.string.days_show_hijri_title),
                    checked = days_show_hijri,
                    on_checked_change = {
                        days_show_hijri = it
                        Prefs.set_days_show_hijri(ctx, it)
                    }
                )

                SettingSwitch(
                    title = ctx.getString(R.string.days_show_prohibited_title),
                    checked = days_show_prohibited,
                    on_checked_change = {
                        days_show_prohibited = it
                        Prefs.set_days_show_prohibited(ctx, it)
                    }
                )

                SettingSwitch(
                    title = ctx.getString(R.string.days_show_night_portions_title),
                    checked = days_show_night,
                    on_checked_change = {
                        days_show_night = it
                        Prefs.set_days_show_night_portions(ctx, it)
                    }
                )

                val hijri_visible = days_show_hijri || days_month_basis == Prefs.days_month_basis_hijri
                if (hijri_visible) {
                    SettingDropdown(
                        title = ctx.getString(R.string.hijri_variant_title),
                        value_label = hijri_variant_label(ctx, hijri_variant),
                        selected = hijri_variant,
                        options = hijri_variant_options(ctx),
                        on_select = { v ->
                            hijri_variant = v
                            Prefs.set_hijri_variant(ctx, v)
                        }
                    )

                    SettingDropdown(
                        title = ctx.getString(R.string.hijri_offset_title),
                        value_label = hijri_offset_label(ctx, hijri_day_offset),
                        selected = hijri_day_offset,
                        options = hijri_offset_options(ctx),
                        on_select = { v ->
                            hijri_day_offset = v
                            Prefs.set_hijri_day_offset(ctx, v.toIntOrNull() ?: 0)
                            WidgetUpdate.request(ctx)
                        }
                    )
                }
            }
        }

        item {
            SettingsSection(ctx.getString(R.string.widget_title)) {
                SettingSwitch(
                    title = ctx.getString(R.string.widget_show_prohibited_title),
                    checked = widget_show_prohibited,
                    on_checked_change = {
                        widget_show_prohibited = it
                        Prefs.set_widget_show_prohibited(ctx, it)
                        WidgetUpdate.request(ctx)
                    }
                )

                SettingSwitch(
                    title = ctx.getString(R.string.widget_show_night_portions_title),
                    checked = widget_show_night,
                    on_checked_change = {
                        widget_show_night = it
                        Prefs.set_widget_show_night_portions(ctx, it)
                        WidgetUpdate.request(ctx)
                    }
                )
            }
        }
    }
}

private fun language_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        "system" to ctx.getString(R.string.language_system),
        "en" to ctx.getString(R.string.language_english),
        "ar" to ctx.getString(R.string.language_arabic)
    )

private fun language_label(ctx: android.content.Context, v: String): String =
    language_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun theme_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        Prefs.theme_system to ctx.getString(R.string.theme_system),
        Prefs.theme_light to ctx.getString(R.string.theme_light),
        Prefs.theme_dark to ctx.getString(R.string.theme_dark)
    )

private fun theme_label(ctx: android.content.Context, v: String): String =
    theme_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun palette_options(ctx: android.content.Context, dynamic_ok: Boolean): List<Pair<String, String>> =
    buildList {
        add(Prefs.palette_parchment to ctx.getString(R.string.palette_parchment))
        if (dynamic_ok) add(Prefs.palette_dynamic to ctx.getString(R.string.palette_dynamic))
        add(Prefs.palette_sapphire to ctx.getString(R.string.palette_sapphire))
        add(Prefs.palette_rose to ctx.getString(R.string.palette_rose))
    }

private fun palette_label(ctx: android.content.Context, v: String): String =
    palette_options(ctx, true).firstOrNull { it.first == v }?.second ?: v

private fun gregorian_date_format_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        Prefs.gregorian_date_format_card to ctx.getString(R.string.gregorian_date_format_card),
        Prefs.gregorian_date_format_medium to ctx.getString(R.string.gregorian_date_format_medium),
        Prefs.gregorian_date_format_long to ctx.getString(R.string.gregorian_date_format_long)
    )

private fun gregorian_date_format_label(ctx: android.content.Context, v: String): String =
    gregorian_date_format_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun method_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        "egypt" to ctx.getString(R.string.method_egypt),
        "mwl" to ctx.getString(R.string.method_mwl),
        "karachi" to ctx.getString(R.string.method_karachi),
        "isna" to ctx.getString(R.string.method_isna),
        "uaq" to ctx.getString(R.string.method_uaq),
        "custom" to ctx.getString(R.string.method_custom)
    )

private fun method_label(ctx: android.content.Context, v: String): String =
    method_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun isha_mode_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        Prefs.isha_mode_angle to ctx.getString(R.string.isha_mode_angle),
        Prefs.isha_mode_fixed to ctx.getString(R.string.isha_mode_fixed)
    )

private fun isha_mode_label(ctx: android.content.Context, v: String): String =
    isha_mode_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun asr_factor_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        "1" to ctx.getString(R.string.asr_factor_1),
        "2" to ctx.getString(R.string.asr_factor_2)
    )

private fun asr_factor_label(ctx: android.content.Context, v: String): String =
    asr_factor_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun makruh_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        "shafi" to ctx.getString(R.string.makruh_preset_shafi),
        "hanafi" to ctx.getString(R.string.makruh_preset_hanafi),
        "custom" to ctx.getString(R.string.makruh_preset_custom)
    )

private fun makruh_label(ctx: android.content.Context, v: String): String =
    makruh_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun month_basis_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        Prefs.days_month_basis_gregorian to ctx.getString(R.string.days_month_basis_gregorian),
        Prefs.days_month_basis_hijri to ctx.getString(R.string.days_month_basis_hijri)
    )

private fun month_basis_label(ctx: android.content.Context, v: String): String =
    month_basis_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun hijri_variant_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        Prefs.hijri_variant_umalqura to ctx.getString(R.string.hijri_variant_umalqura),
        Prefs.hijri_variant_diyanet to ctx.getString(R.string.hijri_variant_diyanet)
    )

private fun hijri_variant_label(ctx: android.content.Context, v: String): String =
    hijri_variant_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun hijri_offset_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        "-2" to ctx.getString(R.string.hijri_offset_minus_2),
        "-1" to ctx.getString(R.string.hijri_offset_minus_1),
        "0" to ctx.getString(R.string.hijri_offset_0),
        "1" to ctx.getString(R.string.hijri_offset_plus_1),
        "2" to ctx.getString(R.string.hijri_offset_plus_2)
    )

private fun hijri_offset_label(ctx: android.content.Context, v: String): String =
    hijri_offset_options(ctx).firstOrNull { it.first == v }?.second ?: v
