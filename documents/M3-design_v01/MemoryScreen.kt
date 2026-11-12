package ai.duagenai.design.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.teamilyPalette

/* ============================================================================
 *  Memory — M3 components used:
 *      Scaffold, TopAppBar, OutlinedTextField, FilterChip, LazyRow,
 *      Surface (cards), Text, LazyColumn, PullToRefreshBox,
 *      CircularProgressIndicator, Button
 *
 *  The bottom nav's Memory tab. It follows the same shell as Discover / Agents
 *  (Scaffold + TopAppBar, no bottom nav of its own) so the tab behaves like
 *  every other tab that has a screen.
 *
 *  FOUR STATES, ONE SCREEN
 *  -----------------------
 *  The data comes from [MemoryRepository], NOT from a hardcoded list in this
 *  file. The screen collects the repository's [MemoryUiState] and renders
 *  exactly one of:
 *
 *    • Loading -> a centred CircularProgressIndicator
 *    • Loaded, non-empty -> a LazyColumn of [MemoryCard]s, newest first
 *    • Loaded, empty     -> the original centred empty state, kept verbatim
 *    • Error   -> the message plus a Retry action
 *
 *  SEARCH + FILTER
 *  ---------------
 *  The same pattern the Artifacts screen uses, so the two filter surfaces read
 *  alike: a search field with a clear (X) button, a row of [FilterChip]s, and a
 *  no-match panel with a way out. Search and chip apply TOGETHER, and both are
 *  a VIEW over the loaded list (a derived `visible`), so the list and the filter
 *  can never disagree.
 *
 *  PULL-TO-REFRESH
 *  ---------------
 *  The list is wrapped in a [PullToRefreshBox] wired to the repository's
 *  [MemoryRepository.refresh], which re-runs the SAME `load()` path. The
 *  indicator is driven by `isRefreshing`, which is deliberately separate from
 *  the UI state, so the list stays visible under the indicator instead of being
 *  replaced by a spinner. The active search and chip are untouched by a refresh
 *  — they are screen state, not repository state.
 *
 *  SWIPE-TO-DELETE (this fix)
 *  --------------------------
 *  Every row is wrapped in a [SwipeToDismissBox], exactly the pattern the
 *  Artifacts grid uses. Dragging a row end-to-start reveals an error-tinted
 *  panel with a delete glyph; completing the swipe opens a confirmation dialog,
 *  and confirming calls [MemoryRepository.delete]. A snackbar then offers Undo,
 *  which restores the row through [MemoryRepository.add] — the same data source
 *  that removed it. The box never settles dismissed, so the row springs back and
 *  the dialog owns the decision: no ghost row, no jump.
 *
 *  The gesture only exists inside the loaded, non-empty branch, so it is
 *  automatically unavailable while loading or in error, and it works over a
 *  filtered list because it wraps the row, not the list.
 *
 *  LONG-PRESS CONTEXT MENU (this fix)
 *  ----------------------------------
 *  Long-pressing a row opens a Material 3 [DropdownMenu] anchored to it, with two
 *  actions: Open and Delete, the destructive one last behind a divider \u2014 the
 *  same pattern the Artifacts grid uses. Open hands the memory's URL to the
 *  SHARED [UrlOpener] (the one helper both screens use); when there is no URL it
 *  falls back to a detail dialog, so the tap is never a silent no-op. Delete sets
 *  the SAME `pendingDelete` the swipe sets, so both gestures share one
 *  confirmation and one repository call.
 *
 *  COPY TEXT (this fix)
 *  --------------------
 *  The detail dialog's actions row carries a "Copy text" button beside "Open
 *  source" (primary, filled) and "Close" (text). It is ALWAYS present \u2014 every
 *  memory has text \u2014 and copies the whole memory (title + summary) to the
 *  platform clipboard, then confirms with a "Copied" snackbar on the SAME host
 *  the undo flow uses, so the two read alike. The row wraps on a narrow phone
 *  rather than overflowing.
 *
 *  SHARE (this fix)
 *  ----------------
 *  The same actions row carries a "Share" button (tonal/outlined, beside "Copy
 *  text"). It hands the memory to the platform's OWN share sheet \u2014 an
 *  ACTION_SEND intent \u2014 with the memory's text, plus its URL when it has one.
 *  No new library and no custom share dialog: the platform chooser is the
 *  surface, exactly as [UrlOpener] leaves the browser to the platform. The
 *  intent is built behind a small injectable seam ([MemorySharer]) so a test can
 *  observe the payload without leaving the process.
 *
 *  Every colour comes from MaterialTheme.colorScheme / DuaGenAITheme.colors and
 *  every gap from LocalDuaGenAISpacing, so light and dark are both correct by
 *  construction — there is no hardcoded colour in this file.
 * ==========================================================================*/

