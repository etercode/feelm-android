package org.feelm.app.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** The four the web app speaks. Anything else falls back to English. */
val SUPPORTED_LOCALES = listOf("en", "az", "tr", "ru")

/**
 * Which language the app draws itself in.
 *
 * Driven by the account's own `locale` preference rather than the phone's,
 * because Feelm already stores one per user and a member who reads the site in
 * Azerbaijani on a device set to English means the choice, not the accident.
 *
 * Below Android 13 AppCompat keeps this itself (`autoStoreLocales` in the
 * manifest), so it survives a relaunch without the app storing anything.
 */
object LocaleController {

    fun apply(locale: String?) {
        val tag = locale?.takeIf { it in SUPPORTED_LOCALES } ?: return
        if (current() == tag) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun current(): String? =
        AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }?.get(0)?.language
}
