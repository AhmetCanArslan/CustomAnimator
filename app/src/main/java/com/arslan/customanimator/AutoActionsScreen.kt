package com.arslan.customanimator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.theme.AppShapes

@Composable
fun AutoActionsScreenContent(
    modifier: Modifier = Modifier,
    hasShizukuPermission: Boolean,
    onNavigateToAutoForceStop: () -> Unit,
    onNavigateToAutoPermissionDisabler: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val openSetup = LocalOpenSetupGuide.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.card
                ) {
                    Column {
                        NavigationRow(
                            icon = Icons.Filled.PlaylistRemove,
                            title = stringResource(R.string.auto_force_stop),
                            description = stringResource(R.string.auto_force_stop_desc),
                            onClick = onNavigateToAutoForceStop
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        NavigationRow(
                            icon = Icons.Filled.Shield,
                            title = stringResource(R.string.auto_permission_disabler),
                            description = stringResource(R.string.auto_permission_disabler_desc),
                            onClick = onNavigateToAutoPermissionDisabler
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
