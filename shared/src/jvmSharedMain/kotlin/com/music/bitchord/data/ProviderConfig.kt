package com.music.bitchord.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Values the outside world changes under us (a Spotify query id, a lyrics
 * service's address) that can be replaced while the app is installed, without
 * a new build. Every value in the code has a built-in default; an installed
 * update only overrides it. Nothing here is code: just strings.
 *
 * The app reads [get] each time it needs one, so an update takes effect on the
 * next request, with no restart.
 */
object ProviderConfig {

    /** What the code shipped with counts as, so a manifest at version 1 changes nothing. */
    const val BUILT_IN_VERSION = 1

    @Volatile
    private var values: Map<String, String> = emptyMap()

    @Volatile
    private var versions: Map<String, Int> = emptyMap()

    /** [key]'s installed value, or [default] when no update has set it. */
    fun get(key: String, default: String): String =
        values[key]?.takeIf { it.isNotBlank() } ?: default

    /** The installed version of a component ("spotify", "lyrics"); the built-in one is 1. */
    fun versionOf(component: String): Int = versions[component] ?: BUILT_IN_VERSION

    /**
     * Replaces everything with what [stored] holds:
     * `{"components":{"spotify":{"version":2,"values":{"key":"value"}}}}`.
     * Anything unreadable is ignored and the built-in defaults stand.
     */
    fun load(stored: String?) {
        if (stored.isNullOrBlank()) return
        runCatching {
            val root = Json.parseToJsonElement(stored).jsonObject
            val components = root["components"] as? JsonObject ?: return
            val newValues = HashMap<String, String>()
            val newVersions = HashMap<String, Int>()
            for ((name, component) in components) {
                val obj = component as? JsonObject ?: continue
                newVersions[name] = obj["version"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                (obj["values"] as? JsonObject)?.forEach { (k, v) ->
                    v.jsonPrimitive.contentOrNull?.let { newValues[k] = it }
                }
            }
            values = newValues
            versions = newVersions
        }
    }
}
