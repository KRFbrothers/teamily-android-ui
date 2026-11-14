package ai.duagenai.design.cube

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.duagenai.design.screens.minTouchTarget
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing

/* ============================================================================
 *  Cube picker — M3 components used:
 *      Scaffold, TopAppBar, OutlinedTextField, FilterChip, LazyRow,
 *      LazyVerticalGrid, Button, OutlinedButton, Text
 *
 *  The screen "Change cube" on Profile opens. It shows all 42 cubes in a grid;
 *  tapping one selects it (a ring + checkmark), and the bottom bar's Save
 *  persists the choice through [onSave] while Cancel discards it through
 *  [onCancel]. The draft is local and `rememberSaveable`, so a rotation keeps
 *  the in-progress selection but never writes it until Save is pressed.
 *
 *  SEARCH + FILTER
 *  ---------------
 *  Finding one cube among 42 was the audit's complaint, so the screen now has:
 *
 *    • a search field — typing a name ("Cube 07") or an id ("cube_07") filters
 *      the grid live;
 *    • a row of category chips — All / Orange / Charcoal / Multi — derived from
 *      the cube art itself (see [CubeCategory]);
 *    • the two combine: the grid shows cubes matching the text AND the chip;
 *    • a "no results" state with a Clear filters action when nothing matches.
 *
 *  Both the query and the chip are `rememberSaveable`, so a rotation keeps the
 *  user's search. Filtering only changes WHICH cubes are shown — selection,
 *  preview sync, the orange ring + checkmark and Save/Cancel are untouched.
 *
 *  The cube ART is drawn untinted — `CubeAvatar` uses Image, not Icon, so the
 *  charcoal/orange fills survive. The only colour applied to a cube is the
 *  selection ring, which is the picker's own `CubeSelectionOrange` and is never
 *  painted onto the art itself.
 *
 *  Every other colour comes from MaterialTheme.colorScheme / DuaGenAITheme and
 *  every gap from LocalDuaGenAISpacing, so light and dark are both correct by
 *  construction — there is no hardcoded colour in this file.
 * ==========================================================================*/

/**
 * The picker's own selection ring colour.
 *
 * WHY A LOCAL CONSTANT
 * --------------------
 * The ring is the picker's affordance, not a product token: it marks "this is
 * the cube you are about to save", and it must stay legible on both the light
 * and the dark surface. It is deliberately NOT taken from the theme's accent —
 * the accent is the brand's, and a brand change must not silently restyle a
 * selection control. It is also never painted onto the cube art itself, only
 * drawn as a 2dp border around it.
 */
private val CubeSelectionOrange = Color(0xFFE8622C)

/**
 * A cube's colour family, derived from the art's own fills.
 *
 * The 42 cubes are drawn from three fills — orange (#FF6A00 family), charcoal
 * (#2C2C2C family) and a mix of the two — so the filter chips are those three
 * plus "All". [matches] is the single place the mapping lives, so the chip row
 * and the grid can never disagree about which cubes a chip shows.
 */
enum class CubeCategory(val label: String) {
    All("All"),
    Orange("Orange"),
    Charcoal("Charcoal"),
    Multi("Multi");

    /** True when [cubeId] belongs to this category. */
    fun matches(cubeId: String): Boolean = when (this) {
        All -> true
        else -> CubeCategories.of(cubeId) == this
    }
}

/**
 * The cube → category mapping.
 *
 * Hardcoded on purpose: the category is a property of the shipped art, not
 * something to recompute at runtime, and a test can pin it. The ids are the
 * same zero-padded ones [CubeIds] uses.
 */
object CubeCategories {
    private val orange = setOf("cube_23", "cube_25", "cube_38")
    private val multi = setOf(
        "cube_01", "cube_06", "cube_16", "cube_17", "cube_20", "cube_21",
        "cube_28", "cube_30", "cube_31", "cube_32", "cube_33", "cube_34",
        "cube_36", "cube_39", "cube_40",
    )

