package com.riyadmondol2006.customanimator.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
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
    private const val MAX_SIZE_DP = 64
    private const val PACKAGE_CACHE_BYTES = 12 * 1024 * 1024

    private val byPackage = object : LruCache<String, ImageBitmap>(PACKAGE_CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.asAndroidBitmap().byteCount
    }
    private val byDrawable = Collections.synchronizedMap(WeakHashMap<Drawable, ImageBitmap>())
    private val drawing = Mutex()

    fun cached(packageName: String): ImageBitmap? = byPackage.get(packageName)

    fun cached(icon: Drawable): ImageBitmap? = byDrawable[icon]

    suspend fun load(context: Context, packageName: String): ImageBitmap? = withContext(Dispatchers.Default) {
        drawing.withLock {
            byPackage.get(packageName) ?: installedIcon(context, packageName)
                ?.let { rasterize(it, maxSizePx(context)) }
                ?.also { byPackage.put(packageName, it) }
        }
    }

    suspend fun load(icon: Drawable): ImageBitmap = withContext(Dispatchers.Default) {
        drawing.withLock {
            byDrawable[icon] ?: rasterize(icon).also { byDrawable[icon] = it }
        }
    }

    fun evict(packageName: String) {
        byPackage.remove(packageName)
    }

    fun clear() = byPackage.evictAll()

    private fun installedIcon(context: Context, packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun maxSizePx(context: Context): Int =
        (MAX_SIZE_DP * context.resources.displayMetrics.density).roundToInt().coerceIn(1, MAX_SIZE_PX)

    internal fun rasterize(icon: Drawable, maxSizePx: Int = MAX_SIZE_PX): ImageBitmap {
        val width = icon.intrinsicWidth.coerceAtLeast(1)
        val height = icon.intrinsicHeight.coerceAtLeast(1)
        val scale = (maxSizePx.toFloat() / maxOf(width, height)).coerceAtMost(1f)
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
