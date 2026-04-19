package com.yshalsager.suntimes.prayertimesaddon.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.HomeSelectedLocation
import com.yshalsager.suntimes.prayertimesaddon.core.HostConfigReader
import com.yshalsager.suntimes.prayertimesaddon.core.HostResolver
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs
import com.yshalsager.suntimes.prayertimesaddon.core.SavedLocations
import com.yshalsager.suntimes.prayertimesaddon.core.addon_runtime_profile_from_prefs
import com.yshalsager.suntimes.prayertimesaddon.core.add_days
import com.yshalsager.suntimes.prayertimesaddon.core.DayItem
import com.yshalsager.suntimes.prayertimesaddon.core.MonthSkeleton
import com.yshalsager.suntimes.prayertimesaddon.core.build_day_item
import com.yshalsager.suntimes.prayertimesaddon.core.build_month_skeleton
import com.yshalsager.suntimes.prayertimesaddon.core.format_method_summary
import com.yshalsager.suntimes.prayertimesaddon.core.resolve_selected_home_location
import com.yshalsager.suntimes.prayertimesaddon.core.valid_timezone_id
import java.util.TimeZone
import java.util.Collections
import java.util.concurrent.Executors

data class DaysUiState(
    val loading: Boolean = false,
    val title: String = "",
    val subtitle: String? = null,
    val skeleton: MonthSkeleton? = null,
    val error: String? = null,
    val required_permission: String? = null,
    val show_reinstall_addon: Boolean = false
)

internal fun build_days_sig(
    ctx: Context,
    host: String,
    month_anchor: Long?,
    show_prohibited: Boolean,
    show_night: Boolean,
    show_hijri_effective: Boolean,
    month_basis: String,
    runtime_profile: com.yshalsager.suntimes.prayertimesaddon.core.AddonRuntimeProfile,
    selected_location_sig: String
): String =
    listOf(
        host,
        selected_location_sig,
        month_anchor?.toString() ?: "",
        show_prohibited.toString(),
        show_night.toString(),
        show_hijri_effective.toString(),
        month_basis,
        runtime_profile.hijri_variant,
        runtime_profile.hijri_day_offset.toString(),
        Prefs.get_gregorian_date_format(ctx),
        Prefs.get_method_preset(ctx),
        Prefs.get_fajr_angle(ctx).toString(),
        runtime_profile.extra_fajr_1_enabled.toString(),
        runtime_profile.extra_fajr_1_angle.toString(),
        runtime_profile.extra_fajr_1_label(ctx),
        Prefs.get_isha_mode(ctx),
        Prefs.get_isha_angle(ctx).toString(),
        Prefs.get_isha_fixed_minutes(ctx).toString(),
        runtime_profile.extra_isha_1_enabled.toString(),
        runtime_profile.extra_isha_1_angle.toString(),
        runtime_profile.extra_isha_1_label(ctx),
        Prefs.get_asr_factor(ctx).toString(),
        Prefs.get_maghrib_offset_minutes(ctx).toString(),
        Prefs.get_makruh_angle(ctx).toString(),
        Prefs.get_makruh_sunrise_minutes(ctx).toString(),
        Prefs.get_zawal_minutes(ctx).toString()
    ).joinToString("|")

class DaysViewModel(app: Application) : AndroidViewModel(app) {
    private val main = Handler(Looper.getMainLooper())
    private var load_id = 0
    private var last_sig: String? = null

    var month_anchor: Long? = null
        private set

    var month_start: Long? = null
        private set

    var month_end: Long? = null
        private set

    val day_cache = mutableStateMapOf<Long, DayItem>()
    private var day_inflight = Collections.synchronizedSet(HashSet<Long>())
    private val workers = Executors.newFixedThreadPool(3)

    private var loaded_host: String? = null
    private var loaded_show_prohibited = false
    private var loaded_show_night = false
    private var loaded_selected_location: HomeSelectedLocation? = null

    var state by mutableStateOf(DaysUiState())
        private set

