package ai.duagenai.design.screens

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import ai.duagenai.design.theme.MaxContentWidth

/* ============================================================================
 *  Notifications — M3 components used:
 *      Scaffold, TopAppBar, Surface (card), Switch, HorizontalDivider, Text
 *
 *  The Profile screen's "Notifications" row opens this. It lists the three
 *  notification switches the product model has — Push notifications, Task
 *  updates, Weekly summary — and reports each change through [onToggle]. The nav
 *  host owns the single copy of the state and persists it through the existing
 *  store, so this screen is a pure function of `enabled` and never keeps a
 *  second copy that could drift.
 *
 *  The Profile row's subtitle ("N of M on") is derived from the SAME map, so the
 *  row and this screen can never disagree about how many are on.
 *
 *  Every colour comes from MaterialTheme.colorScheme / DuaGenAITheme and every
 *  gap from LocalDuaGenAISpacing, so light and dark are both correct by
 *  construction — there is no hardcoded colour in this file.
 * ==========================================================================*/

/**
 * The three notification switches, in the order the product model lists them.
 *
 * [id] is the stable key the host persists against; [label] is what the row
 * shows. Keeping the id separate from the label means a copy change never
 * rewrites a stored preference.
 */
enum class NotificationSetting(val id: String, val label: String) {
    Push("push", "Push notifications"),
    TaskUpdates("task_updates", "Task updates"),
    WeeklySummary("weekly_summary", "Weekly summary"),
}

/** The default state: everything on except the weekly summary. */
val defaultNotificationState: Map<NotificationSetting, Boolean> = mapOf(
    NotificationSetting.Push to true,
    NotificationSetting.TaskUpdates to true,
    NotificationSetting.WeeklySummary to false,
)

/** `"5 of 7 on"`-style summary, derived from the SAME map the switches read. */
fun notificationSummary(state: Map<NotificationSetting, Boolean>): String {
    val on = NotificationSetting.values().count { state[it] == true }
    return "$on of ${NotificationSetting.values().size} on"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    enabled: Map<NotificationSetting, Boolean> = defaultNotificationState,
    onToggle: (NotificationSetting, Boolean) -> Unit = { _, _ -> },
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
                        text = "Notifications",
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
                    text = "ALERTS",
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
                        NotificationSetting.values().forEachIndexed { index, setting ->
                            if (index > 0) HorizontalDivider(color = colors.hairline)
                            NotificationRow(
                                setting = setting,
                                checked = enabled[setting] == true,
                                onCheckedChange = { onToggle(setting, it) },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = notificationSummary(enabled),
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        }
    }
}

/**
 * One notification switch.
 *
 * The whole row is the touch target (48dp+), not just the switch, so a tap
 * anywhere on the row toggles it — the same affordance the Profile rows have.
 */
@Composable
private fun NotificationRow(
    setting: NotificationSetting,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = setting.label,
            style = DuaGenAITextStyles.rowTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(spacing.sm))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Preview(name = "Notifications — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationsLight() = DuaGenAITheme(darkTheme = false) { NotificationsScreen() }

@Preview(name = "Notifications — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationsDark() = DuaGenAITheme(darkTheme = true) { NotificationsScreen() }
