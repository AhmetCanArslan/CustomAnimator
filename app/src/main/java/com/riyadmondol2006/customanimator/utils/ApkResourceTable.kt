package com.riyadmondol2006.customanimator.utils

import java.nio.ByteBuffer

internal class PatchedResourceTable(
    val bytes: ByteArray,
    val repointedEntries: Int,
    val foregroundId: Int,
    val backgroundId: Int
)

internal object ApkResourceTable {
    const val ICON_XML_PATH = "res/customanimator_icon.xml"
    const val FOREGROUND_PATH = "res/customanimator_icon_foreground.png"

    private const val TABLE_HEADER_SIZE = 12
    private const val PACKAGE_TYPE = 0x0200
    private const val TYPE_TYPE = 0x0201
    private const val TYPE_SPEC_TYPE = 0x0202
    private const val PACKAGE_ID_OFFSET = 8
    private const val TYPE_STRINGS_OFFSET = 268
    private const val KEY_STRINGS_OFFSET = 276
    private const val TYPE_ID_OFFSET_OFFSET = 284
    private const val PACKAGE_HEADER_WITH_TYPE_ID_OFFSET = 288
    private const val TYPE_FLAG_SPARSE = 0x01
    private const val TYPE_FLAG_OFFSET16 = 0x02
    private const val ENTRY_FLAG_COMPLEX = 0x0001
    private const val ENTRY_FLAG_COMPACT = 0x0008
    private const val NO_ENTRY = -1
    private const val NO_ENTRY16 = 0xFFFF
    private const val MAX_TYPE_ID = 0xFF
    private const val LAYER_TYPE_NAME = "drawable"
    private const val LAYER_COUNT = 2
    private const val LAYER_DENSITY = 640
    private const val CONFIG_SIZE = 64
    private const val CONFIG_DENSITY_OFFSET = 14
    private const val TYPE_SPEC_HEADER_SIZE = 16
    private const val TYPE_HEADER_SIZE = 20 + CONFIG_SIZE
    private const val ENTRY_SIZE = 8
    private const val VALUE_SIZE = 8
    private const val DEFAULT_PACKAGE_ID = 0x7F

    private class Layers(val foregroundString: Int, val backgroundColor: Int)

    private class PatchedPackage(val bytes: ByteArray, val repointed: Int, val layerTypeId: Int)

    fun withIconLayers(table: ByteArray, iconIds: Set<Int>, backgroundColor: Int): PatchedResourceTable =
        rewrite(table, iconIds, backgroundColor)

    fun withIconEntries(table: ByteArray, iconIds: Set<Int>): PatchedResourceTable = rewrite(table, iconIds, null)

    private fun rewrite(table: ByteArray, iconIds: Set<Int>, backgroundColor: Int?): PatchedResourceTable {
        val buffer = table.littleEndian()
        val poolSize = buffer.getInt(TABLE_HEADER_SIZE + 4)
        val pool = ApkStringPool(table.copyOfRange(TABLE_HEADER_SIZE, TABLE_HEADER_SIZE + poolSize))
        val iconString = pool.stringCount
        val layers = backgroundColor?.let { Layers(iconString + 1, it) }
        val layerPackage = iconIds.firstOrNull()?.ushr(24) ?: DEFAULT_PACKAGE_ID

        val parts = mutableListOf(table.copyOfRange(0, TABLE_HEADER_SIZE), pool.appended(listOf(ICON_XML_PATH, FOREGROUND_PATH)))
        var repointed = 0
        var layerTypeId = 0
        var offset = TABLE_HEADER_SIZE + poolSize
        while (offset + 8 <= table.size) {
            val size = buffer.getInt(offset + 4)
            require(size > 0 && offset + size <= table.size)
            val chunk = table.copyOfRange(offset, offset + size)
            if (buffer.unsignedShort(offset) == PACKAGE_TYPE) {
                val packageId = buffer.getInt(offset + PACKAGE_ID_OFFSET)
                val patched = patchPackage(chunk, packageId, iconIds, iconString, layers.takeIf { packageId == layerPackage })
                repointed += patched.repointed
                if (patched.layerTypeId != 0) layerTypeId = patched.layerTypeId
                parts += patched.bytes
            } else {
                parts += chunk
            }
            offset += size
        }
        require(layers == null || layerTypeId != 0)

        val output = concatBytes(parts)
        output.littleEndian().putInt(4, output.size)
        val layerBase = (layerPackage shl 24) or (layerTypeId shl 16)
        return PatchedResourceTable(output, repointed, layerBase, layerBase or 1)
    }

    private fun patchPackage(
        chunk: ByteArray,
        packageId: Int,
        iconIds: Set<Int>,
        iconString: Int,
        layers: Layers?
    ): PatchedPackage {
        val entriesByType = iconIds
            .filter { it ushr 24 == packageId }
            .groupBy({ (it ushr 16) and 0xFF }, { it and 0xFFFF })
        val repointed = repointEntries(chunk, entriesByType, iconString)
        if (layers == null) return PatchedPackage(chunk, repointed, 0)
        return addLayerType(chunk, layers, repointed)
    }

    private fun repointEntries(chunk: ByteArray, entriesByType: Map<Int, List<Int>>, iconString: Int): Int {
        val buffer = chunk.littleEndian()
        var repointed = 0
        var offset = buffer.unsignedShort(2)
        while (offset + 8 <= chunk.size) {
            val size = buffer.getInt(offset + 4)
            if (size <= 0) break
            if (buffer.unsignedShort(offset) == TYPE_TYPE) {
                val entries = entriesByType[buffer.unsignedByte(offset + 8)].orEmpty()
                repointed += entries.count { repointEntry(buffer, offset, it, iconString) }
            }
            offset += size
        }
        return repointed
    }

