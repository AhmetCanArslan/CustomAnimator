package com.riyadmondol2006.customanimator.data

import android.graphics.Bitmap
import java.io.File

class ApkInstallerPackage(
    val packageName: String,
    val label: String,
    val versionName: String,
    val installedVersion: String?,
    val sizeBytes: Long,
    val icon: Bitmap?,
    val apks: List<File>
) {
    val isSplit: Boolean get() = apks.size > 1
}
