package ai.duagenai.design.screens

import ai.duagenai.design.R
import ai.duagenai.design.bot.BabyGrokMark
import ai.duagenai.design.bot.BotLook
import ai.duagenai.design.bot.LocalBotAppearance
import ai.duagenai.design.theme.DuaGenAITheme
import ai.duagenai.design.theme.TeamilySansFontFamily
import ai.duagenai.design.theme.teamilyPalette
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ClosedCaptionDisabled
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen Voice Live Screen inspired by Gemini Live, styled in
 * Dua Gen AI's Material 3 1.5 green palette.
 */
@Composable
fun VoiceLiveScreen(
    onBack: () -> Unit,
    userName: String = "",
    modifier: Modifier = Modifier,
) {
    val isDark = DuaGenAITheme.isDark
    val palette = teamilyPalette()
    val look = LocalBotAppearance.current

    var isMicMuted by remember { mutableStateOf(false) }
    var isCameraOn by remember { mutableStateOf(false) }
    var isSpeakerMuted by remember { mutableStateOf(false) }
    var isCaptionsOn by remember { mutableStateOf(false) }

    val glowAlpha = if (isDark) 0.28f else 0.52f
    val backgroundBrush = Brush.verticalGradient(
        0.0f to MaterialTheme.colorScheme.surface,
        0.48f to MaterialTheme.colorScheme.surface,
        0.76f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = glowAlpha * 0.45f),
        1.0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = glowAlpha),
    )

    val infiniteTransition = rememberInfiniteTransition(label = "VoiceLivePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseScale",
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "AuraAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
    ) {
        // Main vertical content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Minimize handle (=) matching Gemini Live layout
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Column(
                        modifier = Modifier.size(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.onSurface),
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.onSurface),
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { isCaptionsOn = !isCaptionsOn },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (isCaptionsOn) {
                                Icons.Outlined.ClosedCaption
                            } else {
                                Icons.Outlined.ClosedCaptionDisabled
                            },
                            contentDescription = stringResource(R.string.voice_live_captions_cd),
                            tint = if (isCaptionsOn) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }

                    IconButton(
                        onClick = { isSpeakerMuted = !isSpeakerMuted },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (isSpeakerMuted) {
                                Icons.AutoMirrored.Outlined.VolumeOff
                            } else {
                                Icons.AutoMirrored.Outlined.VolumeUp
                            },
                            contentDescription = stringResource(R.string.voice_live_speaker_cd),
                            tint = if (isSpeakerMuted) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }

                    IconButton(
                        onClick = { /* More options */ },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = stringResource(R.string.voice_live_more_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Callout Speech Bubble: "Get Live Translations"
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (isDark) 2.dp else 6.dp,
                border = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.voice_live_callout_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = TeamilySansFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.voice_live_callout_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = TeamilySansFontFamily,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Center Visualizer & Headline
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Pulsing Center Star / Mark
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp),
                ) {
                    // Soft glowing aura behind the star
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .alpha(auraAlpha)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )

                    if (look == BotLook.DEFAULT) {
                        // Iconic Gemini Live 4-point concave sparkle
                        GeminiLiveSparkle(
                            modifier = Modifier.size(44.dp),
                        )
                    } else {
                        BabyGrokMark(
                            shape = look.shape,
                            color = look.color,
                            size = 52.dp,
                            contentDescription = null,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Headline ("Hi Fareed, what's the plan?" or default greeting)
                val headline = if (userName.isNotBlank()) {
                    stringResource(R.string.voice_live_greeting_name, userName)
                } else {
                    stringResource(R.string.voice_live_greeting)
                }

                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = TeamilySansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Center,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                // Status indication
                val statusText = if (isMicMuted) {
                    stringResource(R.string.voice_live_paused)
                } else {
                    stringResource(R.string.voice_live_listening)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = TeamilySansFontFamily,
                    ),
                    color = if (isMicMuted) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            // Bottom Control Dock (5 floating pill controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Add context button
                VoiceLiveCircleButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.voice_live_add_cd),
                    onClick = { /* Add context action */ },
                    isDark = isDark,
                )

                // 2. Video camera toggle button
                VoiceLiveCircleButton(
                    icon = if (isCameraOn) Icons.Outlined.Videocam else Icons.Outlined.VideocamOff,
                    contentDescription = stringResource(R.string.voice_live_camera_cd),
                    onClick = { isCameraOn = !isCameraOn },
                    isDark = isDark,
                    tint = if (isCameraOn) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                // 3. Center Speaking Visualizer capsule
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = if (isDark) 2.dp else 8.dp,
                    border = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                    modifier = Modifier
                        .width(116.dp)
                        .height(54.dp)
                        .shadow(
                            elevation = if (isDark) 2.dp else 6.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = palette.restingShadowAmbient,
                            spotColor = palette.restingShadowSpot,
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f * pulseScale),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Subtle wave bars with breathing motion
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(12.dp * pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp * pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(15.dp * pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                }

                // 4. Mic mute toggle button
                VoiceLiveCircleButton(
                    icon = if (isMicMuted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                    contentDescription = stringResource(R.string.voice_live_mic_cd),
                    onClick = { isMicMuted = !isMicMuted },
                    isDark = isDark,
                    tint = if (isMicMuted) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                // 5. Close / End button
                VoiceLiveCircleButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.voice_live_end_cd),
                    onClick = onBack,
                    isDark = isDark,
                )
            }
        }
    }
}

/**
 * 4-point concave star sparkle matching the Gemini Live avatar in 12241.jpg,
 * rendered with MaterialTheme semantic color tokens (zero hardcoded colors).
 */
@Composable
private fun GeminiLiveSparkle(
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = Path().apply {
            moveTo(cx, 0f)
            quadraticTo(cx, cy, w, cy)
            quadraticTo(cx, cy, cx, h)
            quadraticTo(cx, cy, 0f, cy)
            quadraticTo(cx, cy, cx, 0f)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.sweepGradient(
                listOf(
                    primary,
                    tertiary,
                    secondary,
                    primaryContainer,
                    primary,
                ),
                center = Offset(cx, cy),
            ),
        )
    }
}

@Composable
private fun VoiceLiveCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val palette = teamilyPalette()
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isDark) 2.dp else 6.dp,
        border = if (isDark) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = modifier
            .size(52.dp)
            .shadow(
                elevation = if (isDark) 2.dp else 4.dp,
                shape = CircleShape,
                ambientColor = palette.restingShadowAmbient,
                spotColor = palette.restingShadowSpot,
            )
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview(name = "Voice Live — Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceLiveLightPreview() = DuaGenAITheme(darkTheme = false) {
    VoiceLiveScreen(onBack = {}, userName = "Fareed")
}

@Preview(name = "Voice Live — Dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun VoiceLiveDarkPreview() = DuaGenAITheme(darkTheme = true) {
    VoiceLiveScreen(onBack = {}, userName = "Fareed")
}
