package com.example.ui.screens.review

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.export.ExportManager
import com.example.data.export.ExportMode
import com.example.ui.MainViewModel
import com.example.ui.components.BanglaAwareText
import com.example.ui.components.cleanDisplayTitle
import com.example.ui.theme.extendedColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalReviewScreen(
    lessonId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSection: ((String, String) -> Unit)? = null,
    onNavigateToHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val extColors = MaterialTheme.extendedColors

    LaunchedEffect(lessonId) {
        viewModel.openLesson(lessonId)
    }

    val lesson by viewModel.currentLesson.collectAsState()

    var viewMode by remember { mutableStateOf(ExportMode.USER_WRITING_ONLY) }
    var exportMode by remember { mutableStateOf(ExportMode.USER_WRITING_ONLY) }
    var showExportSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val currentLesson = lesson
    if (currentLesson == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading review...", color = extColors.textMuted)
        }
        return
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Export Options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Content to include:",
                    style = MaterialTheme.typography.labelMedium,
                    color = extColors.textMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExportMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportMode == mode,
                            onClick = { exportMode = mode }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isExporting) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PDF Export Button
                        Button(
                            onClick = {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val uri = ExportManager.generatePdf(context, currentLesson, exportMode)
                                        showExportSheet = false
                                        ExportManager.shareExportedFile(context, uri, "application/pdf", "Share Lesson PDF")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_button"),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF (A4)", fontWeight = FontWeight.Bold)
                        }

                        // JPEG Export Button
                        OutlinedButton(
                            onClick = {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val uris = ExportManager.generateJpegPages(context, currentLesson, exportMode)
                                        showExportSheet = false
                                        if (uris.isNotEmpty()) {
                                            ExportManager.shareExportedFile(context, uris.first(), "image/jpeg", "Share Lesson Image")
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_jpeg_button"),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JPEG", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Copy Text
                        OutlinedButton(
                            onClick = {
                                val text = ExportManager.generateFormattedText(currentLesson, exportMode)
                                ExportManager.copyToClipboard(context, text, currentLesson.title)
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                showExportSheet = false
                            },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).testTag("export_copy_button"),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Text", fontWeight = FontWeight.SemiBold)
                        }

                        // Share Text
                        OutlinedButton(
                            onClick = {
                                val text = ExportManager.generateFormattedText(currentLesson, exportMode)
                                ExportManager.shareText(context, text, currentLesson.title)
                                showExportSheet = false
                            },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).testTag("export_share_button"),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Final Review",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("review_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showExportSheet = true },
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("review_export_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontWeight = FontWeight.Bold)
                    }
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lesson Header Floating Card
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
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    text = currentLesson.type,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (currentLesson.completedSectionCount == currentLesson.totalSectionCount) extColors.successContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${currentLesson.completedSectionCount}/${currentLesson.totalSectionCount} Completed",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentLesson.completedSectionCount == currentLesson.totalSectionCount) extColors.success else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        BanglaAwareText(
                            text = currentLesson.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        currentLesson.titleBn?.let { bn ->
                            if (bn.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                BanglaAwareText(
                                    text = bn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // View Mode Selector (Tabs)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    TabRow(
                        selectedTabIndex = viewMode.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = viewMode == ExportMode.USER_WRITING_ONLY,
                            onClick = { viewMode = ExportMode.USER_WRITING_ONLY },
                            text = { Text("My Writing", fontWeight = if (viewMode == ExportMode.USER_WRITING_ONLY) FontWeight.Bold else FontWeight.Medium) }
                        )
                        Tab(
                            selected = viewMode == ExportMode.MODEL_ANSWERS_ONLY,
                            onClick = { viewMode = ExportMode.MODEL_ANSWERS_ONLY },
                            text = { Text("Model Answers", fontWeight = if (viewMode == ExportMode.MODEL_ANSWERS_ONLY) FontWeight.Bold else FontWeight.Medium) }
                        )
                        Tab(
                            selected = viewMode == ExportMode.BOTH,
                            onClick = { viewMode = ExportMode.BOTH },
                            text = { Text("Both", fontWeight = if (viewMode == ExportMode.BOTH) FontWeight.Bold else FontWeight.Medium) }
                        )
                    }
                }
            }

            // Checklist from Final Review
            if (currentLesson.finalReview.items.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Final Review Checklist",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            currentLesson.finalReview.items.forEach { item ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = extColors.success,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BanglaAwareText(
                                        text = item,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sections Content Cards: Clean Titles via cleanDisplayTitle()
            items(currentLesson.sections, key = { it.id }) { sec ->
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
                            BanglaAwareText(
                                text = sec.title.cleanDisplayTitle(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (sec.isCompleted) extColors.successContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (sec.isCompleted) "Completed" else if (sec.isSkipped) "Skipped" else "Not Done",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sec.isCompleted) extColors.success else extColors.textMuted
                                )
                            }
                        }

                        if (viewMode == ExportMode.USER_WRITING_ONLY || viewMode == ExportMode.BOTH) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Your Draft:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = extColors.textMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (sec.userDraft.isNotBlank()) {
                                BanglaAwareText(
                                    text = sec.userDraft,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = "(No draft written)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.textMuted
                                )
                            }
                        }

                        if (viewMode == ExportMode.MODEL_ANSWERS_ONLY || viewMode == ExportMode.BOTH) {
                            if (viewMode == ExportMode.BOTH) {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                            Text(
                                text = "Model Answer:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (sec.modelAnswer.isNotBlank()) {
                                BanglaAwareText(
                                    text = sec.modelAnswer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = "(No model answer provided)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.textMuted
                                )
                            }
                        }
                    }
                }
            }

            // Return to Home Button at bottom
            if (onNavigateToHome != null) {
                item {
                    Button(
                        onClick = onNavigateToHome,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finish & Return to Home", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
