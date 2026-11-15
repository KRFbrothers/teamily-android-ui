package ai.duagenai.design.screens

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/* ============================================================================
 *  The ONE delete + undo + failure flow.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  [BaseRemoteRepository] reports a write honestly: `delete` / `add` return a
 *  [WriteResult] and publish the failure on `lastWriteError`. But a repository
 *  that reports the truth is only half the fix — the SCREEN has to act on it.
 *  Before this file, both screens did this:
 *
 *      repository.delete(id)                       // result thrown away
 *      snackbarHostState.showSnackbar("... deleted", actionLabel = "Undo")
 *      if (undo) repository.add(item)              // "restore" of a row that
 *                                                  // was never removed
 *
 *  So a refused delete still said "deleted", and tapping Undo re-added an item
 *  that was still there — a duplicate row. Two copies of that mistake, one per
 *  screen, is exactly the drift this codebase keeps getting bitten by.
 *
 *  So the flow lives here, once, and both screens call it:
 *
 *    • the success snackbar (and its Undo) is shown ONLY when the source
 *      accepted the delete;
 *    • a refused delete shows a failure message and offers NO Undo, because
 *      there is nothing to undo;
 *    • a failed Undo is reported too, instead of silently leaving the row gone;
 *    • the message is always the repository's safe sentence — never a raw
 *      exception's `toString()`.
 *
 *  It is a plain `suspend` function, not a composable: the screens already own
 *  a `rememberCoroutineScope()` and a `SnackbarHostState`, so this needs no
 *  state of its own and stays testable without a UI.
 * ==========================================================================*/

/**
 * Deletes [item] and reports the outcome, offering Undo only when it worked.
 *
 * Call this from the screen's coroutine scope; it suspends for as long as the
 * snackbar is on screen (that is how `showSnackbar` works), which is also what
 * makes the Undo decision possible.
 *
 * @param item the row the user confirmed deleting.
 * @param snackbarHostState the screen's host — the same one every other
 *   snackbar on that screen uses, so the surfaces stay consistent.
 * @param deletedMessage the success text, e.g. `"Artifact deleted"`.
 * @param undoFailedMessage shown when the delete worked but re-adding the item
 *   did not, e.g. `"Artifact restore nahi ho paya."`.
 */
suspend fun <T : Identifiable, S> BaseRemoteRepository<T, S>.deleteWithUndo(
    item: T,
    snackbarHostState: SnackbarHostState,
    deletedMessage: String,
    undoFailedMessage: String,
) {
    when (val result = delete(item.id)) {
        is WriteResult.Success -> {
            // The row is really gone, so Undo is a real offer.
            val snackbar = snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (snackbar == SnackbarResult.ActionPerformed) {
                // The restore goes through the same data source that removed
                // it, and its failure is reported rather than swallowed.
                //
                // `restore`, not `add`: a restore must NOT re-POST the id the
                // server just deleted. A backend that keeps a tombstone or
                // enforces a unique id rejects that, and the undo then fails —
                // the one outcome the snackbar promised would not happen.
                val restored = restore(item)
                if (restored is WriteResult.Failure) {
                    snackbarHostState.showSnackbar(
                        message = undoFailedMessage,
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }

        is WriteResult.Failure -> {
            // Nothing was deleted, so there is nothing to undo — say so plainly
            // and keep the row on screen. `result.message` is already the
            // repository's safe sentence (see BaseRemoteRepository).
            snackbarHostState.showSnackbar(
                message = result.message,
                duration = SnackbarDuration.Long,
            )
            // The message has been shown, so the published error is stale now.
            clearWriteError()
        }
    }
}