    private fun repointEntry(buffer: ByteBuffer, typeStart: Int, entry: Int, iconString: Int): Boolean {
        val entryOffset = entryOffset(buffer, typeStart, entry) ?: return false
        val position = typeStart + buffer.getInt(typeStart + 16) + entryOffset
        val flags = buffer.unsignedShort(position + 2)
        if (flags and ENTRY_FLAG_COMPACT != 0) {
            buffer.putShort(position + 2, ((flags and 0xFF) or (RES_VALUE_TYPE_STRING shl 8)).toShort())
            buffer.putInt(position + 4, iconString)
            return true
        }
        if (flags and ENTRY_FLAG_COMPLEX != 0) return false
        val value = position + buffer.unsignedShort(position)
        buffer.put(value + 3, RES_VALUE_TYPE_STRING.toByte())
        buffer.putInt(value + 4, iconString)
        return true
    }

    private fun entryOffset(buffer: ByteBuffer, typeStart: Int, entry: Int): Int? {
        val flags = buffer.unsignedByte(typeStart + 9)
        val entryCount = buffer.getInt(typeStart + 12)
        val offsets = typeStart + buffer.unsignedShort(typeStart + 2)
        if (flags and TYPE_FLAG_SPARSE != 0) return sparseOffset(buffer, offsets, entryCount, entry)
        if (entry >= entryCount) return null
        if (flags and TYPE_FLAG_OFFSET16 != 0) {
            return buffer.unsignedShort(offsets + entry * 2).takeIf { it != NO_ENTRY16 }?.times(4)
        }
        return buffer.getInt(offsets + entry * 4).takeIf { it != NO_ENTRY }
    }

    private fun sparseOffset(buffer: ByteBuffer, offsets: Int, entryCount: Int, entry: Int): Int? =
        (0 until entryCount)
            .firstOrNull { buffer.unsignedShort(offsets + it * 4) == entry }
            ?.let { buffer.unsignedShort(offsets + it * 4 + 2) * 4 }

    private fun addLayerType(chunk: ByteArray, layers: Layers, repointed: Int): PatchedPackage {
        val buffer = chunk.littleEndian()
        val headerSize = buffer.unsignedShort(2)
        val typeStrings = buffer.getInt(TYPE_STRINGS_OFFSET)
        val keyStrings = buffer.getInt(KEY_STRINGS_OFFSET)
        val typePoolEnd = typeStrings + buffer.getInt(typeStrings + 4)
        val typePool = ApkStringPool(chunk.copyOfRange(typeStrings, typePoolEnd))
        val typeIdOffset = if (headerSize >= PACKAGE_HEADER_WITH_TYPE_ID_OFFSET) buffer.getInt(TYPE_ID_OFFSET_OFFSET) else 0
        val typeId = typePool.stringCount + 1 + typeIdOffset
        require(typeId <= MAX_TYPE_ID)

        val newTypePool = typePool.appended(listOf(LAYER_TYPE_NAME))
        val output = concatBytes(
            listOf(
                chunk.copyOfRange(0, typeStrings),
                newTypePool,
                chunk.copyOfRange(typePoolEnd, chunk.size),
                layerTypeSpec(typeId),
                layerType(typeId, layers)
            )
        )
        val patched = output.littleEndian()
        patched.putInt(4, output.size)
        if (keyStrings > typeStrings) {
            patched.putInt(KEY_STRINGS_OFFSET, keyStrings + newTypePool.size - (typePoolEnd - typeStrings))
        }
        return PatchedPackage(output, repointed, typeId)
    }

    private fun layerTypeSpec(typeId: Int): ByteArray {
        val buffer = littleEndianBuffer(TYPE_SPEC_HEADER_SIZE + LAYER_COUNT * 4)
        buffer.putShort(TYPE_SPEC_TYPE.toShort())
        buffer.putShort(TYPE_SPEC_HEADER_SIZE.toShort())
        buffer.putInt(buffer.capacity())
        buffer.put(typeId.toByte())
        buffer.put(0)
        buffer.putShort(0)
        buffer.putInt(LAYER_COUNT)
        return buffer.array()
    }

    private fun layerType(typeId: Int, layers: Layers): ByteArray {
        val entriesStart = TYPE_HEADER_SIZE + LAYER_COUNT * 4
        val entrySize = ENTRY_SIZE + VALUE_SIZE
        val buffer = littleEndianBuffer(entriesStart + LAYER_COUNT * entrySize)
        buffer.putShort(TYPE_TYPE.toShort())
        buffer.putShort(TYPE_HEADER_SIZE.toShort())
        buffer.putInt(buffer.capacity())
        buffer.put(typeId.toByte())
        buffer.put(0)
        buffer.putShort(0)
        buffer.putInt(LAYER_COUNT)
        buffer.putInt(entriesStart)
        buffer.putInt(CONFIG_SIZE)
        buffer.putShort(20 + CONFIG_DENSITY_OFFSET, LAYER_DENSITY.toShort())
        buffer.position(TYPE_HEADER_SIZE)
        buffer.putInt(0)
        buffer.putInt(entrySize)
        putEntry(buffer, RES_VALUE_TYPE_STRING, layers.foregroundString)
        putEntry(buffer, RES_VALUE_TYPE_COLOR, layers.backgroundColor)
        return buffer.array()
    }

    private fun putEntry(buffer: ByteBuffer, type: Int, data: Int) {
        buffer.putShort(ENTRY_SIZE.toShort())
        buffer.putShort(0)
        buffer.putInt(0)
        buffer.putShort(VALUE_SIZE.toShort())
        buffer.put(0)
        buffer.put(type.toByte())
        buffer.putInt(data)
    }
}
