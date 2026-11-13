package com.music.bitchord.sonara

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * The welcome intro and the little tips under the logo, from the Sonara source.
 *
 * The intro itself is the source's own page (assets/sonara_intro, text and song
 * unchanged) shown in a WebView on top of the app. How often it appears is
 * decided here, once per app start:
 *   1st start: it plays by itself.
 *   2nd start: it asks whether to play it again.
 *   3rd start: it asks one last time.
 *   4th start on: never again (except by tapping the logo seven times).
 */
object SonaraWelcome {
    private const val FILE = "sonara_welcome"
    private const val KEY_LAUNCHES = "launches"

    @Volatile
    private var consumed = false

    /** "camellia" or "andy": which intro text and tips this build carries. */
    val edition: String get() = if (Edition.isCamellia) "camellia" else "andy"

    /**
     * Counts this app start (once per process, however often the activity is
     * recreated) and returns the intro mode it gets: "run", "ask2", "ask3",
     * or null for no intro.
     */
    fun modeForThisLaunch(context: Context): String? {
        if (consumed) return null
        consumed = true
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val launches = prefs.getInt(KEY_LAUNCHES, 0) + 1
        prefs.edit().putInt(KEY_LAUNCHES, launches).apply()
        return when (launches) {
            1 -> "run"
            2 -> "ask2"
            3 -> "ask3"
            else -> null
        }
    }

    /** The lines that rotate under the logo on the home feed (the source's eggs.js). */
    val tips: List<String>
        get() = if (Edition.isCamellia) {
            listOf(
                "made for Camellia 🌸",
                "made by Andy Morgan",
                "Claude typed. Andy vibed.",
                "zero lines coded by Andy",
                "Tame Impala × JENNIE = peak",
                "Andy says: “continue”",
                "bugs? never heard of them",
                "Dracula, but make it us",
                "tip: tap the logo 7 times",
                "headphones on, world off",
                "the bass is a lifestyle",
                "Andy likes the remix because of you",
            )
        } else {
            listOf(
                "made by Andy Morgan",
                "let it happen ✨",
                "Claude typed. Andy vibed.",
                "zero lines coded by Andy",
                "skip the ads, not the drop",
                "Andy says: “continue”",
                "bugs? never heard of them",
                "currents are strong tonight",
                "a Tame Impala fan lives here",
                "tip: tap the logo 7 times",
                "headphones on, world off",
                "the bass is a lifestyle",
            )
        }
}

/** The intro, full screen over the app. [onClose] is called when it has finished or been skipped. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SonaraIntro(mode: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val currentOnClose by rememberUpdatedState(onClose)
    var web by remember { mutableStateOf<WebView?>(null) }

    // Back skips the intro (the page fades its song out first, then closes itself).
    BackHandler(enabled = true) {
        val view = web
        if (view != null) view.evaluateJavascript("window.__back && window.__back()", null) else currentOnClose()
    }
    DisposableEffect(Unit) { onDispose { web?.destroy() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070C))
            .navigationBarsPadding(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun close() {
                                Handler(Looper.getMainLooper()).post { currentOnClose() }
                            }
                        },
                        "SonaraIntro",
                    )
                    loadUrl("file:///android_asset/sonara_intro/intro.html?mode=$mode&edition=${SonaraWelcome.edition}")
                    web = this
                }
            },
        )
    }
}
