package ai.duagenai.design.screens

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CoPresent
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.duagenai.design.R
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.TeamilyMarks
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.MaxContentWidth
import ai.duagenai.design.theme.TeamilyShapes

/* ============================================================================
 *  Composer sheets — Attach (+), Skills and MCP Tools
 *
 *  The three ModalBottomSheets the live thread composer opens (see
 *  `ChatBar` in ChatBar.kt). They reuse the kit's existing sheet recipe
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
 * The Quick Action sheet (`QuickActionSheet`). Matches the 5-section layout:
 * Sticky header with centered title + close button.
 * Body:
 * 1. Agent tools (Skills, Connectors)
 * 2. Frequently used (Upload, Photos, Camera, Videos, Voice call)
 * 3. Create (Website, Document, Slides, Spreadsheet, Image, Video (Pro), Music, 3D model)
 * 4. Automate (Meeting reminder, Daily work brief, Expert group research, Drive a goal)
 * 5. Context (Memory, Recall)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickActionSheet(
    onOpenSkills: () -> Unit,
    onOpenMcp: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCreate: (String) -> Unit = {},
    onUpload: () -> Unit = onDismiss,
    onPhotos: () -> Unit = onDismiss,
    onCamera: () -> Unit = onDismiss,
    onVideos: () -> Unit = onDismiss,
    onVoiceCall: () -> Unit = onDismiss,
    onAutomate: (String) -> Unit = { onDismiss() },
    onContext: (String) -> Unit = { onDismiss() },
    onExploreMore: () -> Unit = onDismiss,
    onAllAutomations: () -> Unit = onDismiss,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        QuickActionSheetContent(
            onOpenSkills = onOpenSkills,
            onOpenMcp = onOpenMcp,
            onDismiss = onDismiss,
            onOpenCreate = onOpenCreate,
            onUpload = onUpload,
            onPhotos = onPhotos,
            onCamera = onCamera,
            onVideos = onVideos,
            onVoiceCall = onVoiceCall,
            onAutomate = onAutomate,
            onContext = onContext,
            onExploreMore = onExploreMore,
            onAllAutomations = onAllAutomations,
        )
    }
}

/**
 * Backward compatibility alias for [QuickActionSheet].
 */
@Composable
internal fun ComposerPlusSheet(
    onOpenSkills: () -> Unit,
    onOpenMcp: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCreate: (String) -> Unit = {},
    onUpload: () -> Unit = onDismiss,
    onPhotos: () -> Unit = onDismiss,
    onCamera: () -> Unit = onDismiss,
    onVideos: () -> Unit = onDismiss,
    onVoiceCall: () -> Unit = onDismiss,
    onAutomate: (String) -> Unit = { onDismiss() },
    onContext: (String) -> Unit = { onDismiss() },
    onExploreMore: () -> Unit = onDismiss,
    onAllAutomations: () -> Unit = onDismiss,
) = QuickActionSheet(
    onOpenSkills = onOpenSkills,
    onOpenMcp = onOpenMcp,
    onDismiss = onDismiss,
    onOpenCreate = onOpenCreate,
    onUpload = onUpload,
    onPhotos = onPhotos,
    onCamera = onCamera,
    onVideos = onVideos,
    onVoiceCall = onVoiceCall,
    onAutomate = onAutomate,
    onContext = onContext,
    onExploreMore = onExploreMore,
    onAllAutomations = onAllAutomations,
)

/** Alias for plural naming [QuickActionsSheet] */
@Composable
internal fun QuickActionsSheet(
    onOpenSkills: () -> Unit,
    onOpenMcp: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCreate: (String) -> Unit = {},
    onUpload: () -> Unit = onDismiss,
    onPhotos: () -> Unit = onDismiss,
    onCamera: () -> Unit = onDismiss,
    onVideos: () -> Unit = onDismiss,
    onVoiceCall: () -> Unit = onDismiss,
    onAutomate: (String) -> Unit = { onDismiss() },
    onContext: (String) -> Unit = { onDismiss() },
    onExploreMore: () -> Unit = onDismiss,
    onAllAutomations: () -> Unit = onDismiss,
) = QuickActionSheet(
    onOpenSkills = onOpenSkills,
    onOpenMcp = onOpenMcp,
    onDismiss = onDismiss,
    onOpenCreate = onOpenCreate,
    onUpload = onUpload,
    onPhotos = onPhotos,
    onCamera = onCamera,
    onVideos = onVideos,
    onVoiceCall = onVoiceCall,
    onAutomate = onAutomate,
    onContext = onContext,
    onExploreMore = onExploreMore,
    onAllAutomations = onAllAutomations,
)

