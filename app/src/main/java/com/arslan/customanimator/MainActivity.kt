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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme
import com.arslan.customanimator.utils.PresetManager
import com.arslan.customanimator.utils.SettingsManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CustomAnimatorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AnimatorSelectorScreen(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatorSelectorScreen(activity: MainActivity) {
    val context = activity
    val contentResolver = context.contentResolver
    val presetManager = remember { PresetManager(context) }
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Custom Animator",
                        fontSize = 18.sp,
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
                                        inputMode = "manual"
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Use Sliders") },
                                    onClick = {
                                        menuExpanded = false
                                        inputMode = "slider"
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
                    Card(modifier = Modifier.fillMaxWidth()) {
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
                        Text("Window Animation Scale: ${String.format("%.2f", windowAnimScale)}", fontSize = 12.sp)
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
                        Text("Transition Animation Scale: ${String.format("%.2f", transitionAnimScale)}", fontSize = 12.sp)
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
                        Text("Animator Duration Scale: ${String.format("%.2f", animatorDurScale)}", fontSize = 12.sp)
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
                    Card(modifier = Modifier.fillMaxWidth()) {
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
                            label = { Text("Window Animation") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
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
                            label = { Text("Transition Animation") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
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
                            label = { Text("Animator Duration") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
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
                        .height(50.dp),
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
                        modifier = Modifier.padding(16.dp)
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
                        "This app requires the WRITE_SECURE_SETTINGS permission to modify animation scales.",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        "Error: $permissionErrorMessage",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        "To grant this permission, run the following command with ADB:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SelectionContainer {
                        Text(
                            "adb shell pm grant com.arslan.customanimator android.permission.WRITE_SECURE_SETTINGS",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp)
                                .fillMaxWidth()
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