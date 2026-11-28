package ai.duagenai.design.screens

import androidx.compose.runtime.saveable.SaverScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The MCP server list survives Activity recreation.
 *
 * `SkillConfigScreen` held `List<McpServerItem>` in a plain `rememberSaveable`.
 * `McpServerItem` is a data class, not `Parcelable`/`Serializable`, so the
 * saved-state registry could not put it in the `Bundle` and threw on rotation,
 * process-death restore and dark-mode toggle. [McpServerListSaver] flattens the
 * list to primitives instead.
 *
 * These tests pin both halves: the encoding is Bundle-safe, and a round trip
 * gives back exactly the servers that went in — a saver that stopped the crash
 * by dropping the user's added servers would be a silent regression.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class McpServerListSaverTest {

    private val scope = SaverScope { true }

    private val servers = listOf(
        McpServerItem("sqlite", "SQLite Explorer", "sqlite3 memory.db", "stdio", true, 4),
        McpServerItem("rea", "REA Agents", "npx rea-agents", "stdio", false, 12),
        McpServerItem("web-search", "WebMCP Hub", "http://localhost:3000/sse", "sse", true, 3),
    )

    /** The saver's save half, narrowed to the flat string list it really writes. */
    @Suppress("UNCHECKED_CAST")
    private fun save(value: List<McpServerItem>): List<String> =
        with(McpServerListSaver) { scope.save(value) } as List<String>

    @Test
    fun `saves only primitives so the bundle can hold it`() {
        val saved = save(servers)

        assertTrue(
            "every saved element must be a Bundle-safe primitive, got $saved",
            saved.all { it is String },
        )
        assertEquals("6 fields per server", servers.size * 6, saved.size)
    }

    @Test
    fun `round trip restores every field of every server`() {
        val restored = McpServerListSaver.restore(save(servers))

        assertNotNull("restore must produce a list", restored)
        assertEquals(servers, restored)
    }

    @Test
    fun `round trip keeps the enabled flag and tool count`() {
        val restored = McpServerListSaver.restore(save(servers))!!

        assertEquals(false, restored[1].enabled)
        assertEquals(12, restored[1].toolCount)
        assertEquals(true, restored[2].enabled)
        assertEquals(3, restored[2].toolCount)
    }

    @Test
    fun `an empty list round trips to an empty list`() {
        assertEquals(emptyList<McpServerItem>(), McpServerListSaver.restore(save(emptyList())))
    }

    @Test
    fun `a truncated payload drops the partial server instead of crashing`() {
        // A saved list cut short (older build, interrupted write) must not throw
        // on restore — the complete servers come back and the partial one goes.
        val truncated = save(servers).dropLast(3)

        val restored = McpServerListSaver.restore(truncated)

        assertEquals(2, restored?.size)
        assertEquals(servers[0], restored?.get(0))
        assertEquals(servers[1], restored?.get(1))
    }
}
