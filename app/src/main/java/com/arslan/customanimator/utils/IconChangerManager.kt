package com.arslan.customanimator.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.os.UserManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.core.graphics.createBitmap
import com.arslan.customanimator.data.ShortcutIconImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.spec.ECGenParameterSpec
import javax.security.auth.x500.X500Principal

object IconChangerManager {
    private const val TAG = "IconChangerManager"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "icon_changer_signer"
    private const val KEY_CURVE = "secp256r1"
    private const val CERTIFICATE_SUBJECT = "CN=Custom Animator Icon Changer"
    private const val WORK_DIR = "icon_changer"
    private const val UNSIGNED_DIR = "unsigned"
    private const val SIGNED_DIR = "signed"
    private const val STAGING_DIR = "/data/local/tmp/customanimator_icon_changer"
    private const val STAGED_PATCHED = "patched"
    private const val STAGED_ORIGINAL = "original"
    private const val EXPORT_SUFFIX = "-icon"
    private const val APK_EXTENSION = ".apk"
    private const val SHELL_SUCCESS = "Success"
    private const val LAYER_PX = 432
    private const val VISIBLE_FRACTION = 72f / 108f
    private const val PNG_QUALITY = 100
    private val SESSION_ID = Regex("\\[(\\d+)]")

    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 1f

    val backgroundColors: List<Int> = listOf(Color.WHITE) + ShortcutMaker.glyphColors

    enum class Step { REBUILDING, INSTALLING }

    enum class Outcome { DONE, BUILD_FAILED, NO_SHIZUKU, INSTALL_BLOCKED, ORIGINAL_RESTORED, APP_LOST }

    private class StagedApk(val path: String, val size: Long)

