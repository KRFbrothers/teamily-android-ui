@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.duagenai.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import kotlinx.coroutines.launch

/* ============================================================================
 *  Live flows from index (1).html that the nav host did not have yet:
 *  profile stat lists, the public profile, consolidated settings, and the
 *  eight Create screens. Colours are the existing theme, except the eight
 *  Create tiles, which keep the product colours the HTML names for them.
 * ==========================================================================*/

internal data class ProfileListRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val meta: String = "",
)

/**
 * The Profile "Chats" list. No fake conversations: empty until real chats
 * exist, so the screen renders its honest empty state.
 */
internal fun profileChatRows(): List<ProfileListRow> = emptyList()

/**
 * The Profile "Agents" list: only the two pinned built-in assistants. No
 * sample guild chats and no fabricated "New Agent N" rows — those were fake
 * data pretending to be the user's agents.
 */
internal fun profileAgentRows(): List<ProfileListRow> = listOf(
    ProfileListRow(PERSONAL_AGENT_CHAT_ID, PERSONAL_AGENT_TITLE, PERSONAL_AGENT_PREVIEW),
    ProfileListRow(AGENT_CHAT_ID, DUA_AGENT_TITLE, DUA_AGENT_SUBTITLE, DUA_AGENT_TIME),
)

/** The Profile "Artifacts" list. Empty until the user really saves one. */
internal fun profileArtifactRows(): List<ProfileListRow> = emptyList()

internal fun linesOf(raw: String): List<String> =
    raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

internal data class ConnectorRow(
    val key: String,
    val label: String,
    val subtitle: String,
    val connected: Boolean,
)

internal data class CatalogApp(val key: String, val label: String, val subtitle: String)

internal fun defaultConnectors(): List<ConnectorRow> = listOf(
    ConnectorRow("github", "GitHub", "Repos, pull requests, issues", true),
    ConnectorRow("notion", "Notion", "Pages and databases", true),
    ConnectorRow("drive", "Google Drive", "Files an agent can read", false),
    ConnectorRow("gmail", "Gmail", "Read and draft mail", false),
    ConnectorRow("slack", "Slack", "Channels and messages", false),
)

internal val CONNECTOR_CATALOG = listOf(
    CatalogApp("figma", "Figma", "Design files and comments"),
    CatalogApp("linear", "Linear", "Issues and projects"),
    CatalogApp("jira", "Jira", "Tickets and sprints"),
    CatalogApp("calendar", "Google Calendar", "Events and availability"),
    CatalogApp("dropbox", "Dropbox", "Files an agent can read"),
    CatalogApp("discord", "Discord", "Servers and channels"),
    CatalogApp("telegram", "Telegram", "Chats and messages"),
    CatalogApp("postgres", "Postgres", "Read-only database access"),
)

internal fun encodeConnectors(rows: List<ConnectorRow>): String =
    rows.joinToString("\n") { "${it.key}\t${it.label}\t${it.subtitle}\t${if (it.connected) 1 else 0}" }

internal fun decodeConnectors(raw: String): List<ConnectorRow> =
    if (raw.isBlank()) defaultConnectors()
    else raw.lineSequence().filter { it.isNotBlank() }.map { line ->
        val p = line.split('\t')
        ConnectorRow(
            key = p.getOrElse(0) { "" },
            label = p.getOrElse(1) { "" },
            subtitle = p.getOrElse(2) { "" },
            connected = p.getOrElse(3) { "0" } == "1",
        )
    }.filter { it.key.isNotEmpty() }.toList()

internal val DEFAULT_CONNECTOR_STATE: String = encodeConnectors(defaultConnectors())

internal data class ChannelRow(
    val key: String,
    val label: String,
    val subtitle: String,
    val on: Boolean,
    val whenLabel: String,
    val unread: Int,
    val fresh: Boolean,
)

