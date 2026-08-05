package org.feelm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.feelm.app.data.BrowseQuery
import org.feelm.app.data.api.FiltersResponse
import org.feelm.app.ui.sortLabel
import org.feelm.app.ui.typeLabelFor

val FILTER_TYPES = listOf("movie", "series", "game", "book")

private val SORT_LABELS = mapOf(
    "popularity" to "Popular",
    "score" to "Top rated",
    "imdb" to "IMDb",
    "recent" to "Recently added",
    "release" to "Newest",
    "title" to "A–Z",
    "votes" to "Most voted",
    "relevance" to "Relevance",
)

/** English names for the language codes the catalog actually holds. */
private val LANGUAGE_NAMES = mapOf(
    "en" to "English", "fr" to "French", "es" to "Spanish", "de" to "German",
    "it" to "Italian", "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese",
    "ru" to "Russian", "pt" to "Portuguese", "tr" to "Turkish", "hi" to "Hindi",
    "ar" to "Arabic", "sv" to "Swedish", "da" to "Danish", "nl" to "Dutch",
    "pl" to "Polish", "fi" to "Finnish", "no" to "Norwegian", "cs" to "Czech",
    "az" to "Azerbaijani",
)

/**
 * Every way the catalog can be narrowed, in one sheet.
 *
 * Shared by browse and search because the server takes the same vocabulary for
 * both — /api/search is /api/items with a query bolted on — and two sheets that
 * drift apart would mean a filter you can reach one way and not the other.
 *
 * Edits go to a draft and only reach the caller on "Show results". Applying
 * each tap live would refetch the catalog five times while somebody picks four
 * genres.
 */
@Composable
fun FilterSheet(
    query: BrowseQuery,
    filters: FiltersResponse?,
    onApply: (BrowseQuery) -> Unit,
    onClear: () -> Unit,
    showTypes: Boolean = false,
) {
    var draft by remember(query) { mutableStateOf(query) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            // Top gap to match the space between sections; without it the
            // first heading sits flush against the sheet's drag handle.
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (showTypes) {
            Section(stringResource(R.string.search_type))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(R.string.profile_everything), draft.type == null) { draft = draft.copy(type = null) }
                FILTER_TYPES.forEach { type ->
                    Pill(typeLabelFor(type), draft.type == type) { draft = draft.copy(type = type) }
                }
            }
        }

        Section(stringResource(R.string.search_sort))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (filters?.sorts ?: listOf("popularity", "score")).forEach { sort ->
                Pill(
                    label = sortLabel(sort),
                    selected = draft.sort == sort,
                ) { draft = draft.copy(sort = sort) }
            }
        }

        Section(stringResource(R.string.search_release))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                null to stringResource(R.string.app_any),
                "released" to stringResource(R.string.search_releaseOut),
                "upcoming" to stringResource(R.string.app_upcoming),
            )
                .forEach { (value, label) ->
                    Pill(label, draft.release == value) { draft = draft.copy(release = value) }
                }
        }

        filters?.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
            Section(stringResource(R.string.browse_genre))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                genres.forEach { genre ->
                    val on = genre.slug in draft.genres
                    Pill(genre.name, on) {
                        draft = draft.copy(
                            genres = if (on) draft.genres - genre.slug
                            else draft.genres + genre.slug,
                        )
                    }
                }
            }
        }

        Section(stringResource(R.string.browse_minScore))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null, 60, 70, 80, 90).forEach { score ->
                Pill(score?.let { "$it+" } ?: stringResource(R.string.app_any), draft.scoreMin == score) {
                    draft = draft.copy(scoreMin = score)
                }
            }
        }

        filters?.decades?.takeIf { it.isNotEmpty() }?.let { decades ->
            Section(stringResource(R.string.browse_decade))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Pill(stringResource(R.string.app_any), draft.yearFrom == null) {
                    draft = draft.copy(yearFrom = null, yearTo = null)
                }
                decades.forEach { decade ->
                    Pill("${decade}s", draft.yearFrom == decade) {
                        draft = draft.copy(yearFrom = decade, yearTo = decade + 9)
                    }
                }
            }
        }

        filters?.certifications?.takeIf { it.isNotEmpty() }?.let { certifications ->
            Section(stringResource(R.string.app_certification))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                certifications.forEach { cert ->
                    val on = cert in draft.certifications
                    Pill(cert, on) {
                        draft = draft.copy(
                            certifications = if (on) draft.certifications - cert
                            else draft.certifications + cert,
                        )
                    }
                }
            }
        }

        filters?.languages?.takeIf { it.isNotEmpty() }?.let { languages ->
            Section(stringResource(R.string.settings_language))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Already ordered by how much of the catalog each one holds,
                // so the twelve worth offering are the first twelve.
                languages.take(12).forEach { language ->
                    val on = language.code in draft.languages
                    Pill(LANGUAGE_NAMES[language.code] ?: language.code.uppercase(), on) {
                        draft = draft.copy(
                            languages = if (on) draft.languages - language.code
                            else draft.languages + language.code,
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.app_clearAll)) }
            Button(onClick = { onApply(draft) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.app_showResults))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/**
 * A selectable chip.
 *
 * Written rather than using M3's FilterChip because the selected fill is the
 * type's own hue on the browse tabs, and FilterChip's colour slots do not reach
 * the container without fighting its elevation defaults.
 */
@Composable
fun Pill(
    label: String,
    selected: Boolean,
    accent: Color? = null,
    onClick: () -> Unit,
) {
    val fill = accent ?: MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) fill else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
