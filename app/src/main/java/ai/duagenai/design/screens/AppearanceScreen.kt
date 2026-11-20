package ai.duagenai.design.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ai.duagenai.design.bot.BabyGrokMark
import ai.duagenai.design.bot.BotColor
import ai.duagenai.design.bot.BotShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import androidx.compose.foundation.layout.widthIn
import ai.duagenai.design.theme.MaxContentWidth

/* ============================================================================
 *  Appearance — M3 components used:
 *      Scaffold, TopAppBar, Surface (card), RadioButton, HorizontalDivider, Text
 *
 *  The Profile screen's "Appearance" row opens this. It offers the three theme
 *  choices the product model has — System default / Light / Dark — as a radio
 *  list, plus the Grok Bot shape and bot color. Each pick is reported through
 *  [onSelect], [onShape], or [onColor]. The nav host owns the values and
 *  persists them through the existing store, so this screen never keeps a
 *  second copy that could drift.
 *
 *  Chrome colours come from MaterialTheme.colorScheme / DuaGenAITheme and every
 *  gap from LocalDuaGenAISpacing. The bot swatches use the Grok Bot palette.
 * ==========================================================================*/

/**
 * The three theme choices, in the order the product model lists them.
 *
 * [label] is the exact string the Profile row's subtitle shows, so the row and
 * this screen can never disagree about what "System default" means.
 */
enum class ThemeMode(val label: String) {
    System("System default"),
    Light("Light"),
    Dark("Dark"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    selected: ThemeMode = ThemeMode.System,
    onSelect: (ThemeMode) -> Unit = {},
    shape: BotShape = BotShape.DEFAULT,
    color: BotColor = BotColor.DEFAULT,
    onShape: (BotShape) -> Unit = {},
    onColor: (BotColor) -> Unit = {},
    onBack: () -> Unit = {},
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
                        text = "Appearance",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
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
        LazyColumn(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.gutter,
                end = spacing.gutter,
                top = spacing.sm,
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item {
                Text(
                    text = "THEME",
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
                        ThemeMode.values().forEachIndexed { index, mode ->
                            if (index > 0) HorizontalDivider(color = colors.hairline)
                            ThemeRow(
                                mode = mode,
                                selected = mode == selected,
                                onClick = { onSelect(mode) },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = "System default follows your device's light / dark setting.",
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BabyGrokMark(
                        shape = shape,
                        color = color,
                        size = 96.dp,
                        contentDescription = "Bot preview, ${shape.label}, ${color.label}",
                    )
                    Spacer(Modifier.size(spacing.sm))
                    Text(
                        text = "${shape.label} · ${color.label}",
                        style = DuaGenAITextStyles.snippet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Text(
                    text = "SHAPE",
                    style = DuaGenAITextStyles.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                ShapeGrid(selected = shape, color = color, onShape = onShape)
            }
            item {
                Text(
                    text = "BOT COLOR",
                    style = DuaGenAITextStyles.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                ColorGrid(selected = color, onColor = onColor)
            }
        }
        }
    }
}

/**
 * One theme choice.
 *
 * The whole row is the touch target (48dp+), not just the radio, so a tap
 * anywhere on the row selects it — the same affordance the Profile rows have.
 */
@Composable
private fun ThemeRow(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .sizeIn(minHeight = 48.dp)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Spacer(Modifier.width(spacing.sm))
        Text(
            text = mode.label,
            style = DuaGenAITextStyles.rowTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val ShapeColumns = 5

/** The 26 Grok Bot shapes. Each cell is a 48dp radio. */
@Composable
private fun ShapeGrid(
    selected: BotShape,
    color: BotColor,
    onShape: (BotShape) -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        BotShape.values().toList().chunked(ShapeColumns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                row.forEach { shape ->
                    val on = shape == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(CircleShape)
                            .selectable(
                                selected = on,
                                role = Role.RadioButton,
                                onClick = { onShape(shape) },
                            )
                            .border(
                                width = if (on) 2.dp else 0.dp,
                                color = if (on) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape,
                            )
                            .semantics { contentDescription = "${shape.label} shape" },
                        contentAlignment = Alignment.Center,
                    ) {
                        BabyGrokMark(
                            shape = shape,
                            color = color,
                            size = 32.dp,
                            // Swatches show the named color, not the dark-theme invert.
                            darkTheme = false,
                        )
                    }
                }
                repeat(ShapeColumns - row.size) {
                    Spacer(Modifier.weight(1f).sizeIn(minHeight = 48.dp))
                }
            }
        }
    }
}

/** The 11 Grok Bot colors. The swatch is the light-theme hex. */
@Composable
private fun ColorGrid(
    selected: BotColor,
    onColor: (BotColor) -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val hairline = DuaGenAITheme.colors.hairline
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        BotColor.values().toList().chunked(6).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                row.forEach { color ->
                    val on = color == selected
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(CircleShape)
                            .selectable(
                                selected = on,
                                role = Role.RadioButton,
                                onClick = { onColor(color) },
                            )
                            .border(
                                width = if (on) 2.dp else 1.dp,
                                color = if (on) MaterialTheme.colorScheme.primary else hairline,
                                shape = CircleShape,
                            )
                            .padding(4.dp)
                            .semantics { contentDescription = "${color.label} bot color" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(color.light)),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Appearance — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppearanceLight() = DuaGenAITheme(darkTheme = false) { AppearanceScreen() }

@Preview(name = "Appearance — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppearanceDark() = DuaGenAITheme(darkTheme = true) { AppearanceScreen() }
