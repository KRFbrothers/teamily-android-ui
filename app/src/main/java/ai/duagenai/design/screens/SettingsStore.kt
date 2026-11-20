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
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ai.duagenai.design.bot.BotColor
import ai.duagenai.design.bot.BotShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

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
    private val botShapeKey = stringPreferencesKey("bot_shape")
    private val botColorKey = stringPreferencesKey("bot_color")
    private val storageUsedKey = doublePreferencesKey("storage_used_gb")
    private val storageTotalKey = doublePreferencesKey("storage_total_gb")
    private val photoPathKey = stringPreferencesKey("profile_photo_path")
    private val nameKey = stringPreferencesKey("profile_name")
    private val aboutKey = stringPreferencesKey("profile_about")
    private val usernameKey = stringPreferencesKey("profile_username")
    private val phoneKey = stringPreferencesKey("profile_phone")
    private val linksKey = stringPreferencesKey("profile_links")
    private val photoVersionKey = longPreferencesKey("profile_photo_version")
    private val mutedAgentsKey = stringSetPreferencesKey("agent_muted_ids")
    private val pinnedAgentsKey = stringSetPreferencesKey("agent_pinned_ids")

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

    /** The saved bot shape. Unknown / missing values fall back to Blob. */
    val botShape: Flow<BotShape> = context.settingsDataStore.data.map { prefs ->
        BotShape.fromKey(prefs[botShapeKey])
    }

    /** The saved bot color. Unknown / missing values fall back to Black. */
    val botColor: Flow<BotColor> = context.settingsDataStore.data.map { prefs ->
        BotColor.fromKey(prefs[botColorKey])
    }

    /** Persists the theme mode. */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs -> prefs[themeKey] = mode.name }
    }

    /** Persists the bot shape from the Appearance screen. */
    suspend fun setBotShape(shape: BotShape) {
        context.settingsDataStore.edit { prefs -> prefs[botShapeKey] = shape.key }
    }

    /** Persists the bot color from the Appearance screen. */
    suspend fun setBotColor(color: BotColor) {
        context.settingsDataStore.edit { prefs -> prefs[botColorKey] = color.name }
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

    private suspend fun setProfileField(key: Preferences.Key<String>, value: String) {
        context.settingsDataStore.edit { prefs ->
            val trimmed = value.trim()
            if (trimmed.isEmpty()) prefs.remove(key) else prefs[key] = trimmed
        }
    }

    /**
     * Writes [bitmap] under app files and remembers the path. Each save gets a
     * NEW file name (the save time), so anything keyed on the path reloads,
     * and the previous photo file is deleted. The bitmap is scaled down to at
     * most [PHOTO_MAX_SIDE_PX] on its longest side first.
     */
    suspend fun saveProfilePhoto(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val version = nextPhotoVersion()
        val dir = File(context.filesDir, PHOTO_DIR)
        dir.mkdirs()
        val file = File(dir, "avatar_$version.jpg")
        val scaled = scaleToMaxSide(bitmap, PHOTO_MAX_SIDE_PX)
        file.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        val path = file.absolutePath
        context.settingsDataStore.edit { prefs ->
            prefs[photoPathKey] = path
            prefs[photoVersionKey] = version
        }
        deleteOldPhotos(keep = file)
        path
    }

    /**
     * Decodes [uri] (the Photo Picker result) on the IO dispatcher, downsampled
     * to about [PHOTO_MAX_SIDE_PX], and stores it the same way as
     * [saveProfilePhoto]. Returns null if the image cannot be read.
     */
    suspend fun saveProfilePhotoFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = runCatching {
            decodeSampled(PHOTO_MAX_SIDE_PX) { context.contentResolver.openInputStream(uri) }
        }.getOrNull() ?: return@withContext null
        saveProfilePhoto(bitmap)
    }

    /** Deletes the saved photo file(s) and clears the stored path and version. */
    suspend fun clearProfilePhoto() = withContext(Dispatchers.IO) {
        deleteOldPhotos(keep = null)
        context.settingsDataStore.edit { prefs ->
            prefs.remove(photoPathKey)
            prefs[photoVersionKey] = nextPhotoVersion()
        }
    }

    /**
     * Bumped on every photo save or clear (the save time in ms; 0 when the
     * photo was never touched). The avatar loader is keyed on it.
     */
    val profilePhotoVersion: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[photoVersionKey] ?: 0L
    }

    private fun deleteOldPhotos(keep: File?) {
        File(context.filesDir, PHOTO_DIR).listFiles()?.forEach { f ->
            if (f.isFile && f != keep) f.delete()
        }
    }

    /** Agent ids whose notifications are muted (agent profile "Mute"). */
    val mutedAgents: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[mutedAgentsKey].orEmpty()
    }

    /** Agent ids pinned to top (agent profile "Pin"). */
    val pinnedAgents: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[pinnedAgentsKey].orEmpty()
    }

    suspend fun setAgentMuted(agentId: String, on: Boolean) = setInSet(mutedAgentsKey, agentId, on)
    suspend fun setAgentPinned(agentId: String, on: Boolean) = setInSet(pinnedAgentsKey, agentId, on)

    /** Drops every per-agent flag of [agentId] (the agent was deleted). */
    suspend fun forgetAgent(agentId: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[mutedAgentsKey] = prefs[mutedAgentsKey].orEmpty() - agentId
            prefs[pinnedAgentsKey] = prefs[pinnedAgentsKey].orEmpty() - agentId
        }
    }

    private suspend fun setInSet(key: Preferences.Key<Set<String>>, id: String, on: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val now = prefs[key].orEmpty()
            prefs[key] = if (on) now + id else now - id
        }
    }

    companion object {
        /** Folder under [Context.getFilesDir] holding the one profile photo. */
        const val PHOTO_DIR = "profile"

        /** Longest side, in px, a saved or displayed profile photo is decoded to. */
        const val PHOTO_MAX_SIDE_PX = 512

        /**
         * Decodes the stream [open] returns, downsampled with inSampleSize so
         * its longest side is about [maxSide] (never below it). Opens the
         * stream twice: once for bounds, once for pixels. Call off the main
         * thread. Null when the data is not an image.
         */
        fun decodeSampled(maxSide: Int, open: () -> InputStream?): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            // decodeStream returns null in bounds-only mode; only the stream matters.
            val stream = open() ?: return null
            stream.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSide)
            }
            return open()?.use { BitmapFactory.decodeStream(it, null, opts) }
        }

        /** [decodeSampled] for a file path; null if the file is missing. */
        fun decodeSampledFile(path: String, maxSide: Int = PHOTO_MAX_SIDE_PX): Bitmap? {
            val file = File(path)
            if (!file.isFile) return null
            return runCatching { decodeSampled(maxSide) { file.inputStream() } }.getOrNull()
        }

        private var lastPhotoVersion = 0L

        /** The save time in ms, strictly increasing even for two saves in one ms. */
        @Synchronized
        private fun nextPhotoVersion(): Long {
            lastPhotoVersion = maxOf(System.currentTimeMillis(), lastPhotoVersion + 1)
            return lastPhotoVersion
        }

        /** Largest power-of-two sample size that keeps the longest side >= [maxSide]. */
        fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
            var sample = 1
            val longest = maxOf(width, height)
            while (longest / (sample * 2) >= maxSide) sample *= 2
            return sample
        }

        private fun scaleToMaxSide(bitmap: Bitmap, maxSide: Int): Bitmap {
            val longest = maxOf(bitmap.width, bitmap.height)
            if (longest <= maxSide) return bitmap
            val ratio = maxSide.toFloat() / longest
            return Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
        }

        /** The demo values the kit ships with, so the first frame matches. */
        const val DEFAULT_STORAGE_USED_GB = 3.2
        const val DEFAULT_STORAGE_TOTAL_GB = 15.0

        /** What "Clear cache" leaves behind — the demo's post-clear value. */
        const val CLEARED_STORAGE_USED_GB = 0.4
    }
}
