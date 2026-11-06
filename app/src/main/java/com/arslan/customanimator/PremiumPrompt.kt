package com.arslan.customanimator

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arslan.customanimator.utils.SettingsManager

@Composable
fun PremiumPromptDialog(enabled: Boolean) {
    val context = LocalContext.current
    val isPremium by rememberIsPremium()
    var visible by remember {
        mutableStateOf(
            !isPremiumNow()
                && SettingsManager.hasCompletedOnboarding(context)
                && SettingsManager.shouldShowPremiumPrompt(context)
        )
    }
    if (!enabled || !visible || isPremium) return

    val dismiss = {
        SettingsManager.markPremiumPromptHandled(context)
        visible = false
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.premium_prompt_title)) },
        text = { Text(stringResource(R.string.premium_prompt_message)) },
        confirmButton = {
            Button(
                onClick = {
                    SettingsManager.markPremiumPromptHandled(context)
                    visible = false
                    startPremiumPurchase(context)
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
