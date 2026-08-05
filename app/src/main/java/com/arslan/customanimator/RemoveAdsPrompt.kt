package com.arslan.customanimator

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.utils.SettingsManager

@Composable
fun RemoveAdsPrompt(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isAdFree by rememberIsAdFree()
    val price by rememberRemoveAdsPrice()
    var dismissed by remember { mutableStateOf(SettingsManager.isRemoveAdsPromptDismissed(context)) }

    AnimatedVisibility(
        visible = !isAdFree && !dismissed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 2.dp,
            onClick = { startRemoveAdsPurchase(context) }
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.remove_ads_prompt_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = price?.let { stringResource(R.string.remove_ads_prompt_body_price, it) }
                            ?: stringResource(R.string.remove_ads_prompt_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                IconButton(
                    onClick = {
                        SettingsManager.dismissRemoveAdsPrompt(context)
                        dismissed = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.remove_ads_prompt_dismiss),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun startRemoveAdsPurchase(context: android.content.Context) {
    val activity = context as? Activity
    if (activity == null) {
        Toast.makeText(context, R.string.remove_ads_unavailable, Toast.LENGTH_SHORT).show()
        return
    }
    launchRemoveAdsPurchase(activity) { result ->
        val message = when (result) {
            Billing.PurchaseResult.PURCHASED -> R.string.remove_ads_thanks
            Billing.PurchaseResult.PENDING -> R.string.remove_ads_pending
            Billing.PurchaseResult.CANCELLED -> null
            Billing.PurchaseResult.ERROR -> R.string.remove_ads_unavailable
        }
        message?.let { Toast.makeText(activity, it, Toast.LENGTH_SHORT).show() }
    }
}
