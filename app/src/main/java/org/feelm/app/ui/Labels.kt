package org.feelm.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.feelm.app.R

/**
 * Words the dictionary keys by something the app only knows at runtime.
 *
 * Statuses are the reason this exists. The web says "Reading" for a book,
 * "Playing" for a game and "Watching" for a film — one shelf, three verbs —
 * so the key is `status_{type}_{status}` and cannot be written out at compile
 * time. Resolving by name keeps that behaviour rather than flattening all
 * three into "Watching", which is what a single generic string would do.
 */
@Composable
fun statusLabel(type: String, status: String): String =
    resolve("status_${type}_$status") ?: genericStatus(status)

@Composable
fun sortLabel(sort: String): String =
    resolve("sort_$sort") ?: sort.replaceFirstChar { it.uppercase() }

@Composable
fun roleLabelFor(role: String): String =
    resolve("role_$role") ?: role.replaceFirstChar { it.uppercase() }

@Composable
fun typeLabelFor(type: String): String = when (type) {
    "movie" -> stringOf(R.string.type_movie_plural)
    "series" -> stringOf(R.string.browse_series_title)
    "game" -> stringOf(R.string.browse_games_title)
    "book" -> stringOf(R.string.browse_books_title)
    else -> type.replaceFirstChar { it.uppercase() }
}

/**
 * The shelf tab has no one type, so it falls back to the neutral set — the
 * only place in the app where "Watching" has to cover a book as well.
 */
@Composable
private fun genericStatus(status: String): String = when (status) {
    "wishlist" -> stringOf(R.string.app_wishlist)
    "active" -> stringOf(R.string.app_watching)
    "done" -> stringOf(R.string.app_done)
    "dropped" -> stringOf(R.string.app_dropped)
    else -> status
}

@Composable
private fun stringOf(id: Int): String = LocalContext.current.getString(id)

/**
 * Look a string up by name.
 *
 * Slower than a compile-time R reference and worth it only here, where the key
 * is assembled from data. Returns null rather than throwing when the catalog
 * grows a type the dictionary has not caught up with.
 */
@Composable
private fun resolve(name: String): String? {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(name, "string", context.packageName)
    return if (id == 0) null else context.getString(id)
}
