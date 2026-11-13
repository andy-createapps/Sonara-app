package com.music.bitchord.sonara

import com.music.bitchord.BuildConfig

/**
 * Which of the two Sonara builds this is. Set at build time with
 * `-Pedition=camellia` (see app/build.gradle.kts); anything else is the plain
 * app. The camellia build adds the welcome quiz and the hidden bits.
 */
object Edition {
    val isCamellia: Boolean = BuildConfig.EDITION == "camellia"
}
