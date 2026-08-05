package com.arslan.customanimator.notify.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.BannerAdView
import com.arslan.customanimator.maybeShowInterstitial
import com.arslan.customanimator.R
import com.arslan.customanimator.notify.data.CustomPattern
import com.arslan.customanimator.notify.data.RulesManager
import com.arslan.customanimator.notify.service.FlashManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePatternSection(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val rulesManager = remember { RulesManager(context) }
    val flashManager = remember { FlashManager(context) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingTimeLeft by remember { mutableLongStateOf(5000L) }
    var showNameDialog by remember { mutableStateOf(false) }

    var events by remember { mutableStateOf(mutableListOf<Pair<Long, Boolean>>()) }
    var startTime by remember { mutableLongStateOf(0L) }
    var patternName by remember { mutableStateOf("") }

    var isFlashOn by remember { mutableStateOf(false) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            startTime = SystemClock.uptimeMillis()
            var currentEvents = mutableListOf<Pair<Long, Boolean>>()
            currentEvents.add(Pair(startTime, false))
            events = currentEvents

            var elapsed = 0L
            while (elapsed < 5000L) {
                delay(16)
                elapsed = SystemClock.uptimeMillis() - startTime
                recordingTimeLeft = maxOf(0L, 5000L - elapsed)
                if (elapsed >= 5000L) {
                    isRecording = false
                    val newEvents = ArrayList(events)
                    newEvents.add(Pair(SystemClock.uptimeMillis(), false))
                    events = newEvents
                    showNameDialog = true
                }
            }
        }
    }

    Scaffold(
        bottomBar = { BannerAdView() },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pn_create_pattern)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.pn_cd_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isRecording) stringResource(R.string.pn_recording_format, recordingTimeLeft / 1000, (recordingTimeLeft % 1000) / 100) else stringResource(R.string.pn_press_start_record),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        color = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .pointerInput(isRecording) {
                        if (isRecording) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown()
                                    isFlashOn = true
                                    flashManager.turnOnFlash()
                                    val newEventsOn = ArrayList(events)
                                    newEventsOn.add(Pair(SystemClock.uptimeMillis(), true))
                                    events = newEventsOn

                                    val up = waitForUpOrCancellation()
                                    isFlashOn = false
                                    flashManager.turnOffFlash()
                                    val newEventsOff = ArrayList(events)
                                    newEventsOff.add(Pair(SystemClock.uptimeMillis(), false))
                                    events = newEventsOff
                                }
                            }
                        } else {
                            isFlashOn = false
                            flashManager.turnOffFlash()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRecording) stringResource(R.string.pn_tap_hold) else stringResource(R.string.pn_flash),
                    color = if (isFlashOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!isRecording) {
                    Button(
                        onClick = {
                            recordingTimeLeft = 5000L
                            isRecording = true
                        },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        Text(stringResource(R.string.pn_start))
                    }
                } else {
                    Button(
                        onClick = {
                            isRecording = false
                            val newEvents = ArrayList(events)
                            newEvents.add(Pair(SystemClock.uptimeMillis(), false))
                            events = newEvents
                            showNameDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        Text(stringResource(R.string.pn_stop))
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.pn_save_pattern)) },
            text = {
                OutlinedTextField(
                    value = patternName,
                    onValueChange = { patternName = it },
                    label = { Text(stringResource(R.string.pn_pattern_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (patternName.isNotBlank()) {
                            val intervals = mutableListOf<Long>()
                            var lastState = false
                            var lastTime = startTime

                            for (event in events) {
                                if (event.first <= lastTime) continue
                                if (event.second != lastState) {
                                    intervals.add(event.first - lastTime)
                                    lastState = event.second
                                    lastTime = event.first
                                }
                            }

                            if (intervals.isEmpty()) {
                                intervals.add(100L)
                            }

                            val customPattern = CustomPattern(
                                name = patternName.trim(),
                                intervals = intervals
                            )
                            rulesManager.saveCustomPattern(customPattern)
                            showNameDialog = false
                            maybeShowInterstitial(context)
                            onNavigateBack()
                        }
                    },
                    enabled = patternName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
