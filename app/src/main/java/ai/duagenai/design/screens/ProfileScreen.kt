package ai.duagenai.design.screens

import ai.duagenai.design.R
import ai.duagenai.design.theme.DemoBadge
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.duagenai.design.cube.CubeAvatar
import ai.duagenai.design.cube.CubeIds
import ai.duagenai.design.theme.LocalDuaGenAISpacing
import ai.duagenai.design.theme.DuaGenAITextStyles
import ai.duagenai.design.theme.DuaGenAITheme
import java.io.File

/* ============================================================================
 *  Profile — identity only.
 *
 *  Settings is the landing page. This screen holds the photo, the five
 *  identity rows (Name, About, Username, Phone, Links) and the existing
 *  plan pill + stat tiles underneath. Appearance / Storage / Notifications
 *  and Sign in live on Settings, once.
 *
 *  The avatar is the locally saved photo when one exists, otherwise the
 *  saved cube. The green camera FAB opens the Profile picture sheet.
 * ==========================================================================*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    cubeId: String = CubeIds.DEFAULT,
    photoPath: String? = null,
    displayName: String = "",
    about: String = "",
    username: String = "",
    phone: String = "",
    links: String = "",
    onChangeCube: () -> Unit = {},
    onBack: () -> Unit = {},
    onPhotoChosen: (Bitmap) -> Unit = {},
    onClearPhoto: () -> Unit = {},
    onSaveName: (String) -> Unit = {},
    onSaveAbout: (String) -> Unit = {},
    onSaveUsername: (String) -> Unit = {},
    onSavePhone: (String) -> Unit = {},
    onSaveLinks: (String) -> Unit = {},
    /** Drives the plan pill only (FREE vs PRO). Sign in lives on Settings. */
    signedIn: Boolean = false,
    chatCount: Int = 128,
    agentCount: Int = 4,
    artifactCount: Int = 37,
    onOpenChats: () -> Unit = {},
    onOpenAgents: () -> Unit = {},
    onOpenArtifacts: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    val context = LocalContext.current
    var showPictureSheet by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            onPhotoChosen(bitmap)
            showPictureSheet = false
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) photoLauncher.launch(null)
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
        if (bitmap != null) {
            onPhotoChosen(bitmap)
            showPictureSheet = false
        }
    }

    fun launchCamera() {
        val granted = context.checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) photoLauncher.launch(null) else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val notSet = stringResource(R.string.profile_not_set)
    val reserveUsername = stringResource(R.string.profile_reserve_username)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
                bottom = spacing.xxl,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .padding(top = spacing.md)
                        .size(148.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ProfileOrCubeAvatar(
                        cubeId = cubeId,
                        photoPath = photoPath,
                        size = 120.dp,
                        contentDescription = if (photoPath != null) {
                            stringResource(R.string.profile_photo_cd)
                        } else {
                            "Your avatar, ${CubeIds.label(cubeId)}"
                        },
                    )
                    SmallFloatingActionButton(
                        onClick = { showPictureSheet = true },
                        modifier = Modifier.align(Alignment.BottomEnd),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = stringResource(R.string.profile_change_picture),
                        )
                    }
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DuaGenAITheme.shapes.card,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column {
                        val nameValue = displayName.ifBlank { notSet }
                        SettingsRow(
                            title = stringResource(R.string.profile_field_name),
                            subtitle = nameValue,
                            icon = Icons.Outlined.Person,
                            onClick = { editing = "name" },
                        )
                        HorizontalDivider(color = colors.hairline)
                        SettingsRow(
                            title = stringResource(R.string.profile_field_about),
                            subtitle = about.ifBlank { notSet },
                            icon = Icons.Outlined.Info,
                            onClick = { editing = "about" },
                        )
                        HorizontalDivider(color = colors.hairline)
                        val usernameEmpty = username.isBlank()
                        SettingsRow(
                            title = stringResource(R.string.profile_field_username),
                            subtitle = if (usernameEmpty) reserveUsername else username,
                            icon = Icons.Outlined.AlternateEmail,
                            onClick = { editing = "username" },
                            subtitleColor = if (usernameEmpty) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Unspecified
                            },
                        )
                        HorizontalDivider(color = colors.hairline)
                        SettingsRow(
                            title = stringResource(R.string.profile_field_phone),
                            subtitle = phone.ifBlank { notSet },
                            icon = Icons.Outlined.Phone,
                            onClick = { editing = "phone" },
                        )
                        HorizontalDivider(color = colors.hairline)
                        SettingsRow(
                            title = stringResource(R.string.profile_field_links),
                            subtitle = links.ifBlank { notSet },
                            icon = Icons.Outlined.Link,
                            onClick = { editing = "links" },
                        )
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(DuaGenAITheme.shapes.pill)
                            .background(colors.logoMint)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = if (signedIn) "DUA GEN AI PRO" else "DUA GEN AI FREE",
                            style = DuaGenAITextStyles.eyebrow,
                            color = colors.onLogoMint,
                        )
                    }
                    Spacer(Modifier.width(spacing.xs))
                    DemoBadge()
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    StatTile(chatCount.toString(), "CHATS", Modifier.weight(1f), onOpenChats)
                    StatTile(agentCount.toString(), "AGENTS", Modifier.weight(1f), onOpenAgents)
                    StatTile(artifactCount.toString(), "ARTIFACTS", Modifier.weight(1f), onOpenArtifacts)
                }
            }
        }
    }

    if (showPictureSheet) {
        ProfilePictureSheet(
            cubeId = cubeId,
            hasPhoto = photoPath != null,
            onDismiss = { showPictureSheet = false },
            onCamera = { launchCamera() },
            onGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onChooseCube = {
                showPictureSheet = false
                onChangeCube()
            },
            onDelete = {
                onClearPhoto()
                showPictureSheet = false
            },
        )
    }

    val field = editing
    if (field != null) {
        val (title, value, type, save) = when (field) {
            "about" -> FieldEdit(
                stringResource(R.string.profile_field_about), about,
                KeyboardType.Text, onSaveAbout,
            )
            "username" -> FieldEdit(
                stringResource(R.string.profile_field_username), username,
                KeyboardType.Text, onSaveUsername,
            )
            "phone" -> FieldEdit(
                stringResource(R.string.profile_field_phone), phone,
                KeyboardType.Phone, onSavePhone,
            )
            "links" -> FieldEdit(
                stringResource(R.string.profile_field_links), links,
                KeyboardType.Uri, onSaveLinks,
            )
            else -> FieldEdit(
                stringResource(R.string.profile_field_name), displayName,
                KeyboardType.Text, onSaveName,
            )
        }
        ProfileFieldDialog(
            title = title,
            initial = value,
            keyboardType = type,
            onDismiss = { editing = null },
            onSave = { next ->
                save(next)
                editing = null
            },
        )
    }
}