/**
 * The category filters the chip row offers.
 *
 * [All] is the empty filter. Every other entry matches one memory `category`
 * exactly — the same human labels [MemoryItem.category] carries, so the chip set
 * cannot drift from the data without a compile-time-visible change here.
 *
 * [Task] and [Note] are real memory types the product will produce but that the
 * demo set has none of yet. They are here for the same two reasons the Artifacts
 * filter carries Link/Code: the filter set should cover the types the product
 * actually makes, and they are what makes the no-match panel REACHABLE — with
 * only the three populated categories every chip matched something and that
 * state could never be seen.
 */
enum class MemoryCategory(val label: String) {
    All("All"),
    Project("Project"),
    Preference("Preference"),
    Finance("Finance"),
    Task("Task"),
    Note("Note");

    /** True when [item] passes this filter. */
    fun matches(item: MemoryItem): Boolean = this == All || item.category == label
}

/**
 * Where a memory actually goes when the user taps "Share".
 *
 * A `fun interface`, so a lambda is a sharer: `MemorySharer { text, url -> \u2026 }`.
 * The default implementation hands the payload to the platform's OWN share sheet
 * (an ACTION_SEND intent); a test supplies its own and observes the payload
 * without leaving the process \u2014 the same seam [UrlLauncher] gives the URL path.
 *
 * @param text the memory's text (title + summary). Never blank.
 * @param url the memory's source URL, or null when it has none. The default
 *   implementation appends it to the shared text when present.
 */
fun interface MemorySharer {
    fun share(text: String, url: String?)
}

/**
 * The [MemorySharer] a composable should use.
 *
 * Defaults to the platform share sheet: an ACTION_SEND intent carrying the
 * memory's text (and its URL when it has one), launched through a chooser so the
 * user picks the target app. No new library \u2014 `Intent` is the platform. Pass
 * [sharer] to override it \u2014 that is the hook the tests use.
 */
