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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.yshalsager.suntimes.prayertimesaddon.core.MethodConfig
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocation
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocations
import com.yshalsager.suntimes.prayertimesaddon.core.addon_event_title
import com.yshalsager.suntimes.prayertimesaddon.core.SettingsBackup
import com.yshalsager.suntimes.prayertimesaddon.core.app_language_locales
import com.yshalsager.suntimes.prayertimesaddon.core.current_app_language
import com.yshalsager.suntimes.prayertimesaddon.core.method_config_from_prefs
import com.yshalsager.suntimes.prayertimesaddon.core.method_config_with_preset
import com.yshalsager.suntimes.prayertimesaddon.core.addon_runtime_profile_from_prefs
import com.yshalsager.suntimes.prayertimesaddon.core.timezone_offset_mismatch_hours
import com.yshalsager.suntimes.prayertimesaddon.core.timezone_likely_mismatch
import com.yshalsager.suntimes.prayertimesaddon.core.valid_timezone_id
import com.yshalsager.suntimes.prayertimesaddon.core.open_url as open_external_url
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingDropdown
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingInlineTextField
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingRow
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingSwitch
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingTextField
import com.yshalsager.suntimes.prayertimesaddon.ui.compose.components.SettingsSection
import com.yshalsager.suntimes.prayertimesaddon.ui.SavedLocationsCardsActivity
import androidx.compose.ui.text.input.KeyboardType
import com.yshalsager.suntimes.prayertimesaddon.widget.WidgetUpdate
import com.yshalsager.suntimes.prayertimesaddon.provider.PrayerTimesProvider
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs

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

