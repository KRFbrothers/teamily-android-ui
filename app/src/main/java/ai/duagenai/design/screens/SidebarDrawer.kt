/*
 * `Enum.entries` is still an experimental stdlib API in Kotlin 1.9.24, and this
 * file reads `DrawerDestination.entries` to build the drawer's nav list. Opting
 * in here is the narrow, honest fix: the API is stable in practice and the
 * alternative (`values()`) allocates a fresh array on every recomposition.
 *
 * A `@file:` annotation must precede the `package` declaration.
 */
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package ai.duagenai.design.screens

import ai.duagenai.design.R
import ai.duagenai.design.cube.CubeAvatar
import ai.duagenai.design.theme.DemoBadge
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.teamilyPalette
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/* ============================================================================
 *  Dua Gen AI — Sidebar drawer
 *
 *  The new shell's navigation. It replaces the bottom bar: the hamburger in
 *  Home's top app bar opens this sheet, and every destination lives here.
 *
 *  What it deliberately reuses instead of re-implementing:
 *
 *    • [CubeAvatar] for the footer avatar — the USER's saved cube, the same id
 *      Home's top bar and Profile draw. No second avatar implementation.
 *    • [ChatRow] for the history entries — the same list Home renders. The
 *      drawer shows only the title (ChatGPT style); opening a row lands on
 *      that conversation's own cube in ChatScreen.
 *
 *  Layout follows the ChatGPT Android drawer in Dua colours. Every nav row
 *  leads with an outline icon. The Dua | Agent mode switch is NOT a nav row:
 *  it is a segmented toggle ([DrawerModeToggle]) directly under the title,
 *  in the same pill language as Dua home's header toggle, with the active
 *  mode filled Dua green.
 *
 *  Colours are the kit's own green tokens — no purple anywhere. The mockup's
 *  #7C3AED voice button and #2563EB avatar ring are NOT carried over.
 * ==========================================================================*/

/**
 * Every destination the drawer offers.
 *
 * `Library`, `Images`, `Computer` and `Videos` have no screen yet
 * ([comingSoon] = true): the drawer answers a tap with the shared "Jaldi aa
 * raha hai" snackbar, stays open, and does NOT call `onSelect`.
 *
 * [NewChat] is not a nav row any more: it is the floating green "Chat" pill at
 * the bottom-left (ChatGPT Android layout), which fires the same `onSelect`.
 */
enum class DrawerDestination(val label: String, val comingSoon: Boolean = false) {
    NewChat("New chat"),
    Library("Library", comingSoon = true),
    Images("Images", comingSoon = true),
    Computer("Computer", comingSoon = true),
    Videos("Videos", comingSoon = true),
}

/** The drawer's one width cap (ChatGPT-like; leaves a home strip beside it). */
internal val DRAWER_MAX_WIDTH = 280.dp

/** Test tag on the footer avatar, carrying the cube it draws. */
internal fun drawerAvatarTag(cubeId: String) = "drawerAvatar:$cubeId"

/**
 * The drawer sheet, laid out like the ChatGPT Android drawer in Dua colours:
 *
 *   "Dua Gen AI" title · round search button
 *   [ Dua | Agent ] segmented toggle (active mode filled green)
 *   Library / Images / Computer / Videos  (icon + label, no highlight)
 *   thin divider
 *   HISTORY [Demo] · plain chat titles · "See all"
 *   green "Chat" pill (new chat)            round avatar (Profile)
 *
 * There is no close X: the M3 scrim (TalkBack "Close navigation menu"), a
 * swipe and system Back close it.
 *
 * Rendered inside [androidx.compose.material3.ModalNavigationDrawer], which
 * supplies the scrim and the open/close animation.
 *
 * @param cubeId the USER's saved cube, for the footer avatar only.
 * @param signedIn whether a session exists; read out on the avatar
 *   ("Profile, Signed in" / "Profile, Not signed in").
 * @param mode the current Home mode, shown as the active segment of the toggle.
 * @param history the same [ChatRow] list Home renders, so the two cannot drift.
 * @param onSelect fired by the Chat pill. The host decides what it does.
 *   Coming-soon rows never reach it.
 * @param onModeChange fired by a toggle segment with THAT segment's mode. The
 *   host closes the drawer and opens that mode's home (also when it is the
 *   active one).
 * @param onOpenChat fired by a history row, with that row's own id and title.
 * @param onOpenProfile fired by the footer avatar: the `profile` route.
 */
