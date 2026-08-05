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
import org.feelm.app.data.api.User

data class FollowListUiState(
    val loading: Boolean = true,
    val users: List<User> = emptyList(),
)

class FollowListViewModel(
    private val repository: FeelmRepository,
    private val username: String,
    private val following: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(FollowListUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val users = runCatching {
                if (following) repository.following(username) else repository.followers(username)
            }.getOrDefault(emptyList())
            _state.value = FollowListUiState(loading = false, users = users)
        }
    }

    companion object {
        fun factory(username: String, following: Boolean): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                            as FeelmApplication
                    FollowListViewModel(app.container.repository, username, following)
                }
            }
    }
}
