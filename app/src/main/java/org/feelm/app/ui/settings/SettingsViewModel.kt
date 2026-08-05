package org.feelm.app.ui.settings

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
import org.feelm.app.data.LocaleController
import org.feelm.app.data.api.User

data class SettingsUiState(
    val loading: Boolean = true,
    val user: User? = null,
    val saving: Boolean = false,
    val notice: String? = null,
    val error: String? = null,
)

class SettingsViewModel(private val repository: FeelmRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repository.me() }
                .onSuccess { _state.value = SettingsUiState(loading = false, user = it) }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = it.message ?: "Could not load your settings.",
                    )
                }
        }
    }

    fun saveProfile(name: String, tagline: String, bio: String, location: String) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "A display name is required.")
            return
        }
        save("Profile saved.") {
            repository.updateProfile(name, tagline, bio, location)
        }
    }

    fun savePreferences(locale: String, timezone: String) =
        save("Preferences saved.") {
            repository.updatePreferences(locale, timezone).also {
                // Recreates the activity, so the new language is on screen
                // before the snackbar confirming it has faded.
                LocaleController.apply(it.locale)
            }
        }

    fun uploadAvatar(bytes: ByteArray) =
        save("Picture updated.") { repository.uploadAvatar(bytes, "image/jpeg") }

    fun removeAvatar() = save("Picture removed.") {
        repository.deleteAvatar()
        repository.me()
    }

    /**
     * An account created through Google has never had a password, so the
     * current one goes up blank — the server accepts the bearer token as proof
     * in that case, and asking for something the user has never set would lock
     * them out of ever setting one.
     */
    fun changePassword(current: String, new: String) {
        if (new.length < 8) {
            _state.value = _state.value.copy(error = "Passwords are at least eight characters.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, notice = null)
            runCatching { repository.changePassword(current, new) }
                .onSuccess {
                    _state.value = _state.value.copy(saving = false, notice = "Password changed.")
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        saving = false,
                        error = "That did not work — check your current password.",
                    )
                }
        }
    }

    private fun save(notice: String, block: suspend () -> User) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, notice = null)
            runCatching { block() }
                .onSuccess { user ->
                    _state.value = _state.value.copy(saving = false, user = user, notice = notice)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        saving = false,
                        error = "Could not save that. Try again.",
                    )
                }
        }
    }

    fun messageShown() {
        _state.value = _state.value.copy(notice = null, error = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FeelmApplication
                SettingsViewModel(app.container.repository)
            }
        }
    }
}
