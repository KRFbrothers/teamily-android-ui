package ai.duagenai.design.cube

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/* ============================================================================
 *  CubeStore — the user's SAVED avatar, persisted.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  `DuaGenAINavHost` hoists one `CubeStore`, collects `store.selected` and writes
 *  it from the picker's Save — but the class itself was never shipped, so the
 *  nav host could not compile. This is it.
 *
 *  WHAT IT IS NOT
 *  --------------
 *  It is NOT a conversation's avatar. A thread's cube comes from its stable id
 *  (`CubeCatalog.forEntity`). This store holds the USER's own avatar — the one
 *  Home's top bar, the drawer footer and Profile draw. Keeping the two apart is
 *  what lets someone change their avatar without every conversation changing
 *  its face.
 *
 *  WHY DATASTORE
 *  -------------
 *  The same store the rest of the settings use (see `SettingsStore`): one
 *  file, no migration, no new library, and `selected` is a `Flow` so every
 *  screen that reads it recomposes on its own — no manual refresh.
 * ==========================================================================*/

/** One DataStore file for the avatar. `preferencesDataStore` must be top-level. */
private val Context.cubeDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "dua_cube")

/**
 * Reads and writes the user's saved cube.
 *
 * @param context an application context. DataStore must not be constructed
 *   twice for the same file, which is why the nav host creates this once at the
 *   top of the graph and passes it down.
 */
class CubeStore(private val context: Context) {

    private val cubeKey = stringPreferencesKey("selected_cube")

    /**
     * The saved cube id.
     *
     * Emits [CubeIds.DEFAULT] before anything is saved, and normalises a stored
     * value that no longer exists in the catalogue — so a screen can always draw
     * an avatar.
     */
    val selected: Flow<String> = context.cubeDataStore.data.map { prefs ->
        CubeIds.normalize(prefs[cubeKey])
    }

    /**
     * Persists [cubeId].
     *
     * An unknown id is normalised rather than rejected: the picker can only
     * hand over catalogue ids, so an unknown one means corrupt input, and
     * refusing to save would leave the user's tap silently lost.
     */
    suspend fun save(cubeId: String) {
        val normalized = CubeIds.normalize(cubeId)
        context.cubeDataStore.edit { prefs -> prefs[cubeKey] = normalized }
    }
}
