package com.arslan.customanimator.utils

import java.io.File

internal object ApkIconPatcher {
    private const val MANIFEST = "AndroidManifest.xml"
    private const val RESOURCE_TABLE = "resources.arsc"

    fun iconIds(baseApk: File): Set<Int> =
        ApkManifestIcons.read(requireNotNull(ApkRepacker.readEntry(baseApk, MANIFEST)))

    fun patch(sources: List<File>, outputDir: File, foreground: ByteArray, backgroundColor: Int): List<File> {
        val base = sources.first()
        val iconIds = iconIds(base)
        require(iconIds.isNotEmpty())
        val baseTable = ApkResourceTable.withIconLayers(
            requireNotNull(ApkRepacker.readEntry(base, RESOURCE_TABLE)),
            iconIds,
            backgroundColor
        )
        val iconXml = AdaptiveIconXml.build(baseTable.backgroundId, baseTable.foregroundId)
        val splitTables = sources.drop(1).map { split ->
            ApkRepacker.readEntry(split, RESOURCE_TABLE)
                ?.let { ApkResourceTable.withIconEntries(it, iconIds) }
                ?.takeIf { it.repointedEntries > 0 }
        }
        require(baseTable.repointedEntries + splitTables.sumOf { it?.repointedEntries ?: 0 } > 0)

        val baseFiles = mapOf(ApkResourceTable.ICON_XML_PATH to iconXml, ApkResourceTable.FOREGROUND_PATH to foreground)
        val splitFiles = mapOf(ApkResourceTable.ICON_XML_PATH to iconXml)
        return (listOf(baseTable) + splitTables).mapIndexed { index, table ->
            val source = sources[index]
            val target = File(outputDir, source.name)
            val replacements = table?.let { mapOf(RESOURCE_TABLE to it.bytes) }.orEmpty()
            val additions = if (index == 0) baseFiles else splitFiles.takeIf { table != null }.orEmpty()
            ApkRepacker.repack(source, target, replacements, additions)
            target
        }
    }
}
