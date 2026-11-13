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

    suspend fun spotifyAnswering(): String = withContext(Dispatchers.IO) {
        if (answering("https://open.spotify.com/")) "Spotify is answering." else "Spotify isn't answering right now."
    }
}

/**
 * The "Components" group in Settings. Each part is checked on its own and, when
 * an update exists, installed from the same row: tap once to check, tap again to
 * install.
 */
@Composable
fun ComponentUpdateRows() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(mapOf<String, String>()) }
    var offers by remember { mutableStateOf(mapOf<String, ProviderUpdates.Offer>()) }
    var busy by remember { mutableStateOf<String?>(null) }

    fun say(key: String, text: String) { status = status + (key to text) }

    fun launchJob(key: String, job: suspend () -> Unit) {
        if (busy != null) return
        busy = key
        scope.launch { job(); busy = null }
    }

    fun subtitle(key: String): String =
        if (busy == key) "Working…" else status[key] ?: "Tap to check for updates"

    // One row per part that can be updated with a few values: check, then install.
    fun providerRow(key: String, extra: suspend () -> String) {
        val offer = offers[key]
        launchJob(key) {
            if (offer != null) {
                val ok = ProviderUpdates.install(context, offer)
                if (ok) {
                    offers = offers - key
                    say(key, "Installed update v${offer.version}. It is active now.")
                } else {
                    say(key, "Couldn't install the update. Try again.")
                }
            } else {
                when (val result = ProviderUpdates.check(key)) {
                    is ProviderUpdates.Check.Available -> {
                        offers = offers + (key to result.offer)
                        val note = result.offer.notes.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
                        say(key, "Update v${result.offer.version} available.$note Tap to install.")
                    }
                    is ProviderUpdates.Check.UpToDate -> say(key, "Up to date (v${result.version}). ${extra()}")
                    is ProviderUpdates.Check.Failed -> say(key, result.message)
                }
            }
        }
    }

    SettingsGroup(
        header = "Components",
        footer = "Each part updates on its own. Sonara never updates unless you tap here.",
    ) {
        Column {
            SettingsRow(
                icon = SfIcons.Download,
                title = "Sonara app",
                subtitle = subtitle("app"),
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
            RowDivider()
            SettingsRow(
                icon = SfIcons.Cloud,
                title = "Sources and addons",
                subtitle = subtitle("addons"),
                onClick = { launchJob("addons") { say("addons", ComponentUpdates.refreshAddons()) } },
            )
            RowDivider()
            SettingsRow(
                icon = SfIcons.MusicNote,
                title = "Lyrics",
                subtitle = subtitle("lyrics"),
                onClick = { providerRow("lyrics") { ComponentUpdates.lyricsAnswering() } },
            )
            RowDivider()
            SettingsRow(
                icon = SfIcons.Info,
                title = "Spotify import",
                subtitle = subtitle("spotify"),
                onClick = { providerRow("spotify") { ComponentUpdates.spotifyAnswering() } },
            )
        }
    }
}
