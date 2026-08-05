package org.feelm.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Feelm's palette, taken from the web app's app.css so the two clients are
 * recognisably the same product.
 *
 * Paper by default, the dark room on request. The dark theme is indigo rather
 * than grey on purpose — a single neutral black is a tiring room to sit in, and
 * posters should be the brightest thing on the screen.
 */

// Light — surfaces are four steps on one hue, with `Surface` left pure white
// because a card is paper laid on the page and the page carries the tint.
val LightPage = Color(0xFFF3F6FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFEBEFF6)
val LightSurface3 = Color(0xFFDCE1EC)
val LightInk = Color(0xFF131823)
val LightMuted = Color(0xFF485268)
val LightFaint = Color(0xFF667085)
val LightLine = Color(0x1C131823)
val LightBrand = Color(0xFF1066D5)

// Dark
val DarkPage = Color(0xFF090F1C)
val DarkSurface = Color(0xFF181F2F)
val DarkSurface2 = Color(0xFF232B3D)
val DarkSurface3 = Color(0xFF303A51)
val DarkInk = Color(0xFFEBEEF5)
val DarkMuted = Color(0xFFA3ABBC)
val DarkFaint = Color(0xFF838CA0)
val DarkLine = Color(0x1AFFFFFF)
val DarkBrand = Color(0xFF649FF6)

/**
 * A hue per activity type.
 *
 * All four sit at the same lightness within a theme and differ only in hue, so
 * no type reads as louder than another — the web app learned that the hard way
 * when game and book drifted apart in the dark theme.
 */
val LightMovie = Color(0xFF1066D5)
val LightSeries = Color(0xFF913BBD)
val LightGame = Color(0xFF147E60)
val LightBook = Color(0xFF935F10)

val DarkMovie = Color(0xFF649FF6)
val DarkSeries = Color(0xFFBF7EE6)
val DarkGame = Color(0xFF33B78F)
val DarkBook = Color(0xFFD48D2C)

val LightStar = Color(0xFFD99521)
val DarkStar = Color(0xFFFFC94D)
val LightNew = Color(0xFFCD3E21)
val DarkNew = Color(0xFFF57055)
val LightDanger = Color(0xFFBC332E)
val DarkDanger = Color(0xFFEF736C)

/** Anything laid over artwork keeps its own dark scale in both themes. */
val OnImage = Color(0xFFFFFFFF)
val Veil = Color(0xA80A0C12)
