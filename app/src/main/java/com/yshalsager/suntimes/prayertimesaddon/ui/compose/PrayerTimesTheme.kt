package com.yshalsager.suntimes.prayertimesaddon.ui.compose

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.platform.LocalContext
import com.yshalsager.suntimes.prayertimesaddon.R
import com.yshalsager.suntimes.prayertimesaddon.core.Prefs

@Composable
fun PrayerTimesTheme(
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val palette = Prefs.get_palette(ctx)
    val dark = isSystemInDarkTheme()
    val dynamic_ok = palette == Prefs.palette_dynamic && Build.VERSION.SDK_INT >= 31

    fun blend(fg: Color, bg: Color, a: Float): Color = Color(
        red = bg.red + (fg.red - bg.red) * a,
        green = bg.green + (fg.green - bg.green) * a,
        blue = bg.blue + (fg.blue - bg.blue) * a,
        alpha = 1f
    )

    val primary = when (palette) {
        Prefs.palette_sapphire -> R.color.primary_sapphire
        Prefs.palette_rose -> R.color.primary_rose
        else -> R.color.teal_earth
    }
    val secondary = when (palette) {
        Prefs.palette_sapphire -> R.color.secondary_sapphire
        Prefs.palette_rose -> R.color.secondary_rose
        else -> R.color.amber_earth
    }
    val p = colorResource(primary)
    val s = colorResource(secondary)

    val bg_res =
        when (palette) {
            Prefs.palette_sapphire -> R.color.parchment_sapphire
            Prefs.palette_rose -> R.color.parchment_rose
            else -> R.color.parchment
        }
    val bg = colorResource(bg_res)
    val surface = colorResource(R.color.surface_light)
    val surface_variant = colorResource(R.color.surface_muted)
    val on = colorResource(R.color.charcoal)
    val on_muted = colorResource(R.color.text_muted)
    val border = colorResource(R.color.border_soft)

    val night_surface = colorResource(R.color.surface_night)
    val prohibited_surface = colorResource(R.color.surface_prohibited)

    val colors =
        if (dynamic_ok) {
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            val primary_container =
                if (palette == Prefs.palette_parchment) night_surface
                else blend(p, bg, if (dark) 0.22f else 0.13f)

            val secondary_container =
                if (palette == Prefs.palette_parchment) prohibited_surface
                else blend(s, bg, if (dark) 0.18f else 0.12f)

            (if (dark) darkColorScheme() else lightColorScheme()).copy(
                primary = p,
                secondary = s,
                tertiary = s,
                background = bg,
                surface = surface,
                surfaceVariant = surface_variant,
                onBackground = on,
                onSurface = on,
                onSurfaceVariant = on_muted,
                outline = border,
                outlineVariant = border,
                primaryContainer = primary_container,
                onPrimaryContainer = on,
                secondaryContainer = secondary_container,
                onSecondaryContainer = on
            )
        }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
