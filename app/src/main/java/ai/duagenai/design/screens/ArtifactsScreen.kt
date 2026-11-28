package ai.duagenai.design.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.disabled
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.TeamilyStroke
import ai.duagenai.design.theme.teamilyPalette
import ai.duagenai.design.theme.teamilyRestingShadow
import androidx.compose.foundation.layout.widthIn
import ai.duagenai.design.theme.MaxContentWidth

/* ============================================================================
 *  Artifacts — M3 components used:
 *      Scaffold, TopAppBar, IconButton + Badge, ModalBottomSheet, FilterChip,
 *      Surface (cards + caption), Text, LazyVerticalGrid, LazyRow,
 *      CircularProgressIndicator, Button, AlertDialog
 *
 *  The bottom nav's Artifacts tab. It follows the same shell as Discover /
 *  Agents / Memory (Scaffold + TopAppBar, no bottom nav of its own) so the tab
 *  behaves like every other tab that has a screen.
 *
 *  FOUR STATES, ONE SCREEN
 *  -----------------------
 *  The data comes from [ArtifactsRepository], NOT from a hardcoded list in this
 *  file. The screen collects the repository's [ArtifactsUiState] and renders
 *  exactly one of:
 *
 *    • Loading -> a centred CircularProgressIndicator
 *    • Loaded, non-empty -> a LazyVerticalGrid (Adaptive 160dp, 2 columns on a 390dp phone) of [ArtifactCard]s
 *    • Loaded, empty     -> the original centred empty state, kept verbatim
 *    • Error   -> the message plus a Retry action
 *
 *  THE THREE LATE AUDIT FIXES (v27)
 *  --------------------------------
 *  #7 FILTER BUTTON — the top bar's filter control opens a real
 *  [ModalBottomSheet] of type chips (All / Image / Document / Webpage / Video),
 *  applies them to the repository's list, shows the active count as a [Badge] on
 *  the button, and offers Clear. Filtering is a VIEW over the repository's state
 *  (a derived list), so it can never disagree with what the repository loaded.
 *
 *  #8 PREVIEW CAPTION — [ArtifactPreviewCaption] states what is being previewed:
 *  the artifact's own name, type and size, all read from [ArtifactItem]. The
 *  size comes from [artifactSizeLabel], so no caption string is hardcoded. It
 *  follows the selection: tapping a different card changes it.
 *
 *  #9 INERT GUARD — nothing here looks tappable and does nothing. Retry works,
 *  cards select, long-press asks before deleting, every sheet action applies,
 *  and when a filter hides everything the grid is replaced by an explicit
 *  no-match panel with a Clear action (never a silently blank area).
 *
 *  #10 TOUCH TARGETS — every interactive box is at least 48dp. IconButton and
 *  the chips already are; the card's tap region is its whole surface. The
 *  helper [minTouchTarget] documents the rule in one place.
 *
 *  THE LONG-PRESS CONTEXT MENU (this fix)
 *  --------------------------------------
 *  Long-pressing a card used to open the delete dialog directly. It now opens a
 *  Material 3 [DropdownMenu] anchored to the card, with FOUR actions in M3's
 *  order — the non-destructive ones first, then a divider, then the destructive
 *  one last:
 *
 *    • Open             -> selects the card, which is what the preview caption
 *                          follows
 *    • Open in new tab  -> hands the artifact's URL to the platform (ACTION_VIEW
 *                          via [LocalUriHandler]); DISABLED when the artifact
 *                          has no URL, so it can never be a silent no-op
 *    • Share            -> DISABLED in this build (a real share sheet is not
 *                          available), with a helper line saying so
 *    • ── divider ──
 *    • Delete           -> opens the SAME confirmation the swipe opens
 *
 *  The menu is only reachable from the loaded grid, so it is automatically
 *  unavailable while loading or in error, and every item is a 48dp target.
 *
 *  Every colour comes from MaterialTheme.colorScheme / DuaGenAITheme.colors and
 *  every gap from LocalDuaGenAISpacing, so light and dark are both correct by
 *  construction — there is no hardcoded colour in this file.
 * ==========================================================================*/

/**
 * The type filters the sheet offers (audit fix #7).
 *
 * [All] is the empty filter. Every other entry matches one artifact `type`
 * exactly — the same human labels [ArtifactItem.type] carries, so the chip set
 * cannot drift from the data without a compile-time-visible change here.
 */
enum class ArtifactFilter(val label: String) {
    All("All"),
    Image("Image"),
    Document("Document"),
    Webpage("Webpage"),
    Video("Video"),
    // Link and Code are real artifact types the app will produce but that the
    // demo set has none of yet. They are here for two reasons: the filter set
    // should cover the types the product actually makes, and they are what makes
    // the no-match panel (fix #9) REACHABLE — with only the four populated types
    // every chip matched something and that state could never be seen.
    Link("Link"),
    Code("Code");

