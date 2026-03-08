package com.yshalsager.suntimes.prayertimesaddon.core

import androidx.core.os.LocaleListCompat

fun current_app_language(): String =
    androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { "system" }

fun app_language_locales(language: String): LocaleListCompat =
    if (language == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language)