    private class SigningIdentity(val key: PrivateKey, val certificate: Certificate)

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    fun foregroundLayer(image: ShortcutIconImage, scale: Float): Bitmap {
        val layer = createBitmap(LAYER_PX, LAYER_PX)
        val side = if (image.legacy) LAYER_PX * VISIBLE_FRACTION * scale.coerceIn(MIN_SCALE, MAX_SCALE) else LAYER_PX.toFloat()
        val inset = (LAYER_PX - side) / 2f
        Canvas(layer).drawBitmap(
            image.bitmap,
            null,
            RectF(inset, inset, inset + side, inset + side),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return layer
    }

    fun keepsData(context: Context, packageName: String): Boolean {
        return try {
            val certificate = keyStore().getCertificate(KEY_ALIAS)?.encoded ?: return false
            installedSigners(context, packageName).any { it.contentEquals(certificate) }
        } catch (e: Exception) {
            false
        }
    }

    fun installBlocker(context: Context, packageName: String): Outcome? {
        if (!ShizukuHelper.hasShizukuPermission()) return Outcome.NO_SHIZUKU
        val userManager = context.getSystemService(UserManager::class.java)
        val installRestricted = userManager.hasUserRestriction(UserManager.DISALLOW_INSTALL_APPS)
        val uninstallRestricted = !keepsData(context, packageName) &&
            userManager.hasUserRestriction(UserManager.DISALLOW_UNINSTALL_APPS)
        return if (installRestricted || uninstallRestricted) Outcome.INSTALL_BLOCKED else null
    }

    fun changeIcon(
        context: Context,
        packageName: String,
        layer: Bitmap,
        backgroundColor: Int,
        onStep: (Step) -> Unit
    ): Outcome {
        installBlocker(context, packageName)?.let { return it }
        onStep(Step.REBUILDING)
        val keepsData = keepsData(context, packageName)
        val originals = sourceApks(context, packageName)
        return try {
            val patched = build(context, originals, layer, backgroundColor) ?: return Outcome.BUILD_FAILED
            onStep(Step.INSTALLING)
            installBlocker(context, packageName) ?: install(packageName, originals, patched, keepsData)
        } finally {
            cleanUp(context)
        }
    }

    fun exportApk(context: Context, packageName: String, layer: Bitmap, backgroundColor: Int): String? {
        if (!ShizukuHelper.hasShizukuPermission()) return null
        return try {
            build(context, sourceApks(context, packageName), layer, backgroundColor)?.let { export(packageName, it) }
        } finally {
            cleanUp(context)
        }
    }

    private fun build(context: Context, originals: List<File>, layer: Bitmap, backgroundColor: Int): List<File>? {
        return try {
            val workDir = File(requireNotNull(context.getExternalFilesDir(null)), WORK_DIR)
            workDir.deleteRecursively()
            val unsignedDir = File(workDir, UNSIGNED_DIR).apply { mkdirs() }
            val signedDir = File(workDir, SIGNED_DIR).apply { mkdirs() }
            val identity = signingIdentity()
            ApkIconPatcher.patch(originals, unsignedDir, png(layer), backgroundColor).map { unsigned ->
                val signed = File(signedDir, unsigned.name)
                ApkV2Signer.sign(unsigned, signed, identity.key, identity.certificate.publicKey, identity.certificate.encoded)
                unsigned.delete()
                signed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rebuild APK", e)
            null
        }
    }

    private fun install(packageName: String, originals: List<File>, patched: List<File>, keepsData: Boolean): Outcome {
        val stagedPatched = stage(patched, STAGED_PATCHED) ?: return Outcome.INSTALL_BLOCKED
        if (keepsData) return if (installSession(stagedPatched)) Outcome.DONE else Outcome.INSTALL_BLOCKED
        val stagedOriginals = stage(originals, STAGED_ORIGINAL) ?: return Outcome.INSTALL_BLOCKED
        if (!installSession(stagedOriginals)) return Outcome.INSTALL_BLOCKED
        if (!ShizukuHelper.hasShizukuPermission()) return Outcome.NO_SHIZUKU
        if (!succeeded(arrayOf("pm", "uninstall", packageName))) return Outcome.INSTALL_BLOCKED
        if (installSession(stagedPatched)) return Outcome.DONE
        return if (installSession(stagedOriginals)) Outcome.ORIGINAL_RESTORED else Outcome.APP_LOST
    }

    private fun stage(files: List<File>, name: String): List<StagedApk>? {
        val directory = "$STAGING_DIR/$name"
        if (!ShizukuHelper.executeShellCommand(arrayOf("mkdir", "-p", directory))) return null
        return files.mapIndexed { index, file ->
            val target = "$directory/$index$APK_EXTENSION"
            if (!ShizukuHelper.executeShellCommand(arrayOf("cp", file.absolutePath, target))) return null
            StagedApk(target, file.length())
        }
    }

    private fun installSession(apks: List<StagedApk>): Boolean {
        val created = ShizukuHelper.executeShellCommandWithOutput(arrayOf("pm", "install-create", "-r"))
        if (!created.isSuccess) return false
        val session = SESSION_ID.find(created.output)?.groupValues?.get(1) ?: return false
        apks.forEachIndexed { index, apk ->
            val written = succeeded(
                arrayOf("pm", "install-write", "-S", apk.size.toString(), session, "$index$APK_EXTENSION", apk.path)
            )
            if (!written) {
                ShizukuHelper.executeShellCommand(arrayOf("pm", "install-abandon", session))
                return false
            }
        }
        return succeeded(arrayOf("pm", "install-commit", session))
    }

    private fun succeeded(command: Array<String>): Boolean {
        val result = ShizukuHelper.executeShellCommandWithOutput(command)
        return result.isSuccess && result.output.contains(SHELL_SUCCESS)
    }

    private fun export(packageName: String, patched: List<File>): String? {
        val root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), InstalledAppsProvider.EXPORT_DIR)
        val single = patched.size == 1
        val destination = File(root, packageName + EXPORT_SUFFIX + if (single) APK_EXTENSION else "")
        val directory = if (single) root else destination
        if (!ShizukuHelper.executeShellCommand(arrayOf("mkdir", "-p", directory.absolutePath))) return null
        patched.forEach { apk ->
            val target = if (single) destination else File(directory, apk.name)
            if (!ShizukuHelper.executeShellCommand(arrayOf("cp", apk.absolutePath, target.absolutePath))) return null
        }
        return destination.absolutePath
    }

    private fun cleanUp(context: Context) {
        ShizukuHelper.executeShellCommand(arrayOf("rm", "-rf", STAGING_DIR))
        context.getExternalFilesDir(null)?.let { File(it, WORK_DIR).deleteRecursively() }
    }

    private fun sourceApks(context: Context, packageName: String): List<File> {
        return try {
            InstalledAppsProvider.sourceApks(context, packageName)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun png(layer: Bitmap): ByteArray {
        val output = ByteArrayOutputStream()
        layer.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
        return output.toByteArray()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun signingIdentity(): SigningIdentity {
        val keyStore = keyStore()
        if (!keyStore.containsAlias(KEY_ALIAS)) generateKey()
        return SigningIdentity(keyStore.getKey(KEY_ALIAS, null) as PrivateKey, keyStore.getCertificate(KEY_ALIAS))
    }

    private fun generateKey() {
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec(KEY_CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setCertificateSubject(X500Principal(CERTIFICATE_SUBJECT))
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE).apply { initialize(spec) }.generateKeyPair()
    }

    @Suppress("DEPRECATION")
    private fun installedSigners(context: Context, packageName: String): List<ByteArray> {
        val packageManager = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
        } else {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        }
        return signatures.orEmpty().map { it.toByteArray() }
    }
}
