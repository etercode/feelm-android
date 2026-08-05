package org.feelm.app.ui.profile

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
import org.feelm.app.data.api.CurrentEntry

data class UserShelfUiState(
    val loading: Boolean = true,
    val items: List<CurrentEntry> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
)

class UserShelfViewModel(
    private val repository: FeelmRepository,
    private val username: String,
    railKey: String,
) : ViewModel() {

    /*
     * The rail decides the query, not the screen: "loved" is done-by-rating
     * while "finished" is done-by-recency, so opening either has to reproduce
     * the exact filter the profile used or the full list will not match the
     * handful it was opened from.
     */
    private val spec = PROFILE_RAILS.firstOrNull { it.key == railKey }

    private val _state = MutableStateFlow(UserShelfUiState())
    val state = _state.asStateFlow()

    private var page = 1

    init {
        viewModelScope.launch {
            runCatching { repository.userShelf(username, spec?.status, spec?.sort, page = 1) }
                .onSuccess { response ->
                    _state.value = UserShelfUiState(
                        loading = false,
                        items = response.items,
                        total = response.total,
                        hasMore = response.page < response.pages,
                    )
                }
                .onFailure { _state.value = UserShelfUiState(loading = false) }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasMore) return

        _state.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            val next = page + 1
            runCatching { repository.userShelf(username, spec?.status, spec?.sort, page = next) }
                .onSuccess { response ->
                    page = next
                    _state.value = _state.value.copy(
                        loadingMore = false,
                        items = (_state.value.items + response.items).distinctBy { it.item.id },
                        hasMore = response.page < response.pages,
                    )
                }
                .onFailure { _state.value = _state.value.copy(loadingMore = false) }
        }
    }

    companion object {
        fun factory(username: String, railKey: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                            as FeelmApplication
                    UserShelfViewModel(app.container.repository, username, railKey)
                }
            }
    }
}
