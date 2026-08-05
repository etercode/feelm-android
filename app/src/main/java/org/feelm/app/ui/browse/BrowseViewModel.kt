package org.feelm.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.feelm.app.FeelmApplication
import org.feelm.app.data.BrowseQuery
import org.feelm.app.data.FeelmRepository
import org.feelm.app.data.api.FiltersResponse
import org.feelm.app.data.api.WorkSummary

data class BrowseUiState(
    val query: BrowseQuery = BrowseQuery(type = "movie"),
    val items: List<WorkSummary> = emptyList(),
    val filters: FiltersResponse? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

/**
 * The catalog, sliced.
 *
 * Paging appends rather than replaces, and a change to the query resets to
 * page one — so switching type or applying a filter starts a new list instead
 * of grafting one set of results onto the tail of another.
 */
class BrowseViewModel(private val repository: FeelmRepository) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state = _state.asStateFlow()

    private var page = 1

    /**
     * Held so a fast tap through the type tabs cannot land results out of
     * order — the second request would otherwise sometimes answer first and be
     * overwritten by the first one's stale page.
     */
    private var loadJob: Job? = null

    init {
        reload()
        loadFilters()
    }

    private fun loadFilters() {
        viewModelScope.launch {
            runCatching { repository.filters() }
                .onSuccess { _state.value = _state.value.copy(filters = it) }
        }
    }

    fun setType(type: String) {
        if (_state.value.query.type == type) return
        update(_state.value.query.copy(type = type))
    }

    fun setSort(sort: String) {
        if (_state.value.query.sort == sort) return
        update(_state.value.query.copy(sort = sort))
    }

    fun apply(query: BrowseQuery) = update(query)

    fun clearFilters() {
        val current = _state.value.query
        update(BrowseQuery(type = current.type, sort = current.sort))
    }

    private fun update(query: BrowseQuery) {
        _state.value = _state.value.copy(query = query)
        reload()
    }

    fun reload() {
        page = 1
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, items = emptyList())
            runCatching { repository.browse(_state.value.query, page = 1) }
                .onSuccess { response ->
                    _state.value = _state.value.copy(
                        loading = false,
                        items = response.items,
                        hasMore = response.hasMore,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Could not load the catalog.",
                    )
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasMore) return

        _state.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            val next = page + 1
            runCatching { repository.browse(current.query, page = next) }
                .onSuccess { response ->
                    page = next
                    _state.value = _state.value.copy(
                        loadingMore = false,
                        // Guard against the same title arriving twice: the sort
                        // is not stable across pages for equal popularity, and
                        // a duplicate key crashes a lazy grid.
                        items = (_state.value.items + response.items).distinctBy { it.id },
                        hasMore = response.hasMore,
                    )
                }
                .onFailure { _state.value = _state.value.copy(loadingMore = false) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FeelmApplication
                BrowseViewModel(app.container.repository)
            }
        }
    }
}
