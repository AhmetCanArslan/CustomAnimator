package com.arslan.customanimator.screenshot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object ScreenshotPermissions {

    val imagesPermission: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    /** Read access to the images collection — enough to find and preview screenshots. */
    fun hasImagesPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, imagesPermission) ==
            PackageManager.PERMISSION_GRANTED

    fun appSettingsIntent(context: Context): Intent = appDetailsIntent(context)

    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    private fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
}
