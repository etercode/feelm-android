package org.feelm.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.feelm.app.data.api.WorkSummary
import org.feelm.app.ui.components.PosterCard
import org.feelm.app.ui.LocalSeenState
import org.feelm.app.ui.daysUntil
import org.feelm.app.ui.formatReleaseDate
import org.feelm.app.ui.theme.LocalFeelmColors
import org.feelm.app.ui.theme.OnImage
import org.feelm.app.ui.typeLabelFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenWork: (type: String, slug: String) -> Unit,
    onOpenAccount: () -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onCatchUp: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val seen = LocalSeenState.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Feelm",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    // Only offered when there is something to catch up on —
                    // a button that clears nothing invites a tap that does
                    // nothing.
                    val ready = state as? HomeUiState.Ready
                    val anyNew = ready?.home?.rails
                        ?.any { rail -> rail.items.any(seen::isNew) } == true
                    if (anyNew) {
                        IconButton(onClick = onCatchUp) {
                            Icon(
                                Icons.Filled.DoneAll,
                                contentDescription = stringResource(R.string.home_markSeen),
                            )
                        }
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = stringResource(if (darkTheme) R.string.app_useLight else R.string.app_useDark),
                        )
                    }
                    IconButton(onClick = onOpenAccount) {
                        Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.nav_account))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { padding ->
        when (val current = state) {
            is HomeUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is HomeUiState.Failed -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = viewModel::load) { Text(stringResource(R.string.app_tryAgain)) }
                }
            }

            is HomeUiState.Ready -> HomeContent(
                state = current,
                contentPadding = padding,
                onOpenWork = onOpenWork,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Ready,
    contentPadding: PaddingValues,
    onOpenWork: (String, String) -> Unit,
) {
    val home = state.home

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // The release plate. The next thing out, given the whole width — it is
        // the one item on this page with a date attached, and the reason to
        // open the app on a Tuesday.
        home.upcoming.firstOrNull()?.let { hero ->
            item(key = "hero") {
                ReleasePlate(work = hero, onClick = { onOpenWork(hero.type, hero.slug) })
            }
        }

        if (home.upcoming.size > 1) {
            item(key = "upcoming") {
                Rail(
                    title = stringResource(R.string.app_comingSoon),
                    works = home.upcoming.drop(1),
                    onOpenWork = onOpenWork,
                )
            }
        }

        items(home.rails, key = { "rail-${it.type}" }) { rail ->
            Rail(
                title = typeLabelFor(rail.type),
                works = rail.items,
                accent = LocalFeelmColors.current.forType(rail.type, MaterialTheme.colorScheme.primary),
                onOpenWork = onOpenWork,
            )
        }

        if (home.latest.isNotEmpty()) {
            item(key = "latest") {
                Rail(
                    title = stringResource(R.string.home_latestKicker),
                    works = home.latest,
                    onOpenWork = onOpenWork,
                )
            }
        }
    }
}

/**
 * A horizontal row of posters under a heading.
 *
 * The accent bar is how a section says which type it is without spending a
 * line of text on it — the same four hues the web app uses.
 */
@Composable
private fun Rail(
    title: String,
    works: List<WorkSummary>,
    onOpenWork: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    if (works.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (accent != null) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 16.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(works, key = { it.id }) { work ->
                PosterCard(work = work, onClick = { onOpenWork(work.type, work.slug) })
            }
        }
    }
}

/**
 * The next release, full width.
 *
 * Backdrop rather than poster, because this is the one place with room for
 * one, and a gradient veil under the text — the alternative is white type over
 * whatever the top-left corner of the artwork happens to be.
 */
@Composable
private fun ReleasePlate(work: WorkSummary, onClick: () -> Unit) {
    val days = daysUntil(work.details.releaseDate)
    val date = formatReleaseDate(work.details.releaseDate)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .aspectRatio(16f / 10f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = work.backdrop ?: work.poster,
            contentDescription = work.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color(0x330A0C12),
                        1f to Color(0xEB080A0F),
                    )
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val countdown = when {
                days == null -> date
                days == 0L -> stringResource(R.string.app_outToday)
                days == 1L -> stringResource(R.string.app_tomorrow)
                days < 30L -> "In $days days"
                else -> date
            }
            if (countdown != null) {
                Text(
                    text = countdown.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = work.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnImage,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (work.genres.isNotEmpty()) {
                Text(
                    text = work.genres.take(3).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnImage.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
