package com.music.bitchord.sonara

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.Http
import com.music.bitchord.data.sources.AddonSource
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.ui.icons.SfIcons
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

/** Reachability checks and addon refreshes, used by the Components rows. */
object ComponentUpdates {

    suspend fun refreshAddons(): String = withContext(Dispatchers.IO) {
        val addons = SourceRegistry.active().filterIsInstance<AddonSource>()
        if (addons.isEmpty()) return@withContext "No addons set up, nothing to refresh."
        var refreshed = 0
        addons.forEach { addon ->
            if (runCatching { addon.refreshPolicy() }.getOrNull() != null) refreshed++
        }
        "Refreshed $refreshed of ${addons.size} addons."
    }

    private fun answering(url: String): Boolean = runCatching {
        Http.client.newCall(Request.Builder().url(url).build()).execute().use { it.code in 200..399 }
    }.getOrDefault(false)

    suspend fun lyricsAnswering(): String = withContext(Dispatchers.IO) {
        val up = listOf("https://lrclib.net/api/search?q=test", "https://lyrics-api.binimum.org/").count { answering(it) }
        if (up == 2) "All lyrics sources are answering." else "$up of 2 lyrics sources answering."
    }

    suspend fun listenTogetherAnswering(): String = withContext(Dispatchers.IO) {
        val probe = runCatching {
            com.music.bitchord.data.listentogether.ListenTogether
                .probeHealthWithLatency(com.music.bitchord.data.listentogether.ListenTogether.defaultServer, 8_000L)
        }.getOrNull()
        if (probe?.isOnline == true) "Listen Together is answering." else "Listen Together isn't answering right now."
    }

    suspend fun spotifyAnswering(): String = withContext(Dispatchers.IO) {
        if (answering("https://open.spotify.com/")) "Spotify is answering." else "Spotify isn't answering right now."
    }
}

/**
 * The "Components" group in Settings. One row checks every part that can change
 * on its own (music and search sources, lyrics, Spotify import, Listen Together)
 * and, when updates exist, installs them all from the same row: tap to check,
 * tap again to install. The app itself has its own row, because only that one
 * replaces the whole app.
 */
@Composable
fun ComponentUpdateRows() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(mapOf<String, String>()) }
    var offers by remember { mutableStateOf(listOf<ProviderUpdates.Offer>()) }
    var busy by remember { mutableStateOf<String?>(null) }

    fun say(key: String, text: String) { status = status + (key to text) }

    fun launchJob(key: String, job: suspend () -> Unit) {
        if (busy != null) return
        busy = key
        scope.launch { job(); busy = null }
    }

    fun subtitle(key: String, idle: String): String =
        if (busy == key) "Working…" else status[key] ?: idle

    SettingsGroup(
        header = "Components",
        footer = "Providers update on their own, without a new app. Sonara never updates unless you tap here.",
    ) {
        Column {
            SettingsRow(
                icon = SfIcons.Cloud,
                title = if (offers.isNotEmpty()) "Install provider updates" else "Check for provider updates",
                subtitle = subtitle("providers", "Music, search, lyrics, Spotify and Listen Together"),
                onClick = {
                    launchJob("providers") {
                        if (offers.isNotEmpty()) {
                            val done = ProviderUpdates.installAll(context, offers)
                            offers = emptyList()
                            say("providers", if (done > 0) "Installed $done update${if (done == 1) "" else "s"}. They are active now." else "Couldn't install the updates. Try again.")
                        } else {
                            val (found, failure) = ProviderUpdates.checkAll()
                            val addons = ComponentUpdates.refreshAddons()
                            val lyrics = ComponentUpdates.lyricsAnswering()
                            val spotify = ComponentUpdates.spotifyAnswering()
                            val party = ComponentUpdates.listenTogetherAnswering()
                            if (found.isNotEmpty()) {
                                offers = found
                                val names = found.joinToString(", ") { it.component }
                                say("providers", "Updates available for $names. Tap to install.")
                            } else {
                                say("providers", (failure ?: "Everything is up to date.") + "\n$addons $lyrics $spotify $party")
                            }
                        }
                    }
                },
            )
            RowDivider()
            SettingsRow(
                icon = SfIcons.Download,
                title = "Sonara app",
                subtitle = subtitle("app", "Tap to check for a new version"),
                onClick = {
                    launchJob("app") {
                        AppUpdateChecker.check()
                        val found = AppUpdateChecker.available.value
                        say(
                            "app",
                            if (found != null) "Version ${found.version} is available. Follow the prompt to install it."
                            else "You have the latest version.",
                        )
                    }
                },
            )
        }
    }
}
