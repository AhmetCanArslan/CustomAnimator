package com.arslan.customanimator.utils

import android.content.ContentResolver
import android.provider.Settings

object SettingsManager {
    
    fun getWindowAnimationScale(contentResolver: ContentResolver): Float {
        return try {
            Settings.Global.getFloat(
                contentResolver,
                Settings.Global.WINDOW_ANIMATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }
    }
    
    fun getTransitionAnimationScale(contentResolver: ContentResolver): Float {
        return try {
            Settings.Global.getFloat(
                contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }
    }
    
    fun getAnimatorDurationScale(contentResolver: ContentResolver): Float {
        return try {
            Settings.Global.getFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (e: Exception) {
            1.0f
        }
    }
    
    fun setWindowAnimationScale(contentResolver: ContentResolver, value: Float): Boolean {
        return try {
            Settings.Global.putFloat(
                contentResolver,
                Settings.Global.WINDOW_ANIMATION_SCALE,
                value
            )
        } catch (e: Exception) {
            false
        }
    }
    
    fun setTransitionAnimationScale(contentResolver: ContentResolver, value: Float): Boolean {
        return try {
            Settings.Global.putFloat(
                contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                value
            )
        } catch (e: Exception) {
            false
        }
    }
    
    fun setAnimatorDurationScale(contentResolver: ContentResolver, value: Float): Boolean {
        return try {
            Settings.Global.putFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                value
            )
        } catch (e: Exception) {
            false
        }
    }
    
    fun applyAllScales(
        contentResolver: ContentResolver,
        windowScale: Float,
        transitionScale: Float,
        animatorScale: Float
    ): Boolean {
        return setWindowAnimationScale(contentResolver, windowScale) &&
                setTransitionAnimationScale(contentResolver, transitionScale) &&
                setAnimatorDurationScale(contentResolver, animatorScale)
    }
}
