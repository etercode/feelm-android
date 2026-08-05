package org.feelm.app.ui.home

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
import org.feelm.app.data.api.HomeResponse

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Failed(val message: String) : HomeUiState
    data class Ready(val home: HomeResponse) : HomeUiState
}

/**
 * The front page, in one request.
 *
 * /api/home already decides which rails are worth drawing — a type the catalog
 * holds nothing for is left out of the response entirely — so this holds the
 * payload as it arrived rather than second-guessing it client-side.
 */
class HomeViewModel(private val repository: FeelmRepository) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            _state.value = runCatching { repository.home() }
                .fold(
                    onSuccess = { HomeUiState.Ready(it) },
                    onFailure = { HomeUiState.Failed(it.message ?: "Could not reach Feelm.") },
                )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FeelmApplication
                HomeViewModel(app.container.repository)
            }
        }
    }
}
