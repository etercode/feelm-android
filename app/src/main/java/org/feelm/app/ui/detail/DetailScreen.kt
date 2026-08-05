package org.feelm.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.feelm.app.data.api.CastMember
import org.feelm.app.data.api.ShelfStatus
import org.feelm.app.data.api.WorkDetail
import org.feelm.app.ui.buildMeta
import org.feelm.app.ui.statusLabel
import org.feelm.app.ui.components.PosterCard
import org.feelm.app.ui.components.ScorePill
import org.feelm.app.ui.components.TrailerDialog
import org.feelm.app.ui.theme.LocalFeelmColors
import org.feelm.app.ui.theme.OnImage

@Composable
fun DetailScreen(
    type: String,
    slug: String,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onOpenWork: (type: String, slug: String) -> Unit,
    onOpenPerson: (slug: String) -> Unit,
    onSeen: (id: Long, type: String, slug: String) -> Unit,
) {
    val viewModel: DetailViewModel = viewModel(
        key = "$type/$slug",
        factory = DetailViewModel.factory(type, slug),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbars.showSnackbar(it)
            viewModel.noticeShown()
        }
    }

    // Opening a title is what "seen" means, so the badge clears here rather
    // than waiting for the next catch-up.
    LaunchedEffect(state.work?.id) {
        state.work?.let { onSeen(it.id, type, slug) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.error != null -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = state.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = viewModel::load) { Text(stringResource(R.string.app_tryAgain)) }
                    }
                }

                state.work != null -> WorkBody(
                    state = state,
                    bottomPadding = padding.calculateBottomPadding(),
                    onSetShelf = viewModel::setShelf,
                    onRate = viewModel::setShelfRating,
                    onProgress = viewModel::setShelfProgress,
                    onSaveReview = viewModel::saveReview,
                    onDeleteReview = viewModel::deleteReview,
                    onSignIn = onSignIn,
                    onOpenWork = onOpenWork,
                    onOpenPerson = onOpenPerson,
                )
            }

            // The page runs under the status bar so the backdrop can reach the
            // top of the screen. That is fine over artwork and unreadable over
            // scrolled body text, so a gradient keeps the clock legible
            // whatever has scrolled beneath it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x990A0C12), Color.Transparent)
                        )
                    ),
            )

            // Floats over the backdrop rather than sitting in an app bar: the
            // artwork is the top of this page and a bar would crop it. The
            // scrim is heavy enough to read against a scrolled paragraph, not
            // only against a dark corner of a poster.
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = padding.calculateTopPadding() + 4.dp, start = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xB30A0C12)),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OnImage,
                )
            }
        }
    }
}

