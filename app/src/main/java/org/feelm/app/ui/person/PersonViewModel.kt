package org.feelm.app.ui.person

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
import org.feelm.app.data.api.CreditGroup
import org.feelm.app.data.api.Person

data class PersonUiState(
    val loading: Boolean = true,
    val person: Person? = null,
    val credits: List<CreditGroup> = emptyList(),
    val total: Int = 0,
    val error: String? = null,
)

class PersonViewModel(
    private val repository: FeelmRepository,
    private val slug: String,
) : ViewModel() {

    private val _state = MutableStateFlow(PersonUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repository.person(slug) }
                .onSuccess { response ->
                    _state.value = PersonUiState(
                        loading = false,
                        person = response.person,
                        credits = response.credits,
                        total = response.total,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Could not load this person.",
                    )
                }
        }
    }

    companion object {
        fun factory(slug: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FeelmApplication
                PersonViewModel(app.container.repository, slug)
            }
        }
    }
}
