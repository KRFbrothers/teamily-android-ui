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

import ai.duagenai.design.theme.DemoBadge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ai.duagenai.design.R
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.duagenai.design.cube.CubeAvatar
import ai.duagenai.design.cube.CubeIds
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyStroke
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.widthIn

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
 *    • [ChatRow] for the history entries — so a history row carries that
 *      conversation's own cube, derived from its stable `chatId` by the same
 *      `CubeCatalog.forEntity` the Home list and ChatScreen use. Opening a
 *      history row therefore lands on the same cube the row showed.
 *
 *  Every nav row leads with a 20dp outline icon (ChatGPT-style). The mode
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
 * raha hai" snackbar, stays open, and does NOT call `onSelect`. They are still
 * real enum entries so a screen can be wired later by flipping the flag.
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

/**
 * The drawer sheet.
 *
 * Rendered inside [androidx.compose.material3.ModalNavigationDrawer], which
 * supplies the scrim and the open/close animation — the M3 pattern the rest of
 * the kit already uses, so no custom animation code is introduced.
 *
 * @param cubeId the USER's saved cube, for the footer avatar only.
 * @param userName footer name ("Not signed in" when there is no session).
 * @param mode the current Home mode, shown by the [DrawerDestination.ModeSwitch] row.
 * @param history the same [ChatRow] list Home renders, so the two cannot drift.
 * @param selected the currently active destination, or null for none.
 * @param onSelect fired for every nav item. The host decides what each one does.
 * @param onOpenChat fired by a history row, with that row's own id and title.
 * @param onOpenProfile fired by the footer gear. The gear is the drawer's
 *   entry point to the PROFILE destination — the same `profile` route the
 *   NavHost already registers. Its icon and styling are unchanged; only the
 *   callback it fires and its accessibility label say what it now does.
 * @param onClose fired by the close X. The scrim is handled by the drawer host.
 */
