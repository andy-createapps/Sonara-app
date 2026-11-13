package com.music.bitchord.sonara

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

/**
 * Sonara never checks for a new version of itself. The parts that go stale when
 * a service changes its rules (how YouTube hands out streams, where lyrics come
 * from, how Spotify pages are read) are checked one at a time from
 * Settings, so nothing changes under the listener unless they ask.
 *
 * What each check can honestly do:
 *  - Addons: re-read the addon's manifest right now (a real refresh).
 *  - YouTube extractor, lyrics, Spotify: report whether the upstream project has
 *    a newer release or whether the service is answering. These parts are
 *    compiled into the app, so a newer one still arrives as a new build.
 */
object ComponentUpdates {

    /** The NewPipeExtractor release this build was compiled against. */
    const val EXTRACTOR_VERSION = "0.26.3"

    private val json = Json { ignoreUnknownKeys = true }

    private fun get(url: String): Pair<Int, String?> {
        val request = Request.Builder().url(url).build()
        return Http.client.newCall(request).execute().use { it.code to it.body?.string() }
    }

    suspend fun checkExtractor(): String = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = get("https://api.github.com/repos/TeamNewPipe/NewPipeExtractor/releases/latest")
            if (code != 200 || body == null) return@runCatching "Couldn't reach the update server. Try again later."
            val tag = (json.parseToJsonElement(body) as? JsonObject)
                ?.get("tag_name")?.jsonPrimitive?.contentOrNull
                ?: return@runCatching "Couldn't read the latest version."
            val latest = tag.removePrefix("v")
            if (latest == EXTRACTOR_VERSION) {
                "Up to date (v$EXTRACTOR_VERSION)"
            } else {
                "Newer version out: v$latest. This build has v$EXTRACTOR_VERSION."
            }
        }.getOrElse { "Check failed: ${it.message ?: "no connection"}" }
    }

    suspend fun refreshAddons(): String = withContext(Dispatchers.IO) {
        val addons = SourceRegistry.active().filterIsInstance<AddonSource>()
        if (addons.isEmpty()) return@withContext "No addons set up, nothing to refresh."
        var refreshed = 0
        addons.forEach { addon ->
            if (runCatching { addon.refreshPolicy() }.getOrNull() != null) refreshed++
        }
        "Refreshed $refreshed of ${addons.size} addons."
    }

    private fun answering(url: String): Boolean =
        runCatching { get(url).first in 200..399 }.getOrDefault(false)

    suspend fun checkLyrics(): String = withContext(Dispatchers.IO) {
        val checks = listOf(
            "https://lrclib.net/api/search?q=test",
            "https://lyrics-api.binimum.org/",
        )
        val up = checks.count { answering(it) }
        if (up == checks.size) "All lyrics sources are answering."
        else "$up of ${checks.size} lyrics sources answering. Some lyrics may be missing."
    }

    suspend fun checkSpotify(): String = withContext(Dispatchers.IO) {
        if (answering("https://open.spotify.com/")) "Spotify is answering. Playlist import should work."
        else "Spotify isn't answering right now. Playlist import may fail."
    }
}

/** The "Components" group in Settings: one check per part, each on its own. */
@Composable
fun ComponentUpdateRows() {
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf(mapOf<String, String>()) }
    var busy by remember { mutableStateOf<String?>(null) }

    fun launchCheck(key: String, job: suspend () -> String) {
        if (busy != null) return
        busy = key
        scope.launch {
            val text = job()
            results = results + (key to text)
            busy = null
        }
    }

    fun status(key: String): String =
        if (busy == key) "Checking…" else results[key] ?: "Tap to check for updates"

    SettingsGroup(
        header = "Components",
        footer = "Each part is checked on its own. Sonara itself never updates unless you install a new build.",
    ) {
        Column {
            SettingsRow(
                icon = SfIcons.Download,
                title = "YouTube playback",
                subtitle = status("yt"),
                onClick = { launchCheck("yt") { ComponentUpdates.checkExtractor() } },
            )
            RowDivider()
            SettingsRow(
                icon = SfIcons.Cloud,
                title = "Sources and addons",
                subtitle = status("addons"),
                onClick = { launchCheck("addons") { ComponentUpdates.refreshAddons() } },
            )
            RowDivider()
            SettingsRow(
                icon = SfIcons.MusicNote,
                title = "Lyrics",
                subtitle = status("lyrics"),
                onClick = { launchCheck("lyrics") { ComponentUpdates.checkLyrics() } },
            )
            RowDivider()
            SettingsRow(
                icon = SfIcons.Info,
                title = "Spotify import",
                subtitle = status("spotify"),
                onClick = { launchCheck("spotify") { ComponentUpdates.checkSpotify() } },
            )
        }
    }
}
