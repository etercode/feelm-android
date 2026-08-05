package org.feelm.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.feelm.app.data.api.Entry
import org.feelm.app.data.api.Progress
import org.feelm.app.ui.components.Stars
import org.feelm.app.ui.theme.LocalFeelmColors
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Your own score and how far in you are.
 *
 * Deliberately separate from the review editor below it, because the two are
 * separate columns: `ReviewService` never writes to the shelf, so rating here
 * publishes nothing. Plenty of people will score everything they watch and
 * write about almost none of it, and making them post a review to record a
 * score would lose most of the ratings.
 *
 * Progress is per type — a season and episode, hours played, or a page — and
 * films have none, which is why there is nothing to show for them.
 */
@Composable
fun ShelfExtras(
    type: String,
    entry: Entry?,
    saving: Boolean,
    onRate: (Double?) -> Unit,
    onProgress: (Progress) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feelm = LocalFeelmColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.shelf_yourScore),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Stars(
                rating = entry?.score,
                size = 26.dp,
                onRate = { value -> onRate(if (entry?.score == value) null else value) },
            )
            if (entry?.score != null) {
                TextButton(onClick = { onRate(null) }) { Text(stringResource(R.string.common_clear)) }
            }
        }

        ProgressFields(
            type = type,
            progress = entry?.progress,
            saving = saving,
            onProgress = onProgress,
        )
    }
}

/**
 * Progress, folded away until asked for.
 *
 * The web app shows one button — "Track progress", or "Update progress" once
 * there is some — and only opens the fields when it is pressed. Leaving two
 * number boxes and a Save button permanently on screen makes every series look
 * like a form waiting to be filled in, when most visits are not about editing
 * anything.
 */
@Composable
private fun ProgressFields(
    type: String,
    progress: kotlinx.serialization.json.JsonElement?,
    saving: Boolean,
    onProgress: (Progress) -> Unit,
) {
    val obj = progress as? JsonObject
    val feelm = LocalFeelmColors.current
    var editing by remember(obj) { mutableStateOf(false) }

    fun stored(key: String): String = obj?.get(key)?.jsonPrimitive?.contentOrNull.orEmpty()

    // Nothing to track for a film: it is one sitting.
    if (type !in setOf("series", "game", "book")) return

    if (!editing) {
        val summary = when (type) {
            "series" -> stored("season").takeIf { it.isNotEmpty() }?.let { season ->
                "Season $season, episode ${stored("episode").ifEmpty { "1" }}"
            }
            "game" -> stored("hours").takeIf { it.isNotEmpty() && it != "0" }?.let { "$it hours" }
            else -> stored("page").takeIf { it.isNotEmpty() }?.let { "Page $it" }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(onClick = { editing = true }) {
                Text(if (summary == null) "Track progress" else "Update progress")
            }
            summary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = feelm.faint,
                )
            }
        }
        return
    }

    var season by remember(obj) { mutableStateOf(stored("season").ifEmpty { "1" }) }
    var episode by remember(obj) { mutableStateOf(stored("episode").ifEmpty { "1" }) }
    var hours by remember(obj) { mutableStateOf(stored("hours").ifEmpty { "0" }) }
    var page by remember(obj) { mutableStateOf(stored("page").ifEmpty { "1" }) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (type) {
                "series" -> {
                    NumberField(stringResource(R.string.progress_season), season, Modifier.weight(1f)) { season = it }
                    NumberField(stringResource(R.string.progress_episode), episode, Modifier.weight(1f)) { episode = it }
                }
                "game" -> NumberField(stringResource(R.string.progress_hoursPlayed), hours, Modifier.weight(1f), decimal = true) {
                    hours = it
                }
                else -> NumberField(stringResource(R.string.progress_page), page, Modifier.weight(1f)) { page = it }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !saving,
                onClick = {
                    onProgress(
                        when (type) {
                            "series" -> Progress.Series(
                                season = season.toIntOrNull() ?: 1,
                                episode = episode.toIntOrNull() ?: 1,
                            )
                            "game" -> Progress.Game(hours.toDoubleOrNull() ?: 0.0)
                            else -> Progress.Book(page.toIntOrNull() ?: 1)
                        }
                    )
                    editing = false
                },
            ) { Text(stringResource(R.string.common_save)) }
            TextButton(onClick = { editing = false }) { Text(stringResource(R.string.common_cancel)) }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { typed ->
            // Keep it numeric as it is typed rather than rejecting it on save.
            if (typed.all { it.isDigit() || (decimal && it == '.') }) onChange(typed)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier,
    )
}

@Composable
private fun SaveProgress(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(stringResource(R.string.common_save)) }
}