private data class FieldEdit(
    val title: String,
    val value: String,
    val keyboardType: KeyboardType,
    val save: (String) -> Unit,
)

@Composable
private fun ProfileFieldDialog(
    title: String,
    initial: String,
    keyboardType: KeyboardType,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfilePictureSheet(
    cubeId: String,
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onChooseCube: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = LocalDuaGenAISpacing.current
    val colors = DuaGenAITheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.xxl),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.profile_picture_close),
                    )
                }
                Text(
                    text = stringResource(R.string.profile_picture_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = onDelete, enabled = hasPhoto) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.profile_picture_delete),
                    )
                }
            }
            SettingsRow(
                title = stringResource(R.string.profile_picture_camera),
                subtitle = " ",
                icon = Icons.Outlined.PhotoCamera,
                onClick = onCamera,
            )
            HorizontalDivider(color = colors.hairline)
            SettingsRow(
                title = stringResource(R.string.profile_picture_gallery),
                subtitle = " ",
                icon = Icons.Outlined.Image,
                onClick = onGallery,
            )
            HorizontalDivider(color = colors.hairline)
            SettingsRow(
                title = stringResource(R.string.profile_picture_choose_cube),
                subtitle = "Avatar · ${CubeIds.label(cubeId)}",
                icon = null,
                onClick = onChooseCube,
                leading = {
                    CubeAvatar(
                        cubeId = cubeId,
                        size = 32.dp,
                        shape = CircleShape,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

/**
 * The Profile / Settings avatar: the locally saved photo when one exists,
 * otherwise the saved cube, always clipped to a circle.
 */
@Composable
internal fun ProfileOrCubeAvatar(
    cubeId: String,
    photoPath: String?,
    size: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val photo = remember(photoPath) {
        photoPath?.takeIf { File(it).isFile }?.let { path ->
            BitmapFactory.decodeFile(path)
        }
    }
    if (photo != null) {
        Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        CubeAvatar(
            cubeId = cubeId,
            size = size,
            shape = CircleShape,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

@Composable
private fun StatTile(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val spacing = LocalDuaGenAISpacing.current
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        shape = DuaGenAITheme.shapes.statTile,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = number,
                style = DuaGenAITextStyles.statNumber,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = DuaGenAITextStyles.kicker,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    onClick: (() -> Unit)? = null,
    /** Optional leading slot, drawn before the icon. Used by "Choose cube" to
     *  show the current cube as a thumbnail. */
    leading: (@Composable () -> Unit)? = null,
    /**
     * Whether the row can be tapped.
     *
     * A disabled row is not clickable AND publishes `disabled()` semantics, so a
     * screen reader announces the state instead of the user discovering it by
     * tapping a dead control. Its chevron is hidden too: a row that cannot go
     * anywhere must not advertise that it can.
     */
    enabled: Boolean = true,
    /** Overrides the title colour. [Color.Unspecified] keeps the enabled/disabled ink. */
    titleColor: Color = Color.Unspecified,
    /** Overrides the subtitle colour. [Color.Unspecified] keeps onSurfaceVariant. */
    subtitleColor: Color = Color.Unspecified,
) {
    val spacing = LocalDuaGenAISpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .sizeIn(minHeight = 48.dp)
            .padding(horizontal = spacing.md, vertical = spacing.md)
            .then(
                if (enabled) {
                    Modifier
                } else {
                    Modifier.semantics(mergeDescendants = true) { disabled() }
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(spacing.md))
        }
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = DuaGenAITextStyles.rowTitle,
                color = when {
                    titleColor != Color.Unspecified -> titleColor
                    enabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = DuaGenAITextStyles.snippet,
                    color = if (subtitleColor != Color.Unspecified) {
                        subtitleColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        if (enabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Profile — light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileLight() = DuaGenAITheme(darkTheme = false) { ProfileScreen() }

@Preview(name = "Profile — dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileDark() = DuaGenAITheme(darkTheme = true) { ProfileScreen() }
