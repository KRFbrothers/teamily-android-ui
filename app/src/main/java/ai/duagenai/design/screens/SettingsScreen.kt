package ai.duagenai.design.screens

import ai.duagenai.design.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.duagenai.design.bot.LocalBotAppearance
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.MaxContentWidth
import ai.duagenai.design.theme.teamilyCanvas
import ai.duagenai.design.theme.teamilyPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable

/* ============================================================================
 *  Settings — the landing page behind Dua home's gear.
 *
 *  Soft atmosphere header (existing tokens, no new bitmap) with a large
 *  round avatar and the profile name (or "Not signed in"). Tapping the
 *  header opens Profile. The directory rows and groups are unchanged;
 *  they sit in a rounded surface sheet under the header. The app bar has
 *  a back arrow and a search icon that focuses the existing search field.
 *
 *  Sign in / Sign out still live here, once.
 * ==========================================================================*/

/** The label of the account-edit control (opens Profile). */
const val SETTINGS_EDIT_PROFILE_LABEL = "Edit profile"

/** The label of the sign-in control. */
const val SETTINGS_SIGN_IN_LABEL = "Sign in"

/** The label of the one sign-out control (Settings → Profile & Device). */
const val SETTINGS_SIGN_OUT_LABEL = "Sign out"

/** Legacy helper — no longer shown; Edit profile is now the way into Profile. */
const val SETTINGS_ACCOUNT_HELPER = "Account editing is not available in this build."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    photoPath: String? = null,
    photoVersion: Long = 0L,
    displayName: String = "",
    accountDataSource: RemoteAccountDataSource = RemoteAccountDataSource(),
    connectorSummary: String = "2/5",
    languageLabel: String = "English",
    themeModeLabel: String = "System default",
    storageLabel: String = "3.2 GB of 15 GB",
    notificationsLabel: String = "5 of 7 on",
    onOpenProfile: () -> Unit = {},
    onBack: () -> Unit = {},
    onOpenConnectors: () -> Unit = {},
    onOpenRuntimes: () -> Unit = {},
    onOpenSkills: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenChannels: () -> Unit = {},
    onOpenAgentApi: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    /**
     * Fired after Profile & Device → Sign out is confirmed and
     * [RemoteAccountDataSource.logout] has returned (the token is cleared
     * even if the server call fails). The nav host sends the user Home.
     */
    onSignedOut: () -> Unit = {},
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    val palette = teamilyPalette()
    val sessionToken by TokenStore.state.collectAsStateWithLifecycle()
    val signedIn = sessionToken != null
    val scope = rememberCoroutineScope()
    var showSignIn by rememberSaveable { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()

    val headerName = displayName.ifBlank { stringResource(R.string.profile_not_signed_in) }
    val openProfileLabel = stringResource(R.string.profile_open)
    val look = LocalBotAppearance.current
    val avatarCd = if (photoPath != null) {
        stringResource(R.string.profile_photo_cd)
    } else {
        stringResource(R.string.profile_avatar_cd, "${look.shape.label}, ${look.color.label}")
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchFocus.requestFocus() }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.settings_search_action),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.atmosphereBase,
                    scrolledContainerColor = palette.atmosphereBase,
                ),
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxSize()
                    .verticalScroll(scroll),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .teamilyCanvas(palette)
                        .padding(horizontal = spacing.gutter, vertical = spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {
                                contentDescription = openProfileLabel
                            }
                            .clickable(role = Role.Button, onClick = onOpenProfile),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ProfileOrCubeAvatar(
                            photoPath = photoPath,
                            photoVersion = photoVersion,
                            size = 96.dp,
                            contentDescription = avatarCd,
                        )
                        Spacer(Modifier.height(spacing.md))
                        Text(
                            text = headerName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = spacing.gutter,
                            vertical = spacing.md,
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_section_account),
                            style = DuaGenAITextStyles.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = DuaGenAITheme.shapes.card,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column {
                                if (!signedIn) {
                                    SettingsRow(
                                        title = SETTINGS_SIGN_IN_LABEL,
                                        subtitle = stringResource(R.string.settings_sign_in_subtitle),
                                        icon = Icons.AutoMirrored.Outlined.Login,
                                        onClick = { showSignIn = true },
                                    )
                                    HorizontalDivider(color = colors.hairline)
                                }
                                SettingsRow(
                                    title = SETTINGS_EDIT_PROFILE_LABEL,
                                    subtitle = stringResource(R.string.settings_edit_profile_subtitle),
                                    icon = Icons.Outlined.Person,
                                    onClick = onOpenProfile,
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.settings_section_about),
                            style = DuaGenAITextStyles.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = DuaGenAITheme.shapes.card,
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            Column(modifier = Modifier.padding(spacing.lg)) {
                                Text(
                                    text = stringResource(R.string.settings_about_app_name),
                                    style = DuaGenAITextStyles.rowTitle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.settings_about_kit),
                                    style = DuaGenAITextStyles.snippet,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        SettingsDirectory(
                            connectorSummary = connectorSummary,
                            languageLabel = languageLabel,
                            themeLabel = themeModeLabel,
                            storageLabel = storageLabel,
                            notificationsLabel = notificationsLabel,
                            onOpenConnectors = onOpenConnectors,
                            onOpenRuntimes = onOpenRuntimes,
                            onOpenSkills = onOpenSkills,
                            onOpenLanguage = onOpenLanguage,
                            onOpenChannels = onOpenChannels,
                            onOpenAgentApi = onOpenAgentApi,
                            onOpenAppearance = onOpenAppearance,
                            onOpenStorage = onOpenStorage,
                            onOpenNotifications = onOpenNotifications,
                            signedIn = signedIn,
                            onSignOut = {
                                scope.launch {
                                    accountDataSource.logout()
                                    onSignedOut()
                                }
                            },
                            searchFocusRequester = searchFocus,
                        )
                    }
                }
            }
        }
    }

    if (showSignIn) {
        AccountSignInDialog(
            accountDataSource = accountDataSource,
            onDismiss = { showSignIn = false },
        )
    }
}

