package com.riyadmondol2006.customanimator.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.net.Uri
import android.os.Bundle
import com.riyadmondol2006.customanimator.data.StaticShortcut
import org.xmlpull.v1.XmlPullParser

object StaticShortcuts {

    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private const val META_SHORTCUTS = "android.app.shortcuts"
    private const val TAG_SHORTCUT = "shortcut"
    private const val TAG_INTENT = "intent"
    private const val TAG_CATEGORIES = "categories"
    private const val TAG_EXTRA = "extra"
    private val genericActions = setOf(Intent.ACTION_MAIN, Intent.ACTION_VIEW)

    private class Draft(val id: String, val label: String, val iconResId: Int, val enabled: Boolean) {
        val intents = mutableListOf<Intent>()
    }

    fun find(context: Context, packageName: String): List<StaticShortcut> {
        val packageManager = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        val activities = try {
            packageManager.queryIntentActivities(launcher, PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }
        return activities
            .flatMap { parse(packageManager, it.activityInfo) }
            .distinctBy { it.id }
    }

    private fun parse(packageManager: PackageManager, activity: ActivityInfo): List<StaticShortcut> {
        val resId = activity.metaData?.getInt(META_SHORTCUTS) ?: 0
        if (resId == 0) return emptyList()
        return try {
            val resources = packageManager.getResourcesForApplication(activity.applicationInfo)
            val shortcuts = mutableListOf<StaticShortcut>()
            var draft: Draft? = null
            resources.getXml(resId).use { xml ->
                while (xml.next() != XmlPullParser.END_DOCUMENT) {
                    draft = xml.step(packageManager, resources, draft, activity.packageName, shortcuts)
                }
            }
            shortcuts
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun XmlResourceParser.step(
        packageManager: PackageManager,
        resources: Resources,
        draft: Draft?,
        packageName: String,
        out: MutableList<StaticShortcut>
    ): Draft? {
        if (eventType == XmlPullParser.END_TAG && name == TAG_SHORTCUT) {
            draft?.toShortcut(packageManager)?.let(out::add)
            return null
        }
        if (eventType != XmlPullParser.START_TAG) return draft
        if (name == TAG_SHORTCUT) return draft(resources)
        val last = draft?.intents?.lastOrNull()
        when (name) {
            TAG_INTENT -> draft?.intents?.add(intent(packageName))
            TAG_CATEGORIES -> attr(resources, "name")?.let { last?.addCategory(it) }
            TAG_EXTRA -> last?.putExtras(Bundle().also { resources.parseBundleExtra(TAG_EXTRA, this, it) })
        }
        return draft
    }

    private fun Draft.toShortcut(packageManager: PackageManager): StaticShortcut? {
        if (!enabled || intents.isEmpty()) return null
        intents.first().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val exported = intents.map { packageManager.exportedIntent(it) }
        if (exported.any { it == null }) return null
        return StaticShortcut(id, label, iconResId, exported.filterNotNull())
    }

    private fun PackageManager.exportedIntent(intent: Intent): Intent? {
        if (resolveActivity(intent, 0)?.activityInfo?.exported == true) return intent
        val target = intent.component?.packageName ?: return null
        if (intent.data == null && intent.action in genericActions) return null
        val implicit = Intent(intent).setComponent(null).setPackage(target)
        val info = resolveActivity(implicit, 0)?.activityInfo ?: return null
        return implicit.takeIf { info.exported && info.packageName == target }
    }

    private fun XmlResourceParser.draft(resources: Resources): Draft? {
        val id = getAttributeValue(ANDROID_NS, "shortcutId") ?: return null
        return Draft(
            id = id,
            label = attr(resources, "shortcutShortLabel") ?: attr(resources, "shortcutLongLabel") ?: id,
            iconResId = getAttributeResourceValue(ANDROID_NS, "icon", 0),
            enabled = getAttributeBooleanValue(ANDROID_NS, "enabled", true)
        )
    }

    private fun XmlResourceParser.intent(packageName: String): Intent {
        val intent = Intent(getAttributeValue(ANDROID_NS, "action") ?: Intent.ACTION_VIEW)
        val targetPackage = getAttributeValue(ANDROID_NS, "targetPackage")
        val targetClass = getAttributeValue(ANDROID_NS, "targetClass")
        if (targetClass != null) {
            intent.component = ComponentName(targetPackage ?: packageName, targetClass)
        } else {
            targetPackage?.let(intent::setPackage)
        }
        val data = getAttributeValue(ANDROID_NS, "data")?.let(Uri::parse)
        return intent.setDataAndType(data, getAttributeValue(ANDROID_NS, "mimeType"))
    }

    private fun XmlResourceParser.attr(resources: Resources, name: String): String? {
        val reference = getAttributeResourceValue(ANDROID_NS, name, 0)
        if (reference == 0) return getAttributeValue(ANDROID_NS, name)
        return try {
            resources.getString(reference)
        } catch (e: Exception) {
            null
        }
    }
}
