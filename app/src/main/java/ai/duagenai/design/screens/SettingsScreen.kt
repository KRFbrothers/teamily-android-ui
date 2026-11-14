package ai.duagenai.design.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.LocalDuaGenAISpacing

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
 *  Profile editing and sign-out are not implemented in this kit, so both
 *  controls are `enabled = false` with a helper line that says why — the same
 *  treatment ProfileScreen gives them. A button that looks live and swallows the
 *  tap is worse than a disabled one that explains itself.
 * ==========================================================================*/

/** The label of the (disabled) account-edit control. */
const val SETTINGS_EDIT_PROFILE_LABEL = "Edit profile"

/** The label of the (disabled) sign-out control. */
const val SETTINGS_SIGN_OUT_LABEL = "Sign out"

/** Why the account controls are disabled. */
const val SETTINGS_ACCOUNT_HELPER = "Account editing is not available in this build."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
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
                    // Both controls are honestly disabled: the kit has no account
                    // backend, so there is nothing for them to do. `enabled =
                    // false` publishes `disabled()` semantics, so a screen reader
                    // announces the state instead of the user discovering it by
                    // tapping a dead button.
                    SettingsRow(
                        title = SETTINGS_EDIT_PROFILE_LABEL,
                        subtitle = SETTINGS_ACCOUNT_HELPER,
                        icon = null,
                        onClick = {},
                        enabled = false,
                    )
                    HorizontalDivider(color = colors.hairline)
                    SettingsRow(
                        title = SETTINGS_SIGN_OUT_LABEL,
                        subtitle = "Signing out is not available in this build.",
                        icon = null,
                        onClick = {},
                        enabled = false,
                    )
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
        }
    }
}

@Preview(name = "Settings \u2014 light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsLight() = DuaGenAITheme(darkTheme = false) { SettingsScreen() }

@Preview(name = "Settings \u2014 dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsDark() = DuaGenAITheme(darkTheme = true) { SettingsScreen() }
