package ai.duagenai.design.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler

/* ============================================================================
 *  Opening a URL in a new tab — ONE shared helper.
 *
 *  WHY THIS FILE EXISTS
 *  --------------------
 *  Two screens hand a URL to the platform: the Artifacts context menu's "Open in
 *  new tab" and the Memory context menu's "Open". They used to each carry their
 *  own copy of the logic — ArtifactsScreen held a `LocalUriHandler` and a
 *  `?: { uriHandler.openUri(it) }` fallback, and MemoryScreen had its own. Two
 *  copies of one rule drift: one of them grows a null check the other forgets,
 *  and the two screens start behaving differently for the same input.
 *
 *  So the rule lives here, once, and both screens call it.
 *
 *  THE CONTRACT
 *  ------------
 *  [UrlOpener.open] takes a NULLABLE url and returns whether it opened anything:
 *
 *    • a real url  -> the launcher is called, `true` is returned
 *    • null / blank -> nothing happens, `false` is returned
 *
 *  That boolean is the whole point. A caller that gets `false` knows the tap
 *  could not do anything, so it can show a fallback (Memory opens its detail
 *  dialog) or disable the control (Artifacts disables the menu item). A helper
 *  that returned Unit would leave the caller guessing, and the guess is how a
 *  control ends up looking live while doing nothing.
 *
 *  WHY A LAUNCHER INTERFACE
 *  ------------------------
 *  [UrlLauncher] is the seam. The default implementation is the platform's own
 *  [LocalUriHandler] — which issues an ACTION_VIEW intent on Android — so a
 *  screen needs no Context plumbing. A test passes its own launcher and observes
 *  the URL without leaving the process, which is what makes the intent path
 *  testable at all.
 *
 *  No new library: `LocalUriHandler` is Compose UI, already a dependency.
 * ==========================================================================*/

/**
 * Where a URL actually goes.
 *
 * A `fun interface`, so a lambda is a launcher: `UrlLauncher { handler.openUri(it) }`.
 * The default is the platform handler; a test supplies its own.
 */
fun interface UrlLauncher {
    /** Hands [url] to the platform. Only ever called with a non-blank url. */
    fun open(url: String)
}

/**
 * The one entry point for "open this URL in a new tab".
 *
 * @param launcher where the URL goes. Defaults to the platform's own handler.
 */
class UrlOpener(private val launcher: UrlLauncher) {

    /**
     * Opens [url] in a new tab / the external browser.
     *
     * @return `true` when the launcher was called, `false` when [url] was null or
     *   blank and nothing happened. The caller uses the `false` to show a
     *   fallback or to keep the control disabled — never to swallow the tap.
     */
    fun open(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        launcher.open(url)
        return true
    }
}

/**
 * The [UrlOpener] a composable should use.
 *
 * Defaults to the platform handler ([LocalUriHandler], an ACTION_VIEW intent on
 * Android). Pass [launcher] to override it — that is the hook the tests use to
 * observe the URL without leaving the process.
 */
@Composable
fun rememberUrlOpener(launcher: UrlLauncher? = null): UrlOpener {
    val handler = LocalUriHandler.current
    return remember(launcher, handler) {
        UrlOpener(launcher ?: UrlLauncher { handler.openUri(it) })
    }
}
