package org.feelm.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.feelm.app.data.api.Season
import org.feelm.app.ui.formatReleaseDate
import org.feelm.app.ui.formatRuntime
import org.feelm.app.ui.mediaUrl
import org.feelm.app.ui.theme.LocalFeelmColors

/**
 * Seasons as a row of pills, with the chosen one's episodes listed under it.
 *
 * The accordion this replaced made you open a season to find out how long it
 * was, and could show two of them at once — neither of which is how anybody
 * reads a series. One selector, one list, the way the web app does it.
 *
 * The tick on each row is a control, not a label. Marking an episode watched
 * while looking at the list is the natural way to record progress, and it beats
 * scrolling back up to type numbers into two fields. Because progress is a
 * single position rather than a set, ticking episode nine also ticks one
 * through eight — which is what somebody means by it.
 */
@Composable
fun SeasonBrowser(
    seasons: List<Season>,
    accent: Color,
    /** Where the shelf says you are. */
    watchedSeason: Int? = null,
    watchedEpisode: Int? = null,
    onMarkWatched: ((season: Int, episode: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (seasons.isEmpty()) return

    val feelm = LocalFeelmColors.current
    var selected by remember(seasons) {
        // Open on the season you are actually watching, not on the first one.
        mutableIntStateOf(
            watchedSeason?.takeIf { s -> seasons.any { it.number == s } }
                ?: seasons.firstOrNull()?.number ?: 1
        )
    }
    val season = seasons.firstOrNull { it.number == selected } ?: seasons.first()
    val totalEpisodes = seasons.sumOf { it.episodes.size }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Seasons",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "${seasons.size} seasons · $totalEpisodes episodes",
                style = MaterialTheme.typography.bodySmall,
                color = feelm.faint,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(seasons, key = { it.number ?: it.name }) { item ->
                SeasonPill(
                    season = item,
                    selected = item.number == selected,
                    accent = accent,
                    onClick = { selected = item.number ?: 1 },
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            season.episodes.forEach { episode ->
                val number = episode.number ?: 0
                val seen = watchedSeason != null &&
                    (
                        selected < watchedSeason ||
                            (selected == watchedSeason && number <= (watchedEpisode ?: 0))
                        )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = feelm.faint,
                        modifier = Modifier.width(20.dp),
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = episode.title.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        val facts = listOfNotNull(
                            formatReleaseDate(episode.airDate),
                            episode.runtime?.let { formatRuntime(it) },
                        ).joinToString("  ·  ")
                        if (facts.isNotEmpty()) {
                            Text(
                                text = facts,
                                style = MaterialTheme.typography.labelMedium,
                                color = feelm.faint,
                            )
                        }
                        episode.overview?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp,
                            )
                        }
                    }

                    WatchedTick(
                        seen = seen,
                        accent = accent,
                        onClick = onMarkWatched?.let { mark -> { mark(selected, number) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonPill(
    season: Season,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val feelm = LocalFeelmColors.current

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) accent.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, top = 6.dp, end = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = mediaUrl(season.poster),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column {
            Text(
                text = season.title?.takeIf { it.isNotBlank() } ?: season.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) accent else MaterialTheme.colorScheme.onBackground,
            )
            val facts = listOfNotNull(
                season.year?.toString(),
                season.episodes.size.takeIf { it > 0 }?.let { "$it ep" },
            ).joinToString(" · ")
            if (facts.isNotEmpty()) {
                Text(
                    text = facts,
                    style = MaterialTheme.typography.labelSmall,
                    color = feelm.faint,
                )
            }
        }
    }
}

/**
 * Filled when watched, a faint circle when not.
 *
 * Empty rather than absent, so it reads as something you can press — a tick
 * that only appears once an episode is watched gives you no way to say that it
 * is.
 */
@Composable
private fun WatchedTick(seen: Boolean, accent: Color, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (seen) accent else MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = if (seen) MaterialTheme.colorScheme.onPrimary
            else LocalFeelmColors.current.faint,
            modifier = Modifier.size(16.dp),
        )
    }
}
