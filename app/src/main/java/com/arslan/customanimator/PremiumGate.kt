package com.arslan.customanimator

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.arslan.customanimator.utils.PremiumLimits

private var useChanges by mutableIntStateOf(0)

@Composable
fun rememberUseLimitReached(feature: PremiumLimits.Limited): Boolean {
    val context = LocalContext.current
    val isPremium by rememberIsPremium()
    return useChanges >= 0 && !isPremium && !PremiumLimits.isAvailable(context, feature)
}

fun runWithUseLimit(context: Context, feature: PremiumLimits.Limited, action: () -> Unit) {
    if (isPremiumNow()) {
        action()
        return
    }
    if (!PremiumLimits.isAvailable(context, feature)) {
        showPremiumLimit(context, context.getString(R.string.premium_use_limit))
        return
    }
    PremiumLimits.markUsed(context, feature)
    useChanges++
    action()
}

fun runWithShortcutLimit(context: Context, shortcutCount: Int, action: () -> Unit) {
    if (isPremiumNow() || shortcutCount < PremiumLimits.FREE_SHORTCUTS) {
        action()
        return
    }
    showPremiumLimit(context, context.getString(R.string.premium_shortcut_limit, PremiumLimits.FREE_SHORTCUTS))
}

fun runWithAppLimit(context: Context, configuredPackages: Collection<String>, packageName: String, action: () -> Unit) {
    if (isPremiumNow() || packageName in configuredPackages || configuredPackages.size < PremiumLimits.FREE_APPS) {
        action()
        return
    }
    showPremiumLimit(context, context.getString(R.string.premium_app_limit, PremiumLimits.FREE_APPS))
}

fun runWithIconChangeLimit(context: Context, action: () -> Unit) {
    if (isPremiumNow() || PremiumLimits.iconChangesLeft(context) > 0) {
        action()
        return
    }
    showPremiumLimit(context, context.getString(R.string.premium_icon_limit, PremiumLimits.FREE_ICON_CHANGES))
}

fun runWithButtonPremium(context: Context, alreadySet: Boolean, action: () -> Unit) {
    if (isPremiumNow() || alreadySet) {
        action()
        return
    }
    showPremiumLimit(context, context.getString(R.string.premium_button_required))
}

private fun showPremiumLimit(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    startPremiumPurchase(context)
}

@Composable
fun rememberShortcutLimitReached(shortcutCount: Int): Boolean {
    val isPremium by rememberIsPremium()
    return !isPremium && shortcutCount >= PremiumLimits.FREE_SHORTCUTS
}
