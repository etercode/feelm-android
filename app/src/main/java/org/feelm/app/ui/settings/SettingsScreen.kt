package org.feelm.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.feelm.app.ui.mediaUrl
import org.feelm.app.ui.theme.LocalFeelmColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Held between picking and cropping: the chosen photo, decoded once.
    var cropping by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                // Decoding a photo is not main-thread work.
                cropping = withContext(Dispatchers.IO) { decodeForCrop(context, uri) }
            }
        }
    }

    cropping?.let { source ->
        AvatarCropper(
            bitmap = source,
            onCancel = { cropping = null },
            onConfirm = { cropped ->
                cropping = null
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) { cropped.toJpegBytes() }
                    viewModel.uploadAvatar(bytes)
                }
            },
        )
    }

    LaunchedEffect(state.notice, state.error) {
        val message = state.notice ?: state.error
        if (message != null) {
            snackbars.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val user = state.user
            if (state.loading || user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.loading) CircularProgressIndicator()
                    else TextButton(onClick = viewModel::load) { Text(stringResource(R.string.app_tryAgain)) }
                }
                return@Box
            }

            var name by remember(user) { mutableStateOf(user.name.orEmpty()) }
            var tagline by remember(user) { mutableStateOf(user.tagline.orEmpty()) }
            var bio by remember(user) { mutableStateOf(user.bio.orEmpty()) }
            var location by remember(user) { mutableStateOf(user.location.orEmpty()) }
            var locale by remember(user) { mutableStateOf(user.locale ?: "en") }
            var timezone by remember(user) { mutableStateOf(user.timezone ?: "UTC") }
            var currentPassword by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Section(stringResource(R.string.settings_picture))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AsyncImage(
                        model = mediaUrl(user.avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                picker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            enabled = !state.saving,
                        ) { Text(stringResource(R.string.app_choosePicture)) }
                        if (user.avatar != null) {
                            TextButton(
                                onClick = viewModel::removeAvatar,
                                enabled = !state.saving,
                            ) { Text(stringResource(R.string.common_remove)) }
                        }
                    }
                }
                HorizontalDivider()
                Section(stringResource(R.string.settings_profile))
                Field(stringResource(R.string.auth_displayName), name) { name = it }
                Field(stringResource(R.string.app_tagline_field), tagline) { tagline = it }
                Field(stringResource(R.string.app_bio), bio, minLines = 3) { bio = it }
                Field(stringResource(R.string.settings_location), location) { location = it }
                Button(
                    onClick = { viewModel.saveProfile(name, tagline, bio, location) },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.settings_saveProfile)) }

                HorizontalDivider()
                Section(stringResource(R.string.app_region))
                ChoiceField(
                    label = stringResource(R.string.settings_language),
                    value = locale,
                    options = LANGUAGE_OPTIONS,
                ) { locale = it }
                ChoiceField(
                    label = stringResource(R.string.settings_timezone),
                    value = timezone,
                    options = TIMEZONE_OPTIONS,
                ) { timezone = it }
                Button(
                    onClick = { viewModel.savePreferences(locale, timezone) },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.settings_savePreferences)) }

                HorizontalDivider()
                Section(stringResource(R.string.settings_password))
                if (user.hasPassword) {
                    Field(
                        label = stringResource(R.string.settings_currentPassword),
                        value = currentPassword,
                        password = true,
                    ) { currentPassword = it }
                } else {
                    Text(
                        text = "You signed in with Google, so there is no current password to confirm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalFeelmColors.current.faint,
                    )
                }
                Field(stringResource(R.string.settings_newPassword), newPassword, password = true) { newPassword = it }
                OutlinedButton(
                    onClick = { viewModel.changePassword(currentPassword, newPassword) },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.settings_changePassword)) }
            }
        }
    }
}

/** The four the site speaks — same list as `lib/i18n/locales.js`. */
private val LANGUAGE_OPTIONS = listOf(
    "en" to "English",
    "az" to "Azərbaycan",
    "tr" to "Türkçe",
    "ru" to "Русский",
)

/**
 * Zones worth offering, not all 400-odd.
 *
 * A free-text box was the wrong control twice over: nobody types
 * "Asia/Baku" from memory, and a typo is silently accepted by a field that
 * cannot know it is wrong.
 */
private val TIMEZONE_OPTIONS = listOf(
    "UTC", "Asia/Baku", "Europe/Istanbul", "Europe/Moscow", "Europe/London",
    "Europe/Berlin", "Europe/Paris", "America/New_York", "America/Los_Angeles",
    "Asia/Dubai", "Asia/Tokyo", "Australia/Sydney",
).map { it to it.substringAfter('/').replace('_', ' ') }

/**
 * A closed set of choices, shown as one.
 *
 * Anything the server validates against a fixed list has no business being a
 * text field — the only thing free text adds is the chance to get it wrong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val shown = options.firstOrNull { it.first == value }?.second ?: value

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = shown,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onChange(code)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    minLines: Int = 1,
    password: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = minLines == 1,
        minLines = minLines,
        visualTransformation = if (password) PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}
