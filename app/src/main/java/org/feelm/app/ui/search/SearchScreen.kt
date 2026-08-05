package org.feelm.app.ui.search

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.feelm.app.ui.components.FilterSheet
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.feelm.app.data.api.WorkSummary
import org.feelm.app.ui.components.PosterCard
import org.feelm.app.ui.metaLine
import org.feelm.app.ui.theme.LocalFeelmColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenWork: (type: String, slug: String) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 6
        }
    }
    LaunchedEffect(shouldLoadMore, state.hasMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Scaffold does not inset its topBar slot; TopAppBar handles that
        // itself and a bare field does not, so without this the search box is
        // drawn underneath the clock.
        topBar = {
            Column {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = { Text(stringResource(R.string.search_quickPlaceholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clear) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    viewModel.submit()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            /*
             * A hairline under the field, not a spinner over the list.
             *
             * The type-ahead fires on a 280ms debounce and answers in well
             * under a second, so a full-screen spinner would flash on every
             * word — and blanking results that are still perfectly good in
             * order to say "loading" reads as the app losing its place. This
             * says "still working" without taking anything away.
             */
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.submitted && state.total > 0) {
                    Text(
                        text = if (state.total == 1) "1 result" else "${state.total} results",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalFeelmColors.current.faint,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                val count = state.filters.filterCount
                TextButton(onClick = { sheetOpen = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = null)
                    Text(if (count > 0) "  Filters ($count)" else "  Filters")
                }
            }

            if (state.suggesting || state.searching) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.error != null -> Message(state.error ?: "")

                // A committed query: the full grid.
                state.submitted && state.results.isNotEmpty() -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 112.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(state.results, key = { it.id }) { work ->
                        PosterCard(
                            work = work,
                            onClick = { onOpenWork(work.type, work.slug) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Only call it empty once the search has actually finished.
                state.submitted && !state.searching ->
                    Message("Nothing found for “${state.query}”.")

                // Mid-typing: the overlay of rows.
                state.suggestions.isNotEmpty() || state.people.isNotEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    state.correction?.takeIf { it != state.query }?.let { correction ->
                        item("correction") {
                            Text(
                                text = "Did you mean “$correction”?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.submit(correction) }
                                    .padding(16.dp),
                            )
                        }
                    }

                    items(state.suggestions, key = { it.id }) { work ->
                        SuggestionRow(work = work, onClick = { onOpenWork(work.type, work.slug) })
                    }

                    if (state.people.isNotEmpty()) {
                        item("people-heading") {
                            Text(
                                text = stringResource(R.string.profile_tabPeople),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        items(state.people, key = { it.slug ?: it.name }) { person ->
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }

                // Mid-request with nothing yet to show: the bar under the field
                // is the feedback, so this stays deliberately blank rather
                // than flashing a second indicator.
                state.searching || state.suggesting -> Unit

                else -> Message("Search the catalog.")
            }

            if (state.loadingMore) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .size(28.dp),
                    strokeWidth = 3.dp,
                )
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            FilterSheet(
                query = state.filters,
                filters = state.available,
                // Search spans every type unless narrowed, so unlike browse it
                // offers the type as a filter rather than as a tab.
                showTypes = true,
                onApply = {
                    viewModel.applyFilters(it)
                    sheetOpen = false
                },
                onClear = {
                    viewModel.clearFilters()
                    sheetOpen = false
                },
            )
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One match, as a row.
 *
 * The subtitle is what tells two films of the same name apart, which is the
 * whole reason somebody is reading a list of matches rather than opening the
 * first one — so it is never dropped, even at this size.
 */
@Composable
private fun SuggestionRow(work: WorkSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = work.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(44.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = work.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = work.metaLine()
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalFeelmColors.current.faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = work.type.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = LocalFeelmColors.current
                .forType(work.type, MaterialTheme.colorScheme.primary),
        )
    }
}
