package com.arslan.customanimator.utils

import java.nio.ByteBuffer

internal object ApkManifestIcons {
    private const val XML_RESOURCE_MAP_TYPE = 0x0180
    private const val XML_START_ELEMENT_TYPE = 0x0102
    private const val XML_END_ELEMENT_TYPE = 0x0103
    private const val XML_HEADER_SIZE = 8
    private const val ELEMENT_NAME_OFFSET = 20
    private const val ATTRIBUTES_BASE = 16
    private const val ATTRIBUTE_START_OFFSET = 24
    private const val ATTRIBUTE_SIZE_OFFSET = 26
    private const val ATTRIBUTE_COUNT_OFFSET = 28
    private const val ATTR_ICON = 0x01010002
    private const val ATTR_NAME = 0x01010003
    private const val ATTR_ROUND_ICON = 0x0101052c
    private const val FRAMEWORK_PACKAGE = 0x01
    private const val LAUNCHER_CATEGORY = "android.intent.category.LAUNCHER"
    private const val APPLICATION_TAG = "application"
    private const val CATEGORY_TAG = "category"
    private val COMPONENT_TAGS = setOf("activity", "activity-alias")

    private class Attribute(val resourceId: Int, val type: Int, val data: Int)

    private class Walk {
        var strings: ApkStringPool? = null
        var resourceIds: IntArray = IntArray(0)
        val icons = LinkedHashSet<Int>()
        var componentIcons: List<Int> = emptyList()
        var componentIsLauncher = false
    }

    fun read(manifest: ByteArray): Set<Int> {
        val buffer = manifest.littleEndian()
        val walk = Walk()
        var offset = XML_HEADER_SIZE
        while (offset + XML_HEADER_SIZE <= manifest.size) {
            val size = buffer.getInt(offset + 4)
            if (size <= 0) break
            visit(manifest, buffer, offset, size, walk)
            offset += size
        }
        return walk.icons.filterTo(LinkedHashSet()) { it ushr 24 != FRAMEWORK_PACKAGE }
    }

    private fun visit(manifest: ByteArray, buffer: ByteBuffer, offset: Int, size: Int, walk: Walk) {
        when (buffer.unsignedShort(offset)) {
            RES_STRING_POOL_TYPE -> walk.strings = ApkStringPool(manifest.copyOfRange(offset, offset + size))
            XML_RESOURCE_MAP_TYPE -> walk.resourceIds = resourceMap(buffer, offset, size)
            XML_START_ELEMENT_TYPE -> startElement(buffer, offset, walk)
            XML_END_ELEMENT_TYPE -> endElement(elementName(buffer, offset, walk), walk)
        }
    }

    private fun resourceMap(buffer: ByteBuffer, offset: Int, size: Int): IntArray {
        val headerSize = buffer.unsignedShort(offset + 2)
        return IntArray((size - headerSize) / 4) { buffer.getInt(offset + headerSize + it * 4) }
    }

    private fun elementName(buffer: ByteBuffer, offset: Int, walk: Walk): String? =
        walk.strings?.get(buffer.getInt(offset + ELEMENT_NAME_OFFSET))

    private fun startElement(buffer: ByteBuffer, offset: Int, walk: Walk) {
        val attributes = attributes(buffer, offset, walk)
        when (elementName(buffer, offset, walk)) {
            APPLICATION_TAG -> walk.icons += iconReferences(attributes)
            in COMPONENT_TAGS -> {
                walk.componentIcons = iconReferences(attributes)
                walk.componentIsLauncher = false
            }
            CATEGORY_TAG -> if (isLauncherCategory(attributes, walk)) walk.componentIsLauncher = true
        }
    }

    private fun endElement(name: String?, walk: Walk) {
        if (name !in COMPONENT_TAGS) return
        if (walk.componentIsLauncher) walk.icons += walk.componentIcons
        walk.componentIcons = emptyList()
        walk.componentIsLauncher = false
    }

    private fun attributes(buffer: ByteBuffer, offset: Int, walk: Walk): List<Attribute> {
        val start = offset + ATTRIBUTES_BASE + buffer.unsignedShort(offset + ATTRIBUTE_START_OFFSET)
        val size = buffer.unsignedShort(offset + ATTRIBUTE_SIZE_OFFSET)
        val count = buffer.unsignedShort(offset + ATTRIBUTE_COUNT_OFFSET)
        return List(count) { index ->
            val position = start + index * size
            Attribute(
                resourceId = walk.resourceIds.getOrElse(buffer.getInt(position + 4)) { 0 },
                type = buffer.unsignedByte(position + 15),
                data = buffer.getInt(position + 16)
            )
        }
    }

    private fun iconReferences(attributes: List<Attribute>): List<Int> =
        attributes
            .filter { it.resourceId == ATTR_ICON || it.resourceId == ATTR_ROUND_ICON }
            .filter { it.type == RES_VALUE_TYPE_REFERENCE }
            .map { it.data }

    private fun isLauncherCategory(attributes: List<Attribute>, walk: Walk): Boolean =
        attributes.any {
            it.resourceId == ATTR_NAME && it.type == RES_VALUE_TYPE_STRING &&
                walk.strings?.get(it.data) == LAUNCHER_CATEGORY
        }
}