private data class SavedLocationDraft(
    val edit_id: String?,
    val label: String,
    val latitude: String,
    val longitude: String,
    val altitude: String,
    val timezone_id: String,
    val calc_mode: String,
    val method_preset: String
)

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
    var saved_locations by remember { mutableStateOf(SavedLocations.load(ctx)) }
    var editing_location by remember { mutableStateOf<SavedLocationDraft?>(null) }

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
        saved_locations = SavedLocations.load(ctx)
    }

    fun save_saved_locations(next: List<SavedLocation>) {
        SavedLocations.save(ctx, next)
        val persisted = SavedLocations.load(ctx)
        saved_locations = persisted
        val selected_source = Prefs.get_home_location_source(ctx)
        if (selected_source == SavedLocations.home_source_saved) {
            val selected_id = Prefs.get_home_location_id(ctx)
            if (selected_id.isBlank() || persisted.none { it.id == selected_id }) {
                Prefs.set_home_location_source(ctx, SavedLocations.home_source_host)
                Prefs.set_home_location_id(ctx, "")
            }
        }
        WidgetUpdate.request(ctx)
    }

    fun move_saved_location(index: Int, delta: Int) {
        val target = index + delta
        if (index !in saved_locations.indices || target !in saved_locations.indices) return
        val next = saved_locations.toMutableList()
        val item = next.removeAt(index)
        next.add(target, item)
        save_saved_locations(next)
    }

    fun remove_saved_location(id: String) {
        save_saved_locations(saved_locations.filterNot { it.id == id })
    }

    fun open_edit_location(location: SavedLocation?) {
        val method_defaults = method_config_from_prefs(ctx)
        editing_location =
            SavedLocationDraft(
                edit_id = location?.id,
                label = location?.label.orEmpty(),
                latitude = location?.latitude.orEmpty(),
                longitude = location?.longitude.orEmpty(),
                altitude = location?.altitude.orEmpty(),
                timezone_id = location?.timezone_id ?: TimeZone.getDefault().id,
                calc_mode = location?.calc_mode ?: SavedLocations.calc_mode_inherit_global,
                method_preset = location?.method_preset ?: method_defaults.method_preset
            )
    }

    fun add_host_location() {
        if (saved_locations.size >= SavedLocations.max_count) return
        val added = SavedLocations.create_from_host(ctx, host_event_authority)
        if (added == null) {
            Toast.makeText(ctx, ctx.getString(R.string.saved_locations_import_failed), Toast.LENGTH_SHORT).show()
            return
        }
        save_saved_locations(saved_locations + added)
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

    fun open_saved_locations_cards() {
        ctx.startActivity(Intent(ctx, SavedLocationsCardsActivity::class.java))
    }

    @Composable
    fun saved_location_add_action(can_add_saved: Boolean) {
        Text(
            text = ctx.getString(R.string.saved_locations_add_action),
            style = MaterialTheme.typography.bodySmall,
            color = if (can_add_saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
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

    editing_location?.let { draft ->
        val can_save =
            SavedLocations.is_valid_lat(draft.latitude) &&
                SavedLocations.is_valid_lon(draft.longitude) &&
                (draft.altitude.isBlank() || SavedLocations.is_valid_alt(draft.altitude)) &&
                draft.timezone_id.isNotBlank() &&
                valid_timezone_id(draft.timezone_id) != null
        SavedLocationEditorDialog(
            draft = draft,
            is_edit = draft.edit_id != null,
            on_dismiss = { editing_location = null },
            on_change = { editing_location = it },
            on_save = on_save@{ edited ->
                val previous = saved_locations.firstOrNull { it.id == edited.edit_id }
                val base_method = previous?.method_config() ?: method_config_from_prefs(ctx)
                val runtime_profile = previous?.let {
                    SavedLocations.addon_runtime_profile_for_location(ctx, it) ?: addon_runtime_profile_from_prefs(ctx)
                } ?: addon_runtime_profile_from_prefs(ctx)
                val method =
                    if (edited.calc_mode == SavedLocations.calc_mode_custom) {
                        method_config_with_preset(base_method, edited.method_preset)
                    } else {
                        base_method
                    }
                val updated =
                    SavedLocation(
                        id = edited.edit_id ?: UUID.randomUUID().toString(),
                        label = edited.label.trim(),
                        latitude = edited.latitude.trim(),
                        longitude = edited.longitude.trim(),
                        altitude = edited.altitude.trim().ifBlank { null },
                        timezone_id = edited.timezone_id.trim(),
                        calc_mode = edited.calc_mode,
                        hijri_variant = runtime_profile.hijri_variant,
                        hijri_day_offset = runtime_profile.hijri_day_offset,
                        method_preset = method.method_preset,
                        method_fajr_angle = method.fajr_angle,
                        method_isha_mode = method.isha_mode,
                        method_isha_angle = method.isha_angle,
                        method_isha_fixed_minutes = method.isha_fixed_minutes,
                        method_asr_factor = method.asr_factor,
                        method_maghrib_offset_minutes = method.maghrib_offset_minutes,
                        method_makruh_angle = method.makruh_angle,
                        method_makruh_sunrise_minutes = method.makruh_sunrise_minutes,
                        method_zawal_minutes = method.zawal_minutes,
                        extra_fajr_1_enabled = runtime_profile.extra_fajr_1_enabled,
                        extra_fajr_1_angle = runtime_profile.extra_fajr_1_angle,
                        extra_fajr_1_label_raw = runtime_profile.extra_fajr_1_label_raw,
                        extra_isha_1_enabled = runtime_profile.extra_isha_1_enabled,
                        extra_isha_1_angle = runtime_profile.extra_isha_1_angle,
                        extra_isha_1_label_raw = runtime_profile.extra_isha_1_label_raw
                    )
                val next =
                    if (edited.edit_id == null) {
                        if (saved_locations.size >= SavedLocations.max_count) {
                            editing_location = null
                            return@on_save
                        }
                        saved_locations + updated
                    } else {
                        saved_locations.map { if (it.id == edited.edit_id) updated else it }
                    }
                save_saved_locations(next)
                editing_location = null
            },
            can_save = can_save
        )
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
            val can_add_saved = saved_locations.size < SavedLocations.max_count
            SettingsSection(ctx.getString(R.string.saved_locations_title)) {
                SettingRow(
                    title = ctx.getString(R.string.saved_locations_import_host_title),
                    subtitle = if (can_add_saved) host_location_label else ctx.getString(R.string.saved_locations_limit_reached, SavedLocations.max_count),
                    on_click = if (can_add_saved) ({ add_host_location() }) else null,
                    trailing = { saved_location_add_action(can_add_saved) }
                )

                SettingRow(
                    title = ctx.getString(R.string.saved_locations_add_manual_title),
                    subtitle = ctx.getString(R.string.saved_locations_add_manual_summary),
                    on_click = if (can_add_saved) ({ open_edit_location(null) }) else null,
                    trailing = { saved_location_add_action(can_add_saved) }
                )

                SettingRow(
                    title = ctx.getString(R.string.saved_locations_cards_title),
                    subtitle = ctx.getString(R.string.saved_locations_cards_subtitle),
                    on_click = { open_saved_locations_cards() },
                    trailing = {
                        Text(
                            text = ctx.getString(R.string.open_in_host),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                if (saved_locations.isEmpty()) {
                    SettingRow(
                        title = ctx.getString(R.string.saved_locations_empty_title),
                        subtitle = ctx.getString(R.string.saved_locations_empty_summary)
                    )
                } else {
                    saved_locations.forEachIndexed { index, location ->
                        val subtitle =
                            buildString {
                                append(location.latitude)
                                append(", ")
                                append(location.longitude)
                                location.altitude?.takeIf { it.isNotBlank() }?.let { append(" | "); append(it) }
                                append(" | ")
                                append(location.timezone_id)
                                append(" | ")
                                append(calc_mode_label(ctx, location.calc_mode, location.method_preset))
                            }

                        SettingRow(
                            title = location.display_label(),
                            subtitle = subtitle,
                            on_click = { open_edit_location(location) },
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (index > 0) {
                                        Text(
                                            text = ctx.getString(R.string.saved_locations_move_up_action),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { move_saved_location(index, -1) }
                                        )
                                    }
                                    if (index < saved_locations.lastIndex) {
                                        Text(
                                            text = ctx.getString(R.string.saved_locations_move_down_action),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { move_saved_location(index, 1) }
                                        )
                                    }
                                    Text(
                                        text = ctx.getString(R.string.saved_locations_delete_action),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { remove_saved_location(location.id) }
                                    )
                                }
                            }
                        )
                    }
                }
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

@Composable
private fun SavedLocationEditorDialog(
    draft: SavedLocationDraft,
    is_edit: Boolean,
    can_save: Boolean,
    on_dismiss: () -> Unit,
    on_change: (SavedLocationDraft) -> Unit,
    on_save: (SavedLocationDraft) -> Unit
) {
    val ctx = LocalContext.current
    var show_timezone_picker by rememberSaveable { mutableStateOf(false) }
    val timezone_mismatch_hours = timezone_offset_mismatch_hours(draft.longitude, draft.timezone_id)
    val show_timezone_warning = timezone_likely_mismatch(draft.longitude, draft.timezone_id)
    AlertDialog(
        onDismissRequest = on_dismiss,
        title = {
            Text(
                text =
                    if (is_edit) ctx.getString(R.string.saved_locations_edit_title)
                    else ctx.getString(R.string.saved_locations_add_manual_title)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.label,
                    onValueChange = { on_change(draft.copy(label = it)) },
                    singleLine = true,
                    label = { Text(ctx.getString(R.string.saved_locations_field_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.latitude,
                    onValueChange = { on_change(draft.copy(latitude = it)) },
                    singleLine = true,
                    label = { Text(ctx.getString(R.string.saved_locations_field_latitude)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.longitude,
                    onValueChange = { on_change(draft.copy(longitude = it)) },
                    singleLine = true,
                    label = { Text(ctx.getString(R.string.saved_locations_field_longitude)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.altitude,
                    onValueChange = { on_change(draft.copy(altitude = it)) },
                    singleLine = true,
                    label = { Text(ctx.getString(R.string.saved_locations_field_altitude)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.timezone_id,
                    onValueChange = { on_change(draft.copy(timezone_id = it)) },
                    singleLine = true,
                    label = { Text(ctx.getString(R.string.saved_locations_field_timezone)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { show_timezone_picker = true }) {
                        Text(ctx.getString(R.string.saved_locations_timezone_pick_action))
                    }
                    TextButton(onClick = { on_change(draft.copy(timezone_id = TimeZone.getDefault().id)) }) {
                        Text(ctx.getString(R.string.saved_locations_timezone_device_action))
                    }
                }
                if (show_timezone_warning) {
                    Text(
                        text = ctx.getString(R.string.saved_locations_timezone_mismatch_warning, timezone_mismatch_hours ?: 0.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                DialogDropdownField(
                    label = ctx.getString(R.string.saved_locations_field_calc_mode),
                    value_label = calc_mode_label(ctx, draft.calc_mode, draft.method_preset),
                    selected = calc_mode_selected_value(draft.calc_mode, draft.method_preset),
                    options = calc_mode_options(ctx),
                    on_select = {
                        val preset = calc_mode_preset_from_value(it)
                        if (preset == null) {
                            on_change(draft.copy(calc_mode = SavedLocations.calc_mode_inherit_global))
                        } else {
                            on_change(
                                draft.copy(
                                    calc_mode = SavedLocations.calc_mode_custom,
                                    method_preset = preset
                                )
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { on_save(draft) }, enabled = can_save) {
                Text(ctx.getString(R.string.saved_locations_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = on_dismiss) {
                Text(ctx.getString(android.R.string.cancel))
            }
        }
    )

    if (show_timezone_picker) {
        TimezonePickerDialog(
            selected_id = draft.timezone_id,
            on_select = {
                on_change(draft.copy(timezone_id = it))
                show_timezone_picker = false
            },
            on_dismiss = { show_timezone_picker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogDropdownField(
    label: String,
    value_label: String,
    selected: String,
    options: List<Pair<String, String>>,
    on_select: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value_label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, option_label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option_label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        if (value != selected) on_select(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun TimezonePickerDialog(
    selected_id: String,
    on_select: (String) -> Unit,
    on_dismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val all_ids = remember { TimeZone.getAvailableIDs().sorted() }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered =
        remember(query, selected_id, all_ids) {
            val q = query.trim()
            val source = if (q.isBlank()) all_ids else all_ids.filter { it.contains(q, ignoreCase = true) }
            val promoted =
                if (selected_id.isNotBlank() && source.contains(selected_id)) {
                    listOf(selected_id) + source.filterNot { it == selected_id }
                } else {
                    source
                }
            promoted.take(120)
        }

    AlertDialog(
        onDismissRequest = on_dismiss,
        title = { Text(ctx.getString(R.string.saved_locations_timezone_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(ctx.getString(R.string.saved_locations_timezone_search_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered) { timezone_id ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { on_select(timezone_id) }
                                    .padding(horizontal = 2.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = timezone_id,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = timezone_offset_label(timezone_id),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (timezone_id == selected_id) {
                                Text(
                                    text = ctx.getString(R.string.saved_locations_timezone_current_tag),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                text = ctx.getString(R.string.saved_locations_timezone_no_results),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = on_dismiss) {
                Text(ctx.getString(android.R.string.cancel))
            }
        }
    )
}

private fun timezone_offset_label(timezone_id: String): String {
    val tz = TimeZone.getTimeZone(timezone_id)
    val total_minutes = tz.getOffset(System.currentTimeMillis()) / 60_000
    val sign = if (total_minutes >= 0) "+" else "-"
    val abs_minutes = abs(total_minutes)
    val hours = abs_minutes / 60
    val minutes = abs_minutes % 60
    return String.format(Locale.US, "UTC%s%02d:%02d", sign, hours, minutes)
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
        "uiof" to ctx.getString(R.string.method_uiof),
        "custom" to ctx.getString(R.string.method_custom)
    )

private fun method_label(ctx: android.content.Context, v: String): String =
    method_options(ctx).firstOrNull { it.first == v }?.second ?: v

private fun calc_mode_options(ctx: android.content.Context): List<Pair<String, String>> =
    buildList {
        add(SavedLocations.calc_mode_inherit_global to ctx.getString(R.string.saved_locations_calc_mode_inherit))
        MethodConfig.supported_presets.forEach { preset ->
            add(calc_mode_value_for_preset(preset) to method_label(ctx, preset))
        }
    }

private fun calc_mode_label(ctx: android.content.Context, calc_mode: String, method_preset: String): String =
    when (calc_mode) {
        SavedLocations.calc_mode_custom -> method_label(ctx, method_preset)
        else -> ctx.getString(R.string.saved_locations_calc_mode_inherit)
    }

private fun calc_mode_value_for_preset(preset: String): String = "preset:$preset"

private fun calc_mode_preset_from_value(value: String): String? {
    if (!value.startsWith("preset:")) return null
    return value.removePrefix("preset:").takeIf { it in MethodConfig.supported_presets }
}

private fun calc_mode_selected_value(calc_mode: String, method_preset: String): String {
    return if (calc_mode == SavedLocations.calc_mode_custom) {
        calc_mode_value_for_preset(method_preset)
    } else {
        SavedLocations.calc_mode_inherit_global
    }
}

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