@Composable
fun SidebarDrawer(
    cubeId: String,
    userName: String,
    mode: HomeMode,
    history: List<ChatRow>,
    selected: DrawerDestination?,
    onSelect: (DrawerDestination) -> Unit,
    onOpenChat: (chatId: String, title: String) -> Unit,
    onOpenProfile: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    // The coming-soon rows answer inside the drawer itself: a snackbar in the
    // app Scaffold would sit behind the drawer's scrim and never be seen.
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.composer_coming_soon)

    ModalDrawerSheet(
        // fillMaxWidth so a narrow multi-window pane still uses the window;
        // widthIn caps the drawer at the phone width of 320dp.
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = DuaGenAITheme.shapes.sheet,
    ) {
        Box(Modifier.fillMaxHeight()) {
        Column(Modifier.fillMaxHeight()) {

            /* ---------- brand + close ---------- */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.lg, end = spacing.sm, top = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Dua",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            /* ---------- search ---------- */
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                shape = DuaGenAITheme.shapes.pill,
                color = colors.field,
                // Lighter than before: the soft hairline instead of the heavy
                // `fieldEdge` outline, and a slimmer 44dp pill (not a tap target).
                border = BorderStroke(1.dp, colors.hairline),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 44.dp)
                        .padding(horizontal = spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(spacing.sm))
                    Text(
                        text = "Search",
                        style = DuaGenAITextStyles.snippet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            /* ---------- nav list ---------- */
            Column(
                modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                DrawerDestination.entries.forEach { destination ->
                    DrawerNavItem(
                        destination = destination,
                        mode = mode,
                        active = destination == selected,
                        onClick = {
                            if (destination.comingSoon) {
                                snackbarScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(comingSoon)
                                }
                            } else {
                                onSelect(destination)
                            }
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
                Spacer(Modifier.weight(1f))
                Text(
                    text = "All",
                    style = DuaGenAITextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = spacing.sm,
                    end = spacing.sm,
                    bottom = spacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                items(history) { row ->
                    DrawerHistoryRow(row = row, onClick = { onOpenChat(row.chatId, row.name) })
                }
            }

            HorizontalDivider(color = colors.hairline)

            /* ---------- footer ---------- */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The USER's saved cube — the same id Home's top bar and Profile
                // draw, so all three agree by construction.
                CubeAvatar(
                    cubeId = cubeId,
                    size = 36.dp,
                    shape = DuaGenAITheme.shapes.avatarList,
                    contentDescription = "Your avatar, ${CubeIds.label(cubeId)}",
                )
                Spacer(Modifier.width(spacing.md))
                // Name only: the plan badge and its Demo label are gone (the
                // plan was never read from billing).
                Text(
                    text = userName,
                    style = DuaGenAITextStyles.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenProfile) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
        )
        }
    }
}

/**
 * One nav row: a 20dp OUTLINE icon, then the label, in a 48dp pill.
 *
 * The active row is a soft brand pill — the same `brandSoft` / `onBrandSoft`
 * pair the kit's chips use. The [DrawerDestination.ModeSwitch] row shows the
 * current mode's glyph and label, and TalkBack hears what a tap does
 * ("Switch to Dua mode").
 */
@Composable
private fun DrawerNavItem(
    destination: DrawerDestination,
    mode: HomeMode,
    active: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    val iconTint = if (active) colors.onBrandSoft else MaterialTheme.colorScheme.onSurfaceVariant
    val isModeRow = destination == DrawerDestination.ModeSwitch
    val label = if (isModeRow) "${mode.label} mode" else destination.label
    val other = if (mode == HomeMode.Agent) HomeMode.Dua else HomeMode.Agent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DuaGenAITheme.shapes.pill)
            .background(if (active) colors.brandSoft else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (isModeRow) {
                    Modifier.semantics { contentDescription = "Switch to ${other.label} mode" }
                } else {
                    Modifier
                },
            )
            .sizeIn(minHeight = 48.dp)
            .padding(horizontal = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconModifier = Modifier.size(20.dp)
        when (destination) {
            DrawerDestination.NewChat -> Icon(Icons.Filled.Add, null, iconModifier, tint = iconTint)
            DrawerDestination.Library ->
                Icon(Icons.AutoMirrored.Outlined.LibraryBooks, null, iconModifier, tint = iconTint)
            DrawerDestination.Images -> Icon(Icons.Outlined.Image, null, iconModifier, tint = iconTint)
            DrawerDestination.Computer -> Icon(Icons.Outlined.Computer, null, iconModifier, tint = iconTint)
            DrawerDestination.Videos -> Icon(Icons.Outlined.VideoLibrary, null, iconModifier, tint = iconTint)
            // Wireframe glyphs, NOT cube avatars — see the file header.
            DrawerDestination.ModeSwitch -> when (mode) {
                HomeMode.Dua -> DuaCubeGlyph(tint = iconTint)
                HomeMode.Agent -> AgentCubeGlyph(tint = iconTint)
            }
        }
        Spacer(Modifier.width(spacing.md))
        Text(
            text = label,
            style = DuaGenAITextStyles.rowTitle,
            color = if (active) colors.onBrandSoft else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * A history entry.
 *
 * Carries that conversation's OWN cube (from [ChatRow.cubeId], i.e.
 * `CubeCatalog.forEntity(chatId)`), never the saved avatar — so the drawer, the
 * Home list and the thread all show the same cube for the same chat.
 */
@Composable
private fun DrawerHistoryRow(row: ChatRow, onClick: () -> Unit) {
    val spacing = LocalDuaGenAISpacing.current
    val cubeId = row.cubeId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DuaGenAITheme.shapes.card)
            .clickable(onClick = onClick)
            .sizeIn(minHeight = 48.dp)
            .padding(horizontal = spacing.sm, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CubeAvatar(
            cubeId = cubeId,
            size = 28.dp,
            shape = DuaGenAITheme.shapes.avatarMessage,
            contentDescription = "${row.name} avatar, ${CubeIds.label(cubeId)}",
        )
        Spacer(Modifier.width(spacing.sm))
        Text(
            text = row.name,
            style = DuaGenAITextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