internal fun defaultChannels(): List<ChannelRow> = listOf(
    ChannelRow("whatsapp", "WhatsApp", "Personal number pe Dua Agent", true, "2m", 3, true),
    ChannelRow("telegram", "Telegram", "Bot handle se messages", true, "18m", 1, false),
    ChannelRow("slack", "Slack", "Workspace channels me mention", false, "1h", 0, false),
    ChannelRow("email", "Email", "Agent ka apna inbox address", false, "3h", 2, false),
    ChannelRow("web", "Web widget", "Website pe embed chat box", false, "Yesterday", 0, false),
    ChannelRow("api", "Agent API", "Apne apps se direct calls", true, "2d", 0, false),
)

internal fun encodeChannels(rows: List<ChannelRow>): String =
    rows.joinToString("\n") {
        "${it.key}\t${it.label}\t${it.subtitle}\t${if (it.on) 1 else 0}\t${it.whenLabel}\t${it.unread}\t${if (it.fresh) 1 else 0}"
    }

internal fun decodeChannels(raw: String): List<ChannelRow> =
    if (raw.isBlank()) defaultChannels()
    else raw.lineSequence().filter { it.isNotBlank() }.map { line ->
        val p = line.split('\t')
        ChannelRow(
            key = p.getOrElse(0) { "" },
            label = p.getOrElse(1) { "" },
            subtitle = p.getOrElse(2) { "" },
            on = p.getOrElse(3) { "0" } == "1",
            whenLabel = p.getOrElse(4) { "" },
            unread = p.getOrElse(5) { "0" }.toIntOrNull() ?: 0,
            fresh = p.getOrElse(6) { "0" } == "1",
        )
    }.filter { it.key.isNotEmpty() }.toList()

internal val DEFAULT_CHANNEL_STATE: String = encodeChannels(defaultChannels())

internal data class LanguageOption(val id: String, val label: String, val subtitle: String)

internal val APP_LANGUAGES = listOf(
    LanguageOption("en", "English", "App aur replies English me"),
    LanguageOption("hi", "Hinglish", "Hindi + English mix"),
    LanguageOption("hi-in", "हिन्दी", "पूरा हिंदी में"),
    LanguageOption("ar", "العربية", "Right-to-left layout"),
    LanguageOption("zh", "中文", "简体中文"),
)

internal data class CreateTile(
    val key: String,
    val title: String,
    val headline: String,
    val subtitle: String,
    val placeholder: String,
    val steps: List<String>,
    /** Product colour from the HTML tile. Null uses the theme's onSurface. */
    val color: Color?,
    val pro: Boolean = false,
)

internal val CREATE_TILES = listOf(
    CreateTile(
        "website", "Website", "Website banao",
        "Landing page, portfolio ya microsite — ek prompt se.",
        "Kaisi website chahiye? Sections, tone aur colour bata do…",
        listOf("Sections aur content likho", "Theme aur tone chuno", "Publish karke live URL lo"),
        Color(0xFF4285F4),
    ),
    CreateTile(
        "document", "Document", "Document likho",
        "Report, brief ya notes — editable doc ke roop me.",
        "Kis baare me likhna hai? Audience aur length bata do…",
        listOf("Topic aur audience do", "Outline review karo", "Doc library me khul jayega"),
        null,
    ),
    CreateTile(
        "slides", "Slides", "Slides banao",
        "Deck ya pitch — poore slides, template ke saath.",
        "Kitne slides chahiye aur kis topic par?…",
        listOf("Story aur slide count do", "Template choose karo", "Deck preview me khulega"),
        Color(0xFFF4B400),
    ),
    CreateTile(
        "spreadsheet", "Spreadsheet", "Spreadsheet banao",
        "Tables, budget, tracker — formulas ke saath.",
        "Kaunse columns chahiye? Data ka source bata do…",
        listOf("Columns aur rows define karo", "Formulas add karo", "Excel file download hogi"),
        Color(0xFF0F9D58),
    ),
    CreateTile(
        "image", "Image", "Image banao",
        "Illustration, banner ya icon — prompt se.",
        "Kya dikhana hai? Style, mood aur ratio bata do…",
        listOf("Scene aur style likho", "Ratio chuno", "Image gallery me milegi"),
        Color(0xFFAB47BC),
    ),
    CreateTile(
        "video", "Video", "Video banao",
        "Chhote clips — text ya reference image se.",
        "Kya ho raha hai clip me? Camera aur mood bata do…",
        listOf("Prompt ya reference image do", "Duration aur ratio chuno", "Clip render hoke milega"),
        Color(0xFFEC407A),
        pro = true,
    ),
    CreateTile(
        "music", "Music", "Music banao",
        "Songs ya instrumental — genre aur mood se.",
        "Genre, mood aur tempo bata do. Lyrics chahiye?…",
        listOf("Genre aur mood likho", "Instrumental ya vocals?", "Audio file milegi"),
        Color(0xFF7986CB),
    ),
    CreateTile(
        "model", "3D model", "3D model banao",
        "Props aur characters — text ya image se.",
        "Kya banana hai? Style aur detail level bata do…",
        listOf("Object aur style likho", "Polygon budget chuno", "GLB / FBX milega"),
        Color(0xFF4DD0E1),
    ),
)

