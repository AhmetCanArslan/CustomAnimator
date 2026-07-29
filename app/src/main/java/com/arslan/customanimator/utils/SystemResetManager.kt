package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.content.Context
import com.arslan.customanimator.service.AutoForceStopService
import com.arslan.customanimator.service.CompileBoosterService

/**
 * Undoes every system-level change this app can make, so a device left in a bad state
 * (stuck rotation, forced density, zeroed animation scales) can be recovered from inside the app.
 *
 * App-local data (presets, app lists) is left alone — that is [BackupManager]'s territory.
 */
object SystemResetManager {

    data class ResetResult(
        val animationScales: Boolean,
        val density: Boolean,
        val rotation: Boolean,
        val tweaks: Boolean,
        val angle: Boolean,
        val permissions: Boolean
    ) {
        val allSucceeded: Boolean
            get() = animationScales && density && rotation && tweaks && angle && permissions
    }

    /** Blocking — call from [kotlinx.coroutines.Dispatchers.IO]. */
    fun revertEverything(context: Context, contentResolver: ContentResolver): ResetResult {
        // Stop background work first so nothing re-applies a setting we are about to reset.
        AutoForceStopService.stop(context)
        CompileBoosterService.stop(context)

        val scales = SettingsManager.applyAllScales(context, contentResolver, 1.0f, 1.0f, 1.0f)
        val density = SettingsManager.setSmallestWidth(contentResolver, context, 0).success
        val rotation = DeveloperOptionsManager.resetRotation(context, contentResolver)

        val fancyIme = DeveloperOptionsManager.setFancyImeAnimations(context, contentResolver, false)
        val clockSeconds = DeveloperOptionsManager.setClockSeconds(context, contentResolver, false)
        val dontKeepActivities = DeveloperOptionsManager.setAlwaysFinishActivities(context, contentResolver, false)
        val bgLimit = DeveloperOptionsManager.setBackgroundProcessLimit(context, contentResolver, false)

        val angle = DeveloperOptionsManager.clearAngleDriverSelections(context, contentResolver)
        val permissions = regrantRevokedPermissions(context)

        return ResetResult(
            animationScales = scales,
            density = density,
            rotation = rotation,
            tweaks = fancyIme && clockSeconds && dontKeepActivities && bgLimit,
            angle = angle,
            permissions = permissions
        )
    }

    /** Gives back every dangerous permission the auto permission disabler took away. */
    private fun regrantRevokedPermissions(context: Context): Boolean {
        val store = RevokedPermissionsStore(context)
        val revoked = store.getAllRevoked()
        if (revoked.isEmpty()) return true
        val allSucceeded = revoked.entries.all { (pkg, permissions) ->
            permissions.all { DeveloperOptionsManager.grantPermission(pkg, it) }
        }
        if (allSucceeded) store.clearAll()
        return allSucceeded
    }
}
