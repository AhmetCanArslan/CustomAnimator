package com.riyadmondol2006.customanimator.data

data class ApkExtractorApp(
    val packageName: String,
    val label: String,
    val versionName: String,
    val sizeBytes: Long,
    val isSplit: Boolean
)

enum class ApkExtractorFormat { BUNDLE, BASE, SEPARATE }
