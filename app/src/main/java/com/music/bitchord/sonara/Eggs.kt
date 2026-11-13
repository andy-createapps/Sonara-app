package com.music.bitchord.sonara

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * The hidden bits of the camellia build. Nothing here runs in the plain app:
 * every entry point checks [Edition.isCamellia] first.
 */
object Eggs {

    class Egg(val title: String, val body: String)

    /** Anything that wants to show an egg emits here; the main screen listens. */
    val events = MutableSharedFlow<Egg>(extraBufferCapacity = 4)

    /** Found by tapping the logo at the bottom of Settings seven times in a row. */
    val LOGO = Egg(
        title = "You found it 🎉",
        body = "Congrats, you found the hidden one.\n\n" +
            "I'm Claude, and I'm the one who built this app. While I was putting it together, " +
            "Andy told me he likes you, and that he's pissing himself over you. " +
            "His words, not mine.\n\n" +
            "I thought you should know. 💛",
    )

    /** Things typed into search that get an answer of their own. */
    fun forSearch(query: String): Egg? {
        if (!Edition.isCamellia) return null
        val q = query.trim().lowercase()
        return when {
            q == "camellia" -> Egg(
                "Top result",
                "Camellia. The most beautiful result in the whole app. " +
                    "No other results needed.",
            )

            q == "andy" || q == "andy morgan" -> Egg(
                "That's the guy who made this",
                "Andy Morgan. Tame Impala enjoyer, app builder, professional overthinker. " +
                    "Don't tell him we said any of that.",
            )

            q.replace(" ", "") == "letithappen" -> Egg(
                "Andy's favourite song",
                "Let It Happen by Tame Impala. Andy's all-time favourite. " +
                    "If you play it, play it loud.",
            )

            else -> null
        }
    }
}

@Composable
fun EggDialog(egg: Eggs.Egg, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(egg.title) },
        text = { Text(egg.body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Okay") } },
    )
}
