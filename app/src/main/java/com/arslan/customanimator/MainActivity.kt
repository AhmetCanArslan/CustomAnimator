package com.arslan.customanimator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme
import com.arslan.customanimator.utils.PresetManager
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.ShizukuHelper
import rikka.shizuku.Shizuku
import androidx.compose.ui.res.stringResource
import com.arslan.customanimator.R

class MainActivity : ComponentActivity() {
    private val shizukuRequestListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (grantResult == 0) {
            // Permission granted
            ShizukuHelper.grantWriteSecureSettingsPermission(this)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Add Shizuku listener
        Shizuku.addRequestPermissionResultListener(shizukuRequestListener)
        
        // Request Shizuku permission on first launch if available and not yet requested
        if (ShizukuHelper.isShizukuAvailable() && !ShizukuHelper.hasShizukuBeenRequested(this) && !ShizukuHelper.hasShizukuPermission()) {
            ShizukuHelper.requestShizukuPermission(this)
            ShizukuHelper.markShizukuRequested(this)
        }
        
        setContent {
            CustomAnimatorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AnimatorSelectorScreen(this)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check if Shizuku permission was granted and try to grant WRITE_SECURE_SETTINGS
        if (ShizukuHelper.hasShizukuPermission()) {
            val hasSecureSettings = ContextCompat.checkSelfPermission(
                this,
                "android.permission.WRITE_SECURE_SETTINGS"
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasSecureSettings) {
                ShizukuHelper.grantWriteSecureSettingsPermission(this)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuRequestListener)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatorSelectorScreen(activity: MainActivity) {
    val context = activity
    val contentResolver = context.contentResolver
    val presetManager = remember { PresetManager(context) }
    val focusManager = LocalFocusManager.current
    
    // Shizuku state and permission state
    val isShizukuAvailable = remember { ShizukuHelper.isShizukuAvailable() }
    val hasShizukuPermission = remember { mutableStateOf(ShizukuHelper.hasShizukuPermission()) }
    val hasWriteSecureSettings = remember { mutableStateOf(ShizukuHelper.hasWriteSecureSettingsPermission(context)) }
    var showPermissionDetailsDialog by remember { mutableStateOf(false) }
    
    // Current values from system settings
    var windowAnimScale by remember {
        mutableStateOf(SettingsManager.getWindowAnimationScale(contentResolver))
    }
    var transitionAnimScale by remember {
        mutableStateOf(SettingsManager.getTransitionAnimationScale(contentResolver))
    }
    var animatorDurScale by remember {
        mutableStateOf(SettingsManager.getAnimatorDurationScale(contentResolver))
    }
    
    // UI state
    var windowInputValue by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", windowAnimScale)) }
    var transitionInputValue by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", transitionAnimScale)) }
    var animatorInputValue by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", animatorDurScale)) }
    
    var presetName by remember { mutableStateOf("") }
    var allPresets by remember { mutableStateOf(presetManager.getAllPresets()) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var expandedPresetId by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var inputMode by remember { mutableStateOf(SettingsManager.getInputMode(context)) }
    var isSimpleMode by remember { mutableStateOf(SettingsManager.getSimpleMode(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionErrorMessage by remember { mutableStateOf("") }
    
    // Animation state for fade in/out when mode changes
    var shouldShowContent by remember { mutableStateOf(true) }
    var pendingInputMode by remember { mutableStateOf<String?>(null) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (shouldShowContent) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "content fade animation"
    )
    
    // Handle mode transition with fade animation
    LaunchedEffect(pendingInputMode) {
        if (pendingInputMode != null) {
            // Fade out current content
            shouldShowContent = false
            delay(300) // Wait for fade out to complete
            // Change the mode
            inputMode = pendingInputMode!!
            // Save input mode preference
            SettingsManager.setInputMode(context, inputMode)
            // Sync input values when switching to manual mode
            if (inputMode == "manual") {
                windowInputValue = String.format(java.util.Locale.US, "%.2f", windowAnimScale)
                transitionInputValue = String.format(java.util.Locale.US, "%.2f", transitionAnimScale)
                animatorInputValue = String.format(java.util.Locale.US, "%.2f", animatorDurScale)
            }
            // Fade in new content
            shouldShowContent = true
            pendingInputMode = null
        }
    }
    
    // Check permission status when Permission Details dialog is opened
    LaunchedEffect(showPermissionDetailsDialog) {
        if (showPermissionDetailsDialog) {
            hasWriteSecureSettings.value = ShizukuHelper.hasWriteSecureSettingsPermission(context)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Custom Animator",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.menu)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.create_new_preset)) },
                                onClick = {
                                    menuExpanded = false
                                    showPresetDialog = true
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(if (isSimpleMode) stringResource(R.string.advanced_mode) else stringResource(R.string.simple_mode)) },
                                onClick = {
                                    menuExpanded = false
                                    isSimpleMode = !isSimpleMode
                                    SettingsManager.setSimpleMode(context, isSimpleMode)
                                    if (isSimpleMode) {
                                        transitionAnimScale = windowAnimScale
                                        animatorDurScale = windowAnimScale
                                        transitionInputValue = windowInputValue
                                        animatorInputValue = windowInputValue
                                    }
                                }
                            )
                            if (inputMode == "slider") {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.use_manual_input)) },
                                    onClick = {
                                        menuExpanded = false
                                        pendingInputMode = "manual"
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.use_sliders)) },
                                    onClick = {
                                        menuExpanded = false
                                        pendingInputMode = "slider"
                                    }
                                )
                            }
                            if (isShizukuAvailable) {
                                Divider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.permission_details)) },
                                    onClick = {
                                        menuExpanded = false
                                        showPermissionDetailsDialog = true
                                    }
                                )
                            }
                            Divider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.source_code)) },
                                onClick = {
                                    menuExpanded = false
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.github.com/ahmetcanarslan/customanimator"))
                                    context.startActivity(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.buy_me_a_coffee)) },
                                onClick = {
                                    menuExpanded = false
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/ahmetcanarslan"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus()
                        }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Animation Speed Preview
            item {
                SyncedAnimationPreview(
                    currentScale = windowAnimScale,
                    modifier = Modifier.graphicsLayer(alpha = contentAlpha)
                )
            }
            
            // Animation Sliders
            if (inputMode == "slider") {
                item {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = contentAlpha)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.animation_scale_slider),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        windowAnimScale = 1.0f
                                        transitionAnimScale = 1.0f
                                        animatorDurScale = 1.0f
                                        windowInputValue = "1.00"
                                        transitionInputValue = "1.00"
                                        animatorInputValue = "1.00"
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.restore_to_default)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        
                        // Window Animation Slider
                        val sliderLabel = if (isSimpleMode) stringResource(R.string.animation_scale_applies_to_all) else stringResource(R.string.window_animation_scale)
                        Text("$sliderLabel: ${String.format(java.util.Locale.US, "%.2f", windowAnimScale)}", fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    windowAnimScale = (windowAnimScale - 0.01f).coerceAtLeast(0f)
                                    windowInputValue = String.format(java.util.Locale.US, "%.2f", windowAnimScale)
                                    if (isSimpleMode) {
                                        transitionAnimScale = windowAnimScale
                                        transitionInputValue = windowInputValue
                                        animatorDurScale = windowAnimScale
                                        animatorInputValue = windowInputValue
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = windowAnimScale,
                                onValueChange = { 
                                    windowAnimScale = it
                                    windowInputValue = String.format(java.util.Locale.US, "%.2f", it)
                                    if (isSimpleMode) {
                                        transitionAnimScale = it
                                        transitionInputValue = windowInputValue
                                        animatorDurScale = it
                                        animatorInputValue = windowInputValue
                                    }
                                },
                                valueRange = 0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    windowAnimScale = (windowAnimScale + 0.01f).coerceAtMost(5f)
                                    windowInputValue = String.format(java.util.Locale.US, "%.2f", windowAnimScale)
                                    if (isSimpleMode) {
                                        transitionAnimScale = windowAnimScale
                                        transitionInputValue = windowInputValue
                                        animatorDurScale = windowAnimScale
                                        animatorInputValue = windowInputValue
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!isSimpleMode) {
                            Spacer(modifier = Modifier.height(16.dp))
                        
                        // Transition Animation Slider
                        Text("${stringResource(R.string.transition_animation_scale)}: ${String.format(java.util.Locale.US, "%.2f", transitionAnimScale)}", fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    transitionAnimScale = (transitionAnimScale - 0.01f).coerceAtLeast(0f)
                                    transitionInputValue = String.format(java.util.Locale.US, "%.2f", transitionAnimScale)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = transitionAnimScale,
                                onValueChange = { 
                                    transitionAnimScale = it
                                    transitionInputValue = String.format(java.util.Locale.US, "%.2f", it)
                                },
                                valueRange = 0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    transitionAnimScale = (transitionAnimScale + 0.01f).coerceAtMost(5f)
                                    transitionInputValue = String.format(java.util.Locale.US, "%.2f", transitionAnimScale)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Animator Duration Slider
                        Text("${stringResource(R.string.animator_duration_scale)}: ${String.format(java.util.Locale.US, "%.2f", animatorDurScale)}", fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    animatorDurScale = (animatorDurScale - 0.01f).coerceAtLeast(0f)
                                    animatorInputValue = String.format(java.util.Locale.US, "%.2f", animatorDurScale)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = animatorDurScale,
                                onValueChange = { 
                                    animatorDurScale = it
                                    animatorInputValue = String.format(java.util.Locale.US, "%.2f", it)
                                },
                                valueRange = 0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    animatorDurScale = (animatorDurScale + 0.01f).coerceAtMost(5f)
                                    animatorInputValue = String.format(java.util.Locale.US, "%.2f", animatorDurScale)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        }
                    }
                }
                }
            }
            
            // Manual Input Fields
            if (inputMode == "manual") {
                item {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = contentAlpha)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    focusManager.clearFocus()
                                }
                            )
                        }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.manual_input),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        windowAnimScale = 1.0f
                                        transitionAnimScale = 1.0f
                                        animatorDurScale = 1.0f
                                        windowInputValue = "1.00"
                                        transitionInputValue = "1.00"
                                        animatorInputValue = "1.00"
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.restore_to_default)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        
                        val inputLabel = if (isSimpleMode) stringResource(R.string.animation_scale_applies_to_all) else stringResource(R.string.window_animation)
                        OutlinedTextField(
                            value = windowInputValue,
                            onValueChange = { 
                                windowInputValue = it
                                val floatVal = it.replace(',', '.').toFloatOrNull()
                                if (floatVal != null && floatVal in 0f..5.0f) {
                                    windowAnimScale = String.format(java.util.Locale.US, "%.2f", floatVal).toFloat()
                                    if (isSimpleMode) {
                                        transitionAnimScale = windowAnimScale
                                        animatorDurScale = windowAnimScale
                                    }
                                }
                                if (isSimpleMode) {
                                    transitionInputValue = it
                                    animatorInputValue = it
                                }
                            },
                            label = { Text(inputLabel) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (!isSimpleMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = transitionInputValue,
                            onValueChange = { 
                                transitionInputValue = it
                                val floatVal = it.replace(',', '.').toFloatOrNull()
                                if (floatVal != null && floatVal in 0f..5.0f) {
                                    transitionAnimScale = String.format(java.util.Locale.US, "%.2f", floatVal).toFloat()
                                }
                            },
                            label = { Text(stringResource(R.string.transition_animation)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = animatorInputValue,
                            onValueChange = { 
                                animatorInputValue = it
                                val floatVal = it.replace(',', '.').toFloatOrNull()
                                if (floatVal != null && floatVal in 0f..5.0f) {
                                    animatorDurScale = String.format(java.util.Locale.US, "%.2f", floatVal).toFloat()
                                }
                            },
                            label = { Text(stringResource(R.string.animator_duration)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        }
                    }
                }
                }
            }
            
            // Apply Button
            item {
                Button(
                    onClick = {
                        // Check if permission is granted
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            "android.permission.WRITE_SECURE_SETTINGS"
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            permissionErrorMessage = context.getString(R.string.permission_not_granted)
                            showPermissionDialog = true
                            return@Button
                        }

                        try {
                            val finalTransition = if (isSimpleMode) windowAnimScale else transitionAnimScale
                            val finalAnimator = if (isSimpleMode) windowAnimScale else animatorDurScale
                            
                            SettingsManager.applyAllScales(
                                contentResolver,
                                windowAnimScale,
                                finalTransition,
                                finalAnimator
                            )
                            
                            if (isSimpleMode) {
                                transitionAnimScale = finalTransition
                                animatorDurScale = finalAnimator
                                transitionInputValue = String.format(java.util.Locale.US, "%.2f", finalTransition)
                                animatorInputValue = String.format(java.util.Locale.US, "%.2f", finalAnimator)
                            }
                            Toast.makeText(
                                context,
                                context.getString(R.string.animation_scales_updated),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            permissionErrorMessage = e.message ?: context.getString(R.string.unknown_error)
                            showPermissionDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .graphicsLayer(alpha = contentAlpha),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.apply_settings), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Presets List Header
            if (allPresets.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.saved_presets),
                        modifier = Modifier.graphicsLayer(alpha = contentAlpha),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Presets List
            if (allPresets.isNotEmpty()) {
                items(allPresets) { preset ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(alpha = contentAlpha)
                            .background(
                                if (expandedPresetId == preset.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        preset.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        stringResource(R.string.preset_values, preset.windowAnimationScale, preset.transitionAnimationScale, preset.animatorDurationScale),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                "android.permission.WRITE_SECURE_SETTINGS"
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (!hasPermission) {
                                                permissionErrorMessage = context.getString(R.string.permission_not_granted)
                                                showPermissionDialog = true
                                                return@Button
                                            }

                                            windowAnimScale = preset.windowAnimationScale
                                            transitionAnimScale = preset.transitionAnimationScale
                                            animatorDurScale = preset.animatorDurationScale
                                            windowInputValue = String.format(java.util.Locale.US, "%.2f", preset.windowAnimationScale)
                                            transitionInputValue = String.format(java.util.Locale.US, "%.2f", preset.transitionAnimationScale)
                                            animatorInputValue = String.format(java.util.Locale.US, "%.2f", preset.animatorDurationScale)
                                            try {
                                                SettingsManager.applyAllScales(
                                                    contentResolver,
                                                    preset.windowAnimationScale,
                                                    preset.transitionAnimationScale,
                                                    preset.animatorDurationScale
                                                )
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.error_applying_preset, e.message),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@Button
                                            }
                                            Toast.makeText(context, context.getString(R.string.preset_loaded_applied), Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text(stringResource(R.string.load), fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            presetManager.deletePreset(preset.id)
                                            allPresets = presetManager.getAllPresets()
                                            Toast.makeText(context, context.getString(R.string.preset_deleted), Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text(stringResource(R.string.delete), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        stringResource(R.string.no_presets_saved),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(16.dp)
                            .graphicsLayer(alpha = contentAlpha)
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Permission Error Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.permission_required)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.write_secure_settings_permission),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        stringResource(R.string.permission_description),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        stringResource(R.string.one_time_only),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        stringResource(R.string.one_time_description),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (isShizukuAvailable && hasShizukuPermission.value) {
                        Text(
                            stringResource(R.string.shizuku_ready),
                            fontSize = 13.sp,
                            color = Color.Green,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            stringResource(R.string.shizuku_ready_description),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else if (isShizukuAvailable && !hasShizukuPermission.value) {
                        Text(
                            stringResource(R.string.shizuku_available),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            stringResource(R.string.shizuku_available_description),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    if (permissionErrorMessage.isNotEmpty()) {
                        Text(
                            stringResource(R.string.error, permissionErrorMessage),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    // Only show permission granting instructions if permission is not granted
                    if (!hasWriteSecureSettings.value) {
                        Text(
                            stringResource(R.string.use_adb_command),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            stringResource(R.string.adb_description),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        SelectionContainer {
                            Text(
                                stringResource(R.string.adb_command),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                        
                        Text(
                            stringResource(R.string.see_permission_details),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val clip = ClipData.newPlainText(
                            "ADB Command",
                            "adb shell pm grant com.arslan.customanimator android.permission.WRITE_SECURE_SETTINGS"
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.command_copied), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.copy_command))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showPermissionDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
    
    // Permission Details Dialog
    if (showPermissionDetailsDialog && isShizukuAvailable) {
        AlertDialog(
            onDismissRequest = { showPermissionDetailsDialog = false },
            title = { Text(stringResource(R.string.permission_details_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "WRITE_SECURE_SETTINGS Permission",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (hasWriteSecureSettings.value) {
                        Text(
                            stringResource(R.string.granted),
                            fontSize = 13.sp,
                            color = Color.Green,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            stringResource(R.string.granted_description),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 16.dp),
                            lineHeight = 16.sp
                        )
                    } else {
                        Text(
                            stringResource(R.string.not_granted),
                            fontSize = 13.sp,
                            color = Color(0xFFE74C3C),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            stringResource(R.string.permission_purpose),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                            lineHeight = 16.sp
                        )
                        Text(
                            stringResource(R.string.note_one_time),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    // Only show permission granting instructions if permission is not granted
                    if (!hasWriteSecureSettings.value) {
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        Text(
                            stringResource(R.string.how_to_grant),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            stringResource(R.string.option_adb),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Text(
                            stringResource(R.string.adb_steps),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        
                        SelectionContainer {
                            Text(
                                stringResource(R.string.adb_command),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                        
                        Text(
                            stringResource(R.string.option_shizuku),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Text(
                            stringResource(R.string.shizuku_steps),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPermissionDetailsDialog = false }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    // Preset Creation Dialog
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text(stringResource(R.string.create_new_preset_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text(stringResource(R.string.preset_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.current_values_saved),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        stringResource(R.string.preset_values, windowAnimScale, transitionAnimScale, animatorDurScale),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            presetManager.savePreset(
                                presetName,
                                windowAnimScale,
                                transitionAnimScale,
                                animatorDurScale
                            )
                            allPresets = presetManager.getAllPresets()
                            presetName = ""
                            showPresetDialog = false
                            Toast.makeText(context, context.getString(R.string.preset_saved), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.enter_preset_name), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showPresetDialog = false
                        presetName = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SyncedAnimationPreview(
    currentScale: Float,
    modifier: Modifier = Modifier
) {
    // Base durations
    val baseSlideMs = 300f
    val pauseMs = 600f
    val restMs = 400f

    // Slide-in durations per card (each at own speed, squared for clearer difference)
    val slideIn1x = baseSlideMs
    val slideInCurrent = if (currentScale <= 0f) 0f else baseSlideMs * (currentScale * currentScale)

    // Slide-out durations per card (each at own speed, squared for clearer difference)
    val slideOut1x = baseSlideMs
    val slideOutCurrent = if (currentScale <= 0f) 0f else baseSlideMs * (currentScale * currentScale)

    // Phase timeline (absolute ms):
    // 0 → maxSlideIn             : slide in (each at own speed, faster waits)
    // maxSlideIn → +pauseMs      : both paused at center
    // slideOutStart → +maxSlideOut: slide out (each at own speed, faster waits)
    // maxSlideOut end → +restMs  : rest before restart
    val maxSlideIn = maxOf(slideIn1x, slideInCurrent)
    val slideOutStart = maxSlideIn + pauseMs
    val maxSlideOut = maxOf(slideOut1x, slideOutCurrent)
    val totalCycleMs = slideOutStart + maxSlideOut + restMs

    var elapsedMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentScale) {
        elapsedMs = 0f
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = (now - last) / 1_000_000f
                last = now
                elapsedMs += dt
                if (elapsedMs >= totalCycleMs) {
                    elapsedMs -= totalCycleMs
                }
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppOpenCloseCard(
            label = "1.0x (Default)",
            slideInMs = slideIn1x,
            slideOutStartMs = slideOutStart,
            slideOutMs = slideOut1x,
            totalCycleMs = totalCycleMs,
            elapsedMs = elapsedMs,
            animOff = false,
            isPrimary = true
        )
        AppOpenCloseCard(
            label = "${String.format(java.util.Locale.US, "%.2f", currentScale)}x (Current)",
            slideInMs = slideInCurrent,
            slideOutStartMs = slideOutStart,
            slideOutMs = slideOutCurrent,
            totalCycleMs = totalCycleMs,
            elapsedMs = elapsedMs,
            animOff = currentScale <= 0f,
            isPrimary = false
        )
    }
}

@Composable
private fun AppOpenCloseCard(
    label: String,
    slideInMs: Float,
    slideOutStartMs: Float,
    slideOutMs: Float,
    totalCycleMs: Float,
    elapsedMs: Float,
    animOff: Boolean,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    val progress: Float  // 0 = scaled down/bottom, 1 = scaled up/centered

    when {
        animOff -> {
            progress = 1f
        }
        elapsedMs < slideInMs -> {
            // Popping in from bottom (at own speed)
            val frac = decelerateInterpolation(elapsedMs / slideInMs)
            progress = frac
        }
        elapsedMs < slideOutStartMs -> {
            // Waiting at center (own slide-in done, waiting for slower + pause)
            progress = 1f
        }
        elapsedMs < slideOutStartMs + slideOutMs -> {
            // Popping out to bottom (at own speed)
            val frac = accelerateInterpolation((elapsedMs - slideOutStartMs) / slideOutMs)
            progress = 1f - frac
        }
        else -> {
            // Rest phase — scaled down
            progress = 0f
        }
    }

    val accentColor = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(surfaceColor, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(40.dp)
                        .graphicsLayer(
                            scaleX = 0.4f + 0.6f * progress,
                            scaleY = 0.4f + 0.6f * progress,
                            translationY = (1f - progress) * 100f,
                            alpha = progress
                        )
                        .background(accentColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (progress > 0.3f) "App" else "",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun decelerateInterpolation(input: Float): Float {
    return 1f - (1f - input) * (1f - input)
}

private fun accelerateInterpolation(input: Float): Float {
    return input * input
}