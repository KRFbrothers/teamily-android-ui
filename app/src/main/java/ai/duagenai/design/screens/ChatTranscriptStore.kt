package ai.duagenai.design.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/* ============================================================================
 *  ChatTranscriptStore — where a thread's messages LIVE.
 *
 *  WHY THIS FILE EXISTS (audit blocker: "chat transcript in rememberSaveable")
 *  ------------------------------------------------------------------------
 *  `ChatScreen` used to hold the whole message list in `rememberSaveable`. That
 *  puts EVERY message of EVERY visited thread into the Activity's saved-instance
 *  Bundle. A Bundle is a Binder transaction with a ~1 MB budget: a long
 *  conversation — or simply a few threads' worth — overflows it and the process
 *  dies with `TransactionTooLargeException` while backgrounding, which the user
 *  sees as "the app crashes when I switch away".
 *
 *  A transcript is APPLICATION STATE, not UI state. It belongs in something that
 *  is RETAINED across a configuration change and is never serialised into the
 *  Bundle. A [ViewModel] is exactly that: it survives rotation and theme
 *  changes, it is cleared when the host really goes away, and nothing in it is
 *  written to the Bundle.
 *
 *  WHAT STAYS IN rememberSaveable
 *  ------------------------------
 *  Only the DRAFT — one short string per thread. That is the state a user would
 *  actually be upset to lose on rotation, and it is small enough that the Bundle
 *  budget is a non-issue. The typing indicator is transient, so it is plain
 *  `remember`: it must not come back after a rotation, because the coroutine
 *  that would clear it died with the old composition.
 *
 *  THREAD SAFETY
 *  -------------
 *  Every mutation goes through a `MutableStateFlow` update, so two threads
 *  (Agent's and a chat's) appending at once cannot lose a message.
 * ==========================================================================*/

/**
 * The message store for every open thread, keyed by chat id.
 *
 * Created once by the nav host (`viewModel()`) and passed down, so all threads
 * share one retained instance — the same shape as the repositories.
 */
class ChatTranscriptStore : ViewModel() {

    private val _threads = MutableStateFlow<Map<String, List<SavedMessage>>>(emptyMap())

    /** Every thread's transcript. Read by the screen; written only here. */
    val threads: StateFlow<Map<String, List<SavedMessage>>> = _threads.asStateFlow()

    /**
     * The messages of [chatId], seeded with [seed] the first time the thread is
     * opened.
     *
     * Seeding happens on FIRST READ, not in the constructor: a thread the user
     * never opened costs nothing, and re-entering an open thread keeps whatever
     * was said in it rather than resetting to the sample history.
     */
    fun transcript(chatId: String, seed: List<SavedMessage>): List<SavedMessage> {
        _threads.value[chatId]?.let { return it }
        // `update` rather than a plain write: two composables can ask for the
        // same thread in the same frame, and only one seed may win.
        _threads.update { current -> if (current.containsKey(chatId)) current else current + (chatId to seed) }
        return _threads.value[chatId].orEmpty()
    }

    /**
     * Seeds [chatId] with [seed] if it has no transcript yet.
     *
     * Called from a `LaunchedEffect`, so the screen can render the seed on its
     * very first frame (a pure read) and only then commit it — mutating state
     * during composition is not allowed.
     */
    fun ensure(chatId: String, seed: List<SavedMessage>) {
        _threads.update { current ->
            if (current.containsKey(chatId)) current else current + (chatId to seed)
        }
    }

    /** Appends one message to [chatId]. */
    fun append(chatId: String, message: SavedMessage) {
        _threads.update { current ->
            val existing = current[chatId].orEmpty()
            current + (chatId to (existing + message))
        }
    }

    /** Drops a thread's transcript (the "Clear chat history" action). */
    fun clear(chatId: String) {
        _threads.update { current -> current - chatId }
    }

    /** `MutableStateFlow.update` without pulling in the coroutines `update` ext. */
    private fun MutableStateFlow<Map<String, List<SavedMessage>>>.update(
        transform: (Map<String, List<SavedMessage>>) -> Map<String, List<SavedMessage>>,
    ) {
        while (true) {
            val current = value
            val next = transform(current)
            if (compareAndSet(current, next)) return
        }
    }
}
