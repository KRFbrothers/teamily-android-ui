package ai.duagenai.design.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import ai.duagenai.design.theme.MaxContentWidth

/* ============================================================================
 *  Storage — M3 components used:
 *      Scaffold, TopAppBar, Surface (card), LinearProgressIndicator,
 *      OutlinedButton, AlertDialog, Text
 *
 *  The Profile screen's "Storage" row opens this. It shows used vs total as a
 *  progress bar and offers a "Clear cache" action that asks for confirmation
 *  first — the destructive-action guard the audit asked for.
 *
 *  The numbers are passed IN as plain values (the same pattern every other
 *  screen in this kit uses), so the nav host owns the single copy and the
 *  Profile row's subtitle can never disagree with this screen. Clearing the
 *  cache reports through [onClearCache]; the host updates the value and this
 *  screen re-reads it.
 *
 *  Every colour comes from MaterialTheme.colorScheme / DuaGenAITheme and every
 *  gap from LocalDuaGenAISpacing, so light and dark are both correct by
 *  construction — there is no hardcoded colour in this file.
 * ==========================================================================*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    usedGb: Double = 3.2,
    totalGb: Double = 15.0,
    onClearCache: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors

    // Confirmation dialog: rememberSaveable so a rotation or resize keeps it open.
    var confirming by rememberSaveable { mutableStateOf(false) }

    val fraction = if (totalGb > 0) (usedGb / totalGb).toFloat().coerceIn(0f, 1f) else 0f

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Storage",
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DuaGenAITheme.shapes.card,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.padding(spacing.lg)) {
                        Text(
                            text = "USED",
                            style = DuaGenAITextStyles.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(spacing.xxs))
                        Text(
                            text = "${formatGb(usedGb)} of ${formatGb(totalGb)}",
                            style = DuaGenAITextStyles.statNumber,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(spacing.md))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(DuaGenAITheme.shapes.pill),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = colors.brandSoft,
                        )
                        Spacer(Modifier.height(spacing.sm))
                        Text(
                            text = "${(fraction * 100).toInt()}% used",
                            style = DuaGenAITextStyles.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { confirming = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = DuaGenAITheme.shapes.pill,
                ) {
                    Text("Clear cache", style = DuaGenAITextStyles.button)
                }
            }
            item {
                Text(
                    text = "Clearing the cache removes temporary files. Your chats, memories and artifacts are not affected.",
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Clear cache?") },
            text = {
                Text("This frees up temporary files. Your chats, memories and artifacts stay put.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onClearCache()
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/** `3.2` -> `"3.2 GB"`, `15.0` -> `"15 GB"` — the same shape the row subtitle uses. */
private fun formatGb(value: Double): String {
    val rounded = Math.round(value * 10) / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()} GB" else "$rounded GB"
}

@Preview(name = "Storage — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StorageLight() = DuaGenAITheme(darkTheme = false) { StorageScreen() }

@Preview(name = "Storage — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StorageDark() = DuaGenAITheme(darkTheme = true) { StorageScreen() }
