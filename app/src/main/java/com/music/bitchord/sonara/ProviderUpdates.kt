package com.music.bitchord.sonara

import android.content.Context
import com.music.bitchord.data.Http
import com.music.bitchord.data.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Request
import java.io.File

/**
 * Installs updates to single parts of the app (Spotify import, lyrics) without
 * a new APK. The updates are a small `providers.json` kept in the project's
 * GitHub repo: per component, a version, a note and the values that changed
 * (see [ProviderConfig]). Checking reads that file; installing saves the
 * component's values on the phone and applies them straight away.
 *
 * Only values can change this way, not code. When a service changes in a way
 * that needs new code, the fix arrives as an app update (Settings > Components >
 * Sonara app), which installs in one tap from inside the app.
 */
object ProviderUpdates {

    const val MANIFEST_URL =
        "https://raw.githubusercontent.com/andy-createapps/Sonara-app/main/providers.json"

    private const val FILE = "providers.json"
    private val json = Json { ignoreUnknownKeys = true }

    class Offer(val component: String, val version: Int, val notes: String, val entry: JsonObject)

    sealed interface Check {
        class Available(val offer: Offer) : Check
        class UpToDate(val version: Int) : Check
        class Failed(val message: String) : Check
    }

    /** Applies what was installed earlier. Called once when the app starts. */
    fun loadSaved(context: Context) {
        val file = File(context.filesDir, FILE)
        if (file.exists()) ProviderConfig.load(runCatching { file.readText() }.getOrNull())
    }

    suspend fun check(component: String): Check = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(MANIFEST_URL).header("Cache-Control", "no-cache").build()
            val body = Http.client.newCall(request).execute().use { if (it.isSuccessful) it.body?.string() else null }
                ?: return@runCatching Check.Failed("Couldn't reach the update server.")
            val entry = json.parseToJsonElement(body).jsonObject["components"]?.jsonObject
                ?.get(component) as? JsonObject
                ?: return@runCatching Check.Failed("No update information for this part.")
            val latest = entry["version"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: ProviderConfig.BUILT_IN_VERSION
            val installed = ProviderConfig.versionOf(component)
            if (latest > installed) {
                Check.Available(Offer(component, latest, entry["notes"]?.jsonPrimitive?.contentOrNull.orEmpty(), entry))
            } else {
                Check.UpToDate(installed)
            }
        }.getOrElse { Check.Failed("Check failed: ${it.message ?: "no connection"}") }
    }

    /** Saves [offer] on the phone and applies it now. */
    suspend fun install(context: Context, offer: Offer): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.filesDir, FILE)
            val saved = file.takeIf { it.exists() }?.readText()
                ?.let { json.parseToJsonElement(it).jsonObject["components"]?.jsonObject }
                ?: JsonObject(emptyMap())
            val merged = buildJsonObject {
                saved.forEach { (name, value) -> if (name != offer.component) put(name, value) }
                put(offer.component, offer.entry)
            }
            val text = buildJsonObject { put("components", merged) }.toString()
            file.writeText(text)
            ProviderConfig.load(text)
            true
        }.getOrDefault(false)
    }
}
