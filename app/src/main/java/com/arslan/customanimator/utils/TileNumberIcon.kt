package com.arslan.customanimator.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Icon
import java.util.Locale

object TileNumberIcon {

    private const val SIZE = 192f
    private const val PADDING = 4f

    fun create(text: String): Icon {
        val bitmap = Bitmap.createBitmap(SIZE.toInt(), SIZE.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val available = SIZE - PADDING * 2
        var size = SIZE
        val bounds = Rect()
        while (size > 8f) {
            paint.textSize = size
            paint.getTextBounds(text, 0, text.length, bounds)
            if (bounds.width() <= available && bounds.height() <= available) break
            size -= 2f
        }

        canvas.drawText(text, SIZE / 2f, SIZE / 2f - bounds.exactCenterY(), paint)
        return Icon.createWithBitmap(bitmap)
    }

    fun widthText(widthDp: Int): String = if (widthDp <= 0) "RST" else widthDp.toString()

    fun animationText(window: Float, transition: Float, animator: Float): String {
        val uniform = nearlyEqual(window, transition) && nearlyEqual(transition, animator)
        return if (uniform) compactScale(window) else "MIX"
    }

    fun compactScale(value: Float): String {
        val text = trimScale(value)
        return if (text.startsWith("0.")) text.substring(1) else text
    }

    fun animationSubtitle(window: Float, transition: Float, animator: Float): String? {
        val uniform = nearlyEqual(window, transition) && nearlyEqual(transition, animator)
        if (uniform) return null
        return "${trimScale(window)} / ${trimScale(transition)} / ${trimScale(animator)}"
    }

    fun trimScale(value: Float): String {
        val formatted = String.format(Locale.US, "%.2f", value)
        return formatted.trimEnd('0').trimEnd('.').ifEmpty { "0" }
    }

    private fun hundredths(value: Float): Int =
        Math.round(String.format(Locale.US, "%.2f", value).toFloat() * 100f)

    fun nearlyEqual(a: Float, b: Float): Boolean = hundredths(a) == hundredths(b)
}
