package com.music.bitchord.sonara

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** What the welcome quiz remembers between launches. */
object CamelliaWelcomePrefs {
    private const val FILE = "sonara_welcome"
    private const val KEY_DONE = "done"
    private const val KEY_ROUND = "round"

    /** The quiz may run this many times in all, then it is gone for good. */
    const val MAX_ROUNDS = 3

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isPending(context: Context): Boolean =
        Edition.isCamellia && !prefs(context).getBoolean(KEY_DONE, false)

    fun round(context: Context): Int =
        prefs(context).getInt(KEY_ROUND, 1).coerceIn(1, MAX_ROUNDS)

    fun setRound(context: Context, round: Int) {
        prefs(context).edit().putInt(KEY_ROUND, round).apply()
    }

    fun setDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_DONE, true).apply()
    }
}

private class Question(
    val text: String,
    val options: List<Pair<String, String>>, // answer to Andy's reaction
)

private val INTRO_TITLE = "hey Camellia"
private val INTRO_BODY =
    "Andy Morgan made this app for you.\n\nBefore you get in, there are a few important questions about him. " +
        "Take them seriously."

private val QUESTIONS = listOf(
    Question(
        "First things first. Who made this app?",
        listOf(
            "Andy Morgan" to "Correct. Very humble guy. Insanely good looking. (He typed that part himself.)",
            "Some guy named Andy" to "Close enough. He answers to both.",
            "Elon" to "Elon wishes he was this cool.",
        ),
    ),
    Question(
        "Is Andy your boyfriend?",
        listOf(
            "No" to "Correct. The app just needed to confirm.",
            "Absolutely not" to "Ouch. He's fine. He's totally fine.",
            "Define boyfriend" to "Andy is googling it right now.",
        ),
    ),
    Question(
        "Andy's favourite band?",
        listOf(
            "Tame Impala" to "Correct. He'd play them at his own funeral.",
            "Coldplay" to "Andy felt that from across the world.",
            "Whatever's on TikTok" to "He's pretending he didn't read that.",
        ),
    ),
    Question(
        "Now playing: Dracula (Jennie remix).\nYou like Jennie. Andy likes Tame Impala. Put them together and it's peak. Agree?",
        listOf(
            "Peak" to "Finally, somebody with taste.",
            "Lowkey peak" to "Lowkey is how the best things start.",
            "It's fine" to "Andy's lip is trembling. Say something nicer.",
        ),
    ),
    Question(
        "What does Andy do when he finds a good song?",
        listOf(
            "Plays it 40 times in a row" to "Accurate.",
            "Sends it to you at 3am" to "Accurate, and a little concerning.",
            "Saves it to a playlist he never opens" to "Also accurate.",
        ),
    ),
    Question(
        "Last one. Rate Andy out of 10.",
        listOf(
            "10" to "Screenshot this. His ego needs it.",
            "11" to "He's now unbearable. Thanks a lot.",
            "A solid 9" to "Fair. He'll say he's fine with it.",
        ),
    ),
)

private val BG = Brush.verticalGradient(listOf(Color(0xFF14091F), Color(0xFF3A1B4F), Color(0xFF7A2E5C)))
private val GOLD = Color(0xFFFFC857)
private val GLASS = Color.White.copy(alpha = 0.12f)

/**
 * The first-launch welcome for the camellia build: a short quiz about Andy,
 * with Dracula (Jennie remix) playing under it. It can be run three times; after
 * the third it is gone for good.
 *
 * [onFinished] is called when it is over for good. Playing the song is the
 * caller's job (it needs the player), via the [onStartMusic] it is handed.
 */
@Composable
fun CamelliaWelcome(onStartMusic: () -> Unit, onFinished: () -> Unit) {
    val context = LocalContext.current
    var round by remember { mutableIntStateOf(CamelliaWelcomePrefs.round(context)) }
    // 0 = intro, 1..QUESTIONS.size = a question, QUESTIONS.size + 1 = the wrap-up.
    var step by remember { mutableIntStateOf(0) }

    // No way out by the back button; the quiz ends by itself.
    BackHandler(enabled = true) {}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
            label = "welcome-step",
        ) { current ->
            when {
                current == 0 -> Page(
                    title = INTRO_TITLE,
                    body = INTRO_BODY,
                    primary = "Let's go" to {
                        onStartMusic()
                        step = 1
                    },
                )

                current <= QUESTIONS.size -> QuestionPage(
                    index = current,
                    question = QUESTIONS[current - 1],
                    isLast = current == QUESTIONS.size,
                    onNext = { step = current + 1 },
                )

                else -> when (round) {
                    1 -> Page(
                        title = "That's the welcome",
                        body = "Andy says thanks for playing along.\n\nWant to roll it again?",
                        primary = "Run it back" to {
                            round = 2
                            CamelliaWelcomePrefs.setRound(context, 2)
                            step = 0
                        },
                        secondary = "Nah, take me in" to {
                            CamelliaWelcomePrefs.setDone(context)
                            onFinished()
                        },
                    )

                    2 -> Page(
                        title = "Round two done",
                        body = "One more go?\n\nThe third time is the last time, then it's gone.",
                        primary = "One last time" to {
                            round = 3
                            CamelliaWelcomePrefs.setRound(context, 3)
                            step = 0
                        },
                        secondary = "Enough, take me in" to {
                            CamelliaWelcomePrefs.setDone(context)
                            onFinished()
                        },
                    )

                    else -> Page(
                        title = "That's it",
                        body = "The quiz is over and it won't come back.\n\n" +
                            "Enjoy Sonara. Andy made it for you.\n\n" +
                            "(Psst. There's some hidden stuff in here. Andy won't say where.)",
                        primary = "Take me in" to {
                            CamelliaWelcomePrefs.setDone(context)
                            onFinished()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Page(
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = body,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 18.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = primary.second,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GOLD, contentColor = Color(0xFF2A1440)),
        ) {
            Text(primary.first, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
        }
        if (secondary != null) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = secondary.second,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GLASS, contentColor = Color.White),
            ) {
                Text(secondary.first, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun QuestionPage(
    index: Int,
    question: Question,
    isLast: Boolean,
    onNext: () -> Unit,
) {
    var picked by remember(index) { mutableStateOf<Int?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$index / ${QUESTIONS.size}",
            color = GOLD,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = question.text,
            color = Color.White,
            fontSize = 26.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        question.options.forEachIndexed { i, (label, _) ->
            val chosen = picked == i
            Button(
                onClick = { if (picked == null) picked = i },
                enabled = picked == null || chosen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (chosen) GOLD else GLASS,
                    contentColor = if (chosen) Color(0xFF2A1440) else Color.White,
                    disabledContainerColor = if (chosen) GOLD else GLASS.copy(alpha = 0.06f),
                    disabledContentColor = if (chosen) Color(0xFF2A1440) else Color.White.copy(alpha = 0.4f),
                ),
            ) {
                Text(label, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
        val reaction = picked?.let { question.options[it].second }
        if (reaction != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = reaction,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 17.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GOLD, contentColor = Color(0xFF2A1440)),
            ) {
                Text(if (isLast) "Done" else "Next", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}
