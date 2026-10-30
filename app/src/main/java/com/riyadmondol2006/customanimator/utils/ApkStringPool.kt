package com.riyadmondol2006.customanimator.utils

internal class ApkStringPool(private val chunk: ByteArray) {
    private val buffer = chunk.littleEndian()
    private val headerSize = buffer.unsignedShort(2)
    private val styleCount = buffer.getInt(12)
    private val flags = buffer.getInt(16)
    private val stringsStart = buffer.getInt(20)
    private val stylesStart = buffer.getInt(24)
    private val utf8 = flags and UTF8_FLAG != 0

    val stringCount: Int = buffer.getInt(8)

    fun get(index: Int): String? {
        if (index !in 0 until stringCount) return null
        val start = stringsStart + buffer.getInt(headerSize + index * 4)
        return if (utf8) readUtf8(start) else readUtf16(start)
    }

    fun appended(values: List<String>): ByteArray {
        val stringDataEnd = if (styleCount > 0 && stylesStart != 0) stylesStart else chunk.size
        val oldData = chunk.copyOfRange(stringsStart, stringDataEnd)
        val encoded = values.map { encode(it, utf8) }
        val addedSize = encoded.sumOf { it.size }
        val data = concatBytes(listOf(oldData) + encoded).copyOf(alignedSize(oldData.size + addedSize))
        val styleData = chunk.copyOfRange(stringDataEnd, chunk.size)
        val newStringsStart = headerSize + (stringCount + values.size + styleCount) * 4
        val output = littleEndianBuffer(newStringsStart + data.size + styleData.size)

        output.put(chunk, 0, headerSize)
        output.put(chunk, headerSize, stringCount * 4)
        var offset = oldData.size
        encoded.forEach {
            output.putInt(offset)
            offset += it.size
        }
        output.put(chunk, headerSize + stringCount * 4, styleCount * 4)
        output.put(data)
        output.put(styleData)

        output.putInt(4, output.capacity())
        output.putInt(8, stringCount + values.size)
        output.putInt(16, flags and SORTED_FLAG.inv())
        output.putInt(20, newStringsStart)
        output.putInt(24, if (styleData.isEmpty()) 0 else newStringsStart + data.size)
        return output.array()
    }

    private fun readUtf8(start: Int): String {
        var position = start
        position += if (buffer.unsignedByte(position) and 0x80 != 0) 2 else 1
        val first = buffer.unsignedByte(position)
        val wide = first and 0x80 != 0
        val length = if (wide) ((first and 0x7F) shl 8) or buffer.unsignedByte(position + 1) else first
        position += if (wide) 2 else 1
        return String(chunk, position, length, Charsets.UTF_8)
    }

    private fun readUtf16(start: Int): String {
        val first = buffer.unsignedShort(start)
        val wide = first and 0x8000 != 0
        val length = if (wide) ((first and 0x7FFF) shl 16) or buffer.unsignedShort(start + 2) else first
        return String(chunk, start + if (wide) 4 else 2, length * 2, Charsets.UTF_16LE)
    }

    companion object {
        private const val HEADER_SIZE = 28
        private const val UTF8_FLAG = 0x100
        private const val SORTED_FLAG = 0x1
        private const val MAX_SHORT_LENGTH = 0x7F

        fun build(values: List<String>): ByteArray {
            val encoded = values.map { encode(it, true) }
            val stringsStart = HEADER_SIZE + values.size * 4
            val dataSize = alignedSize(encoded.sumOf { it.size })
            val output = littleEndianBuffer(stringsStart + dataSize)
            output.putShort(RES_STRING_POOL_TYPE.toShort())
            output.putShort(HEADER_SIZE.toShort())
            output.putInt(output.capacity())
            output.putInt(values.size)
            output.putInt(0)
            output.putInt(UTF8_FLAG)
            output.putInt(stringsStart)
            output.putInt(0)
            var offset = 0
            encoded.forEach {
                output.putInt(offset)
                offset += it.size
            }
            encoded.forEach(output::put)
            return output.array()
        }

        private fun encode(value: String, utf8: Boolean): ByteArray {
            require(value.length <= MAX_SHORT_LENGTH && value.all { it.code <= MAX_SHORT_LENGTH })
            if (utf8) {
                val length = value.length.toByte()
                return byteArrayOf(length, length) + value.toByteArray(Charsets.US_ASCII) + 0
            }
            val output = littleEndianBuffer(value.length * 2 + 4)
            output.putShort(value.length.toShort())
            output.put(value.toByteArray(Charsets.UTF_16LE))
            return output.array()
        }
    }
}
