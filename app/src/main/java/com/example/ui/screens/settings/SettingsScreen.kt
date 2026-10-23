package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExportManager
import com.example.ui.MainViewModel
import com.example.ui.components.BanglaAwareText
import com.example.ui.theme.extendedColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val extColors = MaterialTheme.extendedColors

    val themeMode by viewModel.themeMode.collectAsState()
    val colorPalette by viewModel.colorPalette.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val defaultTimerMinutes by viewModel.defaultTimerMinutes.collectAsState()
    val showWordCount by viewModel.showWordCount.collectAsState()
    val enforceWordLimit by viewModel.enforceWordLimit.collectAsState()

    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val lastBackupCount by viewModel.lastBackupCount.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    var localBackups by remember { mutableStateOf(emptyList<java.io.File>()) }

    LaunchedEffect(lastBackupTime) {
        localBackups = com.example.data.backup.BackupManager.listBackups(context)
    }

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Local Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all imported lessons, user drafts, and progress from this device. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllData {
                            Toast.makeText(context, "All data deleted.", Toast.LENGTH_SHORT).show()
                            showDeleteAllDialog = false
                        }
                    }
                ) {
                    Text("Delete Everything", color = extColors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Library Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (localBackups.isNotEmpty()) {
                        Text(
                            "Saved Backup Files on Device:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        localBackups.take(3).forEach { file ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${file.length() / 1024} KB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = extColors.textMuted
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.restoreBackupFromFile(context, file) { success, count ->
                                                if (success) {
                                                    Toast.makeText(context, "Restored $count lessons from file!", Toast.LENGTH_SHORT).show()
                                                    showRestoreDialog = false
                                                } else {
                                                    Toast.makeText(context, "Failed to restore backup file.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Restore", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        "Or paste your exported JSON backup text below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = extColors.textMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Paste JSON backup here...") },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            viewModel.restoreBackup(restoreJsonText) { success, count ->
                                if (success) {
                                    Toast.makeText(context, "Restored $count lessons successfully!", Toast.LENGTH_SHORT).show()
                                    showRestoreDialog = false
                                } else {
                                    Toast.makeText(context, "Failed to restore: Invalid JSON structure.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = restoreJsonText.isNotBlank(),
                    shape = CircleShape
                ) {
                    Text("Restore JSON", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Preferences",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode (Light / Dark / System)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsBrightness,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Appearance Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Choose light, dark, or system default",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.textMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val modes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            modes.forEach { (modeKey, modeLabel) ->
                                val selected = themeMode.equals(modeKey, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setThemeMode(modeKey) },
                                    label = { Text(modeLabel, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Color Palette Picker (Blue, Orange, Green)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Color Palette",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select from 3 fintech-inspired palettes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.textMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val palettes = listOf(
                            Triple("blue", "Ocean Blue", Color(0xFF1D4ED8)),
                            Triple("orange", "Warm Sunset", Color(0xFFEA580C)),
                            Triple("green", "Emerald Mint", Color(0xFF059669))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            palettes.forEach { (key, label, dotColor) ->
                                val selected = colorPalette.equals(key, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setColorPalette(key) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    },
                                    label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Font Size Scaling Slider
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Text & Font Size",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Scales all English and Bengali text",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extColors.textMuted
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${(fontScale * 100).toInt()}%",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        var sliderVal by remember(fontScale) { mutableFloatStateOf(fontScale) }
                        Slider(
                            value = sliderVal,
                            onValueChange = { sliderVal = it },
                            onValueChangeFinished = {
                                viewModel.setFontScale((sliderVal * 100).toInt() / 100f)
                            },
                            valueRange = 0.85f..1.35f,
                            steps = 9
                        )

                        // Live preview box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Preview Text / প্রিভিউ:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extColors.textMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                BanglaAwareText(
                                    text = "MicroWrite: নির্দেশিত অনুশীলন এবং ফোকাস রাইটিং টুল।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Timer Preferences
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Default Timer Duration",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Used when a section does not specify its own timer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.textMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val timerOptions = listOf(3, 5, 7, 10, 15)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            timerOptions.forEach { mins ->
                                val selected = defaultTimerMinutes == mins
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setDefaultTimerMinutes(mins) },
                                    label = { Text("$mins min", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Writing Tools
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Writing Tools & Guidance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Word Count",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Displays live word count badge in writing areas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.textMuted
                                )
                            }
                            Switch(
                                checked = showWordCount,
                                onCheckedChange = { viewModel.setShowWordCount(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enforce Word Limits",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Show warnings when exceeding configured limits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.textMuted
                                )
                            }
                            Switch(
                                checked = enforceWordLimit,
                                onCheckedChange = { viewModel.setEnforceWordLimit(it) }
                            )
                        }
                    }
                }
            }

            // Backup & Restore
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Backup & Data Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "JSON • 100% Offline",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Automatic Daily Backup at 2:00 AM (with Wi-Fi delay)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Automatic Daily Backup (2:00 AM)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Auto-backs up every night at 2:00 AM. If not on Wi-Fi, backs up as soon as Wi-Fi connects.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.textMuted
                                )
                            }
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { viewModel.setAutoBackupEnabled(it, context) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Info Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (lastBackupTime != null) "Last Backup: $lastBackupTime ($lastBackupCount lessons)"
                                               else "No backup performed yet. Tap 'Backup Now' below.",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Folder: MicroWrite_Backups • All lessons & progress saved separately",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = extColors.textMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Primary Action: Backup Now
                        Button(
                            onClick = {
                                viewModel.performManualBackup(context) { success, file ->
                                    if (success && file != null) {
                                        localBackups = com.example.data.backup.BackupManager.listBackups(context)
                                        Toast.makeText(context, "Backup created: ${file.name}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to create backup", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backup_now_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backup All Data Now", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Secondary Actions: Share Backup & Restore Backup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val backups = com.example.data.backup.BackupManager.listBackups(context)
                                    if (backups.isNotEmpty()) {
                                        com.example.data.backup.BackupManager.shareBackupFile(context, backups.first())
                                    } else {
                                        coroutineScope.launch {
                                            val json = viewModel.exportBackupJson()
                                            ExportManager.shareText(context, json, "MicroWrite_Library_Backup.json")
                                        }
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button"),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("Share / Export", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { showRestoreDialog = true },
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_backup_button"),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Backup", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Danger Zone: Reset & Delete
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Data Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = extColors.danger
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All lesson content, progress, and writing drafts reside 100% locally on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = extColors.textMuted
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = extColors.danger),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_all_data_button")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete All Local Data", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // About MicroWrite
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "About MicroWrite",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "MicroWrite is a guided learning and practice tool for focus and argumentative writing. It faithfully renders author-provided resources without generative hallucination, operates completely offline, and respects your privacy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Version 2.0 • Soft Fintech Floating UI",
                            style = MaterialTheme.typography.labelSmall,
                            color = extColors.textMuted
                        )
                    }
                }
            }
        }
    }
}
