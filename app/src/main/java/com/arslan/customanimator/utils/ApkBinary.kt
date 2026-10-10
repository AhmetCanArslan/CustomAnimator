package com.arslan.customanimator.utils

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal const val RES_STRING_POOL_TYPE = 0x0001
internal const val RES_VALUE_TYPE_REFERENCE = 0x01
internal const val RES_VALUE_TYPE_STRING = 0x03
internal const val RES_VALUE_TYPE_COLOR = 0x1c

internal fun ByteArray.littleEndian(): ByteBuffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)

internal fun littleEndianBuffer(size: Int): ByteBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

internal fun ByteBuffer.unsignedShort(index: Int): Int = getShort(index).toInt() and 0xFFFF

internal fun ByteBuffer.unsignedByte(index: Int): Int = get(index).toInt() and 0xFF

internal fun concatBytes(parts: List<ByteArray>): ByteArray {
    val output = ByteArrayOutputStream(parts.sumOf { it.size })
    parts.forEach(output::write)
    return output.toByteArray()
}

internal fun alignedSize(size: Int): Int = (size + 3) and 3.inv()