@Composable
fun SidebarDrawer(
    cubeId: String,
    signedIn: Boolean,
    mode: HomeMode,
    history: List<ChatRow>,
    onSelect: (DrawerDestination) -> Unit,
    onModeChange: (HomeMode) -> Unit,
    onOpenChat: (chatId: String, title: String) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    val palette = teamilyPalette()
    // Coming-soon taps answer inside the drawer itself: a snackbar in the app
    // Scaffold would sit behind the drawer's scrim and never be seen.
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val comingSoonText = stringResource(R.string.composer_coming_soon)
    val showComingSoon: () -> Unit = {
        snackbarScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(comingSoonText)
        }
    }

    ModalDrawerSheet(
        // Capped at DRAWER_MAX_WIDTH FIRST, then filling: on a phone the home screen
        // stays visible as a strip beside the sheet (ChatGPT reference), and a
        // narrow multi-window pane still uses the whole window.
        modifier = modifier
            .widthIn(max = DRAWER_MAX_WIDTH)
            .fillMaxWidth(),
        // Clean white in light (bgSurface #FFFFFF), dark surface in dark.
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = DuaGenAITheme.shapes.sheet,
    ) {
        Box(Modifier.fillMaxHeight()) {
            Column(Modifier.fillMaxHeight()) {

                /* ---------- title + round search ---------- */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = spacing.lg, end = spacing.md, top = spacing.lg, bottom = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The app's own name (`app_name`, "Dua Gen AI"). headlineSmall
                    // bold, one line, so it fits beside the 48dp search button
                    // at 280dp.
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // Search had no behaviour before (a static pill), so the
                    // round button answers with the coming-soon snackbar.
                    Surface(
                        onClick = showComingSoon,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
                        // Hairline edge so the white circle still reads where
                        // the shadow is faint (dark mode, flat renderers).
                        border = BorderStroke(1.dp, colors.hairline),
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Search" },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                /* ---------- Dua | Agent toggle ---------- */
                DrawerModeToggle(
                    mode = mode,
                    onModeChange = onModeChange,
                    // Wrap-content, left-aligned under the title. No extra
                    // vertical padding: the 48dp touch row already leaves 6dp
                    // of air above and below the 36dp pill.
                    modifier = Modifier.padding(horizontal = spacing.lg),
                )

                /* ---------- nav rows ---------- */
                Column(Modifier.padding(vertical = spacing.xs)) {
                    DrawerDestination.entries
                        .filter { it != DrawerDestination.NewChat }
                        .forEach { destination ->
                            DrawerNavItem(
                                destination = destination,
                                onClick = {
                                    if (destination.comingSoon) showComingSoon() else onSelect(destination)
                                },
                            )
                        }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                    color = colors.hairline,
                )

                /* ---------- history ---------- */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg, vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "HISTORY",
                        style = DuaGenAITextStyles.eyebrow,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    // Room under the last row so the floating footer never
                    // covers it.
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
                ) {
                    if (history.isEmpty()) {
                        // No sample conversations are shipped: the history
                        // says so instead of listing chats that are not there.
                        item {
                            Text(
                                text = "No conversations yet",
                                style = drawerRowText().copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sizeIn(minHeight = 48.dp)
                                    .padding(horizontal = spacing.lg, vertical = spacing.md),
                            )
                        }
                    } else {
                        items(history) { row ->
                            DrawerHistoryRow(row = row, onClick = { onOpenChat(row.chatId, row.name) })
                        }
                        // "All" was a static label; "See all" now answers like
                        // the other unbuilt destinations.
                        item {
                            Text(
                                text = "See all",
                                style = drawerRowText().copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button, onClick = showComingSoon)
                                    .sizeIn(minHeight = 48.dp)
                                    .padding(horizontal = spacing.lg, vertical = spacing.md),
                            )
                        }
                    }
                }
            }

            /* ---------- floating footer: Chat pill · avatar ---------- */
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = { onSelect(DrawerDestination.NewChat) },
                    shape = CircleShape,
                    color = palette.accent,
                    contentColor = palette.sendButtonIcon,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .sizeIn(minHeight = 52.dp)
                        .semantics { contentDescription = "Start a new chat" },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            text = "Chat",
                            style = drawerRowText().copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                // Round avatar only. TalkBack hears one label, e.g.
                // "Profile, Not signed in".
                val profileLabel = "Profile, ${if (signedIn) "Signed in" else "Not signed in"}"
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(role = Role.Button, onClick = onOpenProfile)
                        .semantics { contentDescription = profileLabel }
                        .testTag(drawerAvatarTag(cubeId)),
                    contentAlignment = Alignment.Center,
                ) {
                    CubeAvatar(
                        cubeId = cubeId,
                        size = 40.dp,
                        shape = CircleShape,
                        contentDescription = null,
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp),
            )
        }
    }
}

