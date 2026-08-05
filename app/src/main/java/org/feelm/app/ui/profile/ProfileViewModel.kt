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
import org.feelm.app.data.api.ProfileRail
import org.feelm.app.data.api.ProfileResponse

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: ProfileResponse? = null,
    /** Their shelves, in the order a profile reads them. */
    val rails: List<Pair<RailSpec, ProfileRail>> = emptyList(),
    val error: String? = null,
)

/**
 * The order the web app shows them in, with the shelf query each one stands
 * for — which is what its "see all" has to reproduce.
 *
 * Titles come from the dictionary (`profile.rail.*`), so they read the same
 * as on the site: "Watching now", "Rated highest", "Recently finished".
 */
data class RailSpec(val key: String, val status: String, val sort: String?)

val PROFILE_RAILS = listOf(
    RailSpec("watching", "active", null),
    RailSpec("loved", "done", "rating"),
    RailSpec("finished", "done", null),
    RailSpec("wishlist", "wishlist", null),
    RailSpec("dropped", "dropped", null),
)

class ProfileViewModel(
    private val repository: FeelmRepository,
    private val username: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repository.profile(username) }
                .onSuccess { profile ->
                    _state.value = ProfileUiState(loading = false, profile = profile)
                    // Below the fold, so it loads after the header is drawn
                    // rather than holding the whole page for five shelves.
                    val overview = runCatching { repository.overview(username) }.getOrNull()
                    _state.value = _state.value.copy(
                        rails = PROFILE_RAILS.mapNotNull { spec ->
                            overview?.rails?.get(spec.key)
                                ?.takeIf { it.items.isNotEmpty() }
                                ?.let { spec to it }
                        },
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Could not load this profile.",
                    )
                }
        }
    }

    /**
     * Optimistic, and it moves the follower count with it — a button that
     * changes but leaves "1,204 followers" untouched reads as not having
     * worked.
     */
    fun toggleFollow() {
        val current = _state.value.profile ?: return
        val was = current.isFollowing ?: return

        _state.value = _state.value.copy(
            profile = current.copy(
                isFollowing = !was,
                followersCount = current.followersCount + if (was) -1 else 1,
            )
        )

        viewModelScope.launch {
            runCatching { repository.toggleFollow(username) }
                .onFailure { _state.value = _state.value.copy(profile = current) }
        }
    }

    companion object {
        fun factory(username: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FeelmApplication
                ProfileViewModel(app.container.repository, username)
            }
        }
    }
}
