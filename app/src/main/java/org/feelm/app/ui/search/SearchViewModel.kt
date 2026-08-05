package org.feelm.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.feelm.app.FeelmApplication
import org.feelm.app.data.BrowseQuery
import org.feelm.app.data.FeelmRepository
import org.feelm.app.data.api.FiltersResponse
import org.feelm.app.data.api.PersonRef
import org.feelm.app.data.api.WorkSummary

data class SearchUiState(
    val query: String = "",
    val suggestions: List<WorkSummary> = emptyList(),
    val people: List<PersonRef> = emptyList(),
    /** The server's "did you mean", shown only when the query barely matched. */
    val correction: String? = null,
    val results: List<WorkSummary> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    /** The same filter vocabulary browse uses; /api/search takes all of it. */
    val filters: BrowseQuery = BrowseQuery(type = null, sort = "relevance"),
    val available: FiltersResponse? = null,
    val searching: Boolean = false,
    /** A type-ahead request is in flight. Distinct from a committed search. */
    val suggesting: Boolean = false,
    val submitted: Boolean = false,
    val error: String? = null,
)

/**
 * Type-ahead over the catalog, and the full result page behind it.
 *
 * Two endpoints because the server offers two: /api/search/suggest skips the
 * facet aggregation and returns trimmed rows, which is the difference between
 * a dropdown that keeps up with typing and one that does not. The full
 * /api/search only runs when somebody commits to a query.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: FeelmRepository) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    private val typed = MutableStateFlow("")
    private var page = 1

    init {
        viewModelScope.launch {
            runCatching { repository.filters() }
                .onSuccess { _state.value = _state.value.copy(available = it) }
        }

        viewModelScope.launch {
            typed
                // Long enough that a normal typing rhythm produces one request
                // per word rather than one per keystroke.
                .debounce(280)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _state.value = _state.value.copy(
                            suggestions = emptyList(),
                            people = emptyList(),
                            correction = null,
                            suggesting = false,
                        )
                        return@collectLatest
                    }

                    _state.value = _state.value.copy(suggesting = true)

                    runCatching { repository.suggest(query) }
                        .onSuccess { response ->
                            // collectLatest cancels this block when the next
                            // keystroke arrives, so a slow response for an old
                            // query can never overwrite a newer one.
                            _state.value = _state.value.copy(
                                suggestions = response.items,
                                people = response.people,
                                correction = response.suggestion,
                                suggesting = false,
                            )
                        }
                        .onFailure { _state.value = _state.value.copy(suggesting = false) }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _state.value = _state.value.copy(query = query, submitted = false, error = null)
        typed.value = query
    }

    fun clear() {
        _state.value = SearchUiState()
        typed.value = ""
    }

    fun submit(query: String = _state.value.query) {
        if (query.isBlank()) return

        _state.value = _state.value.copy(
            query = query,
            searching = true,
            submitted = true,
            error = null,
        )
        typed.value = query

        viewModelScope.launch {
            runCatching { repository.search(query, _state.value.filters, page = 1) }
                .onSuccess { response ->
                    page = 1
                    _state.value = _state.value.copy(
                        searching = false,
                        results = response.items,
                        total = response.total,
                        hasMore = response.hasMore,
                        correction = response.suggestion,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        searching = false,
                        error = it.message ?: "Search is unavailable right now.",
                    )
                }
        }
    }

    fun applyFilters(filters: BrowseQuery) {
        _state.value = _state.value.copy(filters = filters)
        // Re-run whatever is already on screen against the new filters, rather
        // than making somebody retype a query they have not changed.
        if (_state.value.submitted) submit()
    }

    fun clearFilters() = applyFilters(BrowseQuery(type = null, sort = "relevance"))

    fun loadMore() {
        val current = _state.value
        if (current.searching || current.loadingMore || !current.hasMore) return

        _state.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            val next = page + 1
            runCatching { repository.search(current.query, current.filters, page = next) }
                .onSuccess { response ->
                    page = next
                    _state.value = _state.value.copy(
                        loadingMore = false,
                        results = (_state.value.results + response.items).distinctBy { it.id },
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
                SearchViewModel(app.container.repository)
            }
        }
    }
}