/**
 * THE sign-in: email and password against [RemoteAccountDataSource.emailLogin].
 *
 * Settings → Sign in is the one sign-in path. It closes itself after a
 * successful login.
 */
@Composable
internal fun AccountSignInDialog(
    accountDataSource: RemoteAccountDataSource,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    SignInDialog(
        onDismiss = onDismiss,
        onSubmit = { email, password, onResult ->
            scope.launch {
                try {
                    accountDataSource.emailLogin(email, password)
                    onDismiss()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiError) {
                    onResult(e.userMessage)
                } catch (e: Exception) {
                    onResult(e.message?.takeIf { it.isNotBlank() } ?: "Sign in: no connection.")
                }
            }
        },
        onResetPassword = { email, onResult ->
            scope.launch {
                try {
                    accountDataSource.resetPassword(email)
                    onResult("Password reset email sent.")
                } catch (e: Exception) {
                    onResult(e.message ?: "Password reset failed.")
                }
            }
        },
    )
}

/**
 * Email and password for [RemoteAccountDataSource.emailLogin].
 *
 * [onSubmit] reports a failure message, or null once the call has succeeded.
 * The dialog closes itself only after success so a refused password stays on
 * screen with the reason.
 */
@Composable
private fun SignInDialog(
    onDismiss: () -> Unit,
    onSubmit: (email: String, password: String, onResult: (String?) -> Unit) -> Unit,
    onResetPassword: ((email: String, onResult: (String?) -> Unit) -> Unit)? = null,
) {
    val spacing = LocalDuaGenAISpacing.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var infoMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Sign in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = null
                        infoMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    singleLine = true,
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                        infoMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (onResetPassword != null && email.isNotBlank()) {
                    TextButton(
                        onClick = {
                            busy = true
                            error = null
                            infoMessage = null
                            onResetPassword(email) { res ->
                                busy = false
                                infoMessage = res
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Forgot password?", style = DuaGenAITextStyles.meta)
                    }
                }
                val info = infoMessage
                if (info != null) {
                    Text(
                        text = info,
                        style = DuaGenAITextStyles.meta,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                val message = error
                if (message != null) {
                    Text(
                        text = message,
                        style = DuaGenAITextStyles.meta,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                onClick = {
                    busy = true
                    error = null
                    infoMessage = null
                    onSubmit(email, password) { failure ->
                        busy = false
                        error = failure
                    }
                },
            ) {
                Text(if (busy) "Signing in\u2026" else "Sign in")
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Preview(name = "Settings \u2014 light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Settings \u2014 light \u2014 tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "Settings \u2014 light \u2014 foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Composable
private fun SettingsLight() = DuaGenAITheme(darkTheme = false) { SettingsScreen() }

@Preview(name = "Settings \u2014 dark", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Settings \u2014 dark \u2014 tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "Settings \u2014 dark \u2014 foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Composable
private fun SettingsDark() = DuaGenAITheme(darkTheme = true) { SettingsScreen() }
