package com.arslan.customanimator.utils

import java.nio.ByteBuffer

internal object AdaptiveIconXml {
    private const val XML_TYPE = 0x0003
    private const val XML_RESOURCE_MAP_TYPE = 0x0180
    private const val XML_START_NAMESPACE_TYPE = 0x0100
    private const val XML_END_NAMESPACE_TYPE = 0x0101
    private const val XML_START_ELEMENT_TYPE = 0x0102
    private const val XML_END_ELEMENT_TYPE = 0x0103
    private const val CHUNK_HEADER_SIZE = 8
    private const val NODE_HEADER_SIZE = 16
    private const val NODE_SIZE = 24
    private const val ELEMENT_SIZE = 36
    private const val ATTRIBUTE_SIZE = 20
    private const val VALUE_SIZE = 8
    private const val NONE = -1
    private const val ATTR_DRAWABLE = 0x01010199
    private const val DRAWABLE = 0
    private const val PREFIX = 1
    private const val NAMESPACE = 2
    private const val ADAPTIVE_ICON = 3
    private const val BACKGROUND = 4
    private const val FOREGROUND = 5
    private val STRINGS = listOf(
        "drawable",
        "android",
        "http://schemas.android.com/apk/res/android",
        "adaptive-icon",
        "background",
        "foreground"
    )

    fun build(backgroundId: Int, foregroundId: Int): ByteArray {
        val body = concatBytes(
            listOf(
                ApkStringPool.build(STRINGS),
                resourceMap(),
                namespace(XML_START_NAMESPACE_TYPE),
                startElement(ADAPTIVE_ICON, null),
                startElement(BACKGROUND, backgroundId),
                endElement(BACKGROUND),
                startElement(FOREGROUND, foregroundId),
                endElement(FOREGROUND),
                endElement(ADAPTIVE_ICON),
                namespace(XML_END_NAMESPACE_TYPE)
            )
        )
        val header = chunk(XML_TYPE, CHUNK_HEADER_SIZE, CHUNK_HEADER_SIZE)
        header.putInt(4, CHUNK_HEADER_SIZE + body.size)
        return header.array() + body
    }

    private fun chunk(type: Int, headerSize: Int, size: Int): ByteBuffer {
        val buffer = littleEndianBuffer(size)
        buffer.putShort(type.toShort())
        buffer.putShort(headerSize.toShort())
        buffer.putInt(size)
        return buffer
    }

    private fun node(type: Int, size: Int): ByteBuffer {
        val buffer = chunk(type, NODE_HEADER_SIZE, size)
        buffer.putInt(1)
        buffer.putInt(NONE)
        return buffer
    }

    private fun resourceMap(): ByteArray =
        chunk(XML_RESOURCE_MAP_TYPE, CHUNK_HEADER_SIZE, CHUNK_HEADER_SIZE + 4).putInt(ATTR_DRAWABLE).array()

    private fun namespace(type: Int): ByteArray = node(type, NODE_SIZE).putInt(PREFIX).putInt(NAMESPACE).array()

    private fun endElement(name: Int): ByteArray = node(XML_END_ELEMENT_TYPE, NODE_SIZE).putInt(NONE).putInt(name).array()

    private fun startElement(name: Int, drawableId: Int?): ByteArray {
        val attributeCount = if (drawableId == null) 0 else 1
        val buffer = node(XML_START_ELEMENT_TYPE, ELEMENT_SIZE + attributeCount * ATTRIBUTE_SIZE)
        buffer.putInt(NONE)
        buffer.putInt(name)
        buffer.putShort(ATTRIBUTE_SIZE.toShort())
        buffer.putShort(ATTRIBUTE_SIZE.toShort())
        buffer.putShort(attributeCount.toShort())
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.putShort(0)
        if (drawableId != null) {
            buffer.putInt(NAMESPACE)
            buffer.putInt(DRAWABLE)
            buffer.putInt(NONE)
            buffer.putShort(VALUE_SIZE.toShort())
            buffer.put(0)
            buffer.put(RES_VALUE_TYPE_REFERENCE.toByte())
            buffer.putInt(drawableId)
        }
        return buffer.array()
    }
}
