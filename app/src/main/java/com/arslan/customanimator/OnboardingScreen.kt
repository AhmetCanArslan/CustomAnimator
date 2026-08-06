package com.arslan.customanimator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val highlightRes: Int?,
    val requiresConsent: Boolean = false
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.Speed,
        titleRes = R.string.onboarding_welcome_title,
        bodyRes = R.string.onboarding_welcome_body,
        highlightRes = null
    ),
    OnboardingPage(
        icon = Icons.Filled.Shield,
        titleRes = R.string.onboarding_permission_title,
        bodyRes = R.string.onboarding_permission_body,
        highlightRes = R.string.onboarding_permission_highlight
    ),
    OnboardingPage(
        icon = Icons.Filled.BatteryAlert,
        titleRes = R.string.onboarding_battery_title,
        bodyRes = R.string.onboarding_battery_body,
        highlightRes = R.string.onboarding_battery_highlight
    ),
    OnboardingPage(
        icon = Icons.Filled.Science,
        titleRes = R.string.onboarding_experimental_title,
        bodyRes = R.string.onboarding_experimental_body,
        highlightRes = R.string.onboarding_experimental_highlight
    ),
    OnboardingPage(
        icon = Icons.Filled.Gavel,
        titleRes = R.string.onboarding_disclaimer_title,
        bodyRes = R.string.onboarding_disclaimer_body,
        highlightRes = R.string.onboarding_disclaimer_highlight,
        requiresConsent = true
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex
    var consentAccepted by rememberSaveable { mutableStateOf(false) }
    val canFinish = consentAccepted

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(onboardingPages.lastIndex) }
                    },
                    enabled = !isLastPage
                ) {
                    Text(
                        text = if (isLastPage) "" else stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page],
                    consentAccepted = consentAccepted,
                    onConsentChange = { consentAccepted = it }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                onboardingPages.indices.forEach { index ->
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                }

                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinished()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    enabled = !isLastPage || canFinish,
                    modifier = Modifier
                        .weight(if (pagerState.currentPage > 0) 1f else 2f)
                        .heightIn(min = 52.dp)
                ) {
                    Text(
                        text = if (isLastPage) {
                            stringResource(R.string.onboarding_start)
                        } else {
                            stringResource(R.string.onboarding_next)
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    consentAccepted: Boolean,
    onConsentChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (page.highlightRes != null) {
            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(page.highlightRes),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (page.requiresConsent) {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .toggleable(
                        value = consentAccepted,
                        onValueChange = onConsentChange,
                        role = Role.Checkbox
                    )
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = consentAccepted,
                    onCheckedChange = null
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.onboarding_disclaimer_consent),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