    /** True when [item] passes this filter. */
    fun matches(item: ArtifactItem): Boolean = this == All || item.type == label
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactsScreen(
    repository: ArtifactsRepository,
    modifier: Modifier = Modifier,
    /**
     * How "Open in new tab" opens a URL.
     *
     * Kept as a plain `(String) -> Unit` so existing callers and tests are
     * unchanged, but it is no longer the opener itself: it is wrapped into a
     * [UrlLauncher] and handed to the SHARED [UrlOpener], which is the one place
     * the "open a URL in a new tab" rule lives. Memory's context menu calls the
     * same helper, so the two screens cannot drift.
     *
     * Defaults to the platform's own handler ([LocalUriHandler], which issues an
     * ACTION_VIEW intent on Android), so the screen needs no Context plumbing.
     * A test can pass its own lambda to observe that the intent fired without
     * leaving the process.
     */
    onOpenInNewTab: ((String) -> Unit)? = null,
    /**
     * Opens the sign-in surface (Profile).
     *
     * A 401 is NOT retryable — tapping Retry on it can never succeed, because the
     * request will go out anonymous again. The error state therefore offers
     * "Sign in" instead of "Retry" when the failure is an auth failure, and this
     * is where that button goes.
     */
    onSignIn: () -> Unit = {},
    /** The app's bottom dock. The nav host passes the shared [HomeBottomNav]. */
    bottomBar: @Composable () -> Unit = {},
) {
    val spacing = LocalDuaGenAISpacing.current
    // The shared opener. The injected lambda (or the platform handler) is the
    // launcher; the null/blank guard and the "did it open?" answer live in
    // UrlOpener, not here.
    val urlOpener = rememberUrlOpener(onOpenInNewTab?.let { launcher -> UrlLauncher(launcher) })
    val state by repository.state.collectAsStateWithLifecycle()
    // Pull-to-refresh's in-flight flag. Separate from `state` on purpose: a
    // refresh must not blank the grid, so it cannot go through Loading.
    val isRefreshing by repository.isRefreshing.collectAsStateWithLifecycle()
    // The undo snackbar's host, and the scope its coroutine runs in.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load once when the screen first appears. Re-running on Retry is the
    // repository's own `load()`, called from the error state's button.
    LaunchedEffect(repository) { repository.load() }

    // Which artifact is awaiting delete confirmation, or null. The id is
    // rememberSaveable (ArtifactItem is not a saveable type); the item is
    // looked up from the loaded list.
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    // The long-press context menu's subject, or null when the menu is closed.
    // Local UI state: the menu is a transient surface, not something a
    // rotation should resurrect.
    var menuFor by remember { mutableStateOf<ArtifactItem?>(null) }
    var revisionSubject by remember { mutableStateOf<ArtifactItem?>(null) }

    // The type filter (fix #7) and which card is being previewed (fix #8). Both
    // `rememberSaveable`, the same pattern the picker's search uses, so a
    // rotation keeps the user's filter and selection. Neither touches the
    // repository: the filter is a view over its state, the selection is UI only.
    var filter by rememberSaveable { mutableStateOf(ArtifactFilter.All) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    val loaded = state as? ArtifactsUiState.Loaded
    val all = loaded?.artifacts.orEmpty()
    // The visible list: the repository's list through the active filter. Derived,
    // never stored, so it cannot go stale.
    val visible = all.filter { filter.matches(it) }
    // What the caption describes. Falls back to the first visible card, so the
    // caption is never empty while a grid is on screen, and it follows the
    // selection when the user taps another card.
    val previewed = visible.firstOrNull { it.id == selectedId } ?: visible.firstOrNull()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Artifacts",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                actions = {
                    // Fix #7: the filter control. It OPENS a sheet (it used to be
                    // a bare toggle that filtered nothing), and the Badge is the
                    // active-filter count, so the button says whether a filter is
                    // on before the sheet is opened.
                    val activeCount = if (filter == ArtifactFilter.All) 0 else 1
                    BadgedBox(
                        badge = {
                            if (activeCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Text("$activeCount")
                                }
                            }
                        },
                    ) {
                        IconButton(
                            onClick = { sheetOpen = true },
                            modifier = Modifier
                                .minTouchTarget()
                                .semantics {
                                    contentDescription = "Filter"
                                },
                        ) {
                            FilterGlyph(
                                tint = if (activeCount > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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
                .fillMaxSize(),
        ) {
            // Fix #8: the caption sits directly under the top bar, above the
            // grid, so it describes the card the user is looking at. It is only
            // drawn when there IS something to preview — a caption over a
            // spinner or an empty account would be a lie.
            if (previewed != null) {
                ArtifactPreviewCaption(
                    artifact = previewed,
                    modifier = Modifier.padding(
                        start = spacing.gutter,
                        end = spacing.gutter,
                        bottom = spacing.sm,
                    ),
                )
            }

            // Pull-to-refresh (this fix). The box wraps the whole content area,
            // so the gesture works over the grid AND over the empty / error /
            // no-match states (each of which is scrollable — see
            // [ScrollableCentered]). `isRefreshing` drives the M3 indicator; the
            // content underneath is never replaced by a spinner, so the grid
            // stays visible while a refresh runs.
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { scope.launch { repository.refresh() } },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                when (val s = state) {
                    is ArtifactsUiState.Loading -> {
                        ArtifactsLoading(modifier = Modifier.fillMaxSize())
                    }

                    is ArtifactsUiState.Error -> {
                        ScrollableCentered {
                            ArtifactsError(
                                message = s.message,
                                onRetry = { scope.launch { repository.load() } },
                                // A 401/403 is not retryable: the honest action is
                                // to sign in, not to tap Retry forever. The
                                // repository already decided this via
                                // ApiError.retryable; the screen just has to read
                                // it instead of always offering Retry.
                                retryable = s.retryable,
                                onSignIn = onSignIn,
                                modifier = Modifier.padding(horizontal = spacing.gutter),
                            )
                        }
                    }

                    is ArtifactsUiState.Loaded -> {
                        when {
                            // A fresh account: the original empty state, verbatim.
                            s.artifacts.isEmpty() -> ScrollableCentered {
                                ArtifactsEmptyState(
                                    modifier = Modifier.padding(horizontal = spacing.gutter),
                                )
                            }

                            // Fix #9: the filter hid everything. An explicit
                            // panel with a way out — never a blank area that
                            // looks like a broken grid.
                            visible.isEmpty() -> ScrollableCentered {
                                ArtifactsNoMatch(
                                    filter = filter,
                                    onClear = { filter = ArtifactFilter.All },
                                    modifier = Modifier.padding(horizontal = spacing.gutter),
                                )
                            }

                            else -> LazyVerticalGrid(
                                // 160dp keeps 2 columns at 390dp (gutter 14 + spacedBy 6).
                                columns = GridCells.Adaptive(160.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = spacing.gutter,
                                    end = spacing.gutter,
                                    top = spacing.xxs,
                                    bottom = spacing.lg,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                            ) {
                                items(visible, key = { it.id }) { artifact ->
                                    SwipeToDeleteArtifactCard(
                                        artifact = artifact,
                                        selected = artifact.id == previewed?.id,
                                        onSelect = { selectedId = artifact.id },
                                        // Long-press opens the CONTEXT MENU (Open /
                                        // Share / Delete), not the delete dialog
                                        // directly. The menu's Delete item is what
                                        // opens the confirmation.
                                        menuOpen = menuFor?.id == artifact.id,
                                        onLongPress = { menuFor = artifact },
                                        onMenuDismiss = { menuFor = null },
                                        onOpen = {
                                            selectedId = artifact.id
                                            menuFor = null
                                        },
                                        onOpenInNewTab = {
                                            menuFor = null
                                            // The SHARED helper: it no-ops on a
                                            // null/blank URL and reports whether
                                            // it opened anything. The menu item is
                                            // already disabled in that case, so
                                            // this is belt-and-braces.
                                            urlOpener.open(artifact.url)
                                        },
                                        onRevise = {
                                            menuFor = null
                                            revisionSubject = artifact
                                        },
                                        onDelete = {
                                            menuFor = null
                                            pendingDeleteId = artifact.id
                                        },
                                        onSwipeDelete = { pendingDeleteId = artifact.id },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    // Fix #7: the filter sheet. Applying is immediate (a chip tap updates
    // `filter`, which recomputes the visible list) and Clear resets it.
    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            FilterSheetContent(
                filter = filter,
                onFilterChange = { filter = it },
                onClear = {
                    filter = ArtifactFilter.All
                    sheetOpen = false
                },
                onDone = { sheetOpen = false },
            )
        }
    }

    // The delete confirmation. Confirming calls the repository; the screen never
    // touches the list itself.
    val pendingDelete = all.firstOrNull { it.id == pendingDeleteId }
    pendingDelete?.let { artifact ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete artifact?") },
            text = { Text("\"${artifact.title}\" hamesha ke liye hat jayega.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // The ONE delete path: the context menu's Delete and the
                        // swipe both land here, so there is no duplicate logic to
                        // drift. The repository call is suspend (it goes through
                        // the data source), so it runs in the screen's scope.
                        pendingDeleteId = null
                        scope.launch {
                            // The ONE delete + undo + failure flow, shared with
                            // Memory (see WriteFeedback.kt). The success snackbar
                            // and its Undo appear ONLY when the source accepted
                            // the delete; a refused delete says so and offers no
                            // Undo, because there is nothing to undo.
                            repository.deleteWithUndo(
                                item = artifact,
                                snackbarHostState = snackbarHostState,
                                deletedMessage = "Artifact deleted",
                                undoFailedMessage = "Artifact restore nahi ho paya.",
                            )
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (revisionSubject != null) {
        val artifact = revisionSubject!!
        ArtifactRevisionSheet(
            artifact = artifact,
            onDismiss = { revisionSubject = null },
            onFetchVersions = { repository.fetchVersions(artifact.id) },
            onRevise = { instruction ->
                repository.reviseArtifact(artifact.id, instruction)
            },
        )
    }
}

/**
 * Fix #8 — the preview caption.
 *
 * "PREVIEW" eyebrow, then the artifact's name, type and size on one readable
 * line: `Q3 Launch Deck · Webpage · 2.3 MB`. Every part comes from the
 * [ArtifactItem] it describes (the size through [artifactSizeLabel]), so the
 * caption cannot say anything the card does not show.
 *
 * The whole line is also published as one `contentDescription`, which is what
 * makes "the caption matches the selected artifact" a checkable statement
 * rather than a visual impression.
 */
@Composable
private fun ArtifactPreviewCaption(artifact: ArtifactItem, modifier: Modifier = Modifier) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    val caption = "${artifact.title} · ${artifact.type} · ${artifactSizeLabel(artifact.sizeBytes)}"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Previewing $caption" },
        shape = DuaGenAITheme.shapes.card,
        color = colors.brandSoft,
    ) {
        Column(Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)) {
            Text(
                text = "PREVIEW",
                style = DuaGenAITextStyles.eyebrow,
                color = colors.onBrandSoft,
            )
            Text(
                text = caption,
                style = DuaGenAITextStyles.rowTitle,
                color = colors.onBrandSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Fix #7 — the filter sheet's body.
 *
 * A row of [FilterChip]s, the same control the cube picker uses, so the two
 * filter surfaces read alike. Selecting a chip applies it at once; Clear resets
 * to [ArtifactFilter.All]; Done just closes.
 */
@Composable
private fun FilterSheetContent(
    filter: ArtifactFilter,
    onFilterChange: (ArtifactFilter) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .padding(bottom = spacing.lg),
        ) {
        Text(
            text = "Filter by type",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                start = spacing.gutter,
                end = spacing.gutter,
                bottom = spacing.sm,
            ),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(ArtifactFilter.values().toList()) { entry ->
                FilterChip(
                    selected = entry == filter,
                    onClick = { onFilterChange(entry) },
                    // A distinct description per chip: the grid's cards also
                    // carry their type as text ("Image"), so a bare text match
                    // would be ambiguous. This makes the chip addressable.
                    modifier = Modifier
                        // The M3 48dp minimum, from the SHARED helper \u2014 the
                        // chip's drawn size is unchanged, only its tappable box
                        // grows. Same rule the Memory and Cube picker chip rows
                        // follow, so all three filter surfaces agree.
                        .minTouchTarget()
                        .semantics {
                            contentDescription = "Filter: ${entry.label}"
                        },
                    label = { Text(entry.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DuaGenAITheme.colors.brandSoft,
                        selectedLabelColor = DuaGenAITheme.colors.onBrandSoft,
                    ),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.gutter, vertical = spacing.sm),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onClear, modifier = Modifier.minTouchTarget()) {
                Text("Clear", style = DuaGenAITextStyles.button)
            }
            Spacer(Modifier.width(spacing.xs))
            TextButton(onClick = onDone, modifier = Modifier.minTouchTarget()) {
                Text("Done", style = DuaGenAITextStyles.button)
            }
        }
        }
    }
}

/**
 * Centres [content] in the content area AND keeps it scrollable.
 *
 * Pull-to-refresh is a nested-scroll gesture, so it only fires over a scrollable
 * child. The grid is one; the empty / error / no-match states are not, so they
 * are wrapped here in a one-item LazyColumn whose item fills the viewport
 * (`fillParentMaxSize`). That gives the state its centred layout back while
 * still giving the gesture something to scroll.
 */
@Composable
private fun ScrollableCentered(content: @Composable () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

/**
 * One artifact card, wrapped in a swipe-to-delete gesture.
 *
 * SWIPE (this fix) — dragging the card from end to start reveals an
 * error-tinted panel with a delete glyph, and completing the swipe opens the
 * SAME confirmation dialog the long-press opens. The box is told never to settle
 * dismissed (`confirmValueChange` returns false), so the card springs back and
 * the dialog owns the decision — no ghost card, no jump, and nothing is
 * destroyed by a stray swipe.
 *
 * The gesture is only reachable from the loaded grid, so it is automatically
 * unavailable while loading or in error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteArtifactCard(
    artifact: ArtifactItem,
    selected: Boolean,
    onSelect: () -> Unit,
    menuOpen: Boolean,
    onLongPress: () -> Unit,
    onMenuDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onRevise: () -> Unit = {},
    onDelete: () -> Unit,
    onSwipeDelete: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onSwipeDelete()
            // Never settle dismissed: the confirmation dialog decides, and the
            // card must not leave a ghost behind.
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(DuaGenAITheme.shapes.card)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                DeleteGlyph(
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(end = spacing.md)
                        .size(24.dp),
                )
            }
        },
    ) {
        // The card and its context menu share a Box, so the menu anchors to the
        // CARD the user pressed rather than to the screen. The menu is only
        // reachable from the loaded grid, so it is automatically unavailable
        // while loading or in error.
        Box {
            ArtifactCard(
                artifact = artifact,
                selected = selected,
                onSelect = onSelect,
                onLongPress = onLongPress,
            )
            ArtifactContextMenu(
                artifact = artifact,
                expanded = menuOpen,
                onDismiss = onMenuDismiss,
                onOpen = onOpen,
                onOpenInNewTab = onOpenInNewTab,
                onRevise = onRevise,
                onDelete = onDelete,
            )
        }
    }
}

/**
 * The long-press context menu (this fix).
 *
 * Four actions, in Material 3's order — non-destructive first, destructive last
 * behind a divider:
 *   • Open             -> selects the card, which is what the preview caption
 *                         follows
 *   • Open in new tab  -> opens the artifact's URL; DISABLED when it has none
 *   • Share            -> a real share sheet is not available in this build, so
 *                         the item is DISABLED and says so (never a silent no-op)
 *   • ── divider ──
 *   • Delete           -> opens the SAME confirmation the swipe opens
 *
 * Every item is a 48dp touch target, and each disabled item publishes
 * `disabled()` semantics so a screen reader announces it as unavailable.
 */
@Composable
private fun ArtifactContextMenu(
    artifact: ArtifactItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onRevise: () -> Unit = {},
    onDelete: () -> Unit,
) {
    // The URL is the item's own precondition: no URL, no enabled action.
    val hasUrl = !artifact.url.isNullOrBlank()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        DropdownMenuItem(
            text = { Text("Open", style = DuaGenAITextStyles.rowTitle) },
            onClick = onOpen,
            modifier = Modifier
                .minTouchTarget()
                .semantics { contentDescription = "Open ${artifact.title}" },
        )
        // Open in new tab. Enabled only when there is a URL to open — otherwise
        // it is a real disabled item with a helper line, not a tap that does
        // nothing.
        DropdownMenuItem(
            text = {
                Column {
                    Text(
                        text = "Open in new tab",
                        style = DuaGenAITextStyles.rowTitle,
                        color = if (hasUrl) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (!hasUrl) {
                        Text(
                            text = "No link for this artifact",
                            style = DuaGenAITextStyles.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            onClick = onOpenInNewTab,
            enabled = hasUrl,
            modifier = Modifier
                .minTouchTarget()
                .semantics {
                    contentDescription = "Open ${artifact.title} in new tab"
                    if (!hasUrl) disabled()
                },
        )
        DropdownMenuItem(
            text = { Text("Revisions & History", style = DuaGenAITextStyles.rowTitle) },
            onClick = onRevise,
            modifier = Modifier
                .minTouchTarget()
                .semantics { contentDescription = "View revisions for ${artifact.title}" },
        )
        // Share is inert in this build. It is DISABLED rather than a no-op tap,
        // and the helper line says why — audit fix #9's rule applied to a menu
        // item.
        DropdownMenuItem(
            text = {
                Column {
                    Text(
                        text = "Share",
                        style = DuaGenAITextStyles.rowTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Not available in this build",
                        style = DuaGenAITextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            onClick = {},
            enabled = false,
            modifier = Modifier
                .minTouchTarget()
                .semantics {
                    contentDescription = "Share ${artifact.title}"
                    disabled()
                },
        )
        // The destructive action is separated from the safe ones, per M3.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DropdownMenuItem(
            text = {
                Text(
                    text = "Delete",
                    style = DuaGenAITextStyles.rowTitle,
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = onDelete,
            modifier = Modifier
                .minTouchTarget()
                .semantics { contentDescription = "Delete ${artifact.title}" },
        )
    }
}

/**
 * The swipe panel's delete glyph: a bin with a lid, drawn with [Canvas].
 *
 * Same reason as the other glyphs — the kit ships `material-icons-core` only.
 * It takes a `tint`, never a literal colour.
 */
@Composable
private fun DeleteGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bar = TeamilyStroke.icon.toPx()
        // Lid.
        drawLine(color = tint, start = Offset(w * 0.16f, h * 0.28f), end = Offset(w * 0.84f, h * 0.28f), strokeWidth = bar, cap = StrokeCap.Round)
        // Handle.
        drawLine(color = tint, start = Offset(w * 0.40f, h * 0.28f), end = Offset(w * 0.40f, h * 0.16f), strokeWidth = bar, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.60f, h * 0.28f), end = Offset(w * 0.60f, h * 0.16f), strokeWidth = bar, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.40f, h * 0.16f), end = Offset(w * 0.60f, h * 0.16f), strokeWidth = bar, cap = StrokeCap.Round)
        // Body.
        val p = Path().apply {
            moveTo(w * 0.24f, h * 0.28f)
            lineTo(w * 0.30f, h * 0.86f)
            lineTo(w * 0.70f, h * 0.86f)
            lineTo(w * 0.76f, h * 0.28f)
        }
        drawPath(path = p, color = tint, style = Stroke(width = bar, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Two ribs.
        drawLine(color = tint, start = Offset(w * 0.42f, h * 0.42f), end = Offset(w * 0.44f, h * 0.72f), strokeWidth = bar, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.58f, h * 0.42f), end = Offset(w * 0.56f, h * 0.72f), strokeWidth = bar, cap = StrokeCap.Round)
    }
}

/** The first-load state: a centred spinner. */
@Composable
private fun ArtifactsLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** The error state: the message plus the action that can actually help. */
@Composable
private fun ArtifactsError(
    message: String,
    onRetry: () -> Unit,
    /**
     * Whether trying again could plausibly succeed.
     *
     * Comes from [ApiError.retryable] via the repository. A 401/403 is false, and
     * offering "Retry" for it was a dead button: the request goes out anonymous
     * again and fails identically. False shows "Sign in" instead.
     */
    retryable: Boolean = true,
    onSignIn: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ArtifactsGlyph(
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(spacing.lg))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.lg))
            Button(
                onClick = if (retryable) onRetry else onSignIn,
                shape = DuaGenAITheme.shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = if (retryable) "Retry" else "Sign in",
                    style = DuaGenAITextStyles.button,
                )
            }
        }
    }
}

/**
 * Fix #9 — the filtered-to-nothing state.
 *
 * When the active filter hides every artifact this panel replaces the grid, and
 * it says WHY the area is empty plus the one tap that fixes it. This is the
 * difference between "no results" and a grid that silently looks broken.
 */
@Composable
private fun ArtifactsNoMatch(
    filter: ArtifactFilter,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ArtifactsGlyph(
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(spacing.lg))
            Text(
                text = "Koi ${filter.label} artifact nahi mila",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = "Filter hata kar saari artifacts dekhein.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.lg))
            Button(
                onClick = onClear,
                shape = DuaGenAITheme.shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Clear filter", style = DuaGenAITextStyles.button)
            }
        }
    }
}

/**
 * One artifact, as a card.
 *
 * The layout is the kit's own card anatomy: a square thumbnail area (a soft
 * brand tile holding the type glyph), then the title, the type chip and the
 * timestamp. It reuses `DuaGenAITheme.shapes.card`, `colors.brandSoft` /
 * `onBrandSoft` and the shared text styles, so it reads like the memory cards
 * rather than like a new component.
 *
 * TAP vs LONG-PRESS — the card carries two actions, so it uses
 * `combinedClickable` rather than a single `clickable`:
 *   • tap        -> selects it, which is what the preview caption describes
 *                   (fix #8). Selection is drawn as the brand-soft card fill, so
 *                   the caption and the card always agree.
 *   • long-press -> opens the context menu (Open / Share / Delete); the menu's
 *                   Delete item opens the confirmation, so nothing is destroyed
 *                   on a single stray tap.
 *
 * Both are real actions — the card is not an inert surface that swallows taps.
 * Its whole area is the touch target, well above the 48dp minimum, and it
 * publishes `selected` so a screen reader announces the previewed card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtifactCard(
    artifact: ArtifactItem,
    selected: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // Resting glass shadow (`--nv-shadow-resting`).
            .teamilyRestingShadow(teamilyPalette(), TeamilyShapes.card)
            .clip(TeamilyShapes.card)
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongPress,
            )
            .semantics {
                this.selected = selected
                contentDescription = "${artifact.title}, ${artifact.type}"
            },
        // Forest-slate card: `cardGradient` fill, 1dp `borderGradient` edge,
        // 20dp corners. The selected (previewed) card keeps its existing
        // brandSoft fill and 2dp onBrandSoft ring so selection stays visible.
        shape = TeamilyShapes.card,
        // Solid `bgCanvas` under the translucent gradient keeps the swipe-to-
        // delete background from showing through the card.
        color = if (selected) colors.brandSoft else teamilyPalette().bgCanvas,
        border = if (selected) BorderStroke(2.dp, colors.onBrandSoft)
        else BorderStroke(1.dp, teamilyPalette().borderGradient),
    ) {
        Column(
            Modifier
                .then(
                    if (selected) Modifier
                    else Modifier.background(teamilyPalette().cardGradient),
                )
                .padding(spacing.sm),
        ) {
            // Thumbnail area: a square brand-soft tile holding the type glyph.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(DuaGenAITheme.shapes.card)
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface else colors.brandSoft,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ArtifactTypeGlyph(
                    type = artifact.type,
                    tint = if (selected) colors.onBrandSoft else colors.onBrandSoft,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = artifact.title,
                style = DuaGenAITextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(spacing.xxs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The type chip — the same brand-soft pill the memory cards use,
                // so it is a token, not a new colour.
                Box(
                    modifier = Modifier
                        .clip(DuaGenAITheme.shapes.pill)
                        .background(
                            if (selected) MaterialTheme.colorScheme.surface else colors.brandSoft,
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = artifact.type,
                        style = DuaGenAITextStyles.eyebrow,
                        color = colors.onBrandSoft,
                    )
                }
                Spacer(Modifier.width(spacing.sm))
                Text(
                    text = artifact.timestamp,
                    style = DuaGenAITextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Fix #7's button glyph: three staggered bars, drawn with [Canvas].
 *
 * `material-icons-core` ships no filter/sliders icon (and
 * `material-icons-extended` is a multi-megabyte dependency for one shape), so
 * the shape is drawn — the same call the header's filter button and the bottom
 * nav's `TabGlyph` make. It takes a `tint`, never a literal colour.
 */
@Composable
private fun FilterGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val bar = 2.dp.toPx()
        listOf(0.70f, 0.50f, 0.60f).forEachIndexed { index, widthFraction ->
            val y = h * (0.28f + index * 0.22f)
            drawLine(
                color = tint,
                start = Offset(w * 0.12f, y),
                end = Offset(w * (0.12f + 0.76f * widthFraction), y),
                strokeWidth = bar,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * A small glyph for an artifact's type.
 *
 * Drawn with [Canvas] for the same reason as `MemoryGlyph` — the kit ships
 * `material-icons-core` only, which has no per-type glyphs. Each type gets a
 * simple stroke shape so the card reads at a glance.
 */
@Composable
private fun ArtifactTypeGlyph(type: String, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height

        when (type) {
            // Image: a framed picture with a sun and a mountain.
            "Image" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.12f),
                    size = Size(w * 0.76f, h * 0.76f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = stroke,
                )
                drawCircle(color = tint, radius = w * 0.08f, center = Offset(w * 0.34f, h * 0.34f), style = stroke)
                val p = Path().apply {
                    moveTo(w * 0.22f, h * 0.72f)
                    lineTo(w * 0.44f, h * 0.50f)
                    lineTo(w * 0.60f, h * 0.66f)
                    lineTo(w * 0.72f, h * 0.54f)
                    lineTo(w * 0.84f, h * 0.72f)
                }
                drawPath(path = p, color = tint, style = stroke)
            }
            // Video: a rounded rectangle with a play triangle.
            "Video" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.10f, h * 0.22f),
                    size = Size(w * 0.80f, h * 0.56f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                    style = stroke,
                )
                val p = Path().apply {
                    moveTo(w * 0.42f, h * 0.36f)
                    lineTo(w * 0.66f, h * 0.50f)
                    lineTo(w * 0.42f, h * 0.64f)
                    close()
                }
                drawPath(path = p, color = tint, style = stroke)
            }
            // Webpage: a browser window with a title bar and two lines.
            "Webpage" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.10f, h * 0.14f),
                    size = Size(w * 0.80f, h * 0.72f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = stroke,
                )
                drawLine(color = tint, start = Offset(w * 0.10f, h * 0.30f), end = Offset(w * 0.90f, h * 0.30f), strokeWidth = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.24f, h * 0.46f), end = Offset(w * 0.76f, h * 0.46f), strokeWidth = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.24f, h * 0.60f), end = Offset(w * 0.60f, h * 0.60f), strokeWidth = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round)
            }
            // Document (default): a page with folded corner and two lines.
            else -> {
                val p = Path().apply {
                    moveTo(w * 0.30f, h * 0.12f)
                    lineTo(w * 0.70f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.30f)
                    lineTo(w * 0.88f, h * 0.88f)
                    lineTo(w * 0.12f, h * 0.88f)
                    lineTo(w * 0.12f, h * 0.12f)
                    close()
                }
                drawPath(path = p, color = tint, style = stroke)
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.30f), end = Offset(w * 0.70f, h * 0.30f), strokeWidth = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.44f), end = Offset(w * 0.70f, h * 0.44f), strokeWidth = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.58f), end = Offset(w * 0.70f, h * 0.58f), strokeWidth = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

/** The empty state — the screen's original body, kept as the fallback. */
@Composable
private fun ArtifactsEmptyState(modifier: Modifier = Modifier) {
    val spacing = LocalDuaGenAISpacing.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ArtifactsGlyph(
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(spacing.lg))
            Text(
                text = "Abhi koi artifact nahi hai",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = "Artifacts yahan dikhengi — jo files, pages aur decks Dua aur Agent banayenge.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The Artifacts glyph: three stacked layers, stroke only.
 *
 * Drawn with [Canvas] for the same reason as `MemoryGlyph` — the kit ships
 * `material-icons-core` only, which has no artifacts/layers glyph.
 */
@Composable
private fun ArtifactsGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = TeamilyStroke.icon.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height

        // Three chevron-like layers, top to bottom.
        listOf(0.30f, 0.50f, 0.70f).forEach { y ->
            val path = Path().apply {
                moveTo(w * 0.18f, h * y)
                lineTo(w * 0.50f, h * (y - 0.12f))
                lineTo(w * 0.82f, h * y)
            }
            drawPath(path = path, color = tint, style = stroke)
        }
    }
}

@Preview(name = "Artifacts — light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Artifacts — light — tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "Artifacts — light — foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Composable
private fun ArtifactsLight() = DuaGenAITheme(darkTheme = false) {
    ArtifactsScreen(repository = ArtifactsRepository(seed = demoArtifacts, loadDelayMs = 0))
}

@Preview(name = "Artifacts — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Artifacts — dark — tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "Artifacts — dark — foldable", showBackground = true, widthDp = 673, heightDp = 841)
@Composable
private fun ArtifactsDark() = DuaGenAITheme(darkTheme = true) {
    ArtifactsScreen(repository = ArtifactsRepository(seed = demoArtifacts, loadDelayMs = 0))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtifactRevisionSheet(
    artifact: ArtifactItem,
    onDismiss: () -> Unit,
    onFetchVersions: suspend () -> List<ArtifactVersionItem>,
    onRevise: suspend (String) -> Pair<ArtifactItem, ArtifactVersionItem>?,
) {
    val spacing = LocalDuaGenAISpacing.current
    val scope = rememberCoroutineScope()
    var versions by remember { mutableStateOf<List<ArtifactVersionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var instructionText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    // A failed fetch or revise used to escape the coroutine and take the app
    // down. Both now land here and are shown in the sheet instead.
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(artifact.id) {
        isLoading = true
        errorText = null
        versions = try {
            onFetchVersions()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorText = e.message?.takeIf { it.isNotBlank() }
                ?: "Revisions load nahi ho paye."
            emptyList()
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.gutter)
                .padding(bottom = spacing.xxl),
        ) {
            Text(
                text = "${artifact.title} · Revisions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = "Current Type: ${artifact.type} · ${artifactSizeLabel(artifact.sizeBytes)}",
                style = DuaGenAITextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(spacing.md))

            OutlinedTextField(
                value = instructionText,
                onValueChange = { instructionText = it },
                label = { Text("Quick Revision Instruction") },
                placeholder = { Text("e.g. Make header bold and add executive summary") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                enabled = !isSubmitting,
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            Button(
                onClick = {
                    val prompt = instructionText.trim()
                    if (prompt.isNotBlank()) {
                        scope.launch {
                            isSubmitting = true
                            errorText = null
                            try {
                                val result = onRevise(prompt)
                                if (result != null) {
                                    instructionText = ""
                                    versions = onFetchVersions()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                errorText = e.message?.takeIf { it.isNotBlank() }
                                    ?: "Revision fail ho gayi."
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                },
                enabled = instructionText.isNotBlank() && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("Revising...")
                } else {
                    Text("Revise with AI")
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(spacing.sm))

            errorText?.let { message ->
                Text(
                    text = message,
                    style = DuaGenAITextStyles.meta,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.xs)
                        .testTag("artifact_revision_error"),
                )
            }

            Text(
                text = "Version History (${versions.size})",
                style = DuaGenAITextStyles.rowTitle,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (versions.isEmpty()) {
                Text(
                    text = "No previous revisions recorded yet.",
                    style = DuaGenAITextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = spacing.sm),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    items(versions, key = { it.id }) { ver ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = DuaGenAITheme.shapes.card,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ) {
                            Column(modifier = Modifier.padding(spacing.sm)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Version ${ver.versionNum}",
                                        style = DuaGenAITextStyles.rowTitle,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (ver.createdAt.isNotBlank()) {
                                        Text(
                                            text = ver.createdAt,
                                            style = DuaGenAITextStyles.meta,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                ver.promptNote?.takeIf { it.isNotBlank() }?.let { note ->
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Instruction: $note",
                                        style = DuaGenAITextStyles.meta,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Artifacts — empty — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ArtifactsEmpty() = DuaGenAITheme(darkTheme = false) {
    ArtifactsScreen(repository = ArtifactsRepository(seed = emptyList(), loadDelayMs = 0))
}

@Preview(name = "Artifacts — empty — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ArtifactsEmptyDark() = DuaGenAITheme(darkTheme = true) {
    ArtifactsScreen(repository = ArtifactsRepository(seed = emptyList(), loadDelayMs = 0))
}

@Preview(name = "Artifacts — error — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ArtifactsErrorPreview() = DuaGenAITheme(darkTheme = false) {
    ArtifactsScreen(repository = ArtifactsRepository(seed = demoArtifacts, failOnLoad = true, loadDelayMs = 0))
}

@Preview(name = "Artifacts — error — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ArtifactsErrorDark() = DuaGenAITheme(darkTheme = true) {
    ArtifactsScreen(repository = ArtifactsRepository(seed = demoArtifacts, failOnLoad = true, loadDelayMs = 0))
}
