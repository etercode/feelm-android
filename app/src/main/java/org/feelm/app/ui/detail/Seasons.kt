package org.feelm.app.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.feelm.app.data.api.Season
import org.feelm.app.ui.formatReleaseDate
import org.feelm.app.ui.formatRuntime
import org.feelm.app.ui.theme.LocalFeelmColors

/**
 * Seasons, one open at a time.
 *
 * An accordion rather than a flat list: House of the Dragon's episodes alone
 * would push everything below them off the bottom of a phone, and somebody
 * looking at series four does not need series one expanded underneath it.
 * Nothing is open initially — the seasons are the answer to "how much is
 * there", and the episodes only matter once one is picked.
 */
@Composable
fun SeasonBrowser(
    seasons: List<Season>,
    /** Where the shelf says you are, so watched episodes can be ticked off. */
    watchedSeason: Int? = null,
    watchedEpisode: Int? = null,
    modifier: Modifier = Modifier,
) {
    if (seasons.isEmpty()) return

    var openSeason by remember { mutableIntStateOf(-1) }
    val feelm = LocalFeelmColors.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.seasons_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        seasons.forEach { season ->
            val number = season.number ?: -1
            val open = openSeason == number

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { openSeason = if (open) -1 else number },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = season.title?.takeIf { it.isNotBlank() } ?: season.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        val facts = listOfNotNull(
                            season.year?.toString(),
                            season.episodes.size.takeIf { it > 0 }
                                ?.let { if (it == 1) "1 episode" else "$it episodes" },
                        )
                        if (facts.isNotEmpty()) {
                            Text(
                                text = facts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = feelm.faint,
                            )
                        }
                    }
                    Icon(
                        imageVector = if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(if (open) R.string.app_collapse else R.string.app_expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }

                AnimatedVisibility(visible = open) {
                    Column(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        season.overview?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        season.episodes.forEach { episode ->
                            /*
                             * Anything before where you are counts as watched.
                             * Progress is a single position, not a set of ticks,
                             * so "seen" is everything up to it — which is how
                             * somebody halfway through series three thinks about
                             * series one.
                             */
                            val seen = watchedSeason != null && number > 0 &&
                                (
                                    number < watchedSeason ||
                                        (number == watchedSeason &&
                                            (episode.number ?: 0) <= (watchedEpisode ?: 0))
                                    )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (seen) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = feelm.star,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    Spacer(Modifier.size(16.dp))
                                }
                                Text(
                                    text = episode.number?.toString().orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = feelm.faint,
                                    modifier = Modifier.size(width = 22.dp, height = 20.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = episode.title.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val line = listOfNotNull(
                                        formatReleaseDate(episode.airDate),
                                        episode.runtime?.let { formatRuntime(it) },
                                    ).joinToString(" · ")
                                    if (line.isNotEmpty()) {
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = feelm.faint,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
