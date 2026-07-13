package com.arslan.customanimator.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo

object DangerousPermissionsHelper {

    @Suppress("DEPRECATION")
    fun getGrantedDangerousPermissions(context: Context, packageName: String): List<String> {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val requested = info.requestedPermissions ?: return emptyList()
            val flags = info.requestedPermissionsFlags ?: return emptyList()
            requested.indices.mapNotNull { i ->
                val perm = requested[i]
                val granted = (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                if (!granted) return@mapNotNull null
                val protectionLevel = try {
                    pm.getPermissionInfo(perm, 0).protectionLevel
                } catch (e: PackageManager.NameNotFoundException) {
                    return@mapNotNull null
                }
                val base = protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
                if (base == PermissionInfo.PROTECTION_DANGEROUS) perm else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
