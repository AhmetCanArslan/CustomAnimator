package com.arslan.customanimator.utils

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkIconPatcherTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val sampleIcon = 0x7f030000
    private val foreground = byteArrayOf(1, 2, 3, 4, 5)

    private fun resource(name: String): File {
        val target = folder.newFile()
        javaClass.classLoader!!.getResourceAsStream("icon_changer/$name")!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        return target
    }

    private fun sampleApk(): File = resource("sample.apk").let { file ->
        File(file.parentFile, "base.apk").also { file.renameTo(it) }
    }

    private fun patchedSample(): File =
        ApkIconPatcher.patch(listOf(sampleApk()), folder.newFolder(), foreground, 0xFF336699.toInt()).single()

    @Test
    fun stringPoolKeepsOldStringsAndAppendsNewOnes() {
        val pool = ApkStringPool(ApkStringPool.build(listOf("alpha", "res/icon.png")))
        val grown = ApkStringPool(pool.appended(listOf("res/new.xml", "x")))

        assertEquals(4, grown.stringCount)
        assertEquals("alpha", grown.get(0))
        assertEquals("res/icon.png", grown.get(1))
        assertEquals("res/new.xml", grown.get(2))
        assertEquals("x", grown.get(3))
        assertNull(grown.get(4))
    }

    @Test
    fun iconIdsComeFromTheApplicationAndLauncherActivities() {
        assertEquals(setOf(sampleIcon), ApkIconPatcher.iconIds(sampleApk()))
    }

    @Test
    fun resourceTableGainsLayerTypeAndRepointsEveryIconConfig() {
        val table = ApkRepacker.readEntry(sampleApk(), "resources.arsc")!!
        val patched = ApkResourceTable.withIconLayers(table, setOf(sampleIcon), 0xFF336699.toInt())

        assertEquals(2, patched.repointedEntries)
        assertEquals(0x7f050000, patched.foregroundId)
        assertEquals(0x7f050001, patched.backgroundId)
        assertEquals(patched.bytes.size, patched.bytes.littleEndian().getInt(4))
        assertEquals(0, ApkResourceTable.withIconEntries(table.copyOf(), setOf(0x7f090000)).repointedEntries)
    }

    @Test
    fun patchedApkKeepsEntriesAddsIconFilesAndStaysAligned() {
        val patched = patchedSample()

        ZipFile(patched).use { zip ->
            assertNotNull(zip.getEntry("AndroidManifest.xml"))
            assertNotNull(zip.getEntry("res/drawable/fg.xml"))
            assertNotNull(zip.getEntry(ApkResourceTable.ICON_XML_PATH))
            assertArrayEquals(
                foreground,
                zip.getInputStream(zip.getEntry(ApkResourceTable.FOREGROUND_PATH)).use { it.readBytes() }
            )
            assertEquals(ZipEntry.STORED, zip.getEntry("resources.arsc").method)
        }
        assertEquals(setOf(sampleIcon), ApkIconPatcher.iconIds(patched))
        storedDataOffsets(patched).forEach { assertEquals(0L, it % 4) }
    }

    @Test
    fun repackDropsOldSignatureFiles() {
        val source = folder.newFile("signed.apk")
        java.util.zip.ZipOutputStream(source.outputStream()).use { zip ->
            listOf("META-INF/MANIFEST.MF", "META-INF/CERT.SF", "META-INF/CERT.RSA", "META-INF/services/x", "a.txt").forEach {
                zip.putNextEntry(ZipEntry(it))
                zip.write(it.toByteArray())
                zip.closeEntry()
            }
        }
        val target = folder.newFile("unsigned.apk")
        ApkRepacker.repack(source, target, emptyMap(), emptyMap())

        ZipFile(target).use { zip ->
            assertEquals(listOf("META-INF/services/x", "a.txt"), zip.entries().asSequence().map { it.name }.toList())
        }
    }

    @Test
    fun signedApkCarriesVerifiableV2Signature() {
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            resource("signer.p12").inputStream().use { load(it, "android".toCharArray()) }
        }
        val key = keyStore.getKey("signer", "android".toCharArray()) as PrivateKey
        val certificate = keyStore.getCertificate("signer") as X509Certificate
        val unsigned = patchedSample()
        val signed = folder.newFile("signed.apk")

        ApkV2Signer.sign(unsigned, signed, key, certificate.publicKey, certificate.encoded)

        ZipFile(signed).use { assertNotNull(it.getEntry("resources.arsc")) }
        val block = signingBlock(signed)
        val signer = block.nested(block.int(), block.int())
        val signedData = signer.bytes(signer.int())
        val signature = signer.nested(signer.int(), signer.int()).let {
            it.int()
            it.bytes(it.int())
        }
        val verifier = Signature.getInstance("SHA256withECDSA").apply {
            initVerify(certificate.publicKey)
            update(signedData)
        }
        assertTrue(verifier.verify(signature))
        assertFalse(signed.length() == unsigned.length())
    }

    private fun ByteBuffer.int(): Int = getInt()

    private fun ByteBuffer.bytes(length: Int): ByteArray = ByteArray(length).also { get(it) }

    private fun ByteBuffer.nested(outerLength: Int, innerLength: Int): ByteBuffer {
        assertTrue(innerLength <= outerLength)
        return bytes(innerLength).littleEndian()
    }

    private fun signingBlock(apk: File): ByteBuffer {
        val bytes = apk.readBytes()
        val buffer = bytes.littleEndian()
        val directoryOffset = buffer.getInt(bytes.size - 6)
        assertEquals("APK Sig Block 42", String(bytes, directoryOffset - 16, 16))
        val blockSize = buffer.getLong(directoryOffset - 24).toInt()
        val blockStart = directoryOffset - blockSize - 8
        assertEquals(blockSize.toLong(), buffer.getLong(blockStart))
        assertEquals(0x7109871a, buffer.getInt(blockStart + 16))
        val valueSize = buffer.getLong(blockStart + 8).toInt() - 4
        return bytes.copyOfRange(blockStart + 20, blockStart + 20 + valueSize).littleEndian()
    }

    private fun storedDataOffsets(apk: File): List<Long> {
        val names = ZipFile(apk).use { zip ->
            zip.entries().asSequence().filter { it.method == ZipEntry.STORED }.map { it.name }.toSet()
        }
        val offsets = mutableListOf<Long>()
        RandomAccessFile(apk, "r").use { file ->
            val bytes = ByteArray(file.length().toInt()).also { file.readFully(it) }
            val buffer = bytes.littleEndian()
            var position = buffer.getInt(bytes.size - 6)
            val count = buffer.unsignedShort(bytes.size - 12)
            repeat(count) {
                val nameLength = buffer.unsignedShort(position + 28)
                val extraLength = buffer.unsignedShort(position + 30)
                val commentLength = buffer.unsignedShort(position + 32)
                val header = buffer.getInt(position + 42)
                val name = String(bytes, position + 46, nameLength)
                if (name in names) {
                    offsets += header + 30L + buffer.unsignedShort(header + 26) + buffer.unsignedShort(header + 28)
                }
                position += 46 + nameLength + extraLength + commentLength
            }
        }
        assertTrue(offsets.isNotEmpty())
        return offsets
    }
}
