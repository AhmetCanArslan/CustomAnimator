package com.arslan.customanimator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    
    val maxValue = 5.0f
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Animation Speed Controller",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Current Values Display
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Current System Values",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Window Animation", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                String.format("%.2f", windowAnimScale),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Transition Animation", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                String.format("%.2f", transitionAnimScale),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Animator Duration", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                String.format("%.2f", animatorDurScale),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        // Animation Sliders
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Adjust Animation Scales (0.0 - $maxValue)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Window Animation Slider
                    Text("Window Animation Scale: ${String.format("%.2f", windowAnimScale)}", fontSize = 12.sp)
                    Slider(
                        value = windowAnimScale,
                        onValueChange = { 
                            windowAnimScale = it
                            windowInputValue = String.format("%.2f", it)
                        },
                        valueRange = 0f..maxValue,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Transition Animation Slider
                    Text("Transition Animation Scale: ${String.format("%.2f", transitionAnimScale)}", fontSize = 12.sp)
                    Slider(
                        value = transitionAnimScale,
                        onValueChange = { 
                            transitionAnimScale = it
                            transitionInputValue = String.format("%.2f", it)
                        },
                        valueRange = 0f..maxValue,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Animator Duration Slider
                    Text("Animator Duration Scale: ${String.format("%.2f", animatorDurScale)}", fontSize = 12.sp)
                    Slider(
                        value = animatorDurScale,
                        onValueChange = { 
                            animatorDurScale = it
                            animatorInputValue = String.format("%.2f", it)
                        },
                        valueRange = 0f..maxValue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Manual Input Fields
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Manual Input",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = windowInputValue,
                        onValueChange = { 
                            windowInputValue = it
                            val floatVal = it.toFloatOrNull()
                            if (floatVal != null && floatVal in 0f..maxValue) {
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
                            if (floatVal != null && floatVal in 0f..maxValue) {
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
                            if (floatVal != null && floatVal in 0f..maxValue) {
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
        
        // Apply Button
        item {
            Button(
                onClick = {
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
                        Toast.makeText(
                            context,
                            "Error: ${e.message}. You may need WRITE_SECURE_SETTINGS permission.",
                            Toast.LENGTH_LONG
                        ).show()
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
        
        // Preset Management
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Manage Presets",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = presetName,
                            onValueChange = { presetName = it },
                            label = { Text("Preset name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
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
                                    Toast.makeText(context, "Preset saved!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a preset name", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        windowAnimScale = preset.windowAnimationScale
                                        transitionAnimScale = preset.transitionAnimationScale
                                        animatorDurScale = preset.animatorDurationScale
                                        windowInputValue = String.format("%.2f", preset.windowAnimationScale)
                                        transitionInputValue = String.format("%.2f", preset.transitionAnimationScale)
                                        animatorInputValue = String.format("%.2f", preset.animatorDurationScale)
                                        Toast.makeText(context, "Preset loaded!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.width(80.dp)
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
                                    modifier = Modifier.width(80.dp)
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
                    "No presets saved yet. Create one above!",
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