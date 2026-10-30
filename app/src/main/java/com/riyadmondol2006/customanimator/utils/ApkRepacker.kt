package com.riyadmondol2006.customanimator.utils

import java.io.File
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal object ApkRepacker {
    private const val LOCAL_HEADER_SIZE = 30
    private const val ALIGNMENT_EXTRA_ID = 0xD935
    private const val ALIGNMENT_EXTRA_HEADER = 6
    private const val DEFAULT_ALIGNMENT = 4
    private const val LIBRARY_ALIGNMENT = 16384
    private const val LIBRARY_SUFFIX = ".so"
    private const val ENTRY_TIME = 1230768000000L
    private const val BUFFER_SIZE = 64 * 1024
    private val SIGNATURE_FILE = Regex("META-INF/([^/]+\\.(SF|RSA|DSA|EC)|MANIFEST\\.MF)", RegexOption.IGNORE_CASE)

    private class CountingStream(target: OutputStream) : FilterOutputStream(target) {
        var count = 0L

        override fun write(value: Int) {
            out.write(value)
            count++
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            out.write(bytes, offset, length)
            count += length
        }
    }

    fun readEntry(apk: File, name: String): ByteArray? =
        ZipFile(apk).use { zip ->
            zip.getEntry(name)?.let { entry -> zip.getInputStream(entry).use { it.readBytes() } }
        }

    fun repack(source: File, target: File, replacements: Map<String, ByteArray>, additions: Map<String, ByteArray>) {
        ZipFile(source).use { zip ->
            val counter = CountingStream(target.outputStream().buffered(BUFFER_SIZE))
            ZipOutputStream(counter).use { output ->
                zip.entries().asSequence()
                    .filterNot { it.isDirectory || it.name in additions || SIGNATURE_FILE.matches(it.name) }
                    .forEach { entry -> copyEntry(zip, entry, replacements[entry.name], output, counter) }
                additions.forEach { (name, bytes) -> putStored(output, counter, name, bytes) }
            }
        }
    }

    private fun copyEntry(zip: ZipFile, entry: ZipEntry, replacement: ByteArray?, output: ZipOutputStream, counter: CountingStream) {
        val stored = entry.method == ZipEntry.STORED
        if (replacement != null && stored) {
            putStored(output, counter, entry.name, replacement)
            return
        }
        val copy = ZipEntry(entry.name)
        copy.time = ENTRY_TIME
        copy.method = entry.method
        if (stored) describeStored(copy, entry.size, entry.crc, counter.count)
        output.putNextEntry(copy)
        if (replacement != null) output.write(replacement) else zip.getInputStream(entry).use { it.copyTo(output, BUFFER_SIZE) }
        output.closeEntry()
    }

    private fun putStored(output: ZipOutputStream, counter: CountingStream, name: String, bytes: ByteArray) {
        val entry = ZipEntry(name)
        entry.time = ENTRY_TIME
        entry.method = ZipEntry.STORED
        describeStored(entry, bytes.size.toLong(), CRC32().apply { update(bytes) }.value, counter.count)
        output.putNextEntry(entry)
        output.write(bytes)
        output.closeEntry()
    }

    private fun describeStored(entry: ZipEntry, size: Long, crc: Long, headerOffset: Long) {
        entry.size = size
        entry.compressedSize = size
        entry.crc = crc
        entry.extra = alignmentExtra(headerOffset, entry.name)
    }

    private fun alignmentExtra(headerOffset: Long, name: String): ByteArray? {
        val alignment = if (name.endsWith(LIBRARY_SUFFIX)) LIBRARY_ALIGNMENT else DEFAULT_ALIGNMENT
        val dataOffset = headerOffset + LOCAL_HEADER_SIZE + name.toByteArray(Charsets.UTF_8).size
        if (dataOffset % alignment == 0L) return null
        val padding = ((alignment - (dataOffset + ALIGNMENT_EXTRA_HEADER) % alignment) % alignment).toInt()
        val extra = littleEndianBuffer(ALIGNMENT_EXTRA_HEADER + padding)
        extra.putShort(ALIGNMENT_EXTRA_ID.toShort())
        extra.putShort((2 + padding).toShort())
        extra.putShort(alignment.toShort())
        return extra.array()
    }
}
