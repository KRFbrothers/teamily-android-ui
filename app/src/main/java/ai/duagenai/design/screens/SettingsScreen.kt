package ai.duagenai.design.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import ai.duagenai.design.theme.MaxContentWidth

/* ============================================================================
 *  Settings — the destination behind Home's gear.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  `DuaGenAINavHost` registered `composable(SETTINGS) { SettingsScreen() }`, but
 *  the composable was never shipped, so the nav graph could not compile and the
 *  gear led nowhere. This is that destination.
 *
 *  WHAT IT IS
 *  ----------
 *  The app-level settings that are NOT part of the Profile screen: the account
 *  controls (edit profile / sign out) and the build information. The Profile
 *  screen owns the personalisation rows (avatar, appearance, storage,
 *  notifications); this screen owns the account and the "about" block, so the
 *  two never duplicate a control.
 *
 *  HONESTY RULE
 *  ------------
 *  Profile editing is not implemented, so "Edit profile" stays disabled.
 *  "Sign in" is the ONE sign-in in the app: it asks for email and password and
 *  writes the session token (Profile's SESSION row opens this same dialog).
 *  "Sign out" exists once, in the directory's Profile & Device group, only
 *  while a session exists: confirm -> server logout -> Home.
 * ==========================================================================*/

/** The label of the (disabled) account-edit control. */
const val SETTINGS_EDIT_PROFILE_LABEL = "Edit profile"

/** The label of the sign-in control. */
const val SETTINGS_SIGN_IN_LABEL = "Sign in"

/** The label of the one sign-out control (Settings → Profile & Device). */
const val SETTINGS_SIGN_OUT_LABEL = "Sign out"

/** Why the account controls are disabled. */
const val SETTINGS_ACCOUNT_HELPER = "Account editing is not available in this build."


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    accountDataSource: RemoteAccountDataSource = RemoteAccountDataSource(),
    connectorSummary: String = "2/5",
    languageLabel: String = "English",
    themeModeLabel: String = "System default",
    storageLabel: String = "3.2 GB of 15 GB",
    notificationsLabel: String = "5 of 7 on",
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
    val sessionToken by TokenStore.state.collectAsStateWithLifecycle()
    val signedIn = sessionToken != null
    val scope = rememberCoroutineScope()
    var showSignIn by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = "ACCOUNT",
                style = DuaGenAITextStyles.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DuaGenAITheme.shapes.card,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    // Signed out, this row is the way in. Signed in, it is gone:
                    // the session already exists, and "Sign out" is the action.
                    if (!signedIn) {
                        SettingsRow(
                            title = SETTINGS_SIGN_IN_LABEL,
                            subtitle = "Use your Dua account email and password.",
                            icon = null,
                            onClick = { showSignIn = true },
                        )
                        HorizontalDivider(color = colors.hairline)
                    }
                    // Profile editing is not implemented. The row stays disabled
                    // whether or not a session exists, with the same helper line.
                    SettingsRow(
                        title = SETTINGS_EDIT_PROFILE_LABEL,
                        subtitle = SETTINGS_ACCOUNT_HELPER,
                        icon = null,
                        onClick = {},
                        enabled = false,
                    )
                    // Sign out is not repeated here: the one Sign out row
                    // lives in the directory below (Profile & Device).
                }
            }

            Text(
                text = "ABOUT",
                style = DuaGenAITextStyles.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DuaGenAITheme.shapes.card,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(spacing.lg)) {
                    Text(
                        text = "Dua Gen AI",
                        style = DuaGenAITextStyles.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Design kit \u00b7 Material 3",
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
            )
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
 * Settings → Sign in and Profile → SESSION both open this one dialog, so the
 * app has a single sign-in path. It closes itself after a successful login.
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
) {
    val spacing = LocalDuaGenAISpacing.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // In-flight flag is remember-only: the coroutine dies with the composition.
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
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
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
