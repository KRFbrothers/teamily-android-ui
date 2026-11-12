package ai.duagenai.design.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
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

/* ============================================================================
 *  Appearance — M3 components used:
 *      Scaffold, TopAppBar, Surface (card), RadioButton, HorizontalDivider, Text
 *
 *  The Profile screen's "Appearance" row opens this. It offers the three theme
 *  choices the product model has — System default / Light / Dark — as a radio
 *  list, and reports the pick through [onSelect]. The nav host owns the single
 *  copy of the value and persists it through the existing store, so this screen
 *  is a pure function of `selected` and never keeps a second copy that could
 *  drift.
 *
 *  Every colour comes from MaterialTheme.colorScheme / DuaGenAITheme and every
 *  gap from LocalDuaGenAISpacing, so light and dark are both correct by
 *  construction — there is no hardcoded colour in this file.
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
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

@Preview(name = "Appearance — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppearanceLight() = DuaGenAITheme(darkTheme = false) { AppearanceScreen() }

@Preview(name = "Appearance — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppearanceDark() = DuaGenAITheme(darkTheme = true) { AppearanceScreen() }
