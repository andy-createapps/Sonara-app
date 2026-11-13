package com.music.bitchord

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * The home-screen icon choices. Each is an `<activity-alias>` in the manifest
 * carrying the launcher intent filter; exactly one is enabled at a time.
 *
 * The glass icons are adaptive icons whose foreground already holds the finished
 * icon in its own shape (Apple squircle, Samsung squircle or Pixel circle), so
 * [previewRes] — that foreground — shows the variant as designed rather than
 * through the device's launcher mask. The first two keep yesterday's alias
 * names so a choice made before the update stays enabled.
 */
enum class AppIcon(val alias: String, val iconRes: Int, val labelRes: Int, val previewRes: Int? = null) {
    Sonara1("LauncherSonara1", R.mipmap.ic_sonara_1, R.string.app_icon_sonara_1, R.mipmap.ic_sonara_1_preview),
    Sonara2("LauncherSonara2", R.mipmap.ic_sonara_2, R.string.app_icon_sonara_2, R.mipmap.ic_sonara_2_preview),
    Sonara3("LauncherSonara3", R.mipmap.ic_sonara_3, R.string.app_icon_sonara_3, R.mipmap.ic_sonara_3_preview),
    Sonara4("LauncherSonara4", R.mipmap.ic_sonara_4, R.string.app_icon_sonara_4, R.mipmap.ic_sonara_4_preview),
    Sonara5("LauncherSonara5", R.mipmap.ic_sonara_5, R.string.app_icon_sonara_5, R.mipmap.ic_sonara_5_preview),
    Sonara6("LauncherSonara6", R.mipmap.ic_sonara_6, R.string.app_icon_sonara_6, R.mipmap.ic_sonara_6_preview),
    Sonara7("LauncherSonara7", R.mipmap.ic_sonara_7, R.string.app_icon_sonara_7, R.mipmap.ic_sonara_7_preview),
    Sonara8("LauncherSonara8", R.mipmap.ic_sonara_8, R.string.app_icon_sonara_8, R.mipmap.ic_sonara_8_preview),
    Sonara9("LauncherSonara9", R.mipmap.ic_sonara_9, R.string.app_icon_sonara_9, R.mipmap.ic_sonara_9_preview),
    Sonara10("LauncherSonara10", R.mipmap.ic_sonara_10, R.string.app_icon_sonara_10, R.mipmap.ic_sonara_10_preview),
    Sonara11("LauncherSonara11", R.mipmap.ic_sonara_11, R.string.app_icon_sonara_11, R.mipmap.ic_sonara_11_preview),
    Sonara12("LauncherSonara12", R.mipmap.ic_sonara_12, R.string.app_icon_sonara_12, R.mipmap.ic_sonara_12_preview);

    private fun component(context: Context) =
        ComponentName(context.packageName, "com.music.bitchord.$alias")

    companion object {
        fun current(context: Context): AppIcon {
            val pm = context.packageManager
            return entries.firstOrNull {
                pm.getComponentEnabledSetting(it.component(context)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } ?: Sonara1
        }

        /** Enables [icon] before disabling the rest so there is never a moment with no launcher entry. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                icon.component(context),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(
                    it.component(context),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