internal fun createTile(key: String): CreateTile =
    CREATE_TILES.firstOrNull { it.key == key } ?: CREATE_TILES.first()

@Composable
internal fun ProfileListScreen(
    title: String,
    kicker: String,
    rows: List<ProfileListRow>,
    onBack: () -> Unit,
    onOpenRow: (ProfileListRow) -> Unit,
) {
    FlowPage(title = title, onBack = onBack) {
        if (rows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LocalDuaGenAISpacing.current.gutter, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Abhi kuch nahi hai",
                    style = DuaGenAITextStyles.rowTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(LocalDuaGenAISpacing.current.xs))
                Text(
                    text = "Yahan jo aap banayenge wahi dikhega.",
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = LocalDuaGenAISpacing.current.xxl),
            ) {
                item {
                    Text(
                        text = "$kicker · ${rows.size}",
                        style = DuaGenAITextStyles.eyebrow,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = LocalDuaGenAISpacing.current.gutter,
                            end = LocalDuaGenAISpacing.current.gutter,
                            top = LocalDuaGenAISpacing.current.md,
                            bottom = LocalDuaGenAISpacing.current.sm,
                        ),
                    )
                }
                items(rows, key = { it.id }) { row ->
                    SettingsRow(
                        title = row.title,
                        subtitle = if (row.meta.isBlank()) row.subtitle else "${row.subtitle} · ${row.meta}",
                        icon = null,
                        onClick = { onOpenRow(row) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun PublicProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    var tab by rememberSaveable { mutableStateOf("Posts") }
    var following by rememberSaveable { mutableStateOf(false) }
    var alertsOn by rememberSaveable { mutableStateOf(false) }
    val tabs = listOf("Posts", "Favorites", "Likes")
    val empty = when (tab) {
        "Favorites" -> "No favorites yet."
        "Likes" -> "No likes yet."
        else -> "No posts yet."
    }
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xs, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Settings")
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(DuaGenAITheme.shapes.avatarProfile)
                        .background(DuaGenAITheme.colors.logoMint),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "f",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DuaGenAITheme.colors.onLogoMint,
                    )
                }
                Spacer(Modifier.width(spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "fareed",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Dua Gen AI",
                        style = DuaGenAITextStyles.snippet,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                TextButton(onClick = { following = !following }) {
                    Text(if (following) "Following" else "Follow")
                }
                TextButton(onClick = { alertsOn = !alertsOn }) {
                    Text(if (alertsOn) "Alerts on" else "Notifications")
                }
                TextButton(onClick = { /* share stays on this screen */ }) {
                    Text("Share")
                }
            }
            Spacer(Modifier.height(spacing.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PublicStat("0", "Following")
                PublicStat("0", "Followers")
                PublicStat("0", "Likes & Saves")
            }
            Spacer(Modifier.height(spacing.lg))
            Row(Modifier.fillMaxWidth()) {
                for (name in tabs) {
                    val selected = name == tab
                    Text(
                        text = name,
                        modifier = Modifier
                            .weight(1f)
                            .clip(DuaGenAITheme.shapes.pill)
                            .then(
                                if (selected) Modifier.background(DuaGenAITheme.colors.brandSoft)
                                else Modifier,
                            )
                            .clickable(role = Role.Tab) { tab = name }
                            .padding(vertical = spacing.sm),
                        textAlign = TextAlign.Center,
                        style = DuaGenAITextStyles.button,
                        color = if (selected) DuaGenAITheme.colors.onBrandSoft
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(spacing.lg))
            Text(
                text = empty,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = DuaGenAITextStyles.snippet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.lg))
            Text(
                text = "AGENTS",
                style = DuaGenAITextStyles.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.sm))
            PublicAgentRow("Webpage Developer", Color(0xFF79C79A))
            PublicAgentRow("Feishu Integration Developer", Color(0xFFE0D7FF))
        }
    }
}

@Composable
private fun PublicStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = DuaGenAITextStyles.statNumber, color = MaterialTheme.colorScheme.onBackground)
        Text(text = label, style = DuaGenAITextStyles.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PublicAgentRow(label: String, swatch: Color) {
    val spacing = LocalDuaGenAISpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(DuaGenAITheme.shapes.avatarList)
                .background(swatch),
        )
        Spacer(Modifier.width(spacing.md))
        Text(text = label, style = DuaGenAITextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun SettingsDirectory(
    connectorSummary: String,
    languageLabel: String,
    themeLabel: String,
    storageLabel: String,
    notificationsLabel: String,
    onOpenConnectors: () -> Unit,
    onOpenRuntimes: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenChannels: () -> Unit,
    onOpenAgentApi: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onEnterReadOnly: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    var query by rememberSaveable { mutableStateOf("") }
    var linkPreviews by rememberSaveable { mutableStateOf(true) }
    var taskAlerts by rememberSaveable { mutableStateOf(true) }
    var folding by rememberSaveable { mutableStateOf("Off") }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    val needle = query.trim().lowercase()

    class DirRow(
        val group: String,
        val title: String,
        val subtitle: String = "",
        val trailing: String = "",
        val danger: Boolean = false,
        val checked: Boolean? = null,
        val onClick: () -> Unit = {},
    )

    val rows = listOf(
        DirRow("AI & Agent Setup", "App Connectors", "Third-party accounts your agents can use", connectorSummary, onClick = onOpenConnectors),
        DirRow("AI & Agent Setup", "Runtimes", "Python 3.11, Node 20, Chromium", onClick = onOpenRuntimes),
        DirRow("AI & Agent Setup", "Skill Configuration", onClick = onOpenSkills),
        DirRow("AI & Agent Setup", "Language", trailing = languageLabel, onClick = onOpenLanguage),
        DirRow("AI & Agent Setup", "Channels for Dua Agent", "Where Dua Agent can be reached", onClick = onOpenChannels),
        DirRow("AI & Agent Setup", "Agent API", "Who can reach your agents from outside Dua Gen AI", onClick = onOpenAgentApi),
        DirRow("Preferences & Security", "Show Link Previews", checked = linkPreviews, onClick = { linkPreviews = !linkPreviews }),
        DirRow("Preferences & Security", "Task completion alerts", checked = taskAlerts, onClick = { taskAlerts = !taskAlerts }),
        DirRow("Preferences & Security", "Message folding", trailing = folding, onClick = { folding = if (folding == "Off") "On" else "Off" }),
        DirRow("Profile & Device", "Appearance", trailing = themeLabel, onClick = onOpenAppearance),
        DirRow("Profile & Device", "Storage", trailing = storageLabel, onClick = onOpenStorage),
        DirRow("Profile & Device", "Notifications", trailing = notificationsLabel, onClick = onOpenNotifications),
        DirRow(
            "Profile & Device",
            "Sign out",
            "Aap is device se sign out ho jayenge",
            danger = true,
            onClick = { confirmSignOut = true },
        ),
    )
    val visible = rows.filter { row ->
        needle.isEmpty() ||
            row.group.lowercase().contains(needle) ||
            row.title.lowercase().contains(needle) ||
            row.subtitle.lowercase().contains(needle)
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Search settings" },
        singleLine = true,
        placeholder = { Text("Search settings") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
    if (visible.isEmpty()) {
        Text(
            text = "Kuch nahi mila",
            style = DuaGenAITextStyles.rowTitle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Doosra shabd try karein.",
            style = DuaGenAITextStyles.snippet,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        var lastGroup = ""
        for (row in visible) {
            if (row.group != lastGroup) {
                lastGroup = row.group
                Text(
                    text = row.group.uppercase(),
                    style = DuaGenAITextStyles.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.sm),
                )
            }
            if (row.checked != null) {
                SwitchLine(title = row.title, checked = row.checked, onCheckedChange = { row.onClick() })
            } else {
                val trailing = row.trailing
                SettingsRow(
                    title = row.title,
                    subtitle = if (trailing.isBlank()) row.subtitle.ifBlank { " " } else {
                        if (row.subtitle.isBlank()) trailing else "${row.subtitle} · $trailing"
                    },
                    icon = null,
                    onClick = row.onClick,
                    titleColor = if (row.danger) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = {
                Text(
                    "Aap Dua Gen AI se sign out ho jayenge. Aapke chats, memories aur artifacts safe rahenge — dobara sign in karke wahi sab milega.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onEnterReadOnly()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SwitchLine(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val spacing = LocalDuaGenAISpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { contentDescription = title }
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = DuaGenAITextStyles.rowTitle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun ConnectorsScreen(
    rows: List<ConnectorRow>,
    onChange: (List<ConnectorRow>) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val connected = rows.count { it.connected }
    FlowPage(title = "App Connectors", onBack = onBack, snackbar = snackbar) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xxl),
        ) {
            Note("Third-party accounts your agents can use. A connected account lets an agent read and act in that service.")
            for ((index, row) in rows.withIndex()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .padding(horizontal = spacing.sm, vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = "Reorder ${row.label}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .pointerInput(row.key, rows.size) {
                                var accum = 0f
                                detectDragGesturesAfterLongPress(
                                    onDragEnd = { accum = 0f },
                                    onDragCancel = { accum = 0f },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        accum += drag.y
                                        val step = 56.dp.toPx()
                                        val list = rows
                                        val from = list.indexOfFirst { it.key == row.key }
                                        if (from >= 0 && accum > step && from < list.lastIndex) {
                                            onChange(list.moved(from, from + 1))
                                            accum = 0f
                                        } else if (accum < -step && from > 0) {
                                            onChange(list.moved(from, from - 1))
                                            accum = 0f
                                        }
                                    },
                                )
                            },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(row.label, style = DuaGenAITextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                        Text(row.subtitle, style = DuaGenAITextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = if (row.connected) "Connected" else "Connect",
                        modifier = Modifier
                            .clip(DuaGenAITheme.shapes.pill)
                            .background(
                                if (row.connected) DuaGenAITheme.colors.brandSoft
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable {
                                onChange(rows.mapIndexed { i, item ->
                                    if (i == index) item.copy(connected = !item.connected) else item
                                })
                            }
                            .padding(horizontal = spacing.md, vertical = spacing.sm)
                            .semantics { contentDescription = if (row.connected) "Connected" else "Connect" },
                        style = DuaGenAITextStyles.button,
                        color = if (row.connected) DuaGenAITheme.colors.onBrandSoft
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            SettingsRow(title = "Add connector", subtitle = "Koi naya app jodo", icon = null, onClick = onAdd)
            if (connected > 0) {
                SettingsRow(
                    title = "Disconnect all",
                    subtitle = if (connected == 1) "1 app connected" else "$connected apps connected",
                    icon = null,
                    onClick = { confirm = true },
                    titleColor = MaterialTheme.colorScheme.error,
                )
            }
            Note("Yeh list is device par rehti hai. Koi asli OAuth nahi hota.")
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Disconnect all?") },
            text = {
                Text(
                    if (connected == 1) "1 app hat jayega. Aap baad me dobara connect kar sakte hain."
                    else "$connected apps hat jayenge. Aap baad me dobara connect kar sakte hain.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    val snapshot = rows
                    onChange(rows.map { it.copy(connected = false) })
                    scope.launch {
                        val result = snackbar.showSnackbar(
                            message = "Saare apps disconnect ho gaye",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) onChange(snapshot)
                    }
                }) { Text("Disconnect all") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun AddConnectorScreen(
    already: Set<String>,
    recentKeys: List<String>,
    onAdd: (CatalogApp) -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    var query by rememberSaveable { mutableStateOf("") }
    val needle = query.trim().lowercase()
    val catalog = CONNECTOR_CATALOG.filter { app ->
        needle.isEmpty() || app.label.lowercase().contains(needle) || app.subtitle.lowercase().contains(needle)
    }
    val recent = recentKeys.mapNotNull { key -> catalog.firstOrNull { it.key == key } }
    val rest = catalog.filter { app -> recent.none { it.key == app.key } }
    FlowPage(title = "Add connector", onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xxl),
        ) {
            Note("Ek app chuno — wo App Connectors list me jud jayega.")
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gutter),
                singleLine = true,
                placeholder = { Text("Search apps") },
            )
            if (recent.isNotEmpty()) {
                SectionLabel("Recently used")
                for (app in recent) CatalogRow(app, already.contains(app.key)) { onAdd(app) }
            }
            SectionLabel(if (recent.isEmpty()) "Suggested apps" else "All apps")
            for (app in rest) CatalogRow(app, already.contains(app.key)) { onAdd(app) }
            Note("Koi asli OAuth nahi hota; app sirf list me judta hai.")
        }
    }
}

@Composable
private fun CatalogRow(app: CatalogApp, connected: Boolean, onAdd: () -> Unit) {
    SettingsRow(
        title = app.label,
        subtitle = if (connected) "Connected" else app.subtitle,
        icon = null,
        onClick = if (connected) null else onAdd,
        enabled = !connected,
    )
}

@Composable
internal fun RuntimesScreen(onBack: () -> Unit) {
    val spacing = LocalDuaGenAISpacing.current
    FlowPage(title = "Runtimes", onBack = onBack) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xxl),
        ) {
            Note("Woh environments jahan tumhare agents ka code chalta hai.")
            RuntimeRow("Python", "Scripts, data processing, ML", "3.11")
            RuntimeRow("Node.js", "Builds, tooling, web apps", "20.x")
            RuntimeRow("Chromium", "Browser automation, screenshots", "120+")
            Note("Har run ko 1 CPU, 2 GB RAM aur 4 GB disk milta hai, 90 minute tak.")
        }
    }
}

@Composable
private fun RuntimeRow(title: String, subtitle: String, value: String) {
    SettingsRow(
        title = title,
        subtitle = "$subtitle · $value · Ready",
        icon = null,
        onClick = null,
        enabled = false,
    )
}

@Composable
internal fun LanguageScreen(
    selectedId: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    FlowPage(title = "Language", onBack = onBack) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xxl),
        ) {
            Note("App aur agent replies isi language me honge.")
            for (option in APP_LANGUAGES) {
                val selected = option.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(option.id) })
                        .padding(horizontal = spacing.gutter, vertical = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Spacer(Modifier.width(spacing.md))
                    Column {
                        Text(option.label, style = DuaGenAITextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                        Text(option.subtitle, style = DuaGenAITextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Note("Language badalne se Arabic par layout right-to-left ho jata hai. Chats translate nahi hote.")
        }
    }
}

@Composable
internal fun ChannelsScreen(
    rows: List<ChannelRow>,
    onChange: (List<ChannelRow>) -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val unreadTotal = rows.sumOf { it.unread }
    FlowPage(title = "Channels for Dua Agent", onBack = onBack) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xxl),
        ) {
            Note("Woh jagah jahan se Dua Agent tak baat pahunchti hai.")
            for (row in rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .padding(horizontal = spacing.sm, vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = "Reorder ${row.label}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .pointerInput(row.key) {
                                var accum = 0f
                                detectDragGesturesAfterLongPress(
                                    onDragEnd = { accum = 0f },
                                    onDragCancel = { accum = 0f },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        accum += drag.y
                                        val step = 56.dp.toPx()
                                        val list = rows
                                        val from = list.indexOfFirst { it.key == row.key }
                                        if (from >= 0 && accum > step && from < list.lastIndex) {
                                            onChange(list.moved(from, from + 1))
                                            accum = 0f
                                        } else if (accum < -step && from > 0) {
                                            onChange(list.moved(from, from - 1))
                                            accum = 0f
                                        }
                                    },
                                )
                            },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(row.label, style = DuaGenAITextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                        Text(row.subtitle, style = DuaGenAITextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (row.unread > 0) {
                        Text(
                            text = row.unread.toString(),
                            modifier = Modifier
                                .clip(DuaGenAITheme.shapes.pill)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    onChange(rows.map { if (it.key == row.key) it.copy(unread = 0) else it })
                                }
                                .padding(horizontal = spacing.sm, vertical = spacing.xs)
                                .semantics { contentDescription = "${row.unread} unread" },
                            style = DuaGenAITextStyles.meta.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(spacing.xs))
                    }
                    Text(
                        text = row.whenLabel,
                        style = DuaGenAITextStyles.meta,
                        color = if (row.fresh) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(spacing.sm))
                    Switch(
                        checked = row.on,
                        onCheckedChange = { on ->
                            onChange(rows.map { if (it.key == row.key) it.copy(on = on) else it })
                        },
                        modifier = Modifier.semantics { contentDescription = row.label },
                    )
                }
            }
            if (unreadTotal > 0) {
                SettingsRow(
                    title = "Mark all as read",
                    subtitle = if (unreadTotal == 1) "1 unread message clear ho jayega"
                    else "$unreadTotal unread messages clear ho jayenge",
                    icon = null,
                    onClick = { onChange(rows.map { it.copy(unread = 0) }) },
                )
            }
            Note("Switch is device ki state badalta hai. Koi asli channel verify nahi hota.")
        }
    }
}

@Composable
internal fun SkillConfigScreen(
    enabledIds: Set<String>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    FlowPage(title = "Skill Configuration", onBack = onBack) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xxl),
        ) {
            Note("Yahi skills Dua Agent ki chat mein lagti hain.")
            for (skill in DuaAgentSkills.all) {
                val on = skill.id in enabledIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp)
                        .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle(skill.id) })
                        .semantics { contentDescription = skill.title }
                        .padding(horizontal = spacing.gutter, vertical = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(skill.title, style = DuaGenAITextStyles.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                        Text(skill.summary, style = DuaGenAITextStyles.snippet, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = on, onCheckedChange = null)
                }
            }
        }
    }
}

