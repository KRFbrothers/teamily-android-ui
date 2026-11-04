package ai.teamily.app.screens

import ai.teamily.app.R
import ai.teamily.app.ui.Avatar
import ai.teamily.app.ui.Card
import ai.teamily.app.ui.DetailBar
import ai.teamily.app.ui.Gutter
import ai.teamily.app.ui.HairlineDivider
import ai.teamily.app.ui.LocalThemeController
import ai.teamily.app.ui.OutlinedPill
import ai.teamily.app.ui.Pill
import ai.teamily.app.ui.ScreenTitleBar
import ai.teamily.app.ui.SectionLabel
import ai.teamily.app.ui.SettingsGroup
import ai.teamily.app.ui.SettingsRow
import ai.teamily.app.ui.T
import ai.teamily.app.ui.ThemeMode
import ai.teamily.app.ui.tc
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(onNavigate: (String) -> Unit, onSignOut: () -> Unit) {
    val theme = LocalThemeController.current
    var confirmSignOut by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitleBar("Profile")
        Column(Modifier.padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Avatar(R.drawable.user_alex, size = 80.dp)
            Spacer(Modifier.height(6.dp))
            Text("Fareed Khan", style = T.display, color = tc.ink)
            Text("fareed.khan@gmail.com", style = T.body, color = tc.ink2)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Teamily Pro", container = tc.primarySoft, content = tc.primaryInk)
                Text("Member since Mar 2024", style = T.caption, color = tc.ink3)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedPill("Edit profile", onClick = { onNavigate("edit") })
        }
        Row(Modifier.padding(horizontal = Gutter, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Chats" to "128", "Agents" to "4", "Artifacts" to "37").forEach { (label, value) ->
                Card(Modifier.weight(1f)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, style = T.rowTitle, color = tc.ink)
                        Text(label, style = T.caption, color = tc.ink3)
                    }
                }
            }
        }
        SettingsGroup("Quick actions") {
            SettingsRow("Add an agent", Icons.Default.Add, value = "4 active", onClick = { onNavigate("agents") })
            HairlineDivider()
            SettingsRow("Appearance", Icons.Default.Star, value = theme.mode.label + " mode", onClick = { onNavigate("appearance") })
            HairlineDivider()
            SettingsRow("Storage", Icons.Default.Delete, value = "3.2 GB of 15 GB", onClick = { onNavigate("storage") })
            HairlineDivider()
            SettingsRow("Notifications", Icons.Default.Notifications, value = "5 of 7 on", onClick = { onNavigate("notifications") })
        }
        SettingsGroup("Account") {
            SettingsRow("Personal information", Icons.Default.Person, "Name, email, avatar", onClick = { onNavigate("edit") })
            HairlineDivider()
            SettingsRow("Privacy & security", Icons.Default.Lock, "Passcode, blocked users", onClick = { onNavigate("privacy") })
            HairlineDivider()
            SettingsRow("All settings", Icons.Default.Settings, onClick = { onNavigate("settings") })
            HairlineDivider(0.dp)
            SettingsRow("Sign out", danger = true, onClick = { confirmSignOut = true })
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out of Teamily AI?") },
            text = { Text("Your chats and artifacts stay on your account. You'll need to sign in again on this phone.") },
            confirmButton = { TextButton({ confirmSignOut = false; onSignOut() }) { Text("Sign out", color = tc.danger) } },
            dismissButton = { TextButton({ confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val theme = LocalThemeController.current
    Column(Modifier.fillMaxSize()) {
        DetailBar("Settings", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SettingsGroup("Account") {
                SettingsRow("Fareed Khan", Icons.Default.Person, "fareed.khan@gmail.com", onClick = { onNavigate("edit") })
                HairlineDivider()
                SettingsRow("Subscription", Icons.Default.Star, "Renews 12 Nov", value = "Pro plan")
            }
            SettingsGroup("Look & feel") {
                SettingsRow("Appearance", Icons.Default.Star, value = theme.mode.label, onClick = { onNavigate("appearance") })
                HairlineDivider()
                SettingsRow("Language", Icons.Default.Create, value = "English (US)", onClick = { onNavigate("language") })
            }
            SettingsGroup("Messages") {
                SettingsRow("Notifications", Icons.Default.Notifications, value = "5 of 7 on", onClick = { onNavigate("notifications") })
                HairlineDivider()
                SettingsRow("Chat preferences", Icons.Default.Create, "Send with Enter, typing indicators", onClick = { onNavigate("chatprefs") })
            }
            SettingsGroup("Privacy & security") {
                SettingsRow("Privacy & security", Icons.Default.Lock, onClick = { onNavigate("privacy") })
            }
            SettingsGroup("Data") {
                SettingsRow("Storage", Icons.Default.Delete, "3.2 GB used of 15 GB", onClick = { onNavigate("storage") })
            }
            SettingsGroup("Support") {
                SettingsRow("About Teamily AI", Icons.Default.Info, "Version, terms, licenses", onClick = { onNavigate("about") })
            }
            Text(
                "Teamily AI 1.8.2 (build 1820) · Made with Lato", style = T.caption, color = tc.ink3,
                modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val theme = LocalThemeController.current
    var textSize by remember { mutableStateOf("Default") }
    Column(Modifier.fillMaxSize()) {
        DetailBar("Appearance", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Gutter)) {
            Text("THEME", style = T.section, color = tc.ink3, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val selected = theme.mode == mode
                    val shape = RoundedCornerShape(20.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(shape)
                            .background(if (selected) tc.primarySoft else tc.surface)
                            .border(if (selected) 1.5.dp else 1.dp, if (selected) tc.primary else tc.line, shape)
                            .clickable(role = Role.RadioButton) { theme.set(mode) }.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        androidx.compose.material3.RadioButton(selected = selected, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(mode.label, style = T.rowTitle, color = tc.ink)
                                Pill(mode.tag, container = tc.surface2, content = tc.ink2)
                            }
                            Text(mode.description, style = T.rowSub, color = tc.ink2)
                        }
                    }
                }
            }
            Text("TEXT SIZE", style = T.section, color = tc.ink3, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Small", "Default", "Large").forEach { Pill(it, selected = it == textSize, onClick = { textSize = it }) }
            }
            Text(
                "Changing the theme applies instantly across every screen and is remembered on this device.",
                style = T.caption, color = tc.ink2, modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("Fareed Khan") }
    var handle by remember { mutableStateOf("fareed") }
    var bio by remember { mutableStateOf("") }
    val dirty = name != "Fareed Khan" || handle != "fareed" || bio.isNotEmpty()
    Column(Modifier.fillMaxSize()) {
        DetailBar("Edit profile", onBack) {
            Text("Save", style = T.rowTitle, color = if (dirty) tc.primaryInk else tc.ink3,
                modifier = Modifier.clickable(enabled = dirty) { onBack() }.padding(8.dp))
        }
        Column(Modifier.padding(Gutter), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(R.drawable.user_alex, size = 96.dp)
            Field("Display name", name) { name = it }
            Field("Handle", handle) { handle = it }
            Field("Bio", bio) { bio = it }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column {
        Text(label, style = T.caption, color = tc.ink3, modifier = Modifier.padding(bottom = 4.dp))
        BasicTextField(
            value = value, onValueChange = onChange, textStyle = T.body.copy(color = tc.ink),
            cursorBrush = SolidColor(tc.primary),
            modifier = Modifier.fillMaxWidth().clip(shape).background(tc.field).border(1.dp, tc.fieldEdge, shape).padding(16.dp),
        )
    }
}

@Composable
fun TogglesScreen(title: String, onBack: () -> Unit, items: List<Pair<String, Boolean>>) {
    val state = remember { items.map { mutableStateOf(it.second) } }
    Column(Modifier.fillMaxSize()) {
        DetailBar(title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            SettingsGroup {
                items.forEachIndexed { i, (label, _) ->
                    if (i > 0) HairlineDivider(14.dp)
                    SettingsRow(label, chevron = false, onClick = { state[i].value = !state[i].value }) {
                        Switch(
                            checked = state[i].value, onCheckedChange = { state[i].value = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = tc.primary, checkedThumbColor = tc.onPrimary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoScreen(title: String, onBack: () -> Unit, rows: List<Pair<String, String>>) {
    Column(Modifier.fillMaxSize()) {
        DetailBar(title, onBack)
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            rows.forEachIndexed { i, (k, v) ->
                if (i > 0) HairlineDivider(14.dp)
                SettingsRow(k, value = v, chevron = false)
            }
        }
    }
}

@Composable
fun LanguageScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf("English (US)") }
    Column(Modifier.fillMaxSize()) {
        DetailBar("Language", onBack)
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            listOf("English (US)", "العربية", "हिन्दी", "Español", "Français", "中文").forEachIndexed { i, lang ->
                if (i > 0) HairlineDivider(14.dp)
                SettingsRow(lang, chevron = false, onClick = { selected = lang }) {
                    androidx.compose.material3.RadioButton(selected = selected == lang, onClick = null)
                }
            }
        }
    }
}