@Composable
private fun WorkBody(
    state: DetailUiState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSetShelf: (String) -> Unit,
    onRate: (Double?) -> Unit,
    onProgress: (org.feelm.app.data.api.Progress) -> Unit,
    onSaveReview: (Double, String) -> Unit,
    onDeleteReview: () -> Unit,
    onSignIn: () -> Unit,
    onOpenWork: (String, String) -> Unit,
    onOpenPerson: (String) -> Unit,
) {
    val work = state.work ?: return
    val shelfStatus = state.shelfStatus
    val signedIn = state.signedIn
    val feelm = LocalFeelmColors.current
    val accent = feelm.forType(work.type, MaterialTheme.colorScheme.primary)
    var playingTrailer by remember { mutableStateOf(false) }

    if (playingTrailer) {
        work.trailer?.key?.let { key ->
            TrailerDialog(videoKey = key, onDismiss = { playingTrailer = false })
        }
    }

    val credits = listOfNotNull(
        work.details.directors.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.facet_directedBy) to it },
        work.details.creators.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.facet_createdBy) to it },
        work.details.authors.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.role_author) to it },
        work.details.developers.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.facet_developer) to it },
        work.details.writers.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.facet_writtenBy) to it },
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item("hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = work.backdrop ?: work.poster,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color(0x4D0A0C12),
                            0.5f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background,
                        )
                    ),
                )

                work.trailer?.key?.let { key ->
                    FilledTonalButton(
                        onClick = { playingTrailer = true },
                        modifier = Modifier.align(Alignment.Center),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xE6FFFFFF),
                            contentColor = Color(0xFF131823),
                        ),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text(stringResource(R.string.app_trailer), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item("heading") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                work.tagline?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = feelm.faint,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = work.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    val meta = buildMeta(work.type, work.year, work.details)
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (work.ratings.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        work.ratings.entries.take(3).forEach { (source, rating) ->
                            RatingChip(source = source, value = rating.rating, scale = rating.scale)
                        }
                    }
                }
            }
        }

        if (work.genres.isNotEmpty()) {
            item("genres") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(work.genres) { genre ->
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        item("shelf") {
            if (signedIn) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ShelfRow(type = work.type, status = shelfStatus, accent = accent, onSet = onSetShelf)
                    // Only once it is on a shelf: a score for something you
                    // have not said you are watching is a control with nothing
                    // to attach itself to.
                    if (shelfStatus != null) {
                        ShelfExtras(
                            type = work.type,
                            entry = state.entry,
                            saving = state.savingProgress,
                            onRate = onRate,
                            onProgress = onProgress,
                        )
                    }
                }
            } else {
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) { Text(stringResource(R.string.app_signInTrack)) }
            }
        }

        work.overview?.takeIf { it.isNotBlank() }?.let { overview ->
            item("overview") {
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (credits.isNotEmpty()) {
            item("credits") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    credits.forEach { (label, names) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = feelm.faint,
                                modifier = Modifier.width(88.dp),
                            )
                            Text(
                                text = names.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }

        if (work.details.seasons.isNotEmpty()) {
            item("seasons") {
                SeasonBrowser(
                    seasons = work.details.seasons,
                    watchedSeason = (state.entry?.progress as? kotlinx.serialization.json.JsonObject)
                        ?.get("season")?.let { it.toString().trim('"').toIntOrNull() },
                    watchedEpisode = (state.entry?.progress as? kotlinx.serialization.json.JsonObject)
                        ?.get("episode")?.let { it.toString().trim('"').toIntOrNull() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (work.details.cast.isNotEmpty()) {
            item("cast") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.cast_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(work.details.cast.take(20)) { member ->
                            CastCard(
                                member = member,
                                onClick = member.slug?.let { slug -> { onOpenPerson(slug) } },
                            )
                        }
                    }
                }
            }
        }

        // The rest of the series, in story order. Answered by one indexed
        // query server-side rather than assembled from whatever the home page
        // happened to have cached, which is how the web app used to do it.
        if (state.collection.size > 1) {
            item("collection") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.app_collection),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.collection) { sibling ->
                            val current = sibling.id == work.id
                            Text(
                                text = sibling.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                                color = if (current) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (current) accent
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable(enabled = !current) {
                                        onOpenWork(sibling.type, sibling.slug)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        item("reviews") {
            ReviewsSection(
                reviews = state.reviews,
                myReview = state.myReview,
                communityRating = state.communityRating,
                signedIn = signedIn,
                onSave = onSaveReview,
                onDelete = onDeleteReview,
                onSignIn = onSignIn,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.related.isNotEmpty()) {
            item("related") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.work_related),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.related, key = { it.id }) { other ->
                            PosterCard(
                                work = other,
                                onClick = { onOpenWork(other.type, other.slug) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Wishlist / Watching / Done.
 *
 * Three buttons rather than a dropdown: the shelves are the app's main verb,
 * and burying them one tap deeper to save a row of space is the wrong trade on
 * the one screen where somebody has decided they care about a title. "Dropped"
 * is deliberately absent — it belongs on the shelf itself, not here.
 */
@Composable
private fun ShelfRow(
    type: String,
    status: String?,
    accent: Color,
    onSet: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShelfButton(statusLabel(type, ShelfStatus.WISHLIST), Icons.Filled.Bookmark, status == ShelfStatus.WISHLIST, accent, Modifier.weight(1f)) {
            onSet(ShelfStatus.WISHLIST)
        }
        ShelfButton(statusLabel(type, ShelfStatus.ACTIVE), Icons.Filled.Visibility, status == ShelfStatus.ACTIVE, accent, Modifier.weight(1f)) {
            onSet(ShelfStatus.ACTIVE)
        }
        ShelfButton(statusLabel(type, ShelfStatus.DONE), Icons.Filled.CheckCircle, status == ShelfStatus.DONE, accent, Modifier.weight(1f)) {
            onSet(ShelfStatus.DONE)
        }
    }
}

@Composable
private fun ShelfButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(
            text = "  $label",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Each source in its own units — IMDb 8.2 reads as itself, not as 82%. */
@Composable
private fun RatingChip(source: String, value: Double, scale: Int) {
    val feelm = LocalFeelmColors.current
    val shown = if (scale == 100) (value / 10).toString().take(3) else value.toString().take(3)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = source.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = feelm.faint,
        )
        Text(
            text = shown,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = feelm.star,
        )
    }
}

@Composable
private fun CastCard(member: CastMember, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = member.photo,
            contentDescription = member.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        member.character?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = LocalFeelmColors.current.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

