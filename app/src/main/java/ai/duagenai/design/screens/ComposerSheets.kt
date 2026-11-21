package ai.duagenai.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.TeamilyMarks
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.TeamilyShapes
import androidx.compose.foundation.layout.widthIn
import ai.duagenai.design.theme.MaxContentWidth

/* ============================================================================
 *  Composer sheets — Attach (+), Skills and MCP Tools
 *
 *  The three ModalBottomSheets the live thread composer opens (see
 *  `DuaComposer` in HomeScreen.kt). They reuse the kit's existing sheet recipe
 *  (ArtifactsScreen's filter sheet: ModalBottomSheet on `colorScheme.surface`,
 *  `titleMedium` heading, gutter padding) and the Notifications row recipe
 *  (48dp+ row, label + M3 Switch) — no new visual language, no new colour.
 *
 *  DATA, NOT COPY
 *  --------------
 *  • [DuaAgentSkills] mirrors Dua-ai's `com.dua.ai.model.DuaAgentSkills`
 *    one-for-one (same ids, titles and summaries), so the kit and the app never
 *    describe the same skill two ways.
 *  • [ADK_BUILT_IN_TOOLS] is the exact built-in tool id list from the composer
 *    plan.
 *  • [ADK_MCP_SERVERS] is EMPTY on purpose. In Dua-ai it is a server-side
 *    environment variable (a JSON object the operator sets on the engine);
 *    there is no client-side list of names to reuse, and "nothing is exposed by
 *    default". So the sheet shows the none state instead of inventing servers.
 * ==========================================================================*/

/** One of the skills installed inside Dua Agent. Clients send the id. */
data class DuaAgentSkill(val id: String, val title: String, val summary: String)

/** The five Dua Agent skills — mirrored from Dua-ai's `DuaAgentSkills`. */
object DuaAgentSkills {
    val all = listOf(
        DuaAgentSkill("frontend-developer", "Frontend Developer", "UI, screens, design implementation, web pages"),
        DuaAgentSkill("backend-architect", "Backend Architect", "API design, data model, server-side architecture"),
        DuaAgentSkill("api-tester", "API Tester", "Endpoint testing, contract verification, edge cases"),
        DuaAgentSkill("devops-automator", "DevOps Automator", "CI/CD, builds, deploys, release automation"),
        DuaAgentSkill("code-reviewer", "Code Reviewer", "Code review, correctness, blockers, PR feedback"),
    )
    val ids = all.map { it.id }
}

/** Default: every skill enabled. */
val DEFAULT_DUA_SKILLS: Set<String> = DuaAgentSkills.ids.toSet()

/** Configured external MCP servers. Empty: see the file header. */
val ADK_MCP_SERVERS: List<String> = emptyList()

/** The agent's built-in tools, by id. */
val ADK_BUILT_IN_TOOLS: List<String> = listOf(
    "google_search",
    "fetch_url",
    "run_python",
    "generate_image",
    "save_note",
    "search_notes",
    "remember",
    "recall",
    "search_documents",
)

/** Which composer sheet is open. Stored by name so `rememberSaveable` can keep it. */
internal enum class ComposerSheet { None, Plus, Skills, Mcp }

