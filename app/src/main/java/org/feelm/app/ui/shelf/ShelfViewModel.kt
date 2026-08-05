package org.feelm.app.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.feelm.app.FeelmApplication
import org.feelm.app.data.FeelmRepository
import org.feelm.app.data.api.ShelfStatus
import org.feelm.app.data.api.WorkSummary

data class ShelfUiState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
    /** Titles grouped by shelf, in the order the API returned them. */
    val byStatus: Map<String, List<WorkSummary>> = emptyMap(),
    /** How many entries exist in total, including ones with no title loaded. */
    val counts: Map<String, Int> = emptyMap(),
    val selected: String = ShelfStatus.WISHLIST,
    val error: String? = null,
)

val SHELF_TABS = listOf(
    ShelfStatus.WISHLIST to "Wishlist",
    ShelfStatus.ACTIVE to "Watching",
    ShelfStatus.DONE to "Done",
    ShelfStatus.DROPPED to "Dropped",
)

/**
 * Your shelves.
 *
 * /api/me/entries answers in two parts on purpose: the state of every entry
 * you have, and the sixty most recent titles behind them. The counts therefore
 * come from the full set while the posters come from the recent slice — a
 * shelf of three thousand cannot ship three thousand hydrated works, and the
 * server learned that by running out of memory trying.
 */
class ShelfViewModel(private val repository: FeelmRepository) : ViewModel() {

    private val _state = MutableStateFlow(ShelfUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            val signedIn = repository.isSignedIn.first()
            if (!signedIn) {
                _state.value = ShelfUiState(loading = false, signedIn = false)
                return@launch
            }

            runCatching { repository.entries() }
                .onSuccess { response ->
                    val statusById = response.entries.associate { it.itemId to it.status }
                    val grouped = response.items
                        .groupBy { statusById[it.id] }
                        .filterKeys { it != null }
                        .mapKeys { (status, _) -> status!! }

                    _state.value = ShelfUiState(
                        loading = false,
                        signedIn = true,
                        byStatus = grouped,
                        counts = response.entries
                            .mapNotNull { it.status }
                            .groupingBy { it }
                            .eachCount(),
                        selected = _state.value.selected,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        signedIn = true,
                        error = it.message ?: "Could not load your shelf.",
                    )
                }
        }
    }

    fun select(status: String) {
        _state.value = _state.value.copy(selected = status)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FeelmApplication
                ShelfViewModel(app.container.repository)
            }
        }
    }
}
