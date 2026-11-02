package com.example.ui.screens.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReelBackgroundTheme
import com.example.data.model.ReelFilterMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsSettingsSheet(
    viewModel: ReelsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settings by viewModel.settings.collectAsState()
    val doc by viewModel.document.collectAsState()
    val availableDocs by viewModel.availableDocs.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val filterQuery by viewModel.filterQuery.collectAsState()
    val isLoadingDoc by viewModel.isLoadingDoc.collectAsState()
    val docLoadError by viewModel.docLoadError.collectAsState()
    val audioTracks by viewModel.audioManager.tracks.collectAsState()
    val isPlaying by viewModel.audioManager.isPlaying.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()

    val totalPagesCount by viewModel.totalPagesCount.collectAsState()
    val unseenPagesCount by viewModel.unseenPagesCount.collectAsState()
    val seenPagesCount by viewModel.seenPagesCount.collectAsState()

    var docUrlInput by remember { mutableStateOf(settings.docUrl) }
    var newDriveUrlInput by remember { mutableStateOf("") }
    var newDriveTitleInput by remember { mutableStateOf("") }

    val highlightColors = listOf(
        Pair("Amber Gold", "#FFB703"),
        Pair("Neon Cyan", "#00F0FF"),
        Pair("Mint Green", "#10B981"),
        Pair("Coral Red", "#F43F5E"),
        Pair("Royal Purple", "#A855F7")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            // Minimal Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reels Settings & Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("reels_settings_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }

                // SECTION 1: Document Selection
                item {
                    MinimalSection(title = "Document Selection", icon = Icons.Default.Description) {
                        // Document picker chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableDocs.forEach { availableDoc ->
                                val isSelected = availableDoc.title == doc.title
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        )
                                        .clickable { viewModel.selectDocument(availableDoc) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = availableDoc.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${availableDoc.pages.size} slides",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // URL input for custom Google Doc / DOCX
                        OutlinedTextField(
                            value = docUrlInput,
                            onValueChange = { docUrlInput = it },
                            label = { Text("Google Docs or DOCX URL") },
                            placeholder = { Text("https://docs.google.com/document/d/...") },
                            singleLine = true,
                            trailingIcon = {
                                if (docUrlInput.isNotEmpty()) {
                                    IconButton(onClick = { docUrlInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reels_doc_url_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (docLoadError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = docLoadError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.loadDocumentFromUrl(docUrlInput) },
                                enabled = !isLoadingDoc && docUrlInput.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reels_load_doc_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isLoadingDoc) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fetching...", fontSize = 13.sp)
                                } else {
                                    Text("Fetch URL", fontSize = 13.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    docUrlInput = ""
                                    viewModel.loadDefaultDocument()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset Default", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // SECTION 2: Seen / Unseen Reels Filter
                item {
                    MinimalSection(title = "Reels Filter", icon = Icons.Default.FilterList) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = filterMode == ReelFilterMode.ALL,
                                onClick = { viewModel.setFilterMode(ReelFilterMode.ALL) },
                                label = { Text("All ($totalPagesCount)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = filterMode == ReelFilterMode.UNSEEN,
                                onClick = { viewModel.setFilterMode(ReelFilterMode.UNSEEN) },
                                label = { Text("Unseen ($unseenPagesCount)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = filterMode == ReelFilterMode.SEEN,
                                onClick = { viewModel.setFilterMode(ReelFilterMode.SEEN) },
                                label = { Text("Seen ($seenPagesCount)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = filterQuery,
                                onValueChange = { viewModel.setFilterQuery(it) },
                                placeholder = { Text("Filter keywords...") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    if (filterQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setFilterQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reels_filter_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.resetSeenPages() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Seen", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // SECTION 3: Mode & Reading Experience
                item {
                    MinimalSection(title = "Display & Fullscreen", icon = Icons.Default.Fullscreen) {
                        // Fullscreen Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fullscreen Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = isFullscreen,
                                onCheckedChange = { viewModel.setFullscreen(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Text Size
                        Text(
                            text = "Font Size: ${settings.fontSizeSp.roundToInt()} sp",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = settings.fontSizeSp,
                            onValueChange = { size ->
                                viewModel.updateSettings { it.copy(fontSizeSp = size) }
                            },
                            valueRange = 16f..32f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Colorize Numbers & Dates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Colorize Numbers & Dates",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = settings.highlightNumbers,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings { it.copy(highlightNumbers = checked) }
                                }
                            )
                        }

                        if (settings.highlightNumbers) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                highlightColors.forEach { (_, hex) ->
                                    val isSelected = settings.numberHighlightColorHex.equals(hex, ignoreCase = true)
                                    val parsedColor = Color(android.graphics.Color.parseColor(hex))
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                viewModel.updateSettings { it.copy(numberHighlightColorHex = hex) }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 4: Background Music
                item {
                    MinimalSection(title = "Background Music", icon = Icons.Default.LibraryMusic) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Background Music",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = settings.musicEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings { it.copy(musicEnabled = checked) }
                                }
                            )
                        }

                        if (settings.musicEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Change Track Per Slide",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Switch(
                                    checked = settings.randomTrackPerPage,
                                    onCheckedChange = { checked ->
                                        viewModel.updateSettings { it.copy(randomTrackPerPage = checked) }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Volume: ${(settings.volume * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = settings.volume,
                                onValueChange = { vol ->
                                    viewModel.updateSettings { it.copy(volume = vol) }
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Add Google Drive Audio Link
                            OutlinedTextField(
                                value = newDriveUrlInput,
                                onValueChange = { newDriveUrlInput = it },
                                label = { Text("Google Drive Audio Link") },
                                placeholder = { Text("https://drive.google.com/file/d/...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newDriveTitleInput,
                                    onValueChange = { newDriveTitleInput = it },
                                    label = { Text("Track Label (Optional)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        if (newDriveUrlInput.isNotBlank()) {
                                            viewModel.addDriveAudioTrack(newDriveUrlInput, newDriveTitleInput)
                                            newDriveUrlInput = ""
                                            newDriveTitleInput = ""
                                        }
                                    },
                                    enabled = newDriveUrlInput.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text("Add", fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Compact track listing
                            audioTracks.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!track.isPreset) {
                                        IconButton(
                                            onClick = { viewModel.removeDriveAudioTrack(track.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 5: Theme & Auto-Scroll
                item {
                    MinimalSection(title = "Theme & Auto-Scroll", icon = Icons.Default.Palette) {
                        Text(
                            text = "Background Theme:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ReelBackgroundTheme.values().forEach { themeOption ->
                                val isSelected = settings.theme == themeOption
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .clickable { viewModel.updateSettings { it.copy(theme = themeOption) } }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = themeOption.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Auto-Advance Slides:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val speeds = listOf(0, 5, 8, 12, 16, 20)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            speeds.forEach { sec ->
                                val selected = settings.autoScrollSeconds == sec
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        viewModel.updateSettings { it.copy(autoScrollSeconds = sec) }
                                    },
                                    label = { Text(if (sec == 0) "Off" else "${sec}s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("reels_settings_apply_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun MinimalSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
