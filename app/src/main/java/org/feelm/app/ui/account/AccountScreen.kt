package org.feelm.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.feelm.app.data.api.User
import org.feelm.app.ui.mediaUrl
import org.feelm.app.ui.theme.LocalFeelmColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenProfile: (username: String) -> Unit,
    onOpenFollows: (username: String, following: Boolean) -> Unit,
    onOpenFeed: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AccountViewModel = viewModel(factory = AccountViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val title = when {
        state.needsHandle -> stringResource(R.string.app_pickUsername)
        state.user != null -> stringResource(R.string.nav_account)
        state.mode == AuthMode.REGISTER -> stringResource(R.string.auth_registerTitle)
        else -> stringResource(R.string.nav_signIn)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Filled.LightMode
                            else Icons.Filled.DarkMode,
                            contentDescription = stringResource(if (darkTheme) R.string.app_useLight else R.string.app_useDark),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.checking -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                // Signing in with Google leaves the account holding a
                // placeholder handle. Everything public — a profile URL, a
                // review byline — reads from it, so this is asked before the
                // profile is shown rather than left to be discovered later.
                state.needsHandle -> HandleForm(
                    submitting = state.submitting,
                    error = state.error,
                    onSubmit = viewModel::chooseHandle,
                )

                state.user != null -> Profile(
                    user = state.user!!,
                    shelfCount = state.shelfCount,
                    onSignOut = viewModel::signOut,
                    onOpenProfile = onOpenProfile,
                    onOpenFollows = onOpenFollows,
                    onOpenFeed = onOpenFeed,
                    onOpenSettings = onOpenSettings,
                )

                else -> AuthForm(
                    mode = state.mode,
                    submitting = state.submitting,
                    error = state.error,
                    onModeChange = viewModel::setMode,
                    onSignIn = viewModel::signIn,
                    onRegister = viewModel::register,
                    onGoogle = viewModel::signInWithGoogle,
                )
            }
        }
    }
}

@Composable
private fun Profile(
    user: User,
    shelfCount: Int,
    onSignOut: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenFollows: (String, Boolean) -> Unit,
    onOpenFeed: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = mediaUrl(user.avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            text = user.name?.takeIf { it.isNotBlank() } ?: user.username.orEmpty(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        user.username?.let {
            Text(
                text = "@$it",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalFeelmColors.current.faint,
            )
        }
        user.tagline?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (shelfCount == 1) "1 title on your shelf" else "$shelfCount titles on your shelf",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The header links the web app carries, which have nowhere else to
        // live once the bottom bar is full.
        user.username?.let { username ->
            OutlinedButton(
                onClick = { onOpenProfile(username) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.app_yourProfile)) }

            /*
             * Straight to the two lists rather than only through the stat row
             * on the public profile — that was two taps deep behind a number
             * nothing marked as tappable, which is the same as not being there.
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onOpenFollows(username, false) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.profile_followers)) }
                OutlinedButton(
                    onClick = { onOpenFollows(username, true) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.profile_following)) }
            }
        }
        OutlinedButton(onClick = onOpenFeed, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.feed_activity))
        }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_title))
        }
        TextButton(onClick = onSignOut) { Text(stringResource(R.string.nav_signOut)) }
    }
}

@Composable
private fun AuthForm(
    mode: AuthMode,
    submitting: Boolean,
    error: String?,
    onModeChange: (AuthMode) -> Unit,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onGoogle: (android.content.Context) -> Unit,
) {
    val context = LocalContext.current
    val registering = mode == AuthMode.REGISTER

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    fun submit() {
        if (registering) onRegister(username, email, password, name)
        else onSignIn(username, password)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = { onGoogle(context) },
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.app_continueGoogle)) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.auth_googleOr),
                style = MaterialTheme.typography.labelMedium,
                color = LocalFeelmColors.current.faint,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.auth_username)) },
            singleLine = true,
            enabled = !submitting,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        if (registering) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email)) },
                singleLine = true,
                enabled = !submitting,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.auth_displayName)) },
                singleLine = true,
                enabled = !submitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.settings_password)) },
            singleLine = true,
            enabled = !submitting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = { submit() },
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(if (registering) "Create account" else "Sign in")
            }
        }

        TextButton(
            onClick = {
                onModeChange(if (registering) AuthMode.SIGN_IN else AuthMode.REGISTER)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(if (registering) R.string.app_haveAccount else R.string.app_newHere)
            )
        }
    }
}

@Composable
private fun HandleForm(
    submitting: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
) {
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "This is how other people will find you, and it shows on everything you review.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.auth_username)) },
            prefix = { Text("@") },
            singleLine = true,
            enabled = !submitting,
            supportingText = { Text(stringResource(R.string.app_handleHint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit(username) }),
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = { onSubmit(username) },
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.app_continue)) }
    }
}
