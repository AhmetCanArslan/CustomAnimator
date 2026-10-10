package com.arslan.customanimator.utils

import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

internal object ApkV2Signer {
    private const val END_OF_CENTRAL_DIRECTORY_SIZE = 22
    private const val END_OF_CENTRAL_DIRECTORY_MAGIC = 0x06054b50
    private const val DIRECTORY_SIZE_OFFSET = 12
    private const val DIRECTORY_OFFSET_OFFSET = 16
    private const val COMMENT_LENGTH_OFFSET = 20
    private const val UNSIGNED_INT_MASK = 0xFFFFFFFFL
    private const val CHUNK_SIZE = 1024 * 1024
    private const val CHUNK_PREFIX = 0xa5
    private const val TOP_LEVEL_PREFIX = 0x5a
    private const val SIGNATURE_SCHEME_V2_ID = 0x7109871a
    private const val ECDSA_WITH_SHA256 = 0x0201
    private const val DIGEST_ALGORITHM = "SHA-256"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private val SIGNING_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)

    private class Section(val offset: Long, val size: Long)

    fun sign(unsigned: File, output: File, key: PrivateKey, publicKey: PublicKey, certificate: ByteArray) {
        RandomAccessFile(unsigned, "r").use { file ->
            val endOffset = file.length() - END_OF_CENTRAL_DIRECTORY_SIZE
            val end = ByteArray(END_OF_CENTRAL_DIRECTORY_SIZE)
            file.seek(endOffset)
            file.readFully(end)
            val endBuffer = end.littleEndian()
            val directorySize = endBuffer.getInt(DIRECTORY_SIZE_OFFSET).toLong() and UNSIGNED_INT_MASK
            val directoryOffset = endBuffer.getInt(DIRECTORY_OFFSET_OFFSET).toLong() and UNSIGNED_INT_MASK
            require(endBuffer.getInt(0) == END_OF_CENTRAL_DIRECTORY_MAGIC)
            require(endBuffer.unsignedShort(COMMENT_LENGTH_OFFSET) == 0)
            require(directoryOffset + directorySize == endOffset)

            val contents = Section(0, directoryOffset)
            val directory = Section(directoryOffset, directorySize)
            val digest = contentDigest(file, listOf(contents, directory), end)
            val block = signingBlock(signer(digest, key, publicKey, certificate))

            endBuffer.putInt(DIRECTORY_OFFSET_OFFSET, (directoryOffset + block.size).toInt())
            output.outputStream().buffered(CHUNK_SIZE).use { stream ->
                copy(file, contents, stream)
                stream.write(block)
                copy(file, directory, stream)
                stream.write(end)
            }
        }
    }

    private fun contentDigest(file: RandomAccessFile, sections: List<Section>, end: ByteArray): ByteArray {
        val chunkCount = sections.sumOf { (it.size + CHUNK_SIZE - 1) / CHUNK_SIZE } + 1
        val topLevel = MessageDigest.getInstance(DIGEST_ALGORITHM)
        topLevel.update(TOP_LEVEL_PREFIX.toByte())
        topLevel.update(int(chunkCount.toInt()))
        val buffer = ByteArray(CHUNK_SIZE)
        sections.forEach { section ->
            var position = section.offset
            val limit = section.offset + section.size
            while (position < limit) {
                val length = minOf(CHUNK_SIZE.toLong(), limit - position).toInt()
                file.seek(position)
                file.readFully(buffer, 0, length)
                topLevel.update(chunkDigest(buffer, length))
                position += length
            }
        }
        topLevel.update(chunkDigest(end, end.size))
        return topLevel.digest()
    }

    private fun chunkDigest(bytes: ByteArray, length: Int): ByteArray {
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        digest.update(CHUNK_PREFIX.toByte())
        digest.update(int(length))
        digest.update(bytes, 0, length)
        return digest.digest()
    }

    private fun signer(digest: ByteArray, key: PrivateKey, publicKey: PublicKey, certificate: ByteArray): ByteArray {
        val signedData = concatBytes(
            listOf(
                prefixed(prefixed(int(ECDSA_WITH_SHA256) + prefixed(digest))),
                prefixed(prefixed(certificate)),
                int(0)
            )
        )
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(key)
            update(signedData)
            sign()
        }
        return concatBytes(
            listOf(
                prefixed(signedData),
                prefixed(prefixed(int(ECDSA_WITH_SHA256) + prefixed(signature))),
                prefixed(publicKey.encoded)
            )
        )
    }

    private fun signingBlock(signer: ByteArray): ByteArray {
        val value = prefixed(prefixed(signer))
        val blockSize = 8L + 4 + value.size + 8 + SIGNING_BLOCK_MAGIC.size
        val buffer = littleEndianBuffer((blockSize + 8).toInt())
        buffer.putLong(blockSize)
        buffer.putLong(4L + value.size)
        buffer.putInt(SIGNATURE_SCHEME_V2_ID)
        buffer.put(value)
        buffer.putLong(blockSize)
        buffer.put(SIGNING_BLOCK_MAGIC)
        return buffer.array()
    }

    private fun copy(file: RandomAccessFile, section: Section, stream: OutputStream) {
        val buffer = ByteArray(CHUNK_SIZE)
        var remaining = section.size
        file.seek(section.offset)
        while (remaining > 0) {
            val length = minOf(buffer.size.toLong(), remaining).toInt()
            file.readFully(buffer, 0, length)
            stream.write(buffer, 0, length)
            remaining -= length
        }
    }

    private fun int(value: Int): ByteArray = littleEndianBuffer(4).putInt(value).array()

    private fun prefixed(bytes: ByteArray): ByteArray = int(bytes.size) + bytes
}
