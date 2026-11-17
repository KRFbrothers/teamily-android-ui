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
import ai.duagenai.design.theme.TeamilyStroke
import ai.duagenai.design.theme.teamilyPalette
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
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
 *  leads with an outline icon. The mode
 *  row shows "Agent mode" with a WIREFRAME nav icon (hexagon + Y seam,
 *  stroke only, no fill), or "Dua mode" with a front-facing
 *  wireframe cube, also stroke only — distinct from Agent's pointy hexagon
 *  and from Library's isometric box in the HTML preview. Neither is a cube
 *  avatar and must not be drawn with [CubeAvatar]: the 42 cube assets are
 *  solid charcoal/orange fills, and these are 1.75dp (`--nv-icon-stroke`) outlines. Keeping them
 *  separate is what stops the two from being confused later.
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
    /**
     * ONE row for the Dua | Agent mode. It shows the CURRENT mode ("Agent mode"
     * / "Dua mode", with that mode's glyph) and a tap switches to the other
     * one. Agent home has no toggle, so this is its way to Dua.
     */
    ModeSwitch("Mode"),
}

/** Test tag on the footer avatar, carrying the cube it draws. */
internal fun drawerAvatarTag(cubeId: String) = "drawerAvatar:$cubeId"

/**
 * The drawer sheet, laid out like the ChatGPT Android drawer in Dua colours:
 *
 *   "Dua" title · round search button
 *   Library / Images / Computer / Videos / mode row  (icon + label, no highlight)
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
 * @param mode the current Home mode, shown by the [DrawerDestination.ModeSwitch] row.
 * @param history the same [ChatRow] list Home renders, so the two cannot drift.
 * @param onSelect fired by the mode row and the Chat pill. The host decides
 *   what each one does. Coming-soon rows never reach it.
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
        // Capped at 320dp FIRST, then filling: on a phone the home screen
        // stays visible as a strip beside the sheet (ChatGPT reference), and a
        // narrow multi-window pane still uses the whole window.
        modifier = modifier
            .widthIn(max = 320.dp)
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
                    Text(
                        text = "Dua",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
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

                /* ---------- nav rows ---------- */
                Column(Modifier.padding(vertical = spacing.xs)) {
                    DrawerDestination.entries
                        .filter { it != DrawerDestination.NewChat }
                        .forEach { destination ->
                            DrawerNavItem(
                                destination = destination,
                                mode = mode,
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
                    // Sample history, not the user's real chats yet.
                    Spacer(Modifier.width(spacing.xs))
                    DemoBadge()
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    // Room under the last row so the floating footer never
                    // covers it.
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
                ) {
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
 * One nav row: a 22dp outline icon, then the label, 56dp tall, no highlight.
 * The [DrawerDestination.ModeSwitch] row shows the current mode's glyph and
 * label, and TalkBack hears what a tap does ("Switch to Dua mode").
 */
@Composable
private fun DrawerNavItem(
    destination: DrawerDestination,
    mode: HomeMode,
    onClick: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val iconTint = MaterialTheme.colorScheme.onSurface
    val isModeRow = destination == DrawerDestination.ModeSwitch
    val label = if (isModeRow) "${mode.label} mode" else destination.label
    val other = if (mode == HomeMode.Agent) HomeMode.Dua else HomeMode.Agent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (isModeRow) {
                    Modifier.semantics { contentDescription = "Switch to ${other.label} mode" }
                } else {
                    Modifier
                },
            )
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
            // Wireframe glyphs, NOT cube avatars — see the file header.
            DrawerDestination.ModeSwitch -> when (mode) {
                HomeMode.Dua -> DuaCubeGlyph(tint = iconTint, modifier = iconModifier)
                HomeMode.Agent -> AgentCubeGlyph(tint = iconTint, modifier = iconModifier)
            }
            // Not listed as a row (it is the Chat pill).
            DrawerDestination.NewChat -> Unit
        }
        Spacer(Modifier.width(spacing.lg))
        Text(
            text = label,
            style = drawerRowText(),
            color = MaterialTheme.colorScheme.onSurface,
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

/**
 * The "Dua" nav glyph: a front-facing cube outline (square face, top and
 * right planes), stroke only. Drawn with [Canvas] for the same reason as
 * [AgentCubeGlyph] — it is a 1.75dp wireframe nav icon, not a catalogue avatar.
 */
@Composable
internal fun DuaCubeGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = Stroke(
            width = TeamilyStroke.icon.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val fl = size.width * 0.20f
        val ft = size.height * 0.34f
        val fr = size.width * 0.68f
        val fb = size.height * 0.82f
        val dx = size.width * 0.16f
        val dy = size.height * 0.20f

        val front = Path().apply {
            moveTo(fl, ft)
            lineTo(fr, ft)
            lineTo(fr, fb)
            lineTo(fl, fb)
            close()
        }
        drawPath(front, color = tint, style = stroke)

        val top = Path().apply {
            moveTo(fl, ft)
            lineTo(fl + dx, ft - dy)
            lineTo(fr + dx, ft - dy)
            lineTo(fr, ft)
            close()
        }
        drawPath(top, color = tint, style = stroke)

        val side = Path().apply {
            moveTo(fr, ft)
            lineTo(fr + dx, ft - dy)
            lineTo(fr + dx, fb - dy)
            lineTo(fr, fb)
            close()
        }
        drawPath(side, color = tint, style = stroke)
    }
}

/**
 * The "Agent" nav glyph: a hexagon outline with a Y seam, stroke only.
 *
 * Drawn with [Canvas] rather than added to `CubeIcons` on purpose. `CubeIcons`
 * is a generated catalogue of 42 solid-colour 24×24 cube avatars; this is a
 * 1.75dp wireframe nav icon. Putting it in the catalogue would break that
 * contract (and, because `CubeCatalog.forEntity` uses `h % COUNT`, changing
 * COUNT would silently re-map every entity's cube).
 */
@Composable
internal fun AgentCubeGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.44f

        fun at(deg: Double) = Offset(
            cx + (r * cos(Math.toRadians(deg))).toFloat(),
            cy + (r * sin(Math.toRadians(deg))).toFloat(),
        )

        val top = at(-90.0)
        val upperRight = at(-30.0)
        val lowerRight = at(30.0)
        val bottom = at(90.0)
        val lowerLeft = at(150.0)
        val upperLeft = at(210.0)

        val hexagon = Path().apply {
            moveTo(top.x, top.y)
            lineTo(upperRight.x, upperRight.y)
            lineTo(lowerRight.x, lowerRight.y)
            lineTo(bottom.x, bottom.y)
            lineTo(lowerLeft.x, lowerLeft.y)
            lineTo(upperLeft.x, upperLeft.y)
            close()
        }
        drawPath(hexagon, color = tint, style = Stroke(width = TeamilyStroke.icon.toPx()))

        // The Y seam: centre to top, centre to lower-left, centre to lower-right.
        val seam = Path().apply {
            moveTo(cx, cy)
            lineTo(top.x, top.y)
            moveTo(cx, cy)
            lineTo(lowerLeft.x, lowerLeft.y)
            moveTo(cx, cy)
            lineTo(lowerRight.x, lowerRight.y)
        }
        drawPath(seam, color = tint, style = Stroke(width = TeamilyStroke.icon.toPx()))
    }
}
