package com.arslan.customanimator.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object AppIconBitmaps {
    private const val MAX_SIZE_PX = 192

    private val cache = Collections.synchronizedMap(WeakHashMap<Drawable, ImageBitmap>())
    private val drawing = Mutex()

    fun cached(icon: Drawable): ImageBitmap? = cache[icon]

    suspend fun load(icon: Drawable): ImageBitmap = withContext(Dispatchers.Default) {
        drawing.withLock {
            cache[icon] ?: rasterize(icon).also { cache[icon] = it }
        }
    }

    internal fun rasterize(icon: Drawable): ImageBitmap {
        val width = icon.intrinsicWidth.coerceAtLeast(1)
        val height = icon.intrinsicHeight.coerceAtLeast(1)
        val scale = (MAX_SIZE_PX.toFloat() / maxOf(width, height)).coerceAtMost(1f)
        val bitmap = Bitmap.createBitmap(
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        icon.setBounds(0, 0, bitmap.width, bitmap.height)
        icon.draw(Canvas(bitmap))
        return bitmap.asImageBitmap()
    }
}
