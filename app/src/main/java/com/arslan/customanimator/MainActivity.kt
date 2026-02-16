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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme
import com.arslan.customanimator.utils.PresetManager
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.ShizukuHelper
import rikka.shizuku.Shizuku

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
    var windowInputValue by remember { mutableStateOf(windowAnimScale.toString()) }
    var transitionInputValue by remember { mutableStateOf(transitionAnimScale.toString()) }
    var animatorInputValue by remember { mutableStateOf(animatorDurScale.toString()) }
    
    var presetName by remember { mutableStateOf("") }
    var allPresets by remember { mutableStateOf(presetManager.getAllPresets()) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var expandedPresetId by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var inputMode by remember { mutableStateOf("slider") }
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
                                contentDescription = "Menu"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Create New Preset") },
                                onClick = {
                                    menuExpanded = false
                                    showPresetDialog = true
                                }
                            )
                            Divider()
                            if (inputMode == "slider") {
                                DropdownMenuItem(
                                    text = { Text("Use Manual Input") },
                                    onClick = {
                                        menuExpanded = false
                                        pendingInputMode = "manual"
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Use Sliders") },
                                    onClick = {
                                        menuExpanded = false
                                        pendingInputMode = "slider"
                                    }
                                )
                            }
                            if (isShizukuAvailable) {
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Permission Details") },
                                    onClick = {
                                        menuExpanded = false
                                        showPermissionDetailsDialog = true
                                    }
                                )
                            }
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Source Code") },
                                onClick = {
                                    menuExpanded = false
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.github.com/ahmetcanarslan/customanimator"))
                                    context.startActivity(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Buy Me a Coffee") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
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
                                    text = "Animation Scale Slider",
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
                                        contentDescription = "Restore to default"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        
                        // Window Animation Slider
                        Text("Window Animation Scale (window opening/closing): ${String.format("%.2f", windowAnimScale)}", fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    windowAnimScale = (windowAnimScale - 0.01f).coerceAtLeast(0f)
                                    windowInputValue = String.format("%.2f", windowAnimScale)
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
                                    windowInputValue = String.format("%.2f", it)
                                },
                                valueRange = 0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    windowAnimScale = (windowAnimScale + 0.01f).coerceAtMost(5f)
                                    windowInputValue = String.format("%.2f", windowAnimScale)
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
                        
                        // Transition Animation Slider
                        Text("Transition Animation Scale (screen transitions): ${String.format("%.2f", transitionAnimScale)}", fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    transitionAnimScale = (transitionAnimScale - 0.01f).coerceAtLeast(0f)
                                    transitionInputValue = String.format("%.2f", transitionAnimScale)
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
                                    transitionInputValue = String.format("%.2f", it)
                                },
                                valueRange = 0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    transitionAnimScale = (transitionAnimScale + 0.01f).coerceAtMost(5f)
                                    transitionInputValue = String.format("%.2f", transitionAnimScale)
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
                        Text("Animator Duration Scale (app animations): ${String.format("%.2f", animatorDurScale)}", fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    animatorDurScale = (animatorDurScale - 0.01f).coerceAtLeast(0f)
                                    animatorInputValue = String.format("%.2f", animatorDurScale)
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
                                    animatorInputValue = String.format("%.2f", it)
                                },
                                valueRange = 0f..5.0f,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    animatorDurScale = (animatorDurScale + 0.01f).coerceAtMost(5f)
                                    animatorInputValue = String.format("%.2f", animatorDurScale)
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
            
            // Manual Input Fields
            if (inputMode == "manual") {
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
                                    text = "Manual Input",
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
                                        contentDescription = "Restore to default"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = windowInputValue,
                            onValueChange = { 
                                windowInputValue = it
                                val floatVal = it.toFloatOrNull()
                                if (floatVal != null && floatVal in 0f..5.0f) {
                                    windowAnimScale = floatVal
                                }
                            },
                            label = { Text("Window Animation (window opening/closing)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = transitionInputValue,
                            onValueChange = { 
                                transitionInputValue = it
                                val floatVal = it.toFloatOrNull()
                                if (floatVal != null && floatVal in 0f..5.0f) {
                                    transitionAnimScale = floatVal
                                }
                            },
                            label = { Text("Transition Animation (screen transitions)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = animatorInputValue,
                            onValueChange = { 
                                animatorInputValue = it
                                val floatVal = it.toFloatOrNull()
                                if (floatVal != null && floatVal in 0f..5.0f) {
                                    animatorDurScale = floatVal
                                }
                            },
                            label = { Text("Animator Duration (app animations)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
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
                            permissionErrorMessage = "Permission not granted"
                            showPermissionDialog = true
                            return@Button
                        }

                        try {
                            SettingsManager.applyAllScales(
                                contentResolver,
                                windowAnimScale,
                                transitionAnimScale,
                                animatorDurScale
                            )
                            Toast.makeText(
                                context,
                                "Animation scales updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            permissionErrorMessage = e.message ?: "Unknown error"
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
                    Text("Apply Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Presets List Header
            if (allPresets.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Presets",
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
                                        "W: ${String.format("%.2f", preset.windowAnimationScale)} | T: ${String.format("%.2f", preset.transitionAnimationScale)} | A: ${String.format("%.2f", preset.animatorDurationScale)}",
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
                                                permissionErrorMessage = "Permission not granted"
                                                showPermissionDialog = true
                                                return@Button
                                            }

                                            windowAnimScale = preset.windowAnimationScale
                                            transitionAnimScale = preset.transitionAnimationScale
                                            animatorDurScale = preset.animatorDurationScale
                                            windowInputValue = String.format("%.2f", preset.windowAnimationScale)
                                            transitionInputValue = String.format("%.2f", preset.transitionAnimationScale)
                                            animatorInputValue = String.format("%.2f", preset.animatorDurationScale)
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
                                                    "Error applying preset: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@Button
                                            }
                                            Toast.makeText(context, "Preset loaded and applied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text("Load", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            presetManager.deletePreset(preset.id)
                                            allPresets = presetManager.getAllPresets()
                                            Toast.makeText(context, "Preset deleted!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text("Delete", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "No presets saved yet. Create one from the menu!",
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
            title = { Text("Permission Required") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "WRITE_SECURE_SETTINGS Permission",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        "This app requires the WRITE_SECURE_SETTINGS permission to modify system animation scales. This is a privileged permission that needs special setup.",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        "⏱️ One-Time Only",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "After you grant this permission once, it stays granted permanently. You don't need to do it again!",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (isShizukuAvailable && hasShizukuPermission.value) {
                        Text(
                            "✓ Shizuku Ready",
                            fontSize = 13.sp,
                            color = Color.Green,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "You have Shizuku set up! Open this app again to automatically grant the permission.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else if (isShizukuAvailable && !hasShizukuPermission.value) {
                        Text(
                            "💡 Shizuku Available",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "Shizuku app is installed on your device! It can grant this permission with just one tap (see Permission Details in the menu).",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    if (permissionErrorMessage.isNotEmpty()) {
                        Text(
                            "Error: $permissionErrorMessage",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    // Only show permission granting instructions if permission is not granted
                    if (!hasWriteSecureSettings.value) {
                        Text(
                            "Use ADB Command",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "Connect to a computer and run this command once:",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        SelectionContainer {
                            Text(
                                "adb shell pm grant com.arslan.customanimator android.permission.WRITE_SECURE_SETTINGS",
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
                            "\n📖 See 'Permission Details' in the menu for complete step-by-step instructions.",
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
                        Toast.makeText(context, "Command copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy Command")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showPermissionDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Close")
                }
            }
        )
    }
    
    // Permission Details Dialog
    if (showPermissionDetailsDialog && isShizukuAvailable) {
        AlertDialog(
            onDismissRequest = { showPermissionDetailsDialog = false },
            title = { Text("Permission Details") },
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
                            "✓ Granted",
                            fontSize = 13.sp,
                            color = Color.Green,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            "Congratulations! You can now modify system animation settings directly from this app.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 16.dp),
                            lineHeight = 16.sp
                        )
                    } else {
                        Text(
                            "✗ Not Granted",
                            fontSize = 13.sp,
                            color = Color(0xFFE74C3C),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            "What does this permission do?\n" +
                            "This privileged permission allows the app to modify system animation settings:\n" +
                            "• Window Animation Scale (open/close animations)\n" +
                            "• Transition Animation Scale (activity transitions)\n" +
                            "• Animator Duration Scale (animation timing)\n\n" +
                            "Without it, the app can only read current settings but cannot change them.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                            lineHeight = 16.sp
                        )
                        Text(
                            "Note: You only need to do this ONCE. The permission\n" +
                            "stays granted even after closing and reopening the app or restarting the mobile.",
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
                            "How to Grant Permission:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            "⌨️ Option 1: ADB (Requires Computer)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Text(
                            "Step 1: Enable USB Debugging\n" +
                            "• Go to Settings > About Phone\n" +
                            "• Tap Build Number 7 times to unlock Developer Options\n" +
                            "• Go to Settings > Developer Options > Enable USB Debugging\n\n" +
                            "Step 2: Connect to PC and Run Command\n" +
                            "• Connect your phone to a computer with a USB cable\n" +
                            "• Open terminal/command prompt on the computer\n" +
                            "• Copy and run the command below (one-time only):\n",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        
                        SelectionContainer {
                            Text(
                                "adb shell pm grant com.arslan.customanimator android.permission.WRITE_SECURE_SETTINGS",
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
                            "\n📱 Option 2: Shizuku",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Text(
                            "Step 1: Install Shizuku\n" +
                            "• Download 'Shizuku' from Google Play Store or GitHub\n" +
                            "• Open the app and follow initial setup\n" +
                            "• Grant it device admin permission (one-time, in Shizuku settings)\n\n" +
                            "Step 2: Grant Access to Custom Animator\n" +
                            "• Return to this app (or restart it)\n" +
                            "• A dialog will appear in Shizuku asking to grant permission\n" +
                            "• Tap 'Allow' or 'Grant' to approve (takes 1 second)\n" +
                            "• The WRITE_SECURE_SETTINGS permission is now granted\n\n",
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
                    Text("OK")
                }
            }
        )
    }
    
    // Preset Creation Dialog
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("Create New Preset") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text("Preset name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Current values will be saved:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        "W: ${String.format("%.2f", windowAnimScale)} | T: ${String.format("%.2f", transitionAnimScale)} | A: ${String.format("%.2f", animatorDurScale)}",
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
                            Toast.makeText(context, "Preset saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a preset name", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
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
                    Text("Cancel")
                }
            }
        )
    }
}