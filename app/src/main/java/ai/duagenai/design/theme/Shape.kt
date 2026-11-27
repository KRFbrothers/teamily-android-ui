package ai.duagenai.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ===========================================================================
//  DuagenAI — Shape.kt
//  ---------------------------------------------------------------------------
//  Sirf woh shapes jo M3 ke 5 slots me fit nahi hote (chat bubble tail).
//
//  M3 ka `Shapes` object yahan nahi hai: `DuaGenAITheme` `MaterialTheme.shapes`
//  ko `TeamilyShapes` se override karta hai (DuaGenAITheme.kt:285), isliye ek
//  doosra `Shapes` object sirf confusion tha, koi component use nahi karta tha.
//  Wahi baat baaki custom tokens par bhi lagu thi (card, composer, fab, avatar,
//  sheetTop, pill, thumbnail, imagePreview): sab ke sab zero call sites the.
//  Jo do actually use hote hain, wahi yahan bache hain.
// ===========================================================================

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
}
