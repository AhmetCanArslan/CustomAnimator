package com.riyadmondol2006.customanimator.utils

import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApkInstallerManagerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun sampleApk(): File {
        val target = folder.newFile("source.apk")
        javaClass.classLoader!!.getResourceAsStream("icon_changer/sample.apk")!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        return target
    }

    private fun bundle(entries: Map<String, ByteArray>): File {
        val target = folder.newFile("bundle.apks")
        ZipOutputStream(target.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return target
    }

    @Test
    fun aPlainApkIsInstalledAsIs() {
        val apk = sampleApk()

        assertEquals(listOf(apk), ApkInstallerManager.expand(apk, folder.root))
    }

    @Test
    fun aBundleIsUnpackedIntoItsApksOnly() {
        val source = bundle(
            mapOf(
                "base.apk" to byteArrayOf(1, 2),
                "splits/split_config.en.APK" to byteArrayOf(3),
                "icon.png" to byteArrayOf(4),
                "manifest.json" to byteArrayOf(5),
                "Android/obb/com.example/main.obb" to byteArrayOf(6)
            )
        )
        val directory = folder.newFolder("unpacked")

        val apks = ApkInstallerManager.expand(source, directory)

        assertEquals(listOf("0.apk", "1.apk"), apks.map { it.name })
        assertTrue(apks.all { it.parentFile == directory })
        assertArrayEquals(byteArrayOf(1, 2), apks[0].readBytes())
        assertArrayEquals(byteArrayOf(3), apks[1].readBytes())
    }

    @Test
    fun entryPathsNeverEscapeTheWorkFolder() {
        val source = bundle(mapOf("../../evil.apk" to byteArrayOf(7)))
        val directory = folder.newFolder("unpacked")

        val apks = ApkInstallerManager.expand(source, directory)

        assertEquals(listOf(File(directory, "0.apk")), apks)
    }

    @Test
    fun optionsBecomeInstallCreateFlagsInDeclarationOrder() {
        val flags = ApkInstallerManager.flags(
            setOf(ApkInstallerManager.Option.PLAY_STORE_INSTALLER, ApkInstallerManager.Option.GRANT_PERMISSIONS)
        )

        assertEquals(listOf("-g", "-i", "com.android.vending"), flags)
        assertEquals(emptyList<String>(), ApkInstallerManager.flags(emptySet()))
    }

    @Test
    fun theFailureLineIsReportedWhenPmPrintsOne() {
        assertEquals(
            "Failure [INSTALL_FAILED_VERSION_DOWNGRADE: Downgrade detected]",
            ApkInstallerManager.failureReason(
                "Success: streamed 1024 bytes\n  Failure [INSTALL_FAILED_VERSION_DOWNGRADE: Downgrade detected]\n"
            )
        )
        assertEquals("Error: no session", ApkInstallerManager.failureReason("  Error: no session \n"))
    }

    @Test
    fun enabledOptionsRoundTrip() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        ApkInstallerManager.setOption(context, ApkInstallerManager.Option.GRANT_PERMISSIONS, true)
        ApkInstallerManager.setOption(context, ApkInstallerManager.Option.ALLOW_DOWNGRADE, true)
        ApkInstallerManager.setOption(context, ApkInstallerManager.Option.ALLOW_DOWNGRADE, false)

        assertEquals(setOf(ApkInstallerManager.Option.GRANT_PERMISSIONS), ApkInstallerManager.enabledOptions(context))
    }
}
