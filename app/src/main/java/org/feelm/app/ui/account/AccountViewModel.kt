package org.feelm.app.ui.account

import android.content.Context
import android.util.Log
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
import org.feelm.app.data.auth.GoogleSignIn
import org.feelm.app.data.auth.GoogleSignInResult

/** Which form the signed-out half of the screen is showing. */
enum class AuthMode { SIGN_IN, REGISTER }

data class AccountUiState(
    val checking: Boolean = true,
    val user: User? = null,
    val shelfCount: Int = 0,
    val mode: AuthMode = AuthMode.SIGN_IN,
    val submitting: Boolean = false,
    val error: String? = null,
) {
    /** A Google sign-up arrives with a handle it never chose. */
    val needsHandle: Boolean get() = user?.handlePending == true
}

/**
 * Sign in, sign up, pick a handle, sign out.
 *
 * One screen for all of it rather than a route each: signing in does not
 * change where you are, it changes what this page has to say, and navigating
 * for that would leave a login form in the back stack behind the profile.
 */
class AccountViewModel(
    private val repository: FeelmRepository,
    private val googleSignIn: GoogleSignIn,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(checking = true)
            val user = runCatching { repository.me() }.getOrNull()
            // The account's own preference wins over the phone's language.
            LocaleController.apply(user?.locale)
            val count = if (user != null) {
                runCatching { repository.entries().total }
                    // Swallowing this silently is what let a decoding failure
                    // masquerade as an empty shelf. It still must not break the
                    // page, but it should say so somewhere.
                    .onFailure { Log.w("Feelm", "Shelf count unavailable", it) }
                    .getOrDefault(0)
            } else {
                0
            }
            _state.value = _state.value.copy(checking = false, user = user, shelfCount = count)
        }
    }

    fun setMode(mode: AuthMode) {
        _state.value = _state.value.copy(mode = mode, error = null)
    }

    fun signIn(username: String, password: String) {
        if (username.isBlank() || password.isEmpty()) {
            _state.value = _state.value.copy(error = "Enter your username and password.")
            return
        }

        submit {
            runCatching { repository.signIn(username, password) }
                .onSuccess { refresh() }
                .onFailure {
                    // The API answers 401 with a body the user cannot act on,
                    // so say the one thing that is actually true.
                    fail("That username and password did not match.")
                }
        }
    }

    fun register(username: String, email: String, password: String, name: String) {
        when {
            username.length < 3 -> fail("Usernames are at least three characters.")
            !username.matches(Regex("^[A-Za-z0-9_]+$")) ->
                fail("Usernames may only contain letters, numbers and underscores.")
            !email.contains("@") -> fail("That does not look like an email address.")
            password.length < 8 -> fail("Passwords are at least eight characters.")
            else -> submit {
                runCatching { repository.register(username, email, password, name) }
                    .onSuccess { refresh() }
                    .onFailure { failure ->
                        // 409 is the only failure worth naming: it is the one
                        // the user can fix by choosing something else.
                        fail(
                            if (failure.message?.contains("409") == true) {
                                "That username or email is already taken."
                            } else {
                                "Could not create the account. Try again."
                            }
                        )
                    }
            }
        }
    }

    /**
     * Needs an Activity, not the application context — Credential Manager
     * shows a system sheet and has to have a window to show it over.
     */
    fun signInWithGoogle(activityContext: Context) {
        submit {
            when (val result = googleSignIn.requestIdToken(activityContext)) {
                is GoogleSignInResult.Success ->
                    runCatching { repository.signInWithGoogle(result.idToken) }
                        .onSuccess { refresh() }
                        .onFailure { fail("Feelm did not accept that Google account.") }

                // Dismissing the sheet is a decision, not a fault.
                is GoogleSignInResult.Cancelled -> Unit

                is GoogleSignInResult.Failed -> fail(result.message)
            }
        }
    }

    fun chooseHandle(username: String) {
        if (username.length < 3) {
            fail("Usernames are at least three characters.")
            return
        }

        submit {
            runCatching { repository.chooseHandle(username) }
                .onSuccess { user -> _state.value = _state.value.copy(user = user) }
                .onFailure { fail("That username is taken.") }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _state.value = AccountUiState(checking = false)
        }
    }

    private fun submit(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, error = null)
            block()
            _state.value = _state.value.copy(submitting = false)
        }
    }

    private fun fail(message: String) {
        _state.value = _state.value.copy(submitting = false, error = message)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FeelmApplication
                AccountViewModel(app.container.repository, app.container.googleSignIn)
            }
        }
    }
}
