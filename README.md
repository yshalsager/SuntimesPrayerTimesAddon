# Suntimes Prayer Times Addon

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)](https://developer.android.com/jetpack/compose)
[![Design](https://img.shields.io/badge/Material-Material%203-757575)](https://m3.material.io/)
[![minSdk](https://img.shields.io/badge/minSdk-21-2ea44f)](https://developer.android.com/about/versions/android-5.0)

Prayer times, prohibited (makruh) windows, and night portions as a **SuntimesWidget addon**.

This project intentionally avoids implementing astronomical algorithms: it delegates solar/shadow calculations to the installed **SuntimesWidget** app via its exported `ContentProvider`s.

## What This App Does

- Shows today’s timeline (Home) and a month/day-card list (Days).
- Exposes events to **SuntimesWidget Alarms**:
  - 5 prayers (Fajr, Dhuhr/Jumu'ah, Asr, Maghrib, Isha)
  - prohibited (makruh) boundaries and windows
  - night portions (midpoint, last third, last sixth)
- Provides an Android home-screen widget ("Prayer Times (Today)") with the same day-card model.
- Supports English + Arabic and RTL.
- Supports theme mode (System/Light/Dark) + palette selection:
  - Parchment / Sapphire / Rose
  - Dynamic colors on Android 12+ (Compose + widget backgrounds/accent)

## Requirements

- Android `minSdk 21` (Lollipop)
- **SuntimesWidget installed** (stable/nightly/legacy supported)
- Host permissions granted (SuntimesWidget protects its providers with `suntimes.*.permission.*`)

## Architecture (Host Delegation)

The app is split into:
- `core/*`: host discovery, provider contracts, event-id mapping, and small derived calculations (night portions, Hijri date)
- `provider/*`: addon `ContentProvider` implementation for SuntimesWidget integration
- `ui/*` and `ui/compose/*`: Compose UI (Home/Days/Settings/Event Picker)
- `widget/*`: RemoteViews widget and update scheduling

### Data Flow

```
            +-------------------+
            |  SuntimesWidget   |
            |-------------------|
            | event.provider    |  content://<host>.event.provider/eventCalc/<eventId>
            | calculator.provider| content://<host>.calculator.provider/sun/<millis>
            +---------^---------+
                      |
                      |  queries (delegation)
                      |
+---------------------+----------------------+
|         PrayerTimesAddon (this app)       |
|-------------------------------------------|
| UI (Compose)                              |
|  - Home/Days/Settings/EventPicker         |
|  - mostly queries host providers directly |
|                                           |
| Addon Provider (exported)                 |
|  - content://...event.provider/eventCalc  |
|  - used by SuntimesWidget alarms          |
|  - also used by widget for eventCalc      |
|                                           |
| Widget (RemoteViews)                      |
|  - reads prefs + provider results         |
+-------------------------------------------+
```

### “No Reinvention” Policy

We treat the host app as the source of truth for all solar/shadow events:
- Prayer times are resolved by translating settings into **host event ids** and asking the host to compute `eventCalc/<eventId>`.
- Calculator provider is used for "bulk" sun times (noon/sunrise/sunset) to reduce query volume and to support past-day computations.

The only non-host math intentionally left is a **compatibility fallback** for Asr when the host does not expose a shadow-ratio event:
- It uses host declination + latitude to translate the juristic factor into an equivalent sun-elevation event id, then still delegates the final time lookup back to the host.

## Host Integration (Addon Contracts)

SuntimesWidget discovers addons via an exported Activity with `suntimes.action.ADDON_EVENT` and metadata pointing to a provider URI.

This project provides:
- `AddonRegistrationActivity` (discovery stub)
- `PrayerTimesProvider` (exported) implementing the same event-provider contract SuntimesWidget expects
- `EventPickerActivity` for `suntimes.action.PICK_EVENT`

### Exported Provider

Authority:
- `com.yshalsager.suntimes.prayertimesaddon.event.provider`

Paths:
- `content://.../eventTypes`
- `content://.../eventInfo/<eventId>`
- `content://.../eventCalc/<eventId>`

### Exposed Events

Prayers:
- `PRAYER_FAJR`
- `PRAYER_DHUHR` (displayed as **Jumu'ah** on Friday)
- `PRAYER_ASR`
- `PRAYER_MAGHRIB`
- `PRAYER_ISHA`

Night:
- `NIGHT_MIDPOINT`
- `NIGHT_LAST_THIRD`
- `NIGHT_LAST_SIXTH`

Makruh boundaries:
- `MAKRUH_DAWN_START` / `MAKRUH_DAWN_END`
- `MAKRUH_SUNRISE_START` / `MAKRUH_SUNRISE_END`
- `MAKRUH_ZAWAL_START` / `MAKRUH_ZAWAL_END`
- `MAKRUH_AFTER_ASR_START` / `MAKRUH_AFTER_ASR_END`
- `MAKRUH_SUNSET_START` / `MAKRUH_SUNSET_END`

## Settings Model (High-Level)

- Host selection:
  - Auto-detect stable/nightly/legacy installs.
  - Persist the selected event-provider authority.
- Prayer method:
  - Presets + custom
  - Fajr angle
  - Isha mode (angle / fixed minutes)
  - Asr factor (Shafi=1 / Hanafi=2)
  - Maghrib offset
- Makruh:
  - Presets (Shafi/Hanafi) + custom angle + zawal minutes
- Hijri:
  - Variant: Umm al-Qura / Diyanet
  - Day offset: -2..+2 (manual correction)
- Widget:
  - Toggle prohibited row
  - Toggle night row
- UI:
  - Language: system / English / Arabic
  - Theme mode: system / light / dark
  - Palette: parchment / dynamic (Android 12+) / sapphire / rose

## Widget

Widget name:
- "Prayer Times (Today)"

Notes:
- Widgets use RemoteViews, so theming is applied by setting background resources + text colors at update time.
- Update scheduling is “best effort”: we update on app settings changes and schedule an alarm for the next meaningful boundary (next prayer / prohibited boundary / midnight rollover).

## Tooling

This repo is intended to be built with:
- Gradle Wrapper (`./gradlew`)
- [`mise`](https://mise.jdx.dev/) for tool/version management (optional but recommended)

Useful commands:

```bash
# Build debug APK
mise x java -- ./gradlew :app:assembleDebug

# Run unit tests
mise x java -- ./gradlew :app:testDebugUnitTest
```

## Project Layout

```
app/src/main/java/com/yshalsager/suntimes/prayertimesaddon/
  core/        # contracts, mapping, prefs, Hijri, delegation helpers
  provider/    # exported addon provider for SuntimesWidget
  ui/          # Activities + ViewModels
  ui/compose/  # Compose screens and shared components
  widget/      # AppWidgetProvider + update glue
```

## Credits

- **SuntimesWidget** by Forrest Guice (host app and addon APIs):  
  [forrestguice/SuntimesWidget](https://github.com/forrestguice/SuntimesWidget)
- Time4J (Hijri calendar variants):  
  [MenoData/Time4J](https://github.com/MenoData/Time4J)
- Jetpack Compose / Material 3:  
  [AndroidX Compose](https://developer.android.com/jetpack/compose)  
  [Material 3](https://m3.material.io/)

## License

GPL-3.0-only. See `LICENSE`.