@Composable
internal fun CreateKindScreen(kind: String, onBack: () -> Unit) {
    val spacing = LocalDuaGenAISpacing.current
    val tile = createTile(kind)
    val ink = tile.color ?: MaterialTheme.colorScheme.onSurface
    var draft by rememberSaveable(kind) { mutableStateOf("") }
    FlowPage(title = tile.title, onBack = onBack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(text = tile.subtitle, style = DuaGenAITextStyles.rowTitle, color = ink)
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "${tile.headline} prompt" },
                placeholder = { Text(tile.placeholder) },
                minLines = 4,
            )
            Text(
                text = "KAISE CHALEGA",
                style = DuaGenAITextStyles.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for ((index, step) in tile.steps.withIndex()) {
                Text(
                    text = "${index + 1}. $step",
                    style = DuaGenAITextStyles.snippet,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "Yahan generation nahi hoti. Yeh screen sirf navigation hai.",
                style = DuaGenAITextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FlowPage(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { if (snackbar != null) SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        content = { inner ->
            Box(Modifier.fillMaxSize().padding(inner)) { content() }
        },
    )
}

@Composable
private fun Note(text: String) {
    val spacing = LocalDuaGenAISpacing.current
    Text(
        text = text,
        style = DuaGenAITextStyles.snippet,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = spacing.gutter, vertical = spacing.sm),
    )
}

@Composable
private fun SectionLabel(text: String) {
    val spacing = LocalDuaGenAISpacing.current
    Text(
        text = text.uppercase(),
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

private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    val copy = toMutableList()
    val item = copy.removeAt(from)
    copy.add(to, item)
    return copy
}

@Preview(name = "Profile list — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileListLight() = DuaGenAITheme(darkTheme = false) {
    ProfileListScreen(
        title = "Chats",
        kicker = "CHATS",
        rows = profileChatRows(),
        onBack = {},
        onOpenRow = {},
    )
}

@Preview(name = "Profile list — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileListDark() = DuaGenAITheme(darkTheme = true) {
    ProfileListScreen(
        title = "Chats",
        kicker = "CHATS",
        rows = profileChatRows(),
        onBack = {},
        onOpenRow = {},
    )
}

@Preview(name = "Public profile — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PublicProfileLight() = DuaGenAITheme(darkTheme = false) {
    PublicProfileScreen(onBack = {}, onOpenSettings = {})
}

@Preview(name = "Public profile — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PublicProfileDark() = DuaGenAITheme(darkTheme = true) {
    PublicProfileScreen(onBack = {}, onOpenSettings = {})
}

@Preview(name = "Connectors — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectorsLight() = DuaGenAITheme(darkTheme = false) {
    ConnectorsScreen(rows = defaultConnectors(), onChange = {}, onAdd = {}, onBack = {})
}

@Preview(name = "Connectors — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectorsDark() = DuaGenAITheme(darkTheme = true) {
    ConnectorsScreen(rows = defaultConnectors(), onChange = {}, onAdd = {}, onBack = {})
}

@Preview(name = "Add connector — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddConnectorLight() = DuaGenAITheme(darkTheme = false) {
    AddConnectorScreen(already = emptySet(), recentKeys = emptyList(), onAdd = {}, onBack = {})
}

@Preview(name = "Add connector — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AddConnectorDark() = DuaGenAITheme(darkTheme = true) {
    AddConnectorScreen(already = emptySet(), recentKeys = emptyList(), onAdd = {}, onBack = {})
}

@Preview(name = "Runtimes — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RuntimesLight() = DuaGenAITheme(darkTheme = false) { RuntimesScreen(onBack = {}) }

@Preview(name = "Runtimes — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RuntimesDark() = DuaGenAITheme(darkTheme = true) { RuntimesScreen(onBack = {}) }

@Preview(name = "Language — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LanguageLight() = DuaGenAITheme(darkTheme = false) {
    LanguageScreen(selectedId = "en", onSelect = {}, onBack = {})
}

@Preview(name = "Language — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LanguageDark() = DuaGenAITheme(darkTheme = true) {
    LanguageScreen(selectedId = "en", onSelect = {}, onBack = {})
}

@Preview(name = "Channels — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChannelsLight() = DuaGenAITheme(darkTheme = false) {
    ChannelsScreen(rows = defaultChannels(), onChange = {}, onBack = {})
}

@Preview(name = "Channels — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChannelsDark() = DuaGenAITheme(darkTheme = true) {
    ChannelsScreen(rows = defaultChannels(), onChange = {}, onBack = {})
}

@Preview(name = "Skill config — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SkillConfigLight() = DuaGenAITheme(darkTheme = false) {
    SkillConfigScreen(enabledIds = emptySet(), onToggle = {}, onBack = {})
}

@Preview(name = "Skill config — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SkillConfigDark() = DuaGenAITheme(darkTheme = true) {
    SkillConfigScreen(enabledIds = emptySet(), onToggle = {}, onBack = {})
}

@Preview(name = "Create — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CreateKindLight() = DuaGenAITheme(darkTheme = false) {
    CreateKindScreen(kind = "image", onBack = {})
}

@Preview(name = "Create — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CreateKindDark() = DuaGenAITheme(darkTheme = true) {
    CreateKindScreen(kind = "image", onBack = {})
}
