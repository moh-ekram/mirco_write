package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Send
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
import com.example.data.backup.BackupManager
import com.example.ui.MainViewModel
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

    val backupFolderUri by viewModel.backupFolderUri.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val lastBackupCount by viewModel.lastBackupCount.collectAsState()

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notificationFrequencyMinutes by viewModel.notificationFrequencyMinutes.collectAsState()
    val notificationMode by viewModel.notificationMode.collectAsState()

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    // Folder selection launcher via Storage Access Framework
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}
            viewModel.setBackupFolderUri(uri.toString())
            Toast.makeText(context, "Backup folder selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Document file picker for restoring progress JSON
    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: ""
                if (json.isNotBlank()) {
                    viewModel.restoreProgressFromJson(context, json) { success, count ->
                        if (success) {
                            Toast.makeText(context, "Restored progress for $count lessons!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not restore: Invalid progress backup file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(context, true)
            Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
            contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Appearance Mode (Default Light)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SettingsBrightness,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val modes = listOf("light" to "Light", "dark" to "Dark", "system" to "System")
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

            // Minimal & Small Color Palette Picker
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Theme Color",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Minimal & small color dots (32dp)
                        val palettes = listOf(
                            "blue" to Color(0xFF1D4ED8),
                            "orange" to Color(0xFFEA580C),
                            "green" to Color(0xFF059669)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            palettes.forEach { (key, dotColor) ->
                                val selected = colorPalette.equals(key, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setColorPalette(key) }
                                        .testTag("color_palette_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bookmark Review Notifications
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Bookmark Notifications",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setNotificationsEnabled(context, enabled)
                                    }
                                },
                                modifier = Modifier.testTag("notifications_switch")
                            )
                        }

                        if (notificationsEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Frequency selection
                            Text(
                                text = "Frequency",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val freqOptions = listOf(
                                15 to "15m",
                                30 to "30m",
                                60 to "1h",
                                120 to "2h",
                                240 to "4h",
                                1440 to "Daily"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                freqOptions.forEach { (mins, label) ->
                                    val isSelected = notificationFrequencyMinutes == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setNotificationFrequency(context, mins) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        shape = CircleShape,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Display mode: Serial vs Random
                            Text(
                                text = "Display Order",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("SERIAL" to "Serial", "RANDOM" to "Random").forEach { (modeKey, label) ->
                                    val isSelected = notificationMode.equals(modeKey, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setNotificationMode(modeKey) },
                                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        shape = CircleShape,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                OutlinedButton(
                                    onClick = {
                                        viewModel.triggerTestNotification(context)
                                        Toast.makeText(context, "Notification sent!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("test_notification_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Progress Backup & Folder Selection (Single file replacement, progress only)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Progress Backup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "Auto-Replace",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Folder Selection Display Box
                        val folderName = BackupManager.getFolderDisplayName(context, backupFolderUri)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Target Folder",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = extColors.textMuted
                                        )
                                        Text(
                                            text = folderName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { folderPickerLauncher.launch(null) },
                                    shape = CircleShape,
                                    modifier = Modifier.testTag("select_folder_button")
                                ) {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Choose")
                                }
                            }
                        }

                        if (lastBackupTime != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Last updated: $lastBackupTime ($lastBackupCount lessons)",
                                style = MaterialTheme.typography.labelSmall,
                                color = extColors.textMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Backup Action: Replaces the same file in selected folder
                        Button(
                            onClick = {
                                viewModel.performProgressBackup(context) { success, file ->
                                    if (success) {
                                        Toast.makeText(context, "Progress backup saved and replaced successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backup_progress_button"),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backup Progress Now", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Restore Action: Matches lesson & section titles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { restoreFilePickerLauncher.launch(arrayOf("application/json", "*/*")) },
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_progress_file_button"),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore File", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { showRestoreDialog = true },
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_progress_json_button"),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("Paste JSON", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Text Scaling Slider
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FormatSize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Text Scale",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${(fontScale * 100).toInt()}%",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

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
                    }
                }
            }

            // Timer & Writing Preferences
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Default Timer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val timerOptions = listOf(3, 5, 7, 10, 15)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Live Word Count",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = showWordCount,
                                onCheckedChange = { viewModel.setShowWordCount(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Enforce Word Limits",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = enforceWordLimit,
                                onCheckedChange = { viewModel.setEnforceWordLimit(it) }
                            )
                        }
                    }
                }
            }

            // Data Reset
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Reset Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = extColors.danger
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = extColors.danger),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_all_data_button")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete All Local Data", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Progress", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Paste the progress backup JSON below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = extColors.textMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        placeholder = { Text("Paste JSON here...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            viewModel.restoreProgressFromJson(context, restoreJsonText) { success, count ->
                                if (success) {
                                    Toast.makeText(context, "Restored progress for $count lessons!", Toast.LENGTH_SHORT).show()
                                    showRestoreDialog = false
                                } else {
                                    Toast.makeText(context, "Failed: Invalid progress JSON structure", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = restoreJsonText.isNotBlank(),
                    shape = CircleShape
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all lessons, drafts, and progress.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllData {
                            Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                            showDeleteAllDialog = false
                        }
                    }
                ) {
                    Text("Delete", color = extColors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
