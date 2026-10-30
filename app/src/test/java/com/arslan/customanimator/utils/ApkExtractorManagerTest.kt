package com.arslan.customanimator.utils

import androidx.test.core.app.ApplicationProvider
import com.arslan.customanimator.data.ApkExtractorApp
import com.arslan.customanimator.data.ApkExtractorFormat
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApkExtractorManagerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun app(version: String, split: Boolean) =
        ApkExtractorApp("com.example.app", "Example", version, 0, split)

    @Test
    fun fileNameCarriesVersionAndMatchesTheContainer() {
        assertEquals(
            "com.example.app_1.2.3.apk",
            ApkExtractorManager.fileName(app("1.2.3", false), ApkExtractorFormat.BASE)
        )
        assertEquals(
            "com.example.app_1.2_beta_1.apks",
            ApkExtractorManager.fileName(app("1.2 beta/1", true), ApkExtractorFormat.BUNDLE)
        )
        assertEquals(
            "com.example.app_1.2_beta_1.apk",
            ApkExtractorManager.fileName(app("1.2 beta/1", true), ApkExtractorFormat.BASE)
        )
        assertEquals("com.example.app.apk", ApkExtractorManager.fileName(app("", false), ApkExtractorFormat.BASE))
        assertEquals("com.example.app_1.2.3", ApkExtractorManager.baseName(app("1.2.3", true)))
    }

    @Test
    fun onlySplitAppsOfferAFormatChoice() {
        assertEquals(listOf(ApkExtractorFormat.BASE), ApkExtractorManager.formats(app("1.0", false)))
        assertEquals(ApkExtractorFormat.entries.toList(), ApkExtractorManager.formats(app("1.0", true)))
    }

    @Test
    fun baseOnlyCopiesTheFirstApkOfASplitApp() {
        val base = folder.newFile("base.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val split = folder.newFile("split_config.en.apk").apply { writeBytes(byteArrayOf(4, 5)) }
        val output = ByteArrayOutputStream()

        ApkExtractorManager.writeTo(listOf(base, split), output, ApkExtractorFormat.BASE)

        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
    }

    @Test
    fun singleApkIsCopiedUnchanged() {
        val base = folder.newFile("base.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val output = ByteArrayOutputStream()

        ApkExtractorManager.writeTo(listOf(base), output, ApkExtractorFormat.BASE)

        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
    }

    @Test
    fun splitApksAreBundledIntoOneArchive() {
        val base = folder.newFile("base.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val split = folder.newFile("split_config.en.apk").apply { writeBytes(byteArrayOf(4, 5)) }
        val output = ByteArrayOutputStream()

        ApkExtractorManager.writeTo(listOf(base, split), output, ApkExtractorFormat.BUNDLE)

        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(output.toByteArray().inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entries[it.name] = zip.readBytes() }
        }
        assertEquals(listOf("base.apk", "split_config.en.apk"), entries.keys.toList())
        assertArrayEquals(byteArrayOf(4, 5), entries["split_config.en.apk"])
    }

    @Test
    fun missingPackageFailsWithoutThrowing() {
        ApkExtractorFormat.entries.forEach { format ->
            assertNull(ApkExtractorManager.extract(context(), app("1.0", true), format))
        }
    }

    @Test
    fun everythingLandsUnderTheDownloadFolder() {
        val split = app("1.0", true)
        assertEquals("Download/CustomAnimator", ApkExtractorManager.exportFolder(split, ApkExtractorFormat.BUNDLE))
        assertEquals("Download/CustomAnimator", ApkExtractorManager.exportFolder(split, ApkExtractorFormat.BASE))
        assertEquals(
            "Download/CustomAnimator/com.example.app_1.0",
            ApkExtractorManager.exportFolder(split, ApkExtractorFormat.SEPARATE)
        )
        assertFalse(ApkExtractorManager.needsStoragePermission(context()))
    }

    private fun context() = ApplicationProvider.getApplicationContext<android.content.Context>()
}
