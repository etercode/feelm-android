package org.feelm.app.ui

import org.feelm.app.data.api.WorkDetails
import org.feelm.app.data.api.WorkSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The fact line under a title.
 *
 * Each type prints its own three things — a film its runtime and rating, a
 * series how many seasons, a book its author and length — because "2010 · 2h
 * 28m" is what tells two titles of the same name apart, and that is the whole
 * reason someone is looking at a list rather than at the first result.
 */
fun WorkSummary.metaLine(): String = buildMeta(type, year, details)

fun buildMeta(type: String, year: Int?, details: WorkDetails): String {
    val parts = mutableListOf<String>()
    year?.let { parts += it.toString() }

    when (type) {
        "series" -> {
            details.seasonCount?.let { parts += if (it == 1) "1 season" else "$it seasons" }
            details.episodeCount?.let { parts += "$it episodes" }
        }
        "book" -> {
            details.authors.firstOrNull()?.let { parts += it }
            details.pages?.let { parts += "$it pages" }
        }
        "game" -> {
            details.developers.firstOrNull()?.let { parts += it }
        }
        else -> {
            details.runtime?.let { parts += formatRuntime(it) }
            details.certification?.takeIf { it.isNotBlank() }?.let { parts += it }
        }
    }

    return parts.joinToString(" · ")
}

fun formatRuntime(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}h ${rest}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

/**
 * A score out of 100, however the source counted it.
 *
 * IMDb answers 8.2 out of 10 and TMDB 80 out of 100; the badge shows one
 * number, so the scale each source used has to be divided out rather than
 * assumed — otherwise every IMDb title scores in the single digits.
 */
fun WorkSummary.score(): Int? = externalScore?.let { it.toInt() }
    ?: ratings.values.firstOrNull()?.let { ((it.rating / it.scale) * 100).toInt() }

private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

/** "12 Sep" for this year, "12 Sep 2027" when the year is not obvious. */
fun formatReleaseDate(raw: String?): String? {
    val date = raw?.let { runCatching { LocalDate.parse(it, isoDate) }.getOrNull() } ?: return null
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return if (date.year == LocalDate.now().year) {
        "${date.dayOfMonth} $month"
    } else {
        "${date.dayOfMonth} $month ${date.year}"
    }
}

/** Days until release, or null once it is out. */
fun daysUntil(raw: String?): Long? {
    val date = raw?.let { runCatching { LocalDate.parse(it, isoDate) }.getOrNull() } ?: return null
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    return if (days >= 0) days else null
}

/**
 * Turn a stored media path into an address.
 *
 * Most payloads arrive already resolved by the server's URL generator, but the
 * two that are answered straight out of SQL — a person's photo and a
 * collection's posters — carry the raw path instead. Coil would silently show
 * nothing for those.
 */
fun mediaUrl(pathOrUrl: String?): String? {
    if (pathOrUrl.isNullOrBlank()) return null
    if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return pathOrUrl
    val path = if (pathOrUrl.startsWith("/")) pathOrUrl else "/$pathOrUrl"
    return "https://feelm.org$path"
}

fun roleLabel(role: String): String = when (role) {
    "cast" -> "Acting"
    "director" -> "Directing"
    "writer" -> "Writing"
    "creator" -> "Created"
    "developer" -> "Development"
    "publisher" -> "Published"
    "author" -> "Written"
    else -> role.replaceFirstChar { it.uppercase() }
}

fun typeLabel(type: String): String = when (type) {
    "movie" -> "Movies"
    "series" -> "Series"
    "game" -> "Games"
    "book" -> "Books"
    else -> type.replaceFirstChar { it.uppercase() }
}