    /** The category of [cubeId]. Anything not orange or multi is charcoal. */
    fun of(cubeId: String): CubeCategory = when (cubeId) {
        in orange -> CubeCategory.Orange
        in multi -> CubeCategory.Multi
        else -> CubeCategory.Charcoal
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CubePickerScreen(
    savedCubeId: String = CubeIds.DEFAULT,
    onSave: (String) -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current

    // The in-progress selection. Starts from the persisted value so reopening
    // the picker always shows the saved cube rather than a stale draft, and it
    // is rememberSaveable so a rotation keeps the user's tap. Nothing is
    // written to the store until Save.
    var draft by rememberSaveable { mutableStateOf(savedCubeId) }

    // The search text and the active category chip. Both rememberSaveable, so a
    // rotation keeps the user's search. They only affect WHICH cubes are shown.
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(CubeCategory.All) }

    // The visible cubes: text AND chip, applied together.
    val visible = CubeIds.all.filter { id ->
        category.matches(id) && matchesQuery(id, query)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Choose cube",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = spacing.gutter, vertical = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = DuaGenAITheme.shapes.pill,
                ) {
                    Text("Cancel", style = DuaGenAITextStyles.button)
                }
                Button(
                    onClick = { onSave(draft) },
                    modifier = Modifier.weight(1f),
                    shape = DuaGenAITheme.shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Save", style = DuaGenAITextStyles.button)
                }
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            // The search field. A clear (X) button appears once there is text.
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gutter, vertical = spacing.sm),
                singleLine = true,
                shape = DuaGenAITheme.shapes.pill,
                placeholder = { Text("Search cubes") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
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

            // The category chips. A LazyRow so the row scrolls on a narrow phone.
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = spacing.gutter),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                items(CubeCategory.values().toList()) { entry ->
                    // The chip's long-press TOOLTIP (this fix). A plain M3 tooltip
                    // shows the FULL label \u2014 the chip truncates on a narrow
                    // phone, so the tooltip is how the whole name is read.
                    // TooltipBox's own long-press handling is used (the standard
                    // M3 behaviour), so the chip's tap still applies the filter
                    // and the long-press never does.
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(
                                    text = entry.label,
                                    modifier = Modifier.semantics {
                                        contentDescription = "Tooltip: ${entry.label}"
                                    },
                                )
                            }
                        },
                        state = rememberTooltipState(),
                    ) {
                        FilterChip(
                            selected = entry == category,
                            onClick = { category = entry },
                            // A distinct description per chip: the grid's cells also
                            // carry their label as text ("Cube 07"), so a bare text
                            // match would be ambiguous. This makes the chip
                            // addressable, and the selected state is published by
                            // FilterChip itself. The description carries the FULL
                            // label, so accessibility reads the whole name too.
                            modifier = Modifier
                                // The M3 48dp minimum, from the SHARED helper \u2014 the
                                // chip's drawn size is unchanged, only its tappable
                                // box grows. Same rule the Memory and Artifacts chips
                                // follow, so all three filter rows agree.
                                .minTouchTarget()
                                .semantics { contentDescription = "Filter: ${entry.label}" },
                            label = { Text(entry.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DuaGenAITheme.colors.brandSoft,
                                selectedLabelColor = DuaGenAITheme.colors.onBrandSoft,
                            ),
                        )
                    }
                }
            }

            if (visible.isEmpty()) {
                // No cube matches the text + chip. Offer a one-tap way out.
                NoResults(
                    onClear = {
                        query = ""
                        category = CubeCategory.All
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.gutter),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spacing.gutter,
                        end = spacing.gutter,
                        top = spacing.sm,
                        bottom = spacing.lg,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    items(visible, key = { it }) { cubeId ->
                        CubeCell(
                            cubeId = cubeId,
                            selected = cubeId == draft,
                            onClick = { draft = cubeId },
                        )
                    }
                }
            }
        }
    }
}

/**
 * True when [cubeId] matches [query].
 *
 * Matches the label ("Cube 07"), the raw id ("cube_07") and the bare number
 * ("07" / "7"), case-insensitively — so any of the ways a user might think of a
 * cube finds it.
 */
private fun matchesQuery(cubeId: String, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    val label = CubeIds.label(cubeId).lowercase()
    val id = cubeId.lowercase()
    val number = cubeId.removePrefix("cube_").lowercase()
    return label.contains(q) || id.contains(q) || number.contains(q)
}

/** The "no results" state, with a Clear filters action. */
@Composable
private fun NoResults(onClear: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalDuaGenAISpacing.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Koi cube nahi mila",
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
            ) {
                Text("Clear filters", style = DuaGenAITextStyles.button)
            }
        }
    }
}

/**
 * One cube in the grid.
 *
 * The art is drawn untinted by [CubeAvatar]. When selected, a ring in the
 * picker's own [CubeSelectionOrange] wraps the avatar and a small checkmark
 * badge sits in the corner — the ring is a border around the avatar box, never
 * a tint on the art. The whole cell is a 48dp+ touch target.
 */
@Composable
private fun CubeCell(
    cubeId: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DuaGenAITheme.shapes.card)
            .background(if (selected) colors.brandSoft else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(DuaGenAITheme.shapes.avatarList)
                .background(
                    if (selected) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant,
                )
                // The selection ring — the picker's own orange, drawn as a
                // border around the avatar, never onto the art.
                .then(
                    if (selected) Modifier.border(
                        width = 2.dp,
                        color = CubeSelectionOrange,
                        shape = DuaGenAITheme.shapes.avatarList,
                    ) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            CubeAvatar(
                cubeId = cubeId,
                size = 40.dp,
                shape = DuaGenAITheme.shapes.avatarList,
                contentDescription = CubeIds.label(cubeId),
            )
        }
        Spacer(Modifier.height(spacing.xxs))
        Text(
            text = CubeIds.label(cubeId),
            style = DuaGenAITextStyles.meta,
            color = if (selected) colors.onBrandSoft
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Cube picker — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CubePickerLight() = DuaGenAITheme(darkTheme = false) { CubePickerScreen() }

@Preview(name = "Cube picker — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CubePickerDark() = DuaGenAITheme(darkTheme = true) { CubePickerScreen() }
