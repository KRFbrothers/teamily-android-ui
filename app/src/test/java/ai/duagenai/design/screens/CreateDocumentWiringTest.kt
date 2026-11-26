package ai.duagenai.design.screens

import ai.duagenai.design.theme.DuaGenAITheme
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CreateDocumentWiringTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createDocumentSavesADocumentCardAndVersionOne() {
        val repository = ArtifactsRepository(
            dataSource = InMemoryArtifactsDataSource(loadDelayMs = 0),
        )

        val result = runBlocking { repository.createDocument("mera school project") }

        assertTrue(result is WriteResult.Success)
        val loaded = repository.state.value as ArtifactsUiState.Loaded
        val card = loaded.artifacts.single()
        assertEquals("Document", card.type)
        assertEquals("mera school project", card.title)
        val versions = runBlocking { repository.fetchVersions(card.id) }
        assertTrue(versions.single().content.contains("mera school project"))
    }

    @Test
    fun createDocumentRejectsAnEmptyTopic() {
        val repository = ArtifactsRepository(
            dataSource = InMemoryArtifactsDataSource(loadDelayMs = 0),
        )

        val result = runBlocking { repository.createDocument("   ") }

        assertEquals("Topic likho.", (result as WriteResult.Failure).message)
        assertTrue(repository.state.value is ArtifactsUiState.Loading)
    }

    @Test
    fun createDocumentRemovesTheCardWhenThePageIsNotWritten() {
        val source = BlankReviseSource()
        val repository = ArtifactsRepository(dataSource = source)

        val result = runBlocking { repository.createDocument("ek page") }

        assertEquals("Document likha nahi gaya.", (result as WriteResult.Failure).message)
        assertTrue(source.items.isEmpty())
    }

    @Test
    fun documentScreenBanaoSendsTheTopic() {
        var sent: String? = null
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                CreateKindScreen(
                    kind = "document",
                    onBack = {},
                    signedIn = true,
                    onBanao = { topic ->
                        sent = topic
                        WriteResult.Success
                    },
                )
            }
        }

        compose.onNodeWithText("Banao").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Document likho prompt").performTextInput("school project")
        compose.onNodeWithText("Banao").assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals("school project", sent)
    }

    @Test
    fun documentScreenAsksToSignInWhenSignedOut() {
        compose.setContent {
            DuaGenAITheme(darkTheme = false) {
                CreateKindScreen(
                    kind = "document",
                    onBack = {},
                    signedIn = false,
                    onBanao = { WriteResult.Success },
                )
            }
        }

        compose.onNodeWithText("Sign in").assertIsEnabled()
        assertEquals(0, compose.onAllNodesWithText("Banao").fetchSemanticsNodes().size)
    }
}

private class BlankReviseSource : ArtifactsDataSource {
    val items = mutableListOf<ArtifactItem>()

    override suspend fun fetch(): List<ArtifactItem> = items.toList()

    override suspend fun add(item: ArtifactItem): ArtifactItem {
        items.add(0, item)
        return item
    }

    override suspend fun delete(id: String) {
        items.removeAll { it.id == id }
    }

    override suspend fun reviseArtifact(
        artifactId: String,
        instruction: String,
    ): Pair<ArtifactItem, ArtifactVersionItem>? = null
}
