package ai.duagenai.design.cube

/* ============================================================================
 *  CubeIds — the catalogue of the 42 cube avatars.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  The screens (and `CubePickerScreen` in particular) refer to `CubeIds.all`,
 *  `CubeIds.DEFAULT` and `CubeIds.label(id)`, but no file defined them, so the
 *  package could not compile. This is the missing catalogue.
 *
 *  The ids are the zero-padded names `cube_01` … `cube_42`. [all] is in that
 *  order, which is the order the picker grid shows.
 * ==========================================================================*/

object CubeIds {

    /** How many cube avatars the art set has. */
    const val COUNT: Int = 42

    /**
     * The avatar a fresh install shows before the user picks one.
     *
     * Deliberately NOT one of the cubes `CubeCatalog.forEntity` hands to a
     * conversation: if the default were also a chat's cube, "this thread shows
     * the user's avatar" would be indistinguishable from correct behaviour.
     */
    const val DEFAULT: String = "cube_01"

    /** Every cube id, in art (and picker) order: `cube_01` … `cube_42`. */
    val all: List<String> = (1..COUNT).map(::idOf)

    /** `7` -> `"cube_07"` — the zero-padded id for the 1-based index. */
    fun idOf(index: Int): String = "cube_" + index.toString().padStart(2, '0')

    /**
     * The id's 1-based index, or null when [cubeId] is not a catalogue id.
     *
     * Used by the picker's search (`"cube 07"` and `"07"` both match) and by
     * the accessibility label, so both read the same number.
     */
    fun indexOf(cubeId: String): Int? =
        cubeId.removePrefix("cube_").toIntOrNull()?.takeIf { it in 1..COUNT }

    /**
     * `"cube_07"` -> `"Cube 07"`.
     *
     * Zero-padded on purpose: the ids are two digits, and a label that dropped
     * the leading zero would make `cube_07` and `cube_7` look like the same
     * cube in a screen reader.
     */
    fun label(cubeId: String): String {
        val index = indexOf(cubeId) ?: return "Cube"
        return "Cube " + index.toString().padStart(2, '0')
    }

    /** True when [cubeId] is one of the shipped ids. */
    fun isKnown(cubeId: String): Boolean = indexOf(cubeId) != null

    /**
     * [cubeId] when it is a known id, else [DEFAULT].
     *
     * The one guard between a stored / deep-linked value and the drawing code:
     * a corrupt preference must not leave the avatar blank.
     */
    fun normalize(cubeId: String?): String = if (cubeId != null && isKnown(cubeId)) cubeId else DEFAULT
}