    fun load(force: Boolean = false) {
        val ctx = getApplication<Application>().applicationContext
        val host = HostResolver.ensure_default_selected(ctx)
        if (host == null) {
            state = DaysUiState(error = ctx.getString(R.string.no_host_found))
            return
        }

        val required_perm = HostResolver.get_required_permission(ctx, host)
        if (required_perm != null && ContextCompat.checkSelfPermission(ctx, required_perm) != PackageManager.PERMISSION_GRANTED) {
            val requestable = required_perm.takeIf { HostResolver.is_runtime_permission(ctx, it) }
            val message =
                if (requestable != null) ctx.getString(R.string.missing_permission, required_perm)
                else ctx.getString(R.string.missing_permission_reinstall, required_perm)
            state = DaysUiState(error = message, required_permission = requestable, show_reinstall_addon = requestable == null)
            return
        }

        val show_prohibited = Prefs.get_days_show_prohibited(ctx)
        val show_night = Prefs.get_days_show_night_portions(ctx)
        val show_hijri = Prefs.get_days_show_hijri(ctx)
        val month_basis = Prefs.get_days_month_basis(ctx)

        val show_hijri_effective = show_hijri || month_basis == Prefs.days_month_basis_hijri
        val host_config = HostConfigReader.read_config(ctx, host)
        val host_label = host_config?.display_label() ?: "--"
        val host_timezone_id = valid_timezone_id(host_config?.timezone) ?: TimeZone.getDefault().id
        val saved_locations = SavedLocations.load(ctx)
        val selected_location = resolve_selected_home_location(ctx, host_label, host_timezone_id, saved_locations)
        val runtime_profile = selected_location.addon_runtime_profile_override ?: addon_runtime_profile_from_prefs(ctx)
        val subtitle = ctx.getString(
            R.string.days_location_context,
            selected_location.label,
            format_method_summary(ctx, selected_location.method_config_override)
        )
        val sig =
            build_days_sig(
                ctx,
                host,
                month_anchor,
                show_prohibited,
                show_night,
                show_hijri_effective,
                month_basis,
                runtime_profile,
                listOf(
                    selected_location.key,
                    selected_location.timezone_id,
                    selected_location.saved_location?.id.orEmpty(),
                    selected_location.saved_location?.latitude.orEmpty(),
                    selected_location.saved_location?.longitude.orEmpty(),
                    selected_location.saved_location?.altitude.orEmpty(),
                    selected_location.saved_location?.calc_mode.orEmpty(),
                    selected_location.method_config_override?.method_preset.orEmpty(),
                    selected_location.method_config_override?.fajr_angle?.toString().orEmpty(),
                    selected_location.method_config_override?.isha_mode.orEmpty(),
                    selected_location.method_config_override?.isha_angle?.toString().orEmpty(),
                    selected_location.method_config_override?.isha_fixed_minutes?.toString().orEmpty(),
                    selected_location.method_config_override?.asr_factor?.toString().orEmpty(),
                    selected_location.method_config_override?.maghrib_offset_minutes?.toString().orEmpty(),
                    selected_location.method_config_override?.makruh_angle?.toString().orEmpty(),
                    selected_location.method_config_override?.makruh_sunrise_minutes?.toString().orEmpty(),
                    selected_location.method_config_override?.zawal_minutes?.toString().orEmpty(),
                    runtime_profile.hijri_variant,
                    runtime_profile.hijri_day_offset.toString(),
                    runtime_profile.extra_fajr_1_enabled.toString(),
                    runtime_profile.extra_fajr_1_angle.toString(),
                    runtime_profile.extra_fajr_1_label_raw,
                    runtime_profile.extra_isha_1_enabled.toString(),
                    runtime_profile.extra_isha_1_angle.toString(),
                    runtime_profile.extra_isha_1_label_raw
                ).joinToString("|")
            )

        if (!force && sig == last_sig && (state.loading || state.skeleton != null)) return

        val this_id = ++load_id
        last_sig = sig
        loaded_host = host
        loaded_selected_location = selected_location
        loaded_show_prohibited = show_prohibited
        loaded_show_night = show_night
        state = DaysUiState(loading = true, subtitle = subtitle)
        day_inflight = Collections.synchronizedSet(HashSet<Long>())
        day_cache.clear()

        workers.execute {
            try {
                val skel =
                    build_month_skeleton(
                        ctx,
                        host,
                        month_basis,
                        show_hijri_effective,
                        runtime_profile.hijri_variant,
                        runtime_profile.hijri_day_offset,
                        month_anchor,
                        selected_location
                    )
                main.post {
                    if (this_id != load_id) return@post
                    month_start = skel.start
                    month_end = skel.end
                    state = DaysUiState(loading = false, title = skel.title, subtitle = subtitle, skeleton = skel)
                }
            } catch (_: ArithmeticException) {
                main.post {
                    if (this_id != load_id) return@post
                    state = DaysUiState(loading = false, subtitle = subtitle, error = ctx.getString(R.string.hijri_out_of_range))
                }
            }
        }
    }

    fun ensure_range_loaded(first_index: Int, last_index: Int) {
        val this_id = load_id
        val skel = state.skeleton ?: return
        val host = loaded_host ?: return
        val selected_location = loaded_selected_location
        val show_prohibited = loaded_show_prohibited
        val show_night = loaded_show_night
        val inflight = day_inflight

        val start = (first_index - 7).coerceAtLeast(0)
        val end = (last_index + 7).coerceAtMost(skel.days.lastIndex)
        val visible_start = first_index.coerceAtLeast(0).coerceAtMost(skel.days.lastIndex)
        val visible_end = last_index.coerceAtLeast(0).coerceAtMost(skel.days.lastIndex)

        // Prioritize rendering what's actually on screen, then prefetch the buffer around it.
        val indices =
            buildList {
                for (i in visible_start..visible_end) add(i)
                for (i in (visible_start - 1) downTo start) add(i)
                for (i in (visible_end + 1)..end) add(i)
            }

        for (i in indices) {
            val meta = skel.days[i]
            if (day_cache.containsKey(meta.day_start)) continue
            if (!inflight.add(meta.day_start)) continue

            workers.execute {
                val item = build_day_item(getApplication(), host, meta, show_prohibited, show_night, selected_location)
                main.post {
                    if (this_id != load_id) {
                        inflight.remove(meta.day_start)
                        return@post
                    }
                    day_cache[meta.day_start] = item
                    inflight.remove(meta.day_start)
                }
            }
        }
    }

    fun shift_month(delta: Int) {
        val ctx = getApplication<Application>().applicationContext
        val host = HostResolver.ensure_default_selected(ctx) ?: return
        val tz = loaded_selected_location?.timezone ?: HostConfigReader.read_config(ctx, host)?.timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()

        val start = month_start
        val end = month_end
        if (start == null || end == null) return

        month_anchor = if (delta < 0) add_days(start, -1, tz) else add_days(end, 1, tz)
        load(force = true)
    }

    override fun onCleared() {
        workers.shutdownNow()
    }
}
