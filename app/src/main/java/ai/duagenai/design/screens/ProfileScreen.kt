package ai.duagenai.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.duagenai.design.cube.CubeAvatar
import ai.duagenai.design.cube.CubeIds
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme

/* ============================================================================
 *  Profile — M3 components used:
 *      Scaffold, TopAppBar, Card (stat tiles + quick actions), Button,
 *      OutlinedButton, HorizontalDivider, Surface, Icon
 *
 *  The avatar is the saved cube (see CubePickerScreen / CubeStore), so the
 *  Profile header always agrees with the picker.
 *
 *  "Change cube" is the entry point to the picker: it shows the CURRENT cube as
 *  a thumbnail and, when tapped, calls [onChangeCube] — which the nav host wires
 *  to the `cube_picker` route. The picker's Save persists through CubeStore and
 *  the Flow re-emits, so this screen's `cubeId` updates on its own; Cancel
 *  writes nothing, so the header keeps the previously saved cube.
 *
 *  THE OTHER THREE ROWS
 *  --------------------
 *  Appearance, Storage and Notifications are wired the same way — each row is a
 *  real button that calls its own callback, and each shows the CURRENT value as
 *  its subtitle so the row itself says what is set:
 *
 *    • Appearance    -> [onOpenAppearance], subtitle = the saved theme mode
 *    • Storage       -> [onOpenStorage],    subtitle = used / total
 *    • Notifications -> [onOpenNotifications], subtitle = "N of M on"
 *
 *  The values are passed IN (the same plain-parameter pattern every other screen
 *  in this kit uses — no view-model, no new state library), so the nav host owns
 *  the single copy and the rows can never disagree with the screens they open.
 * ==========================================================================*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    cubeId: String = CubeIds.DEFAULT,
    onChangeCube: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onOpenStorage: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    /** The saved theme mode, shown as the Appearance row's subtitle. */
    themeModeLabel: String = "System default",
    /** The Storage row's subtitle, e.g. "3.2 GB of 15 GB". */
    storageLabel: String = "3.2 GB of 15 GB",
    /** The Notifications row's subtitle, e.g. "5 of 7 on". */
    notificationsLabel: String = "5 of 7 on",
    /**
     * Whether a session token is present.
     *
     * Drives the sign-in card: signed out shows the token field, signed in shows
     * the sign-out button. The nav host passes `TokenStore.session != null`.
     */
    signedIn: Boolean = false,
    /**
     * Hands a pasted session token to the app.
     *
     * THE ONLY WRITE PATH for a token in the whole app. Before this existed,
     * nothing in `app/src/main` ever set `TokenStore.token`, so the remote data
     * sources were never constructed and the app could not reach the backend at
     * all — the single biggest blocker in the review.
     *
     * A real build replaces this field with a Supabase/OAuth sign-in flow; the
     * seam is the same either way (`TokenStore.setToken`).
     */
    onSignIn: (String) -> Unit = {},
    /** Clears the session token. */
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(
                start = spacing.gutter,
                end = spacing.gutter,
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DuaGenAITheme.shapes.card,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // The saved cube, drawn untinted. `CubeAvatar` uses
                        // Image (not Icon) so the charcoal/orange art survives.
                        CubeAvatar(
                            cubeId = cubeId,
                            size = 72.dp,
                            shape = DuaGenAITheme.shapes.avatarProfile,
                            contentDescription = "Your avatar, ${CubeIds.label(cubeId)}",
                        )
                        Spacer(Modifier.height(spacing.md))
                        // NO HARDCODED IDENTITY.
                        //
                        // This used to be a literal name and email address in
                        // the source, which shipped one real person's PII in
                        // every build. The header now says what the app actually
                        // knows: whether there is a session.
                        Text(
                            text = if (signedIn) "Signed in" else "Not signed in",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (signedIn) {
                                "Session active \u00b7 Artifacts and Memory are live"
                            } else {
                                "Sign in below to load Artifacts and Memory"
                            },
                            style = DuaGenAITextStyles.snippet,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(spacing.sm))
                        Box(
                            modifier = Modifier
                                .clip(DuaGenAITheme.shapes.pill)
                                .background(colors.logoMint)
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = if (signedIn) "DUA GEN AI PRO" else "DUA GEN AI FREE",
                                style = DuaGenAITextStyles.eyebrow,
                                color = colors.onLogoMint,
                            )
                        }
                        Spacer(Modifier.height(spacing.lg))
                        // Audit fix #9: this used to be `onClick = {}` — a button
                        // that looked enabled and did nothing. Profile editing is
                        // not part of this kit, so the control is now HONESTLY
                        // disabled: M3 disabled colours, no ripple, not clickable,
                        // and `disabled()` semantics so a screen reader says so.
                        // The helper line below says why, instead of leaving the
                        // user tapping a dead button.
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = DuaGenAITheme.shapes.pill,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Text("Edit profile", style = DuaGenAITextStyles.button)
                        }
                        Spacer(Modifier.height(spacing.xs))
                        Text(
                            text = "Profile editing is not available in this build.",
                            style = DuaGenAITextStyles.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    StatTile("128", "CHATS", Modifier.weight(1f))
                    StatTile("4", "AGENTS", Modifier.weight(1f))
                    StatTile("37", "ARTIFACTS", Modifier.weight(1f))
                }
            }
            item {
                Text(
                    text = "QUICK ACTIONS",
                    style = DuaGenAITextStyles.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DuaGenAITheme.shapes.card,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column {
                        // The entry point to the picker. It shows the CURRENT
                        // cube as a thumbnail, so the row is a preview of what
                        // tapping it will let you change — and it reads the
                        // same `cubeId` the header above draws, so the two can
                        // never disagree.
                        SettingsRow(
                            title = "Change cube",
                            subtitle = "Avatar · ${CubeIds.label(cubeId)}",
                            icon = null,
                            onClick = onChangeCube,
                            leading = {
                                CubeAvatar(
                                    cubeId = cubeId,
                                    size = 32.dp,
                                    shape = DuaGenAITheme.shapes.avatarList,
                                    contentDescription = null,
                                )
                            },
                        )
                        HorizontalDivider(color = colors.hairline)
                        // Each of the three rows is a real button with its own
                        // destination, and its subtitle is the CURRENT value —
                        // so the row says what is set before you open it.
                        SettingsRow(
                            title = "Appearance",
                            subtitle = themeModeLabel,
                            icon = Icons.Filled.DarkMode,
                            onClick = onOpenAppearance,
                        )
                        HorizontalDivider(color = colors.hairline)
                        SettingsRow(
                            title = "Storage",
                            subtitle = storageLabel,
                            icon = null,
                            onClick = onOpenStorage,
                        )
                        HorizontalDivider(color = colors.hairline)
                        SettingsRow(
                            title = "Notifications",
                            subtitle = notificationsLabel,
                            icon = null,
                            onClick = onOpenNotifications,
                        )
                    }
                }
            }
            item {
                // THE SIGN-IN CARD.
                //
                // This is the control the review said was missing: the backend's
                // 401 body literally says "Sign in from Profile to use
                // Artifacts.", and Profile had no way to do it. Without a token
                // the app cannot reach the live service at all.
                //
                // It is deliberately a plain token field, not an OAuth flow: this
                // module has no auth dependency, and adding one is a product
                // decision. What it does provide is the real, working seam —
                // paste a session token, and both repositories flip to the remote
                // sources immediately (the nav host collects the StateFlow).
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DuaGenAITheme.shapes.card,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(modifier = Modifier.padding(spacing.lg)) {
                        Text(
                            text = "SESSION",
                            style = DuaGenAITextStyles.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(spacing.sm))
                        if (signedIn) {
                            Text(
                                text = "A session token is active. Artifacts and Memory load from the live service.",
                                style = DuaGenAITextStyles.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(spacing.md))
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier.fillMaxWidth(),
                                shape = DuaGenAITheme.shapes.pill,
                            ) {
                                Text("Sign out", style = DuaGenAITextStyles.button)
                            }
                        } else {
                            Text(
                                text = "Paste a session token to load Artifacts and Memory from the live service.",
                                style = DuaGenAITextStyles.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(spacing.md))
                            // Local draft only. The token is never logged, never
                            // persisted, and never rendered back once submitted.
                            var draft by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Session token") },
                                placeholder = { Text("eyJ\u2026") },
                            )
                            Spacer(Modifier.height(spacing.md))
                            Button(
                                onClick = {
                                    if (draft.isNotBlank()) {
                                        onSignIn(draft)
                                        // Clear the field so the token is not
                                        // left sitting in a text box.
                                        draft = ""
                                    }
                                },
                                enabled = draft.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = DuaGenAITheme.shapes.pill,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            ) {
                                Text("Sign in", style = DuaGenAITextStyles.button)
                            }
                        }
                    }
                }
            }
            // The old disabled "Sign out" block lived here. It is gone: sign-out
            // is now a real control inside the SESSION card above, so a second,
            // permanently-disabled button would be a lie about what the app can
            // do.
        }
    }
}