@Composable
internal fun QuickActionSheetContent(
    onOpenSkills: () -> Unit,
    onOpenMcp: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCreate: (String) -> Unit = {},
    onUpload: () -> Unit = onDismiss,
    onPhotos: () -> Unit = onDismiss,
    onCamera: () -> Unit = onDismiss,
    onVideos: () -> Unit = onDismiss,
    onVoiceCall: () -> Unit = onDismiss,
    onAutomate: (String) -> Unit = { onDismiss() },
    onContext: (String) -> Unit = { onDismiss() },
    onExploreMore: () -> Unit = onDismiss,
    onAllAutomations: () -> Unit = onDismiss,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth(),
        ) {
            // Header (sticky at top of sheet): Centered "Quick Actions" title (18.sp, bold, onSurface) + far-right ✕ close IconButton (48.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gutter, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.quick_actions_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterEnd),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.quick_actions_close_cd),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Scrollable Body (verticalScroll, navigationBarsPadding() at bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.gutter)
                    .navigationBarsPadding()
                    .padding(bottom = spacing.lg),
            ) {
                // 1. Agent tools (2-column row, 48.dp min-height clickable rows with 22.dp outlined icon + 12.dp gap + label)
                Text(
                    text = stringResource(R.string.quick_actions_agent_tools),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionRowItem(
                        icon = Icons.Outlined.Extension,
                        label = stringResource(R.string.quick_actions_skills),
                        onClick = onOpenSkills,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionRowItem(
                        icon = Icons.Outlined.Link,
                        label = stringResource(R.string.quick_actions_connectors),
                        onClick = onOpenMcp,
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                // 2. Frequently used (2-column grid, 5 items)
                Text(
                    text = stringResource(R.string.quick_actions_frequently_used),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionRowItem(
                        icon = Icons.Outlined.AttachFile,
                        label = stringResource(R.string.quick_actions_upload),
                        onClick = onUpload,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionRowItem(
                        icon = Icons.Outlined.PhotoLibrary,
                        label = stringResource(R.string.quick_actions_photos),
                        onClick = onPhotos,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionRowItem(
                        icon = Icons.Outlined.PhotoCamera,
                        label = stringResource(R.string.quick_actions_camera),
                        onClick = onCamera,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionRowItem(
                        icon = Icons.Outlined.Movie,
                        label = stringResource(R.string.quick_actions_videos),
                        onClick = onVideos,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionRowItem(
                        icon = Icons.Outlined.Phone,
                        label = stringResource(R.string.quick_actions_voice_call),
                        onClick = onVoiceCall,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.weight(1f))
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                // 3. Create (Header left "Create", header right "Explore more >")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.quick_actions_create),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier
                            .sizeIn(minHeight = 48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(role = Role.Button, onClick = onExploreMore),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.quick_actions_explore_more),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                // 4-column icon-above-label grid (2 rows × 4 items, 48.dp+ hit area per tile)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    QuickActionCreateTile(
                        icon = Icons.Outlined.Public,
                        label = stringResource(R.string.quick_actions_website),
                        iconTint = TeamilyMarks.tileWebsite,
                        onClick = {
                            onDismiss()
                            onOpenCreate("website")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCreateTile(
                        icon = Icons.Outlined.Description,
                        label = stringResource(R.string.quick_actions_document),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            onDismiss()
                            onOpenCreate("document")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCreateTile(
                        icon = Icons.Outlined.CoPresent,
                        label = stringResource(R.string.quick_actions_slides),
                        iconTint = TeamilyMarks.tileSlides,
                        onClick = {
                            onDismiss()
                            onOpenCreate("slides")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCreateTile(
                        icon = Icons.Outlined.TableChart,
                        label = stringResource(R.string.quick_actions_spreadsheet),
                        iconTint = TeamilyMarks.tileSheet,
                        onClick = {
                            onDismiss()
                            onOpenCreate("spreadsheet")
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    QuickActionCreateTile(
                        icon = Icons.Outlined.Image,
                        label = stringResource(R.string.quick_actions_image),
                        iconTint = TeamilyMarks.tileImage,
                        onClick = {
                            onDismiss()
                            onOpenCreate("image")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCreateTile(
                        icon = Icons.Outlined.PhotoCamera,
                        label = stringResource(R.string.quick_actions_video),
                        iconTint = TeamilyMarks.tileVideo,
                        isPro = true,
                        onClick = {
                            onDismiss()
                            onOpenCreate("video")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCreateTile(
                        icon = Icons.Outlined.MusicNote,
                        label = stringResource(R.string.quick_actions_music),
                        iconTint = TeamilyMarks.tileMusic,
                        onClick = {
                            onDismiss()
                            onOpenCreate("music")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionCreateTile(
                        icon = Icons.Outlined.ViewInAr,
                        label = stringResource(R.string.quick_actions_3d_model),
                        iconTint = TeamilyMarks.tileModel,
                        onClick = {
                            onDismiss()
                            onOpenCreate("model")
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                // 4. Automate (Header left "Automate", header right "All automations >")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.quick_actions_automate),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier
                            .sizeIn(minHeight = 48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(role = Role.Button, onClick = onAllAutomations),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.quick_actions_all_automations),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionRowItem(
                        icon = Icons.Outlined.Notifications,
                        label = stringResource(R.string.quick_actions_meeting_reminder),
                        onClick = { onAutomate("meeting_reminder") },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionRowItem(
                        icon = Icons.AutoMirrored.Outlined.Article,
                        label = stringResource(R.string.quick_actions_daily_brief),
                        onClick = { onAutomate("daily_brief") },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionRowItem(
                        icon = Icons.Outlined.Biotech,
                        label = stringResource(R.string.quick_actions_expert_research),
                        onClick = { onAutomate("expert_research") },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionRowItem(
                        icon = Icons.Outlined.TrackChanges,
                        label = stringResource(R.string.quick_actions_drive_goal),
                        onClick = { onAutomate("drive_goal") },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                // 5. Context (2-column row, 2 items)
                Text(
                    text = stringResource(R.string.quick_actions_context),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionRowItem(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        label = stringResource(R.string.quick_actions_memory),
                        onClick = { onContext("memory") },
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionRowItem(
                        icon = Icons.Outlined.Psychology,
                        label = stringResource(R.string.quick_actions_recall),
                        onClick = { onContext("recall") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionRowItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Row(
        modifier = modifier
            .sizeIn(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}

@Composable
private fun QuickActionCreateTile(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPro: Boolean = false,
) {
    Column(
        modifier = modifier
            .sizeIn(minHeight = 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )
            if (isPro) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 16.dp, y = (-4).dp),
                ) {
                    Text(
                        text = stringResource(R.string.quick_actions_pro),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ComposerPlusSheetContent(
    onOpenSkills: () -> Unit,
    onOpenMcp: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCreate: (String) -> Unit = {},
    onUpload: () -> Unit = onDismiss,
    onPhotos: () -> Unit = onDismiss,
    onCamera: () -> Unit = onDismiss,
    onVideos: () -> Unit = onDismiss,
    onVoiceCall: () -> Unit = onDismiss,
    onAutomate: (String) -> Unit = { onDismiss() },
    onContext: (String) -> Unit = { onDismiss() },
    onExploreMore: () -> Unit = onDismiss,
    onAllAutomations: () -> Unit = onDismiss,
    modifier: Modifier = Modifier,
) = QuickActionSheetContent(
    onOpenSkills = onOpenSkills,
    onOpenMcp = onOpenMcp,
    onDismiss = onDismiss,
    onOpenCreate = onOpenCreate,
    onUpload = onUpload,
    onPhotos = onPhotos,
    onCamera = onCamera,
    onVideos = onVideos,
    onVoiceCall = onVoiceCall,
    onAutomate = onAutomate,
    onContext = onContext,
    onExploreMore = onExploreMore,
    onAllAutomations = onAllAutomations,
    modifier = modifier,
)

@Preview(name = "QuickActionSheet - Light", showBackground = true)
@Preview(
    name = "QuickActionSheet - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun QuickActionSheetPreview() {
    DuaGenAITheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            QuickActionSheetContent(
                onOpenSkills = {},
                onOpenMcp = {},
                onDismiss = {},
            )
        }
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
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
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
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
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
