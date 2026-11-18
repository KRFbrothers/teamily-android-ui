package ai.duagenai.design.cube

/* ============================================================================
 *  CubeCatalog — which cube belongs to which entity.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  Every screen that draws a conversation's avatar asked
 *  `CubeCatalog.forEntity(chatId)`, but the file defining it was never shipped.
 *  Without it, a chat row and the thread it opens could each have invented their
 *  own avatar — and they would drift the first time either changed.
 *
 *  THE CONTRACT
 *  ------------
 *  The cube is a PURE FUNCTION OF THE STABLE ID. Not of the row's position, not
 *  of the title, not of the user's saved avatar. That is what makes a rename or
 *  a re-order unable to change a conversation's face, and it is why Profile's
 *  picker (which writes the USER's cube) never touches a thread.
 *
 *  TWO LAYERS, ON PURPOSE
 *  ----------------------
 *   1. [CURATED] — the product's own assignments for the entities that ship
 *      with the kit. These are a design decision (the approved mockups pair a
 *      conversation with a specific cube), so they are written down rather than
 *      inferred from a hash that could re-shuffle them.
 *   2. [hashCube] — the fallback for any id the product has not curated (a new
 *      group, a deep link, an entity created at runtime). Deterministic, so the
 *      same id always gets the same cube, on every device and every launch.
 * ==========================================================================*/

object CubeCatalog {

    /**
     * The curated id -> cube assignments.
     *
     * These pairings came from the original mockups and are kept explicit so
     * the hash below can change without silently re-facing a shipped
     * conversation. The ids no longer correspond to listed sample chats — the
     * app ships none — but the curated cube for an id must never change once
     * a real conversation carries it.
     */
    private val CURATED: Map<String, String> = mapOf(
        "chat_webpage_developer" to "cube_34",
        "chat_design_team" to "cube_08",
        "chat_slides_assistant" to "cube_11",
        "chat_david_park" to "cube_35",
        "chat_market_research" to "cube_06",
        "chat_accounting_assistant" to "cube_32",
        "chat_family" to "cube_12",
        // Agent's OWN thread is a conversation like any other, so it gets a
        // cube the same way — from its stable id, not from the saved avatar.
        "chat_agent" to "cube_42",
    )

    /**
     * The cube for a stable entity id.
     *
     * Curated ids get their designed cube; anything else gets a deterministic
     * hash. The result is always a catalogue id, so the caller never has to
     * guard it.
     */
    fun forEntity(entityId: String): String =
        CURATED[entityId] ?: hashCube(entityId)

    /**
     * A deterministic cube for an uncurated id.
     *
     * `abs(hashCode) % COUNT` is spread evenly enough for an avatar and is
     * stable across processes — which is the whole requirement. `abs` is taken
     * on the Long so `Int.MIN_VALUE` cannot stay negative.
     */
    fun hashCube(entityId: String): String {
        val slot = (kotlin.math.abs(entityId.hashCode().toLong()) % CubeIds.COUNT).toInt()
        return CubeIds.idOf(slot + 1)
    }
}
