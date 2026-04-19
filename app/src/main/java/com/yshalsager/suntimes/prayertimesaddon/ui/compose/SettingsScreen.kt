@file:android.annotation.SuppressLint("LocalContextGetResourceValueCall")

package com.yshalsager.suntimes.prayertimesaddon.ui.compose

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.material.color.DynamicColors
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.AddonEvent
import com.yshalsager.suntimes.prayertimesaddon.core.AlarmEventContract
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.addon_event_title
import com.yshalsager.suntimes.prayertimesaddon.core.SettingsBackup
import com.yshalsager.suntimes.prayertimesaddon.core.app_language_locales
import com.yshalsager.suntimes.prayertimesaddon.core.current_app_language
import com.yshalsager.suntimes.prayertimesaddon.core.open_url as open_external_url
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingDropdown
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingInlineTextField
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingRow
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingSwitch
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingTextField
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingsSection
import androidx.compose.ui.text.input.KeyboardType
import com.yshalsager.suntimes.prayertimesaddon.widget.WidgetUpdate
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    var language by rememberSaveable { mutableStateOf(current_app_language()) }
    var theme by rememberSaveable { mutableStateOf(Prefs.get_theme(ctx)) }
    var palette by rememberSaveable { mutableStateOf(Prefs.get_palette(ctx)) }
    var gregorian_date_format by rememberSaveable { mutableStateOf(Prefs.get_gregorian_date_format(ctx)) }

    var method_preset by rememberSaveable { mutableStateOf(Prefs.get_method_preset(ctx)) }
    var fajr_angle_text by rememberSaveable { mutableStateOf(Prefs.get_fajr_angle(ctx).toString()) }
    var extra_fajr_1_enabled by rememberSaveable { mutableStateOf(Prefs.get_extra_fajr_1_enabled(ctx)) }
    var extra_fajr_1_angle_text by rememberSaveable { mutableStateOf(Prefs.get_extra_fajr_1_angle(ctx).toString()) }
    var extra_fajr_1_label_text by rememberSaveable { mutableStateOf(Prefs.get_extra_fajr_1_label(ctx)) }
    var isha_mode by rememberSaveable { mutableStateOf(Prefs.get_isha_mode(ctx)) }
    var isha_angle_text by rememberSaveable { mutableStateOf(Prefs.get_isha_angle(ctx).toString()) }
    var isha_fixed_minutes_text by rememberSaveable { mutableStateOf(Prefs.get_isha_fixed_minutes(ctx).toString()) }
    var extra_isha_1_enabled by rememberSaveable { mutableStateOf(Prefs.get_extra_isha_1_enabled(ctx)) }
    var extra_isha_1_angle_text by rememberSaveable { mutableStateOf(Prefs.get_extra_isha_1_angle(ctx).toString()) }
    var extra_isha_1_label_text by rememberSaveable { mutableStateOf(Prefs.get_extra_isha_1_label(ctx)) }
    var asr_factor by rememberSaveable { mutableStateOf(Prefs.get_asr_factor(ctx).toString()) }
    var maghrib_offset_minutes_text by rememberSaveable { mutableStateOf(Prefs.get_maghrib_offset_minutes(ctx).toString()) }

    var makruh_preset by rememberSaveable { mutableStateOf(Prefs.get_makruh_preset(ctx)) }
    var makruh_sunrise_minutes by rememberSaveable { mutableStateOf(Prefs.get_makruh_sunrise_minutes(ctx).toString()) }
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

    fun reload_state_from_prefs() {
        host_event_authority = Prefs.get_host_event_authority(ctx) ?: HostResolver.ensure_default_selected(ctx) ?: ""
        language = current_app_language()
        theme = Prefs.get_theme(ctx)
        palette = Prefs.get_palette(ctx)
        gregorian_date_format = Prefs.get_gregorian_date_format(ctx)
        method_preset = Prefs.get_method_preset(ctx)
        fajr_angle_text = Prefs.get_fajr_angle(ctx).toString()
        extra_fajr_1_enabled = Prefs.get_extra_fajr_1_enabled(ctx)
        extra_fajr_1_angle_text = Prefs.get_extra_fajr_1_angle(ctx).toString()
        extra_fajr_1_label_text = Prefs.get_extra_fajr_1_label(ctx)
        isha_mode = Prefs.get_isha_mode(ctx)
        isha_angle_text = Prefs.get_isha_angle(ctx).toString()
        isha_fixed_minutes_text = Prefs.get_isha_fixed_minutes(ctx).toString()
        extra_isha_1_enabled = Prefs.get_extra_isha_1_enabled(ctx)
        extra_isha_1_angle_text = Prefs.get_extra_isha_1_angle(ctx).toString()
        extra_isha_1_label_text = Prefs.get_extra_isha_1_label(ctx)
        asr_factor = Prefs.get_asr_factor(ctx).toString()
        maghrib_offset_minutes_text = Prefs.get_maghrib_offset_minutes(ctx).toString()
        makruh_preset = Prefs.get_makruh_preset(ctx)
        makruh_sunrise_minutes = Prefs.get_makruh_sunrise_minutes(ctx).toString()
        makruh_angle_text = Prefs.get_makruh_angle(ctx).toString()
        zawal_minutes_text = Prefs.get_zawal_minutes(ctx).toString()
        days_month_basis = Prefs.get_days_month_basis(ctx)
        days_show_hijri = Prefs.get_days_show_hijri(ctx)
        days_show_prohibited = Prefs.get_days_show_prohibited(ctx)
        days_show_night = Prefs.get_days_show_night_portions(ctx)
        widget_show_prohibited = Prefs.get_widget_show_prohibited(ctx)
        widget_show_night = Prefs.get_widget_show_night_portions(ctx)
        hijri_variant = Prefs.get_hijri_variant(ctx)
        hijri_day_offset = Prefs.get_hijri_day_offset(ctx).toString()
    }

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

    fun selected_host_package(): String? =
        hosts.firstOrNull { it.event_authority == host_event_authority }?.package_name
            ?: ctx.packageManager.resolveContentProvider(host_event_authority, 0)?.packageName

    fun open_host_location_picker() {
        if (host_event_authority.isBlank()) return
        val host_package = selected_host_package() ?: return
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

    fun open_host_alarms() {
        if (host_event_authority.isBlank()) return
        val host_package = selected_host_package() ?: return
        val pm = ctx.packageManager
        val explicit_intent = Intent("android.intent.action.SHOW_ALARMS")
        explicit_intent.component = ComponentName(host_package, "com.forrestguice.suntimeswidget.alarmclock.ui.AlarmClockActivity")
        explicit_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(explicit_intent)
        } catch (_: ActivityNotFoundException) {
            try {
                val fallback_intent = Intent("android.intent.action.SHOW_ALARMS")
                fallback_intent.setPackage(host_package)
                fallback_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(fallback_intent)
            } catch (_: ActivityNotFoundException) {
                pm.getLaunchIntentForPackage(host_package)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(ctx::startActivity)
            }
        }
    }

    fun open_url(url: String) {
        open_external_url(ctx, url)
    }

    val create_preset_file_launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = write_prayer_alarm_preset(ctx, uri)
        val msg = if (ok) ctx.getString(R.string.alarm_preset_export_success) else ctx.getString(R.string.alarm_preset_export_failed)
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    val create_settings_backup_launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = write_settings_backup(ctx, uri)
        val msg = if (ok) ctx.getString(R.string.settings_backup_export_success) else ctx.getString(R.string.settings_backup_export_failed)
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    val open_settings_backup_launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val raw = read_settings_backup(ctx, uri)
        if (raw == null) {
            Toast.makeText(ctx, ctx.getString(R.string.settings_backup_restore_failed), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val result = SettingsBackup.import_json(ctx, raw)
        if (!result.ok) {
            Toast.makeText(ctx, ctx.getString(R.string.settings_backup_restore_failed), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        if (result.applied_count > 0) {
            reload_state_from_prefs()
            refresh_host_location()
            AppCompatDelegate.setApplicationLocales(app_language_locales(language))
            val mode =
                when (theme) {
                    Prefs.theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                    Prefs.theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            AppCompatDelegate.setDefaultNightMode(mode)
            WidgetUpdate.request(ctx)
        }
        val msg = ctx.getString(R.string.settings_backup_restore_success, result.applied_count)
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        if (result.applied_count > 0) activity?.recreate()
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
                        AppCompatDelegate.setApplicationLocales(app_language_locales(v))
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

                SettingRow(
                    title = ctx.getString(R.string.host_alarms_title),
                    subtitle = ctx.getString(R.string.host_alarms_summary),
                    on_click = { open_host_alarms() },
                    trailing = {
                        Text(
                            text = ctx.getString(R.string.open_in_host),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                SettingRow(
                    title = ctx.getString(R.string.export_prayer_alarm_preset_title),
                    subtitle = ctx.getString(R.string.export_prayer_alarm_preset_summary),
                    on_click = { create_preset_file_launcher.launch(ctx.getString(R.string.prayer_alarm_preset_filename)) },
                    trailing = {
                        Text(
                            text = ctx.getString(R.string.export_label),
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

                SettingSwitch(
                    title = ctx.getString(R.string.extra_fajr_1_enabled_title),
                    checked = extra_fajr_1_enabled,
                    on_checked_change = {
                        extra_fajr_1_enabled = it
                        Prefs.set_extra_fajr_1_enabled(ctx, it)
                    }
                )

                if (extra_fajr_1_enabled) {
                    SettingInlineTextField(
                        title = ctx.getString(R.string.extra_fajr_1_angle_title),
                        value = extra_fajr_1_angle_text,
                        on_value_change = { v ->
                            extra_fajr_1_angle_text = v
                            v.toDoubleOrNull()?.let { Prefs.set_extra_fajr_1_angle(ctx, it) }
                        },
                        keyboard_type = KeyboardType.Decimal
                    )

                    SettingTextField(
                        title = ctx.getString(R.string.extra_fajr_1_label_title),
                        value = extra_fajr_1_label_text,
                        on_value_change = { v ->
                            extra_fajr_1_label_text = v
                            Prefs.set_extra_fajr_1_label(ctx, v)
                        }
                    )
                }

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

                SettingSwitch(
                    title = ctx.getString(R.string.extra_isha_1_enabled_title),
                    checked = extra_isha_1_enabled,
                    on_checked_change = {
                        extra_isha_1_enabled = it
                        Prefs.set_extra_isha_1_enabled(ctx, it)
                    }
                )

                if (extra_isha_1_enabled) {
                    SettingInlineTextField(
                        title = ctx.getString(R.string.extra_isha_1_angle_title),
                        value = extra_isha_1_angle_text,
                        on_value_change = { v ->
                            extra_isha_1_angle_text = v
                            v.toDoubleOrNull()?.let { Prefs.set_extra_isha_1_angle(ctx, it) }
                        },
                        keyboard_type = KeyboardType.Decimal
                    )

                    SettingTextField(
                        title = ctx.getString(R.string.extra_isha_1_label_title),
                        value = extra_isha_1_label_text,
                        on_value_change = { v ->
                            extra_isha_1_label_text = v
                            Prefs.set_extra_isha_1_label(ctx, v)
                        }
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

                SettingDropdown(
                    title = ctx.getString(R.string.makruh_sunrise_minutes_title),
                    value_label = makruh_sunrise_minutes_label(ctx, makruh_sunrise_minutes),
                    selected = makruh_sunrise_minutes,
                    options = makruh_sunrise_minutes_options(ctx),
                    on_select = { v ->
                        makruh_sunrise_minutes = v
                        Prefs.set_makruh_sunrise_minutes(ctx, v.toIntOrNull() ?: 15)
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

        item { Spacer(Modifier.height(12.dp)) }

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

        item { Spacer(Modifier.height(12.dp)) }

        item {
            SettingsSection(ctx.getString(R.string.settings_backup_restore_title)) {
                SettingRow(
                    title = ctx.getString(R.string.settings_backup_title),
                    subtitle = ctx.getString(R.string.settings_backup_summary),
                    on_click = { create_settings_backup_launcher.launch(settings_backup_filename(ctx)) },
                    trailing = {
                        Text(
                            text = ctx.getString(R.string.export_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                SettingRow(
                    title = ctx.getString(R.string.settings_restore_title),
                    subtitle = ctx.getString(R.string.settings_restore_summary),
                    on_click = { open_settings_backup_launcher.launch(arrayOf("application/json", "text/plain")) },
                    trailing = {
                        Text(
                            text = ctx.getString(R.string.import_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            SettingsSection(ctx.getString(R.string.about_title)) {
                SettingRow(
                    title = ctx.getString(R.string.about_version_title),
                    subtitle = app_version_label(ctx)
                )

                SettingRow(
                    title = ctx.getString(R.string.about_contribute_title),
                    subtitle = ctx.getString(R.string.about_contribute_summary),
                    on_click = { open_url(ctx.getString(R.string.about_contribute_url)) },
                    trailing = {
                        Text(
                            text = ctx.getString(R.string.open_in_host),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                SettingRow(
                    title = ctx.getString(R.string.about_bugs_title),
                    subtitle = ctx.getString(R.string.about_bugs_summary),
                    on_click = { open_url(ctx.getString(R.string.about_bugs_url)) },
                    trailing = {
                        Text(
                            text = ctx.getString(R.string.open_in_host),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                SettingRow(
                    title = ctx.getString(R.string.about_support_title),
                    subtitle = ctx.getString(R.string.about_support_summary),
                    on_click = { open_url(ctx.getString(R.string.about_support_url)) },
                    trailing = {
                        Text(
                            text = ctx.getString(R.string.open_in_host),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    }
}

private fun app_version_label(ctx: android.content.Context): String {
    val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    val version_code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    return "v${info.versionName} ($version_code)"
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

private fun makruh_sunrise_minutes_options(ctx: android.content.Context): List<Pair<String, String>> =
    listOf(
        "10" to ctx.getString(R.string.makruh_sunrise_minutes_10),
        "15" to ctx.getString(R.string.makruh_sunrise_minutes_15),
        "20" to ctx.getString(R.string.makruh_sunrise_minutes_20)
    )

private fun makruh_sunrise_minutes_label(ctx: android.content.Context, v: String): String =
    makruh_sunrise_minutes_options(ctx).firstOrNull { it.first == v }?.second ?: v

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

private fun write_prayer_alarm_preset(ctx: android.content.Context, uri: Uri): Boolean {
    return write_text_to_uri(ctx, uri, prayer_alarm_preset_json(ctx))
}

private fun write_settings_backup(ctx: android.content.Context, uri: Uri): Boolean {
    return write_text_to_uri(ctx, uri, SettingsBackup.export_json(ctx))
}

private fun write_text_to_uri(ctx: android.content.Context, uri: Uri, text: String): Boolean {
    val out = ctx.contentResolver.openOutputStream(uri) ?: return false
    return try {
        out.bufferedWriter().use { it.write(text) }
        true
    } catch (_: Exception) {
        false
    }
}

private fun read_settings_backup(ctx: android.content.Context, uri: Uri): String? {
    val input = ctx.contentResolver.openInputStream(uri) ?: return null
    return try {
        input.bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    }
}

private fun settings_backup_filename(ctx: android.content.Context): String {
    val base = ctx.getString(R.string.settings_backup_filename)
    val ts = SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(Date())
    return "$base-$ts.json"
}

private fun prayer_alarm_preset_json(ctx: android.content.Context): String {
    val repeat_days = "[1,2,3,4,5,6,7]"
    val events = buildList {
        addAll(
            listOf(
            AddonEvent.prayer_fajr to ctx.getString(R.string.event_prayer_fajr),
            AddonEvent.prayer_duha to ctx.getString(R.string.event_prayer_duha),
            AddonEvent.prayer_dhuhr to ctx.getString(R.string.event_prayer_dhuhr),
            AddonEvent.prayer_asr to ctx.getString(R.string.event_prayer_asr),
            AddonEvent.prayer_maghrib to ctx.getString(R.string.event_prayer_maghrib),
            AddonEvent.prayer_isha to ctx.getString(R.string.event_prayer_isha)
            )
        )
        if (Prefs.get_extra_fajr_1_enabled(ctx)) {
            add(AddonEvent.prayer_fajr_extra_1 to addon_event_title(ctx, AddonEvent.prayer_fajr_extra_1))
        }
        if (Prefs.get_extra_isha_1_enabled(ctx)) {
            add(AddonEvent.prayer_isha_extra_1 to addon_event_title(ctx, AddonEvent.prayer_isha_extra_1))
        }
    }

    val arr = JSONArray()
    events.forEach { (event, label) ->
        val event_uri = "content://${PrayerTimesProvider.authority}/${AlarmEventContract.query_event_info}/${event.event_id}"
        arr.put(
            JSONObject()
                .put("alarmType", "ALARM")
                .put("enabled", 1)
                .put("alarmlabel", label)
                .put("repeating", 1)
                .put("repeatdays", repeat_days)
                .put("datetime", -1)
                .put("alarmtime", -1)
                .put("hour", -1)
                .put("minute", -1)
                .put("timeoffset", 0)
                .put("event", event_uri)
                .put("vibrate", 1)
        )
    }
    return arr.toString()
}