@Composable
fun rememberMemorySharer(sharer: MemorySharer? = null): MemorySharer {
    val context = LocalContext.current
    return remember(sharer, context) {
        sharer ?: MemorySharer { text, url ->
            val body = if (url.isNullOrBlank()) text else "$text\n\n$url"
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(send, "Share memory"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    repository: MemoryRepository,
    modifier: Modifier = Modifier,
    /**
     * How the context menu's "Open" opens a URL.
     *
     * Kept as a plain `(String) -> Unit` so a test can observe the intent without
     * leaving the process. It is wrapped into a [UrlLauncher] and handed to the
     * SHARED [UrlOpener] — the same helper the Artifacts context menu uses, so
     * the two screens cannot drift. Defaults to the platform handler.
     */
    onOpenInNewTab: ((String) -> Unit)? = null,
    /**
     * How the detail dialog's "Share" hands the memory to the platform.
     *
     * Kept as a plain `(String, String?) -> Unit` (text, url) so a test can
     * observe the payload without leaving the process. It is wrapped into a
     * [MemorySharer]; the default is the platform's own share sheet (an
     * ACTION_SEND intent). No new library.
     */
    onShare: ((String, String?) -> Unit)? = null,
) {
    val spacing = LocalDuaGenAISpacing.current
    // The shared opener. The null/blank guard and the "did it open?" answer live
    // in UrlOpener, not here.
    val urlOpener = rememberUrlOpener(onOpenInNewTab?.let { launcher -> UrlLauncher(launcher) })
    // The share seam. The default is the platform share sheet; a test injects
    // its own to observe the payload.
    val memorySharer = rememberMemorySharer(onShare?.let { MemorySharer(it) })
    val state by repository.state.collectAsStateWithLifecycle()
    // Pull-to-refresh's in-flight flag. Separate from `state` on purpose: a
    // refresh must not blank the list, so it cannot go through Loading.
    val isRefreshing by repository.isRefreshing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // The undo snackbar's host. The detail dialog's "Copied" feedback shares
    // this SAME host, so the two snackbars are one surface with one style.
    val snackbarHostState = remember { SnackbarHostState() }
    // The clipboard, for the detail dialog's "Copy text" action. The platform
    // service, not a new dependency.
    val clipboard = LocalClipboardManager.current
    // Which memory is awaiting delete confirmation, or null. Local UI state —
    // it is not persisted and dies with the screen. BOTH the swipe and the
    // long-press menu's Delete set this, so there is ONE confirmation path.
    var pendingDelete by remember { mutableStateOf<MemoryItem?>(null) }
    // The long-press context menu's subject, or null when the menu is closed.
    // Local UI state, like `pendingDelete`: the menu is a transient surface.
    var menuFor by remember { mutableStateOf<MemoryItem?>(null) }
    // The memory whose DETAIL dialog is open, or null. This is the fallback the
    // menu's "Open" uses when the memory has no URL to hand to the platform.
    var detailFor by remember { mutableStateOf<MemoryItem?>(null) }

    // Load once when the screen first appears. Re-running on Retry is the
    // repository's own `load()`, called from the error state's button.
    LaunchedEffect(repository) { repository.load() }

    // The search text and the active category chip. Both rememberSaveable, the
    // same pattern the picker's search uses, so a rotation keeps the user's
    // search. They only affect WHICH memories are shown, and a refresh never
    // resets them.
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(MemoryCategory.All) }

    val loaded = state as? MemoryUiState.Loaded
    val all = loaded?.memories.orEmpty()
    // The visible memories: text AND chip, applied together. Derived, never
    // stored, so it cannot go stale against the loaded list.
    val visible = all.filter { category.matches(it) && matchesMemoryQuery(it, query) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Memory",
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
                .padding(inner),
        ) {
            // The search field and chips. Only drawn when there is something to
            // search — a search field over a fresh account's empty state would
            // be a control with no subject.
            if (all.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.gutter, vertical = spacing.sm),
                    singleLine = true,
                    shape = DuaGenAITheme.shapes.pill,
                    placeholder = { Text("Search memories") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.minTouchTarget(),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )

                // The category chips. A LazyRow so the row scrolls on a narrow
                // phone. Each chip is a 48dp touch target (M3's own minimum).
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    items(MemoryCategory.values().toList()) { entry ->
                        FilterChip(
                            selected = entry == category,
                            onClick = { category = entry },
                            // A distinct description per chip: the cards also
                            // carry their category as text ("Project"), so a bare
                            // text match would be ambiguous.
                            modifier = Modifier
                                // The M3 48dp minimum, from the SHARED helper \u2014
                                // the chip's drawn size is unchanged, only its
                                // tappable box grows. Same rule the Artifacts and
                                // Cube picker chip rows follow, so all three
                                // filter surfaces agree.
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
            }

            // Pull-to-refresh (this fix). The box wraps the whole content area,
            // so the gesture works over the list AND over the empty / error /
            // no-match states (each of which is scrollable — see
            // [ScrollableCentered]). `isRefreshing` drives the M3 indicator; the
            // content underneath is never replaced by a spinner, so the list
            // stays visible while a refresh runs.
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { scope.launch { repository.refresh() } },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                when (val s = state) {
                    is MemoryUiState.Loading -> {
                        MemoryLoading(modifier = Modifier.fillMaxSize())
                    }

                    is MemoryUiState.Error -> {
                        ScrollableCentered {
                            MemoryError(
                                message = s.message,
                                onRetry = { repository.load() },
                                modifier = Modifier.padding(horizontal = spacing.gutter),
                            )
                        }
                    }

                    is MemoryUiState.Loaded -> {
                        when {
                            // A fresh account: the original empty state, verbatim.
                            s.memories.isEmpty() -> ScrollableCentered {
                                MemoryEmptyState(
                                    modifier = Modifier.padding(horizontal = spacing.gutter),
                                )
                            }

                            // The search + chips hid everything. An explicit
                            // panel with a way out — never a blank area that
                            // looks like a broken list.
                            visible.isEmpty() -> ScrollableCentered {
                                MemoryNoMatch(
                                    onClear = {
                                        query = ""
                                        category = MemoryCategory.All
                                    },
                                    modifier = Modifier.padding(horizontal = spacing.gutter),
                                )
                            }

                            else -> LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = spacing.gutter,
                                    end = spacing.gutter,
                                    top = spacing.sm,
                                    bottom = spacing.lg,
                                ),
                                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                            ) {
                                items(visible, key = { it.id }) { memory ->
                                    SwipeToDeleteMemoryRow(
                                        memory = memory,
                                        onSwipeDelete = { pendingDelete = memory },
                                        menuOpen = menuFor?.id == memory.id,
                                        onLongPress = { menuFor = memory },
                                        onMenuDismiss = { menuFor = null },
                                        // Open: the SHARED helper first. A URL
                                        // goes to the platform; no URL falls back
                                        // to the detail dialog, so the tap is
                                        // never a silent no-op.
                                        onOpen = {
                                            menuFor = null
                                            if (!urlOpener.open(memory.url)) detailFor = memory
                                        },
                                        // Delete: the SAME pending-delete state
                                        // the swipe sets, so both gestures share
                                        // one confirmation and one repository call.
                                        onDelete = {
                                            menuFor = null
                                            pendingDelete = memory
                                        },
                                        // A plain tap opens the detail dialog,
                                        // which is where the "Open source" button
                                        // lives when the memory has a URL.
                                        onOpenDetail = { detailFor = memory },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // The delete confirmation. Confirming calls the repository; the screen never
    // touches the list itself. This is the ONE delete path — the swipe is the
    // only way in, and it lands here.
    pendingDelete?.let { memory ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete memory?") },
            text = { Text("\"${memory.title}\" hamesha ke liye hat jayegi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        scope.launch {
                            repository.delete(memory.id)
                            // Undo: the snackbar re-adds through the repository,
                            // so the row is restored by the same data source that
                            // removed it. After the snackbar times out the delete
                            // is final.
                            val result = snackbarHostState.showSnackbar(
                                message = "Memory deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                repository.add(memory)
                            }
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // The memory DETAIL dialog — the fallback the context menu's "Open" uses when
    // the memory has no URL to hand to the platform. It shows the whole memory
    // (title, full summary, category, timestamp) so "Open" always reveals
    // something rather than doing nothing. Every colour is a theme token.
    detailFor?.let { memory ->
        AlertDialog(
            onDismissRequest = { detailFor = null },
            title = { Text(memory.title) },
            text = {
                Column {
                    Text(
                        text = memory.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(spacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(DuaGenAITheme.shapes.pill)
                                .background(DuaGenAITheme.colors.brandSoft)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = memory.category,
                                style = DuaGenAITextStyles.eyebrow,
                                color = DuaGenAITheme.colors.onBrandSoft,
                            )
                        }
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            text = memory.timestamp,
                            style = DuaGenAITextStyles.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                // "Open source" \u2014 shown ONLY when the memory actually has a URL.
                // With no URL there is nothing to open, so the button is absent
                // rather than a disabled or empty control (the task's rule). It
                // routes through the SHARED [UrlOpener], the same helper the
                // context menu's "Open" uses, so there is no second opening path.
                if (!memory.url.isNullOrBlank()) {
                    Button(
                        onClick = {
                            // The helper guards null/blank itself; the check above
                            // is only about whether to DRAW the button.
                            urlOpener.open(memory.url)
                            detailFor = null
                        },
                        shape = DuaGenAITheme.shapes.pill,
                        modifier = Modifier
                            .minTouchTarget()
                            .semantics {
                                contentDescription = "Open source in browser"
                            },
                    ) {
                        Text("Open source", style = DuaGenAITextStyles.button)
                    }
                }
            },
            dismissButton = {
                // "Copy text" — ALWAYS present, because every memory has text.
                // Tonal/outlined (the secondary action), beside the primary
                // "Open source" and the text "Close". It copies the whole
                // memory (title + summary) and confirms with the SAME snackbar
                // the undo flow uses, so the two read alike.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(memory.title + "\n\n" + memory.summary))
                            detailFor = null
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Copied",
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        },
                        shape = DuaGenAITheme.shapes.pill,
                        modifier = Modifier
                            .minTouchTarget()
                            .semantics {
                                contentDescription = "Copy memory text"
                            },
                    ) {
                        Text("Copy text", style = DuaGenAITextStyles.button)
                    }
                    Spacer(Modifier.width(spacing.sm))
                    // "Share" \u2014 the platform's OWN share sheet (an ACTION_SEND
                    // intent), beside "Copy text". Tonal/outlined, the same
                    // secondary weight as Copy text. It hands the memory's text
                    // (plus its URL when it has one) to the chooser; no custom
                    // share dialog and no new library.
                    OutlinedButton(
                        onClick = {
                            memorySharer.share(
                                text = memory.title + "\n\n" + memory.summary,
                                url = memory.url,
                            )
                            detailFor = null
                        },
                        shape = DuaGenAITheme.shapes.pill,
                        modifier = Modifier
                            .minTouchTarget()
                            .semantics {
                                contentDescription = "Share memory"
                            },
                    ) {
                        Text("Share", style = DuaGenAITextStyles.button)
                    }
                    Spacer(Modifier.width(spacing.sm))
                    TextButton(
                        onClick = { detailFor = null },
                        modifier = Modifier.minTouchTarget(),
                    ) {
                        Text("Close")
                    }
                }
            },
        )
    }
}

/**
 * True when [item] matches [query].
 *
 * Matches the title, the summary and the category, case-insensitively — so any
 * of the ways a user might think of a memory finds it.
 */
private fun matchesMemoryQuery(item: MemoryItem, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return item.title.lowercase().contains(q) ||
        item.summary.lowercase().contains(q) ||
        item.category.lowercase().contains(q)
}

/**
 * Centres [content] in the content area AND keeps it scrollable.
 *
 * Pull-to-refresh is a nested-scroll gesture, so it only fires over a scrollable
 * child. The list is one; the empty / error / no-match states are not, so they
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

/** The first-load state: a centred spinner. */
@Composable
private fun MemoryLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** The error state: the message plus a Retry action. */
@Composable
private fun MemoryError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MemoryGlyph(
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
                onClick = onRetry,
                shape = DuaGenAITheme.shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Retry", style = DuaGenAITextStyles.button)
            }
        }
    }
}

/**
 * One memory row, wrapped in a swipe-to-delete gesture and carrying a long-press
 * context menu.
 *
 * SWIPE — dragging the row from end to start reveals an error-tinted panel with a
 * delete glyph, and completing the swipe opens the delete confirmation. The box
 * is told never to settle dismissed (`confirmValueChange` returns false), so the
 * row springs back and the dialog owns the decision — no ghost row, no jump, and
 * nothing is destroyed by a stray swipe.
 *
 * LONG-PRESS (this fix) — pressing and holding the row opens a Material 3
 * [DropdownMenu] anchored to it, with two actions: Open and Delete, the
 * destructive one last behind a divider. This is the SAME pattern the Artifacts
 * grid uses, so the two surfaces read alike. The menu's Delete sets the same
 * `pendingDelete` the swipe sets, so both gestures share ONE confirmation and one
 * repository call — no duplicate delete logic.
 *
 * Both gestures only exist inside the loaded, non-empty branch, so they are
 * automatically unavailable while loading or in error. They wrap the ROW rather
 * than the list, so they keep working over a filtered/searched list.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeToDeleteMemoryRow(
    memory: MemoryItem,
    onSwipeDelete: () -> Unit,
    menuOpen: Boolean,
    onLongPress: () -> Unit,
    onMenuDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onSwipeDelete()
            // Never settle dismissed: the confirmation dialog decides, and the
            // row must not leave a ghost behind.
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
                MemoryDeleteGlyph(
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(end = spacing.md)
                        .size(24.dp),
                )
            }
        },
    ) {
        // The row and its context menu share a Box, so the menu anchors to the
        // ROW the user pressed rather than to the screen. The menu is only
        // reachable from the loaded list, so it is automatically unavailable
        // while loading or in error.
        Box {
            MemoryCard(
                memory = memory,
                onLongPress = onLongPress,
                // A plain tap opens the DETAIL dialog \u2014 the same surface the
                // menu's URL-less "Open" falls back to. It is the one place the
                // "Open source" button lives, so a memory with a URL is reachable
                // by tap as well as by long-press.
                onClick = onOpenDetail,
            )
            MemoryContextMenu(
                memory = memory,
                expanded = menuOpen,
                onDismiss = onMenuDismiss,
                onOpen = onOpen,
                onDelete = onDelete,
            )
        }
    }
}

/**
 * The long-press context menu (this fix).
 *
 * Two actions, in Material 3's order — the safe one first, the destructive one
 * last behind a divider:
 *   • Open   -> hands the memory's URL to the SHARED [UrlOpener]; when there is
 *               no URL the screen falls back to the detail dialog, so the tap is
 *               never a silent no-op
 *   • ── divider ──
 *   • Delete -> opens the SAME confirmation the swipe opens
 *
 * This is the same shape the Artifacts context menu uses, so the two surfaces
 * read alike. Every item is a 48dp touch target.
 */
@Composable
private fun MemoryContextMenu(
    memory: MemoryItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
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
                .semantics { contentDescription = "Open ${memory.title}" },
        )
        // The destructive action is separated from the safe one, per M3.
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
                .semantics { contentDescription = "Delete ${memory.title}" },
        )
    }
}

/**
 * The swipe panel's delete glyph: a bin with a lid, drawn with [Canvas].
 *
 * Same reason as [MemoryGlyph] — the kit ships `material-icons-core` only. It
 * takes a `tint`, never a literal colour.
 */
@Composable
private fun MemoryDeleteGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bar = 2.dp.toPx()
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

/**
 * One memory, as a card.
 *
 * The layout is the kit's own row anatomy: a leading tinted tile (the category
 * indicator), a two-line text column, and the timestamp trailing. It reuses
 * `DuaGenAITheme.shapes.card`, `colors.brandSoft` / `onBrandSoft` and the shared
 * text styles, so it reads like the agent rows rather than like a new component.
 *
 * The card carries a LONG-PRESS (this fix) that opens the context menu, so it
 * uses `combinedClickable` rather than a plain `clickable` — the same choice the
 * Artifacts card makes. The row has no tap action of its own, so only the
 * long-press is wired.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoryCard(
    memory: MemoryItem,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            )
            // A stable handle for the swipe gesture: the row's own text is
            // several nodes, so a test (and a screen reader) needs one node that
            // IS the row.
            .semantics { contentDescription = "Memory: ${memory.title}" },
        // Forest-slate card: `cardGradient` fill, 1dp `borderGradient` edge,
        // 20dp corners. The Surface itself is the solid `bgCanvas` so the
        // translucent gradient never lets the swipe-to-delete red show through.
        shape = TeamilyShapes.card,
        color = teamilyPalette().bgCanvas,
        border = BorderStroke(1.dp, teamilyPalette().borderGradient),
    ) {
        Row(
            modifier = Modifier
                .background(teamilyPalette().cardGradient)
                .padding(spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            // Category indicator: a soft brand tile holding the memory glyph.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(DuaGenAITheme.shapes.avatarMessage)
                    .background(colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) {
                MemoryGlyph(
                    tint = colors.onBrandSoft,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = memory.title,
                    style = DuaGenAITextStyles.rowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(spacing.xxs))
                Text(
                    text = memory.summary,
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The category chip — the same brand-soft pill the drawer's
                    // plan badge uses, so it is a token, not a new colour.
                    Box(
                        modifier = Modifier
                            .clip(DuaGenAITheme.shapes.pill)
                            .background(colors.brandSoft)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = memory.category,
                            style = DuaGenAITextStyles.eyebrow,
                            color = colors.onBrandSoft,
                        )
                    }
                    Spacer(Modifier.width(spacing.sm))
                    Text(
                        text = memory.timestamp,
                        style = DuaGenAITextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * The filtered-to-nothing state.
 *
 * When the search + chips hide every memory this panel replaces the list, and it
 * says WHY the area is empty plus the one tap that fixes it. This is the
 * difference between "no results" and a list that silently looks broken.
 */
@Composable
private fun MemoryNoMatch(onClear: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalDuaGenAISpacing.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MemoryGlyph(
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(spacing.lg))
            Text(
                text = "Koi memory nahi mili",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = "Search ya filter badal kar dekhein.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.lg))
            OutlinedButton(
                onClick = onClear,
                shape = DuaGenAITheme.shapes.pill,
                modifier = Modifier.minTouchTarget(),
            ) {
                Text("Clear filters", style = DuaGenAITextStyles.button)
            }
        }
    }
}

/** The empty state — the screen's original body, kept as the fallback. */
@Composable
private fun MemoryEmptyState(modifier: Modifier = Modifier) {
    val spacing = LocalDuaGenAISpacing.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MemoryGlyph(
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(spacing.lg))
            Text(
                text = "Abhi koi memory nahi hai",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = "Memories yahan dikhengi — jo cheezein Dua aur Agent yaad rakhenge.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The Memory glyph: a rounded card with three lines inside, stroke only.
 *
 * Drawn with [Canvas] rather than imported because the kit's icon set is
 * `material-icons-core` only — it has no memory glyph — and pulling
 * `material-icons-extended` for one shape would add a multi-megabyte
 * dependency. Same approach as the bottom nav's `TabGlyph`.
 */
@Composable
private fun MemoryGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height

        // Outer card.
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.18f),
            size = Size(w * 0.76f, h * 0.64f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = stroke,
        )

        // Three "remembered" lines.
        val lineStart = w * 0.28f
        val lineEnd = w * 0.72f
        listOf(0.38f, 0.50f, 0.62f).forEach { y ->
            drawLine(
                color = tint,
                start = Offset(lineStart, h * y),
                end = Offset(lineEnd, h * y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Preview(name = "Memory — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoryLight() = DuaGenAITheme(darkTheme = false) {
    MemoryScreen(repository = MemoryRepository(loadDelayMs = 0))
}

@Preview(name = "Memory — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoryDark() = DuaGenAITheme(darkTheme = true) {
    MemoryScreen(repository = MemoryRepository(loadDelayMs = 0))
}

@Preview(name = "Memory — empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoryEmpty() = DuaGenAITheme(darkTheme = false) {
    MemoryScreen(repository = MemoryRepository(seed = emptyList(), loadDelayMs = 0))
}

@Preview(name = "Memory — error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MemoryErrorPreview() = DuaGenAITheme(darkTheme = false) {
    MemoryScreen(repository = MemoryRepository(failOnLoad = true, loadDelayMs = 0))
}