/** Drawer row text: regular weight, ~17sp, Lato via the theme. */
@Composable
private fun drawerRowText() = MaterialTheme.typography.bodyLarge.copy(
    fontSize = 17.sp,
    fontWeight = FontWeight.Normal,
)

/**
 * The Dua | Agent segmented control, under the drawer title.
 *
 * A slim, wrap-content pill in the same language as Dua home's header toggle
 * (`surfaceVariant` track, 3dp inset, pill cells, Lato, Role.Tab + selected) —
 * that composable is private to the frozen HomeScreen.kt, so it is not shared
 * from here. In the drawer the ACTIVE cell is filled with the theme primary
 * (Dua green #087C45) with `onPrimary` text, so the current mode reads at a
 * glance.
 *
 * SIZES — drawn 36dp tall (3dp inset + 30dp cells), cells 16dp horizontal
 * padding, 14sp text (bold active, regular inactive). The TOUCH target is the
 * full 48dp row height: each cell's clickable spans the row and the visuals
 * are drawn inset inside it (the track via `drawBehind`, the cell 9dp in), so
 * the pill looks compact without shrinking the tap area. The ripple is clipped
 * to the drawn cell.
 *
 * TalkBack hears "Switch to Dua/Agent mode, Tab, selected/not selected"; a tap
 * reports THAT cell's mode.
 */
@Composable
private fun DrawerModeToggle(
    mode: HomeMode,
    onModeChange: (HomeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier
            .height(MinTouchTarget)
            .drawBehind {
                val inset = (MinTouchTarget - ToggleVisualHeight).toPx() / 2f
                val h = ToggleVisualHeight.toPx()
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, inset),
                    size = Size(size.width, h),
                    cornerRadius = CornerRadius(h / 2f),
                )
            }
            .padding(horizontal = ToggleInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeMode.entries.forEach { entry ->
            val active = entry == mode
            val label = stringResource(
                if (entry == HomeMode.Dua) R.string.drawer_switch_to_dua else R.string.drawer_switch_to_agent,
            )
            val talkback = stringResource(
                if (entry == HomeMode.Dua) {
                    R.string.drawer_switch_to_dua_talkback
                } else {
                    R.string.drawer_switch_to_agent_talkback
                },
            )
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    // 48dp tall tap area; the drawn cell sits inside it.
                    .clickable(interaction, indication = null) { onModeChange(entry) }
                    .semantics {
                        contentDescription = talkback
                        role = Role.Tab
                        selected = active
                    }
                    .padding(vertical = (MinTouchTarget - ToggleVisualHeight) / 2 + ToggleInset)
                    .clip(DuaGenAITheme.shapes.pill)
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .indication(interaction, LocalIndication.current)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Drawn height of the drawer toggle's track (the touch row stays 48dp). */
private val ToggleVisualHeight = 36.dp

/** Gap between the drawer toggle's track edge and its cells. */
private val ToggleInset = 3.dp

/** One nav row: a 22dp outline icon, then the label, 56dp tall, no highlight. */
@Composable
private fun DrawerNavItem(
    destination: DrawerDestination,
    onClick: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val iconTint = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .sizeIn(minHeight = 56.dp)
            .padding(horizontal = spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconModifier = Modifier.size(22.dp)
        when (destination) {
            DrawerDestination.Library ->
                Icon(Icons.AutoMirrored.Outlined.LibraryBooks, null, iconModifier, tint = iconTint)
            DrawerDestination.Images -> Icon(Icons.Outlined.Image, null, iconModifier, tint = iconTint)
            DrawerDestination.Computer -> Icon(Icons.Outlined.Computer, null, iconModifier, tint = iconTint)
            DrawerDestination.Videos -> Icon(Icons.Outlined.VideoLibrary, null, iconModifier, tint = iconTint)
            // Not listed as a row (it is the Chat pill).
            DrawerDestination.NewChat -> Unit
        }
        Spacer(Modifier.width(spacing.lg))
        Text(
            text = destination.label,
            style = drawerRowText(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/** A history entry: the plain chat title, no avatar (ChatGPT style). */
@Composable
private fun DrawerHistoryRow(row: ChatRow, onClick: () -> Unit) {
    val spacing = LocalDuaGenAISpacing.current
    Text(
        text = row.name,
        style = drawerRowText(),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .sizeIn(minHeight = 52.dp)
            .padding(horizontal = spacing.lg, vertical = 14.dp),
    )
}
