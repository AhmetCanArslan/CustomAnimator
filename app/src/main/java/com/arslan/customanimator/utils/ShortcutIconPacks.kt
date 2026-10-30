package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import android.util.Xml
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.arslan.customanimator.data.ShortcutIconImage
import com.arslan.customanimator.data.ShortcutIconPack
import org.xmlpull.v1.XmlPullParser

object ShortcutIconPacks {
    private val THEME_ACTIONS = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME")
    private const val DRAWABLE_LIST = "drawable"
    private const val APP_FILTER = "appfilter"
    private const val ITEM_TAG = "item"
    private const val DRAWABLE_ATTRIBUTE = "drawable"
    private const val COMPONENT_ATTRIBUTE = "component"
    private const val THUMBNAIL_PX = 96
    private const val ICON_PX = 192
    private const val THUMBNAIL_CACHE_SIZE = 300

    private val thumbnails = LruCache<String, Bitmap>(THUMBNAIL_CACHE_SIZE)

    fun listPacks(context: Context): List<ShortcutIconPack> {
        val packageManager = context.packageManager
        return THEME_ACTIONS
            .flatMap { action ->
                try {
                    packageManager.queryIntentActivities(Intent(action), 0)
                } catch (e: Exception) {
                    emptyList()
                }
            }
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .map { ShortcutIconPack(it.packageName, it.loadLabel(packageManager).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    fun listIcons(context: Context, packageName: String, targetPackage: String?): List<String> {
        val resources = resourcesFor(context, packageName) ?: return emptyList()
        val names = LinkedHashSet<String>()
        val suggested = LinkedHashSet<String>()
        readItems(resources, packageName, DRAWABLE_LIST) { parser ->
            parser.getAttributeValue(null, DRAWABLE_ATTRIBUTE)?.let(names::add)
        }
        readItems(resources, packageName, APP_FILTER) { parser ->
            val drawable = parser.getAttributeValue(null, DRAWABLE_ATTRIBUTE) ?: return@readItems
            names.add(drawable)
            if (targetsPackage(parser.getAttributeValue(null, COMPONENT_ATTRIBUTE), targetPackage)) {
                suggested.add(drawable)
            }
        }
        return (suggested + names).filter { drawableId(resources, packageName, it) != 0 }
    }

    fun loadThumbnail(context: Context, packageName: String, name: String): Bitmap? {
        val key = "$packageName/$name"
        thumbnails.get(key)?.let { return it }
        val bitmap = loadDrawable(context, packageName, name)?.toBitmap(THUMBNAIL_PX, THUMBNAIL_PX) ?: return null
        thumbnails.put(key, bitmap)
        return bitmap
    }

    fun loadIcon(context: Context, packageName: String, name: String): ShortcutIconImage? {
        val drawable = loadDrawable(context, packageName, name) ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            ShortcutIconImage(ShortcutMaker.drawableIcon(drawable))
        } else {
            ShortcutIconImage(drawable.toBitmap(ICON_PX, ICON_PX), legacy = true)
        }
    }

    private fun loadDrawable(context: Context, packageName: String, name: String): Drawable? {
        val resources = resourcesFor(context, packageName) ?: return null
        return try {
            ResourcesCompat.getDrawable(resources, drawableId(resources, packageName, name), null)
        } catch (e: Exception) {
            null
        }
    }

    private fun resourcesFor(context: Context, packageName: String): Resources? {
        return try {
            context.packageManager.getResourcesForApplication(packageName)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableId(resources: Resources, packageName: String, name: String): Int =
        resources.getIdentifier(name, "drawable", packageName)

    private fun targetsPackage(component: String?, targetPackage: String?): Boolean =
        targetPackage != null && component?.contains("{$targetPackage/") == true

    private fun readItems(resources: Resources, packageName: String, file: String, onItem: (XmlPullParser) -> Unit) {
        try {
            val id = resources.getIdentifier(file, "xml", packageName)
            if (id != 0) {
                resources.getXml(id).use { walkItems(it, onItem) }
            } else {
                resources.assets.open("$file.xml").use { stream ->
                    walkItems(Xml.newPullParser().apply { setInput(stream, null) }, onItem)
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun walkItems(parser: XmlPullParser, onItem: (XmlPullParser) -> Unit) {
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == ITEM_TAG) onItem(parser)
        }
    }
}
