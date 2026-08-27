package com.arslan.customanimator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arslan.customanimator.utils.UsageAccessHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberUsageAccessState(hasShizukuPermission: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(UsageAccessHelper.hasUsageAccess(context)) }

    LaunchedEffect(hasShizukuPermission) {
        if (hasShizukuPermission && !state.value) {
            withContext(Dispatchers.IO) { UsageAccessHelper.grantUsageAccess(context) }
            state.value = UsageAccessHelper.hasUsageAccess(context)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = UsageAccessHelper.hasUsageAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}