/**
 * The `+` sheet. Agent tools stay Skills and MCP Tools (the attach test clicks
 * the Skills row). Under them, the eight Create tiles from the HTML each open
 * their own screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposerPlusSheet(
    onOpenSkills: () -> Unit,
    onOpenMcp: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCreate: (String) -> Unit = {},
) {
    val spacing = LocalDuaGenAISpacing.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SheetColumn(title = "Quick Actions") {
            SheetSectionLabel("Agent tools")
            SheetActionRow(icon = Icons.Outlined.Build, label = "Skills", onClick = onOpenSkills)
            SheetActionRow(icon = Icons.Outlined.Extension, label = "MCP Tools", onClick = onOpenMcp)
            SheetSectionLabel("Create")
            for (row in CREATE_TILES.chunked(4)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.gutter, vertical = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    for (tile in row) {
                        CreateTileButton(
                            tile = tile,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onDismiss()
                                onOpenCreate(tile.key)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTileButton(
    tile: CreateTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val ink = tile.color ?: MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .sizeIn(minHeight = MinTouchTarget)
            .clip(TeamilyShapes.md)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = tile.title }
            .padding(vertical = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Text(
                text = tile.title.take(1),
                style = DuaGenAITextStyles.rowTitle,
                color = ink,
                modifier = Modifier.padding(top = if (tile.pro) 8.dp else 0.dp),
            )
            if (tile.pro) {
                Text(
                    text = "Pro",
                    modifier = Modifier
                        .clip(TeamilyShapes.capsule)
                        .background(TeamilyMarks.proBadge)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    style = DuaGenAITextStyles.meta.copy(fontWeight = FontWeight.Bold),
                    color = TeamilyMarks.paper,
                )
            }
        }
        Spacer(Modifier.size(spacing.xs))
        Text(
            text = tile.title,
            style = DuaGenAITextStyles.meta,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** The Skills sheet: one 48dp+ switch row per Dua Agent skill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposerSkillsSheet(
    enabledSkillIds: Set<String>,
    onToggleSkill: (String) -> Unit,
    onDismiss: () -> Unit,
    skills: List<SkillItem>? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SheetColumn(title = "Skills") {
            if (!skills.isNullOrEmpty()) {
                skills.forEach { skill ->
                    SwitchRow(
                        title = skill.title,
                        summary = skill.summary.ifBlank { null },
                        checked = skill.id in enabledSkillIds,
                        onCheckedChange = { onToggleSkill(skill.id) },
                    )
                }
            } else {
                DuaAgentSkills.all.forEach { skill ->
                    SwitchRow(
                        title = skill.title,
                        summary = skill.summary,
                        checked = skill.id in enabledSkillIds,
                        onCheckedChange = { onToggleSkill(skill.id) },
                    )
                }
            }
        }
    }
}

/**
 * The MCP sheet: the master "MCP Tools" switch, then read-only status chips for
 * the configured external servers ([ADK_MCP_SERVERS]) and the built-in tools.
 * The chips dim when MCP is off — they are status, not controls.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ComposerMcpSheet(
    mcpEnabled: Boolean,
    onToggleMcp: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SheetColumn(title = "MCP Tools") {
            SwitchRow(
                title = "MCP Tools",
                summary = null,
                checked = mcpEnabled,
                onCheckedChange = onToggleMcp,
            )
            SheetSectionLabel("Server & MCP Capabilities")
            Column(Modifier.alpha(if (mcpEnabled) 1f else 0.45f)) {
                Text(
                    text = "ADK_MCP_SERVERS",
                    style = DuaGenAITextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacing.gutter, vertical = spacing.xs),
                )
                if (ADK_MCP_SERVERS.isEmpty()) {
                    Text(
                        text = "None configured",
                        style = DuaGenAITextStyles.snippet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = spacing.gutter, vertical = spacing.xs),
                    )
                } else {
                    ToolChips(ADK_MCP_SERVERS)
                }
                Text(
                    text = "Built-in tools",
                    style = DuaGenAITextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = spacing.gutter,
                        end = spacing.gutter,
                        top = spacing.sm,
                        bottom = spacing.xs,
                    ),
                )
                ToolChips(ADK_BUILT_IN_TOOLS)
            }
        }
    }
}

/* ---------------------------------------------------------------- pieces */

@Composable
private fun SheetColumn(title: String, content: @Composable () -> Unit) {
    val spacing = LocalDuaGenAISpacing.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.lg),
        ) {
            // Same heading recipe as the Artifacts filter sheet.
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = spacing.gutter, end = spacing.gutter, bottom = spacing.sm),
            )
            content()
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    val spacing = LocalDuaGenAISpacing.current
    Text(
        text = text,
        style = DuaGenAITextStyles.eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = spacing.gutter,
            end = spacing.gutter,
            top = spacing.md,
            bottom = spacing.xs,
        ),
    )
}

/** A 48dp+ tappable row: icon + label. */
@Composable
private fun SheetActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val spacing = LocalDuaGenAISpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = spacing.gutter, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(spacing.md))
        Text(text = label, style = DuaGenAITextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * The Notifications switch row, made fully toggleable: the whole 48dp+ row is
 * the touch target and carries the Switch role, so the Switch itself takes no
 * click of its own (no double announcement).
 */
@Composable
private fun SwitchRow(
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = MinTouchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { contentDescription = title }
            .padding(horizontal = spacing.gutter, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = DuaGenAITextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
            if (summary != null) {
                Text(
                    text = summary,
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(spacing.sm))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * Read-only status chips: capsule on `brandSoft` with `onBrandSoft` ink — the
 * same pair the Artifacts filter chips and the drawer's active row use.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolChips(ids: List<String>) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        ids.forEach { id ->
            Surface(shape = TeamilyShapes.capsule, color = colors.brandSoft) {
                Text(
                    text = id,
                    style = DuaGenAITextStyles.meta,
                    color = colors.onBrandSoft,
                    modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                )
            }
        }
    }
}
