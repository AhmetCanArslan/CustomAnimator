package com.riyadmondol2006.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.riyadmondol2006.customanimator.data.ApkInstallerPackage
import java.io.File
import java.util.zip.ZipFile

object ApkInstallerManager {
    private const val TAG = "ApkInstallerManager"
    private const val PREFS_NAME = "apk_installer_prefs"
    private const val KEY_OPTIONS = "options"
    private const val WORK_DIR = "apk_installer"
    private const val STAGING_DIR = "/data/local/tmp/customanimator_apk_installer"
    private const val MANIFEST = "AndroidManifest.xml"
    private const val APK_EXTENSION = ".apk"
    private const val SOURCE_FILE = "source.apk"
    private const val FAILURE = "Failure"
    private const val BUFFER_SIZE = 64 * 1024

    enum class Option(val flags: List<String>, val minSdk: Int = Build.VERSION_CODES.N) {
        GRANT_PERMISSIONS(listOf("-g")),
        ALLOW_DOWNGRADE(listOf("-d")),
        PLAY_STORE_INSTALLER(listOf("-i", "com.android.vending")),
        ALLOW_LOW_TARGET_SDK(listOf("--bypass-low-target-sdk-block"), Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    }

    enum class Outcome { DONE, NO_SHIZUKU, INSTALL_BLOCKED, FAILED }

    class Result(val outcome: Outcome, val detail: String = "")

    fun options(): List<Option> = Option.entries.filter { Build.VERSION.SDK_INT >= it.minSdk }

    fun enabledOptions(context: Context): Set<Option> {
        val stored = prefs(context).getStringSet(KEY_OPTIONS, null).orEmpty()
        return options().filter { it.name in stored }.toSet()
    }

    fun setOption(context: Context, option: Option, enabled: Boolean) {
        val stored = enabledOptions(context).map { it.name }.toSet()
        val updated = if (enabled) stored + option.name else stored - option.name
        prefs(context).edit().putStringSet(KEY_OPTIONS, updated).apply()
    }

    fun load(context: Context, uris: List<Uri>): ApkInstallerPackage? {
        val directory = workDir(context)
        directory.deleteRecursively()
        return try {
            val apks = uris.flatMapIndexed { index, uri ->
                unpack(context.contentResolver, uri, File(directory, index.toString()))
            }
            describe(context, apks)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read APK files", e)
            null
        }
    }

    fun installBlocker(context: Context): Outcome? {
        if (!ShizukuHelper.hasShizukuPermission()) return Outcome.NO_SHIZUKU
        val restricted = context.getSystemService(UserManager::class.java)
            .hasUserRestriction(UserManager.DISALLOW_INSTALL_APPS)
        return if (restricted) Outcome.INSTALL_BLOCKED else null
    }

    fun install(context: Context, app: ApkInstallerPackage, options: Set<Option>): Result {
        installBlocker(context)?.let { return Result(it) }
        return try {
            val staged = ShellApkInstaller.stage(app.apks, STAGING_DIR) ?: return Result(Outcome.INSTALL_BLOCKED)
            val installed = ShellApkInstaller.install(staged, flags(options))
            if (installed.success) Result(Outcome.DONE) else Result(Outcome.FAILED, failureReason(installed.output))
        } finally {
            ShizukuHelper.executeShellCommand(arrayOf("rm", "-rf", STAGING_DIR))
        }
    }

    fun discard(context: Context) {
        workDir(context).deleteRecursively()
    }

    internal fun flags(options: Set<Option>): List<String> =
        Option.entries.filter { it in options }.flatMap { it.flags }

    internal fun failureReason(output: String): String =
        output.lineSequence().map { it.trim() }.firstOrNull { it.startsWith(FAILURE) } ?: output.trim()

    internal fun expand(source: File, directory: File): List<File> {
        ZipFile(source).use { zip ->
            if (zip.getEntry(MANIFEST) != null) return listOf(source)
            return zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(APK_EXTENSION, ignoreCase = true) }
                .mapIndexed { index, entry ->
                    File(directory, "$index$APK_EXTENSION").also { target ->
                        zip.getInputStream(entry).use { input ->
                            target.outputStream().use { input.copyTo(it, BUFFER_SIZE) }
                        }
                    }
                }
                .toList()
        }
    }

    private fun unpack(resolver: ContentResolver, uri: Uri, directory: File): List<File> {
        directory.mkdirs()
        val source = File(directory, SOURCE_FILE)
        requireNotNull(resolver.openInputStream(uri)).use { input ->
            source.outputStream().use { input.copyTo(it, BUFFER_SIZE) }
        }
        return expand(source, directory).also { if (source !in it) source.delete() }
    }

    private fun describe(context: Context, apks: List<File>): ApkInstallerPackage? {
        val packageManager = context.packageManager
        val base = apks.mapNotNull { InstalledAppsProvider.archiveInfo(context, it.absolutePath) }.singleOrNull() ?: return null
        val appInfo = base.applicationInfo ?: return null
        return ApkInstallerPackage(
            packageName = base.packageName,
            label = appInfo.loadLabel(packageManager).toString(),
            versionName = base.versionName.orEmpty(),
            installedVersion = installedVersion(packageManager, base.packageName),
            sizeBytes = apks.sumOf { it.length() },
            icon = ShortcutMaker.drawableIcon(appInfo.loadIcon(packageManager)),
            apks = apks
        )
    }

    private fun installedVersion(packageManager: PackageManager, packageName: String): String? {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun workDir(context: Context): File = File(requireNotNull(context.getExternalFilesDir(null)), WORK_DIR)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
