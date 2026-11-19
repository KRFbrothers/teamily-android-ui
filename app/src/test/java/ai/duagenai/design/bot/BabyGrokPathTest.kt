package ai.duagenai.design.bot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BabyGrokPathTest {

    @Test
    fun aPathIsABodyPlusTwoEyes() {
        val path = "M0 0L10 0L10 10Z M1 1L2 1L2 2Z M4 4L5 4L5 5Z"
        val subs = parseBabyGrokSubpaths(path)
        assertEquals(3, subs.size)
        assertEquals(6, subs[0].size)
        assertEquals(0f, subs[0][0])
        assertEquals(10f, subs[0][2])
        assertEquals(10f, subs[0][5])
    }

    @Test
    fun unknownKeysFallBackToTheGrokDefault() {
        assertEquals(BotShape.Blob, BotShape.fromKey(null))
        assertEquals(BotShape.Blob, BotShape.fromKey("nope"))
        assertEquals(BotShape.Star, BotShape.fromKey("star"))
        assertEquals(BotColor.Black, BotColor.fromKey(null))
        assertEquals(BotColor.Cyan, BotColor.fromKey("Cyan"))
    }

    @Test
    fun lightBodiesGetDarkEyes() {
        assertEquals(0xFF1A1A1A, eyeArgb(0xFFFFFFFF))
        assertEquals(0xFF1A1A1A, eyeArgb(BotColor.Yellow.light))
        assertEquals(0xFFFFFFFF, eyeArgb(BotColor.Black.light))
        assertEquals(0xFF1A1A1A, eyeArgb(BotColor.Black.dark))
    }

    @Test
    fun shippedGeometryParsesEveryShape() {
        val file = File("src/main/assets/babygrok-geometry.json")
        assertTrue("geometry asset missing at ${file.absolutePath}", file.isFile)
        val text = file.readText()
        for (shape in BotShape.values()) {
            val key = "\"${shape.key}\""
            val at = text.indexOf(key)
            assertTrue(shape.key, at >= 0)
            val pathAt = text.indexOf("\"path\"", at)
            val start = text.indexOf('"', pathAt + "\"path\"".length) + 1
            val end = text.indexOf('"', start)
            val subs = parseBabyGrokSubpaths(text.substring(start, end))
            assertEquals(shape.key, 3, subs.size)
            assertTrue(shape.key, subs[0].size >= 6)
            assertTrue(shape.key, subs[1].size >= 6)
            assertTrue(shape.key, subs[2].size >= 6)
        }
    }
}