@Composable
private fun StatTile(number: String, label: String, modifier: Modifier = Modifier) {
    val spacing = LocalDuaGenAISpacing.current
    Surface(
        modifier = modifier,
        shape = DuaGenAITheme.shapes.statTile,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = number,
                style = DuaGenAITextStyles.statNumber,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = DuaGenAITextStyles.kicker,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: (() -> Unit)? = null,
    /** Optional leading slot, drawn before the icon. Used by "Change cube" to
     *  show the current cube as a thumbnail. */
    leading: (@Composable () -> Unit)? = null,
    /**
     * Whether the row can be tapped.
     *
     * A disabled row is not clickable AND publishes `disabled()` semantics, so a
     * screen reader announces the state instead of the user discovering it by
     * tapping a dead control. Its chevron is hidden too: a row that cannot go
     * anywhere must not advertise that it can.
     */
    enabled: Boolean = true,
) {
    val spacing = LocalDuaGenAISpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .sizeIn(minHeight = 48.dp)
            .padding(horizontal = spacing.md, vertical = spacing.md)
            .then(
                if (enabled) {
                    Modifier
                } else {
                    // Merge so the row title itself is what is announced as
                    // disabled. A separate text node would still sound tappable.
                    Modifier.semantics(mergeDescendants = true) { disabled() }
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(spacing.md))
        }
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = DuaGenAITextStyles.rowTitle,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = subtitle,
                style = DuaGenAITextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (enabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Profile — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileLight() = DuaGenAITheme(darkTheme = false) { ProfileScreen() }

@Preview(name = "Profile — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileDark() = DuaGenAITheme(darkTheme = true) { ProfileScreen() }