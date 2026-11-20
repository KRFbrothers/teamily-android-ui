package ai.duagenai.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ===========================================================================
//  DuagenAI — Shape.kt
//  ---------------------------------------------------------------------------
//  Do hisse:
//    1. `DuagenAIShapes` — M3 `Shapes` object (extraSmall ... extraLarge).
//       MaterialTheme.shapes me wire hota hai, isliye saare M3 components
//       (Card, Button, TextField, BottomSheet, Dialog) apne aap ye use karte hain.
//    2. `DuagenAIShapeTokens` — DuagenAI ke custom shapes jo M3 ke 5 slots me
//       fit nahi hote (chat bubble tail, sheet ke sirf top corners, pill chip).
//       Inhe seedha `shape = DuagenAIShapeTokens.chatBubbleMe` ki tarah use karo.
//
//  NOTE: M3 ke 5 slots har component ke liye kaafi nahi hote — chat bubble ka
//  ek corner chhota hota hai (tail), aur bottom sheet ke sirf top corners
//  round hote hain. Isliye custom tokens alag rakhe hain, M3 slots ko
//  override karne ke bajaye.
// ===========================================================================


/**
 * M3 shape scale — MaterialTheme.shapes me jaata hai.
 *
 * Radius values DuagenAI ke design reference se:
 *   extraSmall  6dp   — badge, chhota chip, tag
 *   small      10dp   — input field, list row highlight
 *   medium     14dp   — card, dialog, dropdown
 *   large      20dp   — bottom sheet, bada panel
 *   extraLarge 28dp   — full-screen sheet, hero card
 */
val DuagenAIShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)


/**
 * DuagenAI ke custom shape tokens — M3 slots se bahar wale cases.
 *
 * Ye `MaterialTheme.shapes` ka hissa nahi hain (M3 me unki jagah nahi hai),
 * isliye inhe directly reference karo:
 *
 *     Surface(shape = DuagenAIShapeTokens.chatBubbleMe) { ... }
 */
object DuagenAIShapeTokens {

    // --- Chat bubbles ------------------------------------------------------
    // Tail = wo corner jo sender ki taraf chhota hota hai (6dp), baaki 18dp.
    // User ka bubble right side pe hai -> bottomEnd chhota.
    val chatBubbleMe = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomEnd = 6.dp,
        bottomStart = 18.dp,
    )

    // Agent ka bubble left side pe hai -> bottomStart chhota.
    val chatBubbleAgent = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomEnd = 18.dp,
        bottomStart = 6.dp,
    )

    // --- Cards / panels ----------------------------------------------------
    /** Chat list row ka card, agent card, artifact card. */
    val card = RoundedCornerShape(16.dp)

    /** Composer ka rounded field + send button ke aas-paas ka container. */
    val composer = RoundedCornerShape(24.dp)

    /** FAB — M3 default large FAB 16dp hai, DuagenAI usi pe hai. */
    val fab = RoundedCornerShape(16.dp)

    /** Avatar — squircle, circle nahi (design reference me 12dp). */
    val avatar = RoundedCornerShape(12.dp)

    /** Chhota avatar (top bar, list row) — 10dp. */
    val avatarSmall = RoundedCornerShape(10.dp)

    // --- Sheets ------------------------------------------------------------
    /** Bottom sheet — sirf top corners round, bottom flat (screen edge pe). */
    val sheetTop = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )

    /** Modal bottom sheet ka drag handle. */
    val sheetHandle = RoundedCornerShape(percent = 50)

    // --- Pills / chips -----------------------------------------------------
    /** FilterChip, AssistChip, Badge — poora pill. */
    val pill = RoundedCornerShape(percent = 50)

    /** SegmentedButton ka group container. */
    val segmented = RoundedCornerShape(percent = 50)

    // --- Media -------------------------------------------------------------
    /** Artifact / image thumbnail — 12dp. */
    val thumbnail = RoundedCornerShape(12.dp)

    /** Image ka full-bleed preview (lightbox) — 8dp. */
    val imagePreview = RoundedCornerShape(8.dp)
}
