package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Environment
import com.arslan.customanimator.data.DebloatAppInfo
import com.arslan.customanimator.data.DebloatState
import java.io.File
import java.util.Collections

object DebloatManager {

    private const val USER = "0"
    private const val ICON_CACHE_SIZE = 150
    private const val EXPORT_DIR = "CustomAnimator"

    private val SYSTEM_PARTITIONS = listOf("/system/", "/system_ext/", "/product/", "/vendor/", "/apex/", "/odm/")

    private val iconCache: MutableMap<String, Drawable> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Drawable>(0, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Drawable>): Boolean {
                return size > ICON_CACHE_SIZE
            }
        }
    )

    fun loadApps(context: Context): List<DebloatAppInfo> {
        if (!ShizukuHelper.hasShizukuPermission()) return emptyList()

        val paths = listPackagePaths()
        val installed = listPackages(arrayOf("pm", "list", "packages", "--user", USER))
        val disabled = listPackages(arrayOf("pm", "list", "packages", "-d", "--user", USER))
        if (paths.isEmpty() || installed.isEmpty()) return emptyList()

        val blocked = DebloatCatalog.protectedPackages(context)
        val visible = visibleApps(context)
        val stored = DebloatStore(context).all().associateBy { it.packageName }
        val remote = DebloatRemoteList.load(context, paths.keys)

        return (paths.keys + stored.keys)
            .filter { it != context.packageName }
            .map { packageName ->
                val installedApp = visible[packageName]
                val sources = Sources(stored, remote, installed, disabled, blocked)
                if (installedApp != null) {
                    fromInstalled(context, installedApp, paths[packageName], sources)
                } else {
                    fromArchive(context, packageName, paths[packageName], sources)
                }
            }
            .sortedBy { it.label.lowercase() }
    }

    fun loadIcon(context: Context, app: DebloatAppInfo): Drawable? {
        iconCache[app.packageName]?.let { return it }
        val apkPath = app.apkPath ?: return null
        val archive = archiveInfo(context, apkPath) ?: return null
        val appInfo = archive.applicationInfo ?: return null
        val icon = try {
            appInfo.loadIcon(context.packageManager)
        } catch (e: Exception) {
            null
        }
        icon?.let { iconCache[app.packageName] = it }
        return icon
    }

    fun exportApk(app: DebloatAppInfo): String? {
        val sources = if (app.state == DebloatState.REMOVED) {
            listOfNotNull(app.apkPath)
        } else {
            apkPaths(app.packageName)
        }
        if (sources.isEmpty()) return null

        val root = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            EXPORT_DIR
        )
        val singleFile = sources.size == 1
        val destination = if (singleFile) {
            File(root, app.packageName + ".apk")
        } else {
            File(root, app.packageName)
        }
        val targetDir = if (singleFile) root else destination
        if (!runShell(arrayOf("mkdir", "-p", targetDir.absolutePath))) return null

        sources.forEach { source ->
            val target = if (singleFile) destination.absolutePath else targetDir.absolutePath
            if (!runShell(arrayOf("cp", source, target))) return null
        }
        return destination.absolutePath
    }

    private fun apkPaths(packageName: String): List<String> {
        val result = ShizukuHelper.executeShellCommandWithOutput(
            arrayOf("pm", "path", "--user", USER, packageName)
        )
        if (!result.isSuccess) return emptyList()
        return result.output
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .filter { it.endsWith(".apk") }
            .toList()
    }

    private fun runShell(command: Array<String>): Boolean {
        return ShizukuHelper.executeShellCommandWithOutput(command).isSuccess
    }

    fun remove(context: Context, app: DebloatAppInfo): Boolean {
        if (app.isProtected) return false
        val done = runPackageCommand(arrayOf("pm", "uninstall", "-k", "--user", USER, app.packageName))
        if (done) DebloatStore(context).record(app.packageName, app.label, true)
        return done
    }

    fun disable(context: Context, app: DebloatAppInfo): Boolean {
        if (app.isProtected) return false
        val done = runPackageCommand(arrayOf("pm", "disable-user", "--user", USER, app.packageName))
        if (done) DebloatStore(context).record(app.packageName, app.label, false)
        return done
    }

    fun restore(context: Context, app: DebloatAppInfo): Boolean {
        val done = restorePackage(app.packageName, app.state == DebloatState.REMOVED)
        if (done) DebloatStore(context).forget(app.packageName)
        return done
    }

    fun restoreAll(context: Context): Int {
        val store = DebloatStore(context)
        var restored = 0
        store.all().forEach { record ->
            if (restorePackage(record.packageName, record.removed)) {
                store.forget(record.packageName)
                restored++
            }
        }
        return restored
    }

    fun changedPackageCount(context: Context): Int = DebloatStore(context).all().size

    private fun restorePackage(packageName: String, wasRemoved: Boolean): Boolean {
        if (wasRemoved &&
            !runPackageCommand(arrayOf("cmd", "package", "install-existing", "--user", USER, packageName))
        ) {
            return false
        }
        return runPackageCommand(arrayOf("pm", "enable", "--user", USER, packageName))
    }

    private fun runPackageCommand(command: Array<String>): Boolean {
        val result = ShizukuHelper.executeShellCommandWithOutput(command)
        if (!result.isSuccess) return false
        return !result.output.contains("Failure", ignoreCase = true)
    }

    private fun listPackagePaths(): Map<String, String> {
        val result = ShizukuHelper.executeShellCommandWithOutput(
            arrayOf("pm", "list", "packages", "-f", "-u", "--user", USER)
        )
        if (!result.isSuccess) return emptyMap()
        return result.output
            .lineSequence()
            .map { it.trim().removePrefix("package:") }
            .mapNotNull { line ->
                val separator = line.lastIndexOf('=')
                if (separator <= 0) return@mapNotNull null
                val path = line.substring(0, separator)
                val name = line.substring(separator + 1)
                if (name.contains('.') && !name.contains(' ')) name to path else null
            }
            .toMap()
    }

    private fun listPackages(command: Array<String>): Set<String> {
        val result = ShizukuHelper.executeShellCommandWithOutput(command)
        if (!result.isSuccess) return emptySet()
        return result.output
            .lineSequence()
            .mapNotNull { line -> line.trim().removePrefix("package:").takeIf { it.isNotBlank() } }
            .filter { it.contains('.') && !it.contains(' ') }
            .toSet()
    }

    private fun visibleApps(context: Context): Map<String, ApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS or
            PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS or
            PackageManager.MATCH_UNINSTALLED_PACKAGES
        val resolved = try {
            context.packageManager.queryIntentActivities(intent, flags)
        } catch (e: Exception) {
            emptyList()
        }
        return resolved.mapNotNull { it.activityInfo?.applicationInfo }.associateBy { it.packageName }
    }

    private data class Sources(
        val stored: Map<String, DebloatStore.Record>,
        val remote: Map<String, DebloatRemoteList.Entry>,
        val installed: Set<String>,
        val disabled: Set<String>,
        val blocked: Set<String>
    )

    private fun fromInstalled(
        context: Context,
        appInfo: ApplicationInfo,
        apkPath: String?,
        sources: Sources
    ): DebloatAppInfo {
        val packageManager = context.packageManager
        return buildAppInfo(
            packageName = appInfo.packageName,
            label = loadLabel(packageManager, appInfo),
            icon = loadInstalledIcon(packageManager, appInfo),
            apkPath = apkPath,
            isSystemApp = hasSystemFlag(appInfo),
            protection = DebloatCatalog.Protection(appInfo.packageName, appInfo.uid, appInfo.flags, null),
            sources = sources
        )
    }

    private fun fromArchive(
        context: Context,
        packageName: String,
        apkPath: String?,
        sources: Sources
    ): DebloatAppInfo {
        val archive = apkPath?.let { archiveInfo(context, it) }
        val archiveApp = archive?.applicationInfo
        val label = archiveApp?.let { loadLabel(context.packageManager, it) }
            ?: sources.stored[packageName]?.label
            ?: packageName
        return buildAppInfo(
            packageName = packageName,
            label = label,
            icon = null,
            apkPath = apkPath,
            isSystemApp = archiveApp?.let { hasSystemFlag(it) } ?: isOnSystemPartition(apkPath),
            protection = DebloatCatalog.Protection(
                packageName = packageName,
                uid = -1,
                flags = archiveApp?.flags ?: 0,
                sharedUserId = archive?.sharedUserId
            ),
            sources = sources
        )
    }

    private fun buildAppInfo(
        packageName: String,
        label: String,
        icon: Drawable?,
        apkPath: String?,
        isSystemApp: Boolean,
        protection: DebloatCatalog.Protection,
        sources: Sources
    ): DebloatAppInfo {
        val remoteEntry = sources.remote[packageName]
        val entry = DebloatCatalog.classify(packageName, isSystemApp)
        return DebloatAppInfo(
            packageName = packageName,
            label = label,
            icon = icon,
            apkPath = apkPath,
            isSystemApp = isSystemApp,
            state = stateOf(packageName, sources.installed, sources.disabled),
            risk = remoteEntry?.risk ?: entry.risk,
            group = remoteEntry?.group ?: entry.group,
            isProtected = DebloatCatalog.isProtected(protection, sources.blocked),
            isRestorable = isSystemApp,
            isChangedByApp = sources.stored.containsKey(packageName),
            description = remoteEntry?.description
        )
    }

    private fun archiveInfo(context: Context, apkPath: String): PackageInfo? {
        val archive = try {
            context.packageManager.getPackageArchiveInfo(apkPath, 0)
        } catch (e: Exception) {
            null
        } ?: return null
        archive.applicationInfo?.let {
            it.sourceDir = apkPath
            it.publicSourceDir = apkPath
        }
        return archive
    }

    private fun isOnSystemPartition(apkPath: String?): Boolean {
        if (apkPath == null) return false
        return SYSTEM_PARTITIONS.any { apkPath.startsWith(it) }
    }

    private fun stateOf(packageName: String, installed: Set<String>, disabled: Set<String>): DebloatState {
        if (disabled.contains(packageName)) return DebloatState.DISABLED
        if (installed.contains(packageName)) return DebloatState.ACTIVE
        return DebloatState.REMOVED
    }

    private fun hasSystemFlag(appInfo: ApplicationInfo): Boolean {
        val systemFlags = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        return (appInfo.flags and systemFlags) != 0
    }

    private fun loadLabel(packageManager: PackageManager, appInfo: ApplicationInfo): String {
        return try {
            appInfo.loadLabel(packageManager).toString().takeIf { it.isNotBlank() } ?: appInfo.packageName
        } catch (e: Exception) {
            appInfo.packageName
        }
    }

    private fun loadInstalledIcon(packageManager: PackageManager, appInfo: ApplicationInfo) = try {
        appInfo.loadIcon(packageManager)
    } catch (e: Exception) {
        null
    }
}
