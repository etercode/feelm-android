package org.feelm.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens Material 3 has no slot for.
 *
 * The type hues, the star and the NEW badge are Feelm's own vocabulary rather
 * than roles in a Material palette, so they ride alongside the colour scheme
 * instead of being crammed into `tertiary` and friends.
 */
@Immutable
data class FeelmColors(
    val movie: Color,
    val series: Color,
    val game: Color,
    val book: Color,
    val star: Color,
    val new: Color,
    val danger: Color,
    val faint: Color,
    val elevated: Color,
) {
    /** The accent for a work's type, defaulting to the brand for anything new. */
    fun forType(type: String, fallback: Color): Color = when (type) {
        "movie" -> movie
        "series" -> series
        "game" -> game
        "book" -> book
        else -> fallback
    }
}

val LocalFeelmColors = staticCompositionLocalOf {
    FeelmColors(
        movie = LightMovie,
        series = LightSeries,
        game = LightGame,
        book = LightBook,
        star = LightStar,
        new = LightNew,
        danger = LightDanger,
        faint = LightFaint,
        elevated = LightSurface2,
    )
}

private val LightScheme = lightColorScheme(
    primary = LightBrand,
    onPrimary = Color.White,
    primaryContainer = Color(0x1F1066D5),
    onPrimaryContainer = LightBrand,
    secondary = LightMuted,
    onSecondary = Color.White,
    background = LightPage,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = LightMuted,
    surfaceContainerHighest = LightSurface3,
    outline = LightLine,
    outlineVariant = LightLine,
    error = LightDanger,
)

private val DarkScheme = darkColorScheme(
    primary = DarkBrand,
    // The page colour, so anything on an accent fill reads as a hole punched
    // through to the background rather than a fifth dark tone.
    onPrimary = DarkPage,
    primaryContainer = Color(0x29649FF6),
    onPrimaryContainer = DarkBrand,
    secondary = DarkMuted,
    onSecondary = DarkPage,
    background = DarkPage,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkMuted,
    surfaceContainerHighest = DarkSurface3,
    outline = DarkLine,
    outlineVariant = DarkLine,
    error = DarkDanger,
)

/**
 * No dynamic colour. Feelm's blue is sampled from the film strip in the logo,
 * and letting the wallpaper repaint it would put the app and its own mark in
 * different colours on every device.
 */
@Composable
fun FeelmTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val extras = if (darkTheme) {
        FeelmColors(
            movie = DarkMovie,
            series = DarkSeries,
            game = DarkGame,
            book = DarkBook,
            star = DarkStar,
            new = DarkNew,
            danger = DarkDanger,
            faint = DarkFaint,
            elevated = DarkSurface2,
        )
    } else {
        FeelmColors(
            movie = LightMovie,
            series = LightSeries,
            game = LightGame,
            book = LightBook,
            star = LightStar,
            new = LightNew,
            danger = LightDanger,
            faint = LightFaint,
            elevated = LightSurface2,
        )
    }

    CompositionLocalProvider(LocalFeelmColors provides extras) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography,
            content = content,
        )
    }
}
