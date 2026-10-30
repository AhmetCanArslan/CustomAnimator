package com.arslan.customanimator.notify.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.ContextCompat

data class ResolvedWidgetColors(
    val background: Long,
    val text: Long,
    val accent: Long,
)

object WidgetMaterialColors {

    private val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun resolve(context: Context): ResolvedWidgetColors {
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        if (!isSupported) {
            return if (dark) {
                ResolvedWidgetColors(0xFF1C1B1F, 0xFFFFFFFF, 0xFF9C27B0)
            } else {
                ResolvedWidgetColors(0xFFFFFFFF, 0xFF000000, 0xFF6750A4)
            }
        }

        val background = if (dark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_50
        val text = if (dark) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900
        val accent = if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600

        return ResolvedWidgetColors(
            background = color(context, background),
            text = color(context, text),
            accent = color(context, accent),
        )
    }

    fun withAlpha(color: Long, alphaPercent: Int): Int {
        val alpha = alphaPercent.coerceIn(0, 100) * 255 / 100
        return (alpha shl 24) or (color.toInt() and 0x00FFFFFF)
    }

    private fun color(context: Context, resId: Int): Long =
        ContextCompat.getColor(context, resId).toLong() and 0xFFFFFFFFL
}
