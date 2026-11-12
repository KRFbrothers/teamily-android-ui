package ai.duagenai.design.screens

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    companion object {
        /** The demo values the kit ships with, so the first frame matches. */
        const val DEFAULT_STORAGE_USED_GB = 3.2
        const val DEFAULT_STORAGE_TOTAL_GB = 15.0

        /** What "Clear cache" leaves behind — the demo's post-clear value. */
        const val CLEARED_STORAGE_USED_GB = 0.4
    }
}
