package ai.duagenai.design.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/* ============================================================================
 *  Dua Gen AI — Profile settings persistence
 *
 *  The three Profile rows (Appearance / Storage / Notifications) each need a
 *  value that survives a restart, so they live in DataStore (Preferences) —
 *  the SAME store and the SAME pattern `CubeStore` already uses. One file, a
 *  handful of keys, no migration, no new library.
 *
 *  Requires (app/build.gradle.kts):
 *      implementation("androidx.datastore:datastore-preferences:1.1.1")
 *
 *  Usage:
 *      val store = SettingsStore(context)
 *      val theme by store.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
 *      // on select:
 *      scope.launch { store.setThemeMode(mode) }
 * ==========================================================================*/

private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "dua_settings")

/**
 * Reads and writes the Profile settings.
 *
 * Every getter falls back to the product default when the key is missing or
 * corrupt, so the UI can always render a value — the same contract
 * `CubeStore.selected` has.
 */
class SettingsStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val storageUsedKey = doublePreferencesKey("storage_used_gb")
    private val storageTotalKey = doublePreferencesKey("storage_total_gb")
    private val photoPathKey = stringPreferencesKey("profile_photo_path")
    private val nameKey = stringPreferencesKey("profile_name")
    private val aboutKey = stringPreferencesKey("profile_about")
    private val usernameKey = stringPreferencesKey("profile_username")
    private val phoneKey = stringPreferencesKey("profile_phone")
    private val linksKey = stringPreferencesKey("profile_links")

    /** The saved theme mode. Unknown / missing values fall back to System. */
    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[themeKey]
        ThemeMode.values().firstOrNull { it.name == raw } ?: ThemeMode.System
    }

    /** Used storage in GB. Defaults to the demo value the kit ships with. */
    val storageUsedGb: Flow<Double> = context.settingsDataStore.data.map { prefs ->
        prefs[storageUsedKey] ?: DEFAULT_STORAGE_USED_GB
    }

    /** Total storage in GB. */
    val storageTotalGb: Flow<Double> = context.settingsDataStore.data.map { prefs ->
        prefs[storageTotalKey] ?: DEFAULT_STORAGE_TOTAL_GB
    }

    /**
     * The notification switches, as a map keyed by [NotificationSetting].
     *
     * A missing key falls back to [defaultNotificationState], so a fresh install
     * reads the same defaults the screen's own parameter defaults to.
     */
    val notifications: Flow<Map<NotificationSetting, Boolean>> =
        context.settingsDataStore.data.map { prefs ->
            NotificationSetting.values().associateWith { setting ->
                prefs[booleanPreferencesKey("notif_${setting.id}")]
                    ?: (defaultNotificationState[setting] == true)
            }
        }

    /** Persists the theme mode. */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs -> prefs[themeKey] = mode.name }
    }

    /** Persists the used-storage value (called after "Clear cache"). */
    suspend fun setStorageUsedGb(value: Double) {
        context.settingsDataStore.edit { prefs -> prefs[storageUsedKey] = value }
    }

    /** Persists one notification switch. */
    suspend fun setNotification(setting: NotificationSetting, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[booleanPreferencesKey("notif_${setting.id}")] = enabled
        }
    }

    /**
     * Absolute path of the locally saved profile photo, or null when the user
     * has not chosen one (or has deleted it). The file itself lives under
     * [Context.getFilesDir].
     */
    val profilePhotoPath: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[photoPathKey]?.takeIf { it.isNotBlank() && File(it).isFile }
    }

    /** User-entered profile name. Empty when it has never been set. */
    val profileName: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[nameKey].orEmpty()
    }

    val profileAbout: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[aboutKey].orEmpty()
    }

    val profileUsername: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[usernameKey].orEmpty()
    }

    val profilePhone: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[phoneKey].orEmpty()
    }

    val profileLinks: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[linksKey].orEmpty()
    }

    suspend fun setProfileName(value: String) = setProfileField(nameKey, value)
    suspend fun setProfileAbout(value: String) = setProfileField(aboutKey, value)
    suspend fun setProfileUsername(value: String) = setProfileField(usernameKey, value)
    suspend fun setProfilePhone(value: String) = setProfileField(phoneKey, value)
    suspend fun setProfileLinks(value: String) = setProfileField(linksKey, value)

    private suspend fun setProfileField(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        context.settingsDataStore.edit { prefs ->
            val trimmed = value.trim()
            if (trimmed.isEmpty()) prefs.remove(key) else prefs[key] = trimmed
        }
    }

    /**
     * Writes [bitmap] under app files and remembers the path. Replaces any
     * previous photo file.
     */
    suspend fun saveProfilePhoto(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val file = photoFile()
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val path = file.absolutePath
        context.settingsDataStore.edit { prefs -> prefs[photoPathKey] = path }
        path
    }

    /**
     * Decodes [uri] (the Photo Picker result) and stores it the same way as
     * [saveProfilePhoto]. Returns null if the image cannot be read.
     */
    suspend fun saveProfilePhotoFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return@withContext null
        saveProfilePhoto(bitmap)
    }

    /** Deletes the saved photo file and clears the stored path. */
    suspend fun clearProfilePhoto() = withContext(Dispatchers.IO) {
        photoFile().delete()
        context.settingsDataStore.edit { prefs -> prefs.remove(photoPathKey) }
    }

    private fun photoFile(): File = File(context.filesDir, PHOTO_RELATIVE_PATH)

    companion object {
        /** Relative path under [Context.getFilesDir] for the profile photo. */
        const val PHOTO_RELATIVE_PATH = "profile/avatar.jpg"

        /** The demo values the kit ships with, so the first frame matches. */
        const val DEFAULT_STORAGE_USED_GB = 3.2
        const val DEFAULT_STORAGE_TOTAL_GB = 15.0

        /** What "Clear cache" leaves behind — the demo's post-clear value. */
        const val CLEARED_STORAGE_USED_GB = 0.4
    }
}
