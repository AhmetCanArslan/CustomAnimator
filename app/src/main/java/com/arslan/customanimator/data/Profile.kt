package com.arslan.customanimator.data

data class ProfileTileConfig(
    val slot: Int,
    val label: String,
    val showToast: Boolean = true,
    val collapsePanel: Boolean = true
)

data class ProfileAnimation(
    val windowAnimationScale: Float,
    val transitionAnimationScale: Float,
    val animatorDurationScale: Float
)

data class ProfileBattery(
    val saverPresetId: String? = null,
    val dozePresetId: String? = null,
    val batterySaverOn: Boolean? = null,
    val triggerLevel: Int? = null,
    val automaticPowerSaveMode: Int? = null,
    val sticky: Boolean? = null,
    val stickyAutoDisable: Boolean? = null,
    val stickyAutoDisableLevel: Int? = null,
    val policy: Map<String, String> = emptyMap(),
    val toggles: Map<String, Boolean> = emptyMap()
) {
    val isEmpty: Boolean
        get() = saverPresetId == null && dozePresetId == null && batterySaverOn == null &&
            triggerLevel == null && automaticPowerSaveMode == null && sticky == null &&
            stickyAutoDisable == null && stickyAutoDisableLevel == null && policy.isEmpty() &&
            toggles.isEmpty()
}

data class Profile(
    val id: String,
    val name: String,
    val iconKey: String,
    val animation: ProfileAnimation? = null,
    val smallestWidthDp: Int? = null,
    val battery: ProfileBattery? = null,
    val developer: Map<String, Boolean> = emptyMap(),
    val tile: ProfileTileConfig? = null
) {
    val actionCount: Int
        get() = (if (animation != null) 3 else 0) +
            (if (smallestWidthDp != null) 1 else 0) +
            (battery?.let { b ->
                (if (b.saverPresetId != null) 1 else 0) +
                    (if (b.dozePresetId != null) 1 else 0) +
                    (if (b.batterySaverOn != null) 1 else 0) +
                    (if (b.triggerLevel != null) 1 else 0) +
                    (if (b.automaticPowerSaveMode != null) 1 else 0) +
                    (if (b.sticky != null) 1 else 0) +
                    (if (b.stickyAutoDisable != null) 1 else 0) +
                    (if (b.stickyAutoDisableLevel != null) 1 else 0) +
                    b.policy.size +
                    b.toggles.size
            } ?: 0) +
            developer.size
}
