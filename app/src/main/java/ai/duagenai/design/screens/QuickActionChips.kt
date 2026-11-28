package ai.duagenai.design.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.TeamilyPalette
import ai.duagenai.design.theme.TeamilyShapes
import ai.duagenai.design.theme.teamilyPalette

/**
 * A caption plus prompt chips in rows of two.
 *
 * Lived in `DuaAgentDemoScreen.kt` until that demo screen was deleted; Dua
 * home's quick starters still use it, so it moved here rather than going with
 * the demo.
 */
@Composable
internal fun QuickActionChips(
    label: String?,
    actions: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val palette = teamilyPalette()
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalAlignment = horizontalAlignment,
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
            )
            Spacer(Modifier.height(2.dp))
        }
        actions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = horizontalArrangement,
            ) {
                row.forEach { action ->
                    ActionChip(label = action, onClick = { onPick(action) }, palette = palette)
                }
            }
        }
    }
}

/**
 * One prompt chip. Drawn as a compact capsule; the touch box is raised to the
 * 48dp minimum ([minTouchTarget]) and it is announced as a button.
 */
@Composable
internal fun ActionChip(
    label: String,
    onClick: () -> Unit,
    palette: TeamilyPalette,
) {
    val isDark = DuaGenAITheme.isDark
    val chipColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else palette.surfaceRecessed
    val chipBorder = if (isDark) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                     else BorderStroke(1.dp, palette.border)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .minTouchTarget()
            .semantics { role = Role.Button },
        shape = TeamilyShapes.capsule,
        color = chipColor,
        border = chipBorder,
    ) {
        Text(
            text = label,
            // Lato ships 400 / 700 only; Medium (500) already resolved to 400.
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
