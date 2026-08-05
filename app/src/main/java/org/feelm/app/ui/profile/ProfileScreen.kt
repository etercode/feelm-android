package org.feelm.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.feelm.app.ui.components.PosterCard
import org.feelm.app.ui.components.Stars
import org.feelm.app.ui.mediaUrl
import org.feelm.app.ui.theme.LocalFeelmColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    username: String,
    onBack: () -> Unit,
    onOpenWork: (type: String, slug: String) -> Unit,
    onOpenFollows: (username: String, following: Boolean) -> Unit,
    onOpenShelf: (username: String, railKey: String) -> Unit,
    viewModel: ProfileViewModel = viewModel(
        key = username,
        factory = ProfileViewModel.factory(username),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feelm = LocalFeelmColors.current
    // Resolved here: the LazyColumn builder below is not a composable scope.
    val recentReviews = stringResource(R.string.app_recentReviews)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("@$username", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val profile = state.profile
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.error != null || profile == null -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.error ?: "Profile unavailable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = viewModel::load,
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text(stringResource(R.string.app_tryAgain)) }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item("banner") {
                        /*
                         * The server picks one title off their shelf as a
                         * banner. Without it the page opened on a small avatar
                         * against an empty screen and read as a stub — this is
                         * the same trick the detail page uses, and it costs
                         * nothing extra because the payload already carries it.
                         */
                        profile.banner?.let { banner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 7f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                AsyncImage(
                                    model = banner.backdrop ?: banner.poster,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(
                                            0f to Color(0x330A0C12),
                                            1f to MaterialTheme.colorScheme.background,
                                        )
                                    ),
                                )
                            }
                        }
                    }

                    item("head") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AsyncImage(
                                model = mediaUrl(profile.user.avatar),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                            Text(
                                text = profile.user.name?.takeIf { it.isNotBlank() }
                                    ?: profile.user.username.orEmpty(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            profile.user.tagline?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                Stat(profile.stats.logged, "logged")
                                Stat(profile.stats.finished, "finished")
                                Stat(profile.followersCount, "followers") {
                                    onOpenFollows(username, false)
                                }
                                Stat(profile.followingCount, "following") {
                                    onOpenFollows(username, true)
                                }
                            }

                            profile.sharedCount?.takeIf { it > 0 }?.let {
                                Text(
                                    text = "$it in common with you",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = feelm.faint,
                                )
                            }

                            // Absent entirely on your own profile, which is
                            // how the server says "not applicable".
                            profile.isFollowing?.let { following ->
                                if (following) {
                                    OutlinedButton(onClick = viewModel::toggleFollow) {
                                        Text(stringResource(R.string.profile_following))
                                    }
                                } else {
                                    Button(onClick = viewModel::toggleFollow) { Text(stringResource(R.string.follow_follow)) }
                                }
                            }
                        }
                    }

                    items(state.rails, key = { it.first.key }) { (spec, rail) ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    // The count reads as a kicker above the
                                    // heading, as it does on the web — it is
                                    // context for the shelf, not a label of
                                    // its own.
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.count_title,
                                            rail.total,
                                            rail.total,
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = feelm.faint,
                                    )
                                    Text(
                                        text = railTitle(spec.key),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                                // Only when the shelf holds more than the rail
                                // shows — otherwise it offers nothing.
                                if (rail.total > rail.items.size) {
                                    TextButton(
                                        onClick = { onOpenShelf(username, spec.key) },
                                    ) {
                                        Text(stringResource(R.string.common_seeAll))
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(rail.items, key = { it.item.id }) { row ->
                                    PosterCard(
                                        work = row.item,
                                        onClick = { onOpenWork(row.item.type, row.item.slug) },
                                    )
                                }
                            }
                        }
                    }

                    if (state.rails.isEmpty() && profile.current.isNotEmpty()) {
                        item("current") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Heading(stringResource(R.string.app_currently))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(profile.current, key = { it.item.id }) { row ->
                                        PosterCard(
                                            work = row.item,
                                            onClick = { onOpenWork(row.item.type, row.item.slug) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (profile.reviews.isNotEmpty()) {
                        item("reviews-heading") { Heading(recentReviews) }
                        items(profile.reviews, key = { it.id ?: 0L }) { review ->
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                review.rating?.let { Stars(rating = it, size = 14.dp) }
                                review.body?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Rail headings come from the dictionary, so they read as they do on the web. */
@Composable
private fun railTitle(key: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val id = context.resources.getIdentifier(
        "profile_rail_$key", "string", context.packageName,
    )
    return if (id == 0) key.replaceFirstChar { it.uppercase() } else context.getString(id)
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun Stat(value: Int, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalFeelmColors.current.faint,
        )
    }
}
