package ai.teamily.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Gutter = 16.dp

@Composable
fun Avatar(@DrawableRes res: Int, size: Dp = 48.dp, agentBadge: Boolean = false) {
    Box(Modifier.size(size)) {
        Image(
            painterResource(res), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
        if (agentBadge) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(size * 0.36f)
                    .background(tc.mint, CircleShape).border(2.dp, tc.surface, CircleShape)
            )
        }
    }
}

@Composable
fun Pill(
    text: String, selected: Boolean = false, onClick: (() -> Unit)? = null,
    container: Color = tc.surface, content: Color = tc.ink2,
) {
    val bg = if (selected) tc.primary else container
    val fg = if (selected) tc.onPrimary else content
    val base = Modifier.clip(RoundedCornerShape(999.dp))
        .background(bg)
        .let { if (selected || container != tc.surface) it else it.border(1.dp, tc.line, RoundedCornerShape(999.dp)) }
        .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
    Box(base.height(30.dp).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.chip, color = fg)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = T.section, color = tc.ink3,
        modifier = modifier.padding(start = Gutter, top = 18.dp, bottom = 6.dp))
}

@Composable
fun ScreenTitleBar(title: String, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = Gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = T.title, color = tc.ink, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun DetailBar(title: String, onBack: () -> Unit, action: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().background(tc.surface).statusBarsPadding().height(60.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = tc.ink)
        }
        Text(title, style = T.title, color = tc.ink, modifier = Modifier.weight(1f))
        action()
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, radius: Dp = 16.dp, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(radius)
    Box(modifier.shadow(1.dp, shape).clip(shape).background(tc.surface)) { content() }
}

@Composable
fun SettingsGroup(label: String? = null, content: @Composable () -> Unit) {
    if (label != null) SectionLabel(label)
    Card(Modifier.padding(horizontal = Gutter).fillMaxWidth(), radius = 20.dp) {
        Column { content() }
    }
}

@Composable
fun SettingsRow(
    title: String, icon: ImageVector? = null, subtitle: String? = null, value: String? = null,
    danger: Boolean = false, chevron: Boolean = true, onClick: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(if (subtitle != null) 72.dp else 60.dp)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(tc.primarySoft), Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tc.primaryInk, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = T.rowTitle, color = if (danger) tc.danger else tc.ink)
            if (subtitle != null) Text(subtitle, style = T.rowSub, color = tc.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) Text(value, style = T.body, color = tc.ink3)
        if (trailing != null) trailing()
        else if (chevron && !danger) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = tc.ink3)
        }
    }
}

@Composable
fun HairlineDivider(inset: Dp = 64.dp) {
    Box(Modifier.fillMaxWidth().padding(start = inset).height(1.dp).background(tc.lineSoft))
}

@Composable
fun SearchField(placeholder: String) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier.padding(horizontal = Gutter).fillMaxWidth().height(52.dp)
            .clip(shape).background(tc.field).border(BorderStroke(1.dp, tc.fieldEdge), shape).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = tc.ink3, modifier = Modifier.size(20.dp))
        Text(placeholder, style = T.body, color = tc.ink3)
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

val NavItems = listOf(
    NavItem("chats", "Chats", Icons.Default.Home),
    NavItem("agents", "Agents", Icons.Default.Star),
    NavItem("discover", "Discover", Icons.Default.Search),
    NavItem("profile", "Profile", Icons.Default.Person),
)

@Composable
fun BottomNav(current: String?, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().shadow(8.dp).background(tc.navBg).navigationBarsPadding().height(80.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItems.forEach { item ->
            val active = item.route == current
            Column(
                Modifier.weight(1f).clickable(role = Role.Tab) { onSelect(item.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(58.dp, 30.dp).clip(RoundedCornerShape(999.dp))
                        .background(if (active) tc.primarySoft else Color.Transparent),
                    Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = null, tint = if (active) tc.primaryInk else tc.ink3, modifier = Modifier.size(24.dp))
                }
                Text(item.label, style = T.caption, color = if (active) tc.ink else tc.ink3)
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier.height(44.dp).clip(shape).background(tc.primary).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        Alignment.Center,
    ) { Text(text, style = T.rowTitle, color = tc.onPrimary) }
}

@Composable
fun OutlinedPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier.height(40.dp).clip(shape).border(1.dp, tc.line, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp),
        Alignment.Center,
    ) { Text(text, style = T.chip, color = tc.ink) }
}
