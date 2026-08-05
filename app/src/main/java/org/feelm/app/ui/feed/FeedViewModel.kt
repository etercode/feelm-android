package org.feelm.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.feelm.app.FeelmApplication
import org.feelm.app.data.FeelmRepository
import org.feelm.app.data.api.ActivityRow

data class FeedUiState(
    val loading: Boolean = true,
    val scope: String = "following",
    val activity: List<ActivityRow> = emptyList(),
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

/** following: you and the people you follow. me: only you. everyone: the site. */
val FEED_SCOPES = listOf(
    "following" to "Following",
    "everyone" to "Everyone",
    "me" to "You",
)

class FeedViewModel(private val repository: FeelmRepository) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state = _state.asStateFlow()

    private var page = 1

    init {
        load()
    }

    fun setScope(scope: String) {
        if (_state.value.scope == scope) return
        _state.value = _state.value.copy(scope = scope)
        load()
    }

    fun load() {
        page = 1
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, activity = emptyList())
            runCatching { repository.feed(_state.value.scope, page = 1) }
                .onSuccess { response ->
                    _state.value = _state.value.copy(
                        loading = false,
                        activity = response.activity,
                        hasMore = response.hasMore,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Could not load your feed.",
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
            runCatching { repository.feed(current.scope, page = next) }
                .onSuccess { response ->
                    page = next
                    _state.value = _state.value.copy(
                        loadingMore = false,
                        activity = _state.value.activity + response.activity,
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
                FeedViewModel(app.container.repository)
            }
        }
    }
}
