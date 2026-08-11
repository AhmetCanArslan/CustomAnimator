package com.arslan.customanimator.utils

import android.content.Context
import com.arslan.customanimator.data.Profile
import com.arslan.customanimator.data.ProfileBattery

object ProfileApplier {

    data class Result(val applied: Int, val failed: Int) {
        val total: Int get() = applied + failed
        val isFullSuccess: Boolean get() = failed == 0 && applied > 0
    }

    fun canApply(context: Context): Boolean =
        ShizukuHelper.hasShizukuPermission() || ShizukuHelper.hasWriteSecureSettingsPermission(context)

    fun apply(context: Context, profile: Profile): Result {
        val appContext = context.applicationContext
        val resolver = appContext.contentResolver
        var applied = 0
        var failed = 0

        fun record(success: Boolean) {
            if (success) applied++ else failed++
        }

        profile.animation?.let { animation ->
            record(
                SettingsManager.setWindowAnimationScale(appContext, resolver, animation.windowAnimationScale)
            )
            record(
                SettingsManager.setTransitionAnimationScale(appContext, resolver, animation.transitionAnimationScale)
            )
            record(
                SettingsManager.setAnimatorDurationScale(appContext, resolver, animation.animatorDurationScale)
            )
        }

        profile.smallestWidthDp?.let { width ->
            record(SettingsManager.setSmallestWidth(resolver, appContext, width).success)
        }

        profile.battery?.let { battery ->
            applyBattery(appContext, battery) { success -> record(success) }
        }

        profile.developer.forEach { (key, value) ->
            val action = ProfileActions.devAction(key) ?: return@forEach
            if (!action.available()) return@forEach
            record(runCatching { action.write(appContext, value) }.getOrDefault(false))
        }

        return Result(applied, failed)
    }

    private fun applyBattery(context: Context, battery: ProfileBattery, record: (Boolean) -> Unit) {
        val resolver = context.contentResolver

        battery.saverPresetId?.let { id ->
            val preset = BatteryTweaksManager.saverPresets.firstOrNull { it.id == id }
            if (preset != null) {
                val success = if (preset.constants.isEmpty()) {
                    BatteryTweaksManager.clearGlobal(
                        context, resolver, BatteryTweaksManager.KEY_BATTERY_SAVER_CONSTANTS
                    )
                } else {
                    BatteryTweaksManager.putGlobal(
                        context, resolver, BatteryTweaksManager.KEY_BATTERY_SAVER_CONSTANTS,
                        BatteryTweaksManager.serialiseConstants(preset.constants)
                    )
                }
                if (success) {
                    BatteryTweaksManager.setAppliedPreset(context, BatteryTweaksManager.GROUP_SAVER, id)
                }
                record(success)
            }
        }

        battery.dozePresetId?.let { id ->
            val preset = BatteryTweaksManager.dozePresets.firstOrNull { it.id == id }
            if (preset != null) {
                val success = if (preset.constants.isEmpty()) {
                    BatteryTweaksManager.clearGlobal(
                        context, resolver, BatteryTweaksManager.KEY_DEVICE_IDLE_CONSTANTS
                    )
                } else {
                    BatteryTweaksManager.putGlobal(
                        context, resolver, BatteryTweaksManager.KEY_DEVICE_IDLE_CONSTANTS,
                        BatteryTweaksManager.serialiseConstants(preset.constants)
                    )
                }
                if (success) {
                    BatteryTweaksManager.setAppliedPreset(context, BatteryTweaksManager.GROUP_DOZE, id)
                }
                record(success)
            }
        }

        battery.batterySaverOn?.let { on ->
            record(
                BatteryTweaksManager.putGlobal(
                    context, resolver, BatteryTweaksManager.KEY_LOW_POWER, if (on) "1" else "0"
                )
            )
        }

        battery.triggerLevel?.let { level ->
            record(
                BatteryTweaksManager.putGlobal(
                    context, resolver, BatteryTweaksManager.KEY_LOW_POWER_TRIGGER, level.toString()
                )
            )
        }

        battery.toggles.forEach { (key, value) ->
            val toggle = ProfileActions.batteryToggle(key) ?: return@forEach
            record(runCatching { toggle.write(context, value) }.getOrDefault(false))
        }
    }
}
