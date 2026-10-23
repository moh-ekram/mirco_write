package com.example.ui.screens.study

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExportManager
import com.example.ui.MainViewModel
import com.example.ui.components.BanglaAwareText
import com.example.ui.components.LessonTableView
import com.example.ui.components.SectionTimerControl
import com.example.ui.components.SegmentedSectionProgressBar
import com.example.ui.components.VocabularySection
import com.example.ui.components.cleanDisplayTitle
import com.example.ui.theme.extendedColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionStudyScreen(
    lessonId: String,
    sectionId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSection: (String, String) -> Unit,
    onNavigateToReview: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lessonId, sectionId) {
        viewModel.openSection(lessonId, sectionId)
    }

    val lesson by viewModel.currentLesson.collectAsState()
    val section by viewModel.currentSection.collectAsState()
    val defaultTimerMinutes by viewModel.defaultTimerMinutes.collectAsState()
    val showWordCount by viewModel.showWordCount.collectAsState()
    val timerIsRunning by viewModel.timerIsRunning.collectAsState()
    val focusLockEnabled by viewModel.focusLockEnabled.collectAsState()

    val answerFontSize by viewModel.answerFontSize.collectAsState()
    val answerLineSelectionMode by viewModel.answerLineSelectionMode.collectAsState()
    val bookmarkedLines by viewModel.bookmarkedLines.collectAsState()

    var showModelAnswer by remember(sectionId) { mutableStateOf(false) }
    var showWritingArea by remember(sectionId) { mutableStateOf(false) }
    var showAnswerSettings by remember { mutableStateOf(false) }

    val currentLesson = lesson
    val currentSection = section

    if (currentLesson == null || currentSection == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading section...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val sections = currentLesson.sections
    val currentIndex = sections.indexOfFirst { it.id == currentSection.id }
    val prevSection = if (currentIndex > 0) sections[currentIndex - 1] else null
    val nextSection = if (currentIndex >= 0 && currentIndex < sections.size - 1) sections[currentIndex + 1] else null

    // Determine timer seconds: section timer if > 0, else settings default minutes
    val timerDurationSeconds = if (currentSection.timerSeconds > 0) currentSection.timerSeconds else defaultTimerMinutes * 60

    // Local draft state with debounced auto-saving
    var draftText by remember(currentSection.id) { mutableStateOf(currentSection.userDraft) }
    var saveStatus by remember { mutableStateOf("Saved ✓") }
    val words = draftText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val wordCount = if (draftText.isBlank()) 0 else words.size

    // Debounced draft saving (~600ms)
    LaunchedEffect(draftText) {
        if (draftText != currentSection.userDraft) {
            saveStatus = "Saving..."
            delay(600L)
            viewModel.saveDraft(currentSection.id, draftText)
            saveStatus = "Saved ✓"
        }
    }

    // Immediate flush on dispose / leaving section
    DisposableEffect(currentSection.id) {
        onDispose {
            if (draftText != currentSection.userDraft) {
                viewModel.saveDraft(currentSection.id, draftText)
            }
        }
    }

    // Auto-save on system back press without blocking
    BackHandler {
        if (draftText != currentSection.userDraft) {
            viewModel.saveDraft(currentSection.id, draftText)
        }
        onNavigateBack()
    }

    val extColors = MaterialTheme.extendedColors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Column {
                            BanglaAwareText(
                                text = currentSection.title.cleanDisplayTitle(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentLesson.title} (${currentSection.order}/${sections.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = extColors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (draftText != currentSection.userDraft) {
                                    viewModel.saveDraft(currentSection.id, draftText)
                                }
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("study_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        // Skip for later action button
                        IconButton(
                            onClick = {
                                if (focusLockEnabled && timerIsRunning) {
                                    Toast.makeText(context, "Focus Lock is active. Finish or pause timer to skip.", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                viewModel.recordSectionAction(currentSection.id, currentSection.isCompleted, currentSection.isSkipped)
                                viewModel.markSectionSkipped(currentSection.id)

                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Section skipped",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoLastSectionAction()
                                    }
                                }

                                if (nextSection != null) {
                                    onNavigateToSection(lessonId, nextSection.id)
                                } else {
                                    onNavigateToReview(lessonId)
                                }
                            },
                            modifier = Modifier.testTag("study_skip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Skip for later",
                                tint = extColors.warning
                            )
                        }

                        // Bookmark toggle
                        IconButton(
                            onClick = { viewModel.toggleSectionBookmark(currentSection.id) },
                            modifier = Modifier.testTag("study_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (currentSection.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark Section",
                                tint = if (currentSection.isBookmarked) MaterialTheme.colorScheme.primary else extColors.textMuted
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Segmented progress bar across all sections
                SegmentedSectionProgressBar(
                    totalSections = sections.size,
                    currentIndex = currentIndex,
                    completedIndices = sections.indices.filter { sections[it].isCompleted }.toSet(),
                    skippedIndices = sections.indices.filter { sections[it].isSkipped }.toSet(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        },
        bottomBar = {
            // Minimal Iconized Grid Bar: Prev, Write, Ans., Next in a single line
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Prev Grid Box
                    Surface(
                        onClick = {
                            if (prevSection != null) {
                                if (draftText != currentSection.userDraft) {
                                    viewModel.saveDraft(currentSection.id, draftText)
                                }
                                onNavigateToSection(lessonId, prevSection.id)
                            }
                        },
                        enabled = prevSection != null,
                        shape = RoundedCornerShape(16.dp),
                        color = if (prevSection != null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (prevSection != null) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("study_prev_button")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Prev",
                                tint = if (prevSection != null) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Prev",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (prevSection != null) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        }
                    }

                    // 2. Write Grid Box (Opens bottom popup for writing)
                    Surface(
                        onClick = {
                            showModelAnswer = false
                            showWritingArea = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("study_toggle_writing_button")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Write",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (draftText.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Write",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 3. Ans. Grid Box (Opens bottom popup for model answer)
                    Surface(
                        onClick = {
                            showWritingArea = false
                            showModelAnswer = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("toggle_model_answer_button")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Answer",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Ans.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // 4. Next / Done Grid Box
                    Surface(
                        onClick = {
                            if (focusLockEnabled && timerIsRunning) {
                                Toast.makeText(context, "Focus Lock is active. Complete or pause the timer first.", Toast.LENGTH_SHORT).show()
                                return@Surface
                            }

                            if (draftText != currentSection.userDraft) {
                                viewModel.saveDraft(currentSection.id, draftText)
                            }

                            viewModel.recordSectionAction(currentSection.id, currentSection.isCompleted, currentSection.isSkipped)
                            viewModel.markSectionDone(currentSection.id)

                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Section marked done",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoLastSectionAction()
                                }
                            }

                            if (nextSection != null) {
                                onNavigateToSection(lessonId, nextSection.id)
                            } else {
                                onNavigateToReview(lessonId)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("study_done_button")
                            .testTag("study_next_button")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (nextSection != null) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                                contentDescription = if (nextSection != null) "Next" else "Done",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (nextSection != null) "Next" else "Done",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header Card: Clean Title, Focus Type, Completion Status
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
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = currentSection.type,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (currentSection.isCompleted) {
                                Surface(shape = RoundedCornerShape(12.dp), color = extColors.successContainer) {
                                    Text(
                                        text = "Completed",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = extColors.success
                                    )
                                }
                            } else if (currentSection.isSkipped) {
                                Surface(shape = RoundedCornerShape(12.dp), color = extColors.warningContainer) {
                                    Text(
                                        text = "Skipped",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = extColors.warning
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        BanglaAwareText(
                            text = currentSection.title.cleanDisplayTitle(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Practice Countdown Timer (Survives rotation, drift-free)
            if (timerDurationSeconds > 0) {
                item {
                    SectionTimerControl(
                        viewModel = viewModel,
                        initialSeconds = timerDurationSeconds
                    )
                }
            }

            // Section Guide & Learning Resources Card (Guideline)
            val hasGuideContent = currentSection.guide.paragraphs.isNotEmpty() ||
                    currentSection.guide.bullets.isNotEmpty() ||
                    currentSection.guide.tables.isNotEmpty()
            if (hasGuideContent) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Guideline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            currentSection.guide.paragraphs.forEach { paragraph ->
                                BanglaAwareText(
                                    text = paragraph,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            currentSection.guide.bullets.forEach { bullet ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    BanglaAwareText(
                                        text = bullet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Tables
                            currentSection.guide.tables.forEach { table ->
                                Spacer(modifier = Modifier.height(10.dp))
                                LessonTableView(table = table)
                            }
                        }
                    }
                }
            }

        }
    }

    // Bottom Pop-up for Writing Area (Ans. & write বাটনে ক্লিক করলে নিচের থেকে একটা পপ-আপ মূল স্ক্রিনে আসবে)
    if (showWritingArea) {
        ModalBottomSheet(
            onDismissRequest = {
                if (draftText != currentSection.userDraft) {
                    viewModel.saveDraft(currentSection.id, draftText)
                }
                showWritingArea = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                // Pop-up Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Writing Area",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (saveStatus.contains("✓")) extColors.successContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = saveStatus,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (saveStatus.contains("✓")) extColors.success else extColors.textMuted
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showWordCount) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$wordCount words",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = {
                                if (draftText != currentSection.userDraft) {
                                    viewModel.saveDraft(currentSection.id, draftText)
                                }
                                showWritingArea = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = extColors.textMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Text Input Area
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { newDraft ->
                        draftText = newDraft
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("study_writing_input"),
                    placeholder = {
                        Text(
                            text = "Compose your answer here...",
                            color = extColors.textMuted
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pop-up Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (draftText.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                ExportManager.copyToClipboard(context, draftText, "Draft")
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Draft", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (draftText != currentSection.userDraft) {
                                viewModel.saveDraft(currentSection.id, draftText)
                            }
                            showWritingArea = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Bottom Pop-up for Model Answer (Ans. বাটন ক্লিক করলে পপ-আপ)
    if (showModelAnswer) {
        val answerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = {
                showModelAnswer = false
                showAnswerSettings = false
            },
            sheetState = answerSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .navigationBarsPadding()
            ) {
                // Header: Settings gear replaces 'X' button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Model Answer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentSection.modelAnswer.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    ExportManager.copyToClipboard(
                                        context,
                                        currentSection.modelAnswer,
                                        "Model Answer"
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Model Answer",
                                    tint = extColors.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Settings Gear button (replacing 'X' button as requested)
                        IconButton(
                            onClick = { showAnswerSettings = !showAnswerSettings },
                            modifier = Modifier.testTag("answer_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Answer Settings",
                                tint = if (showAnswerSettings) MaterialTheme.colorScheme.primary else extColors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Settings Panel (Font Size Controls & Line Selection Toggle)
                AnimatedVisibility(visible = showAnswerSettings) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .testTag("answer_settings_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Answer Settings",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Auto-saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. Font Size Control
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Font Size: ${answerFontSize.toInt()} sp",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(14f, 16f, 18f, 20f, 22f).forEach { size ->
                                        val isSelected = answerFontSize.toInt() == size.toInt()
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .clickable { viewModel.setAnswerFontSize(size) }
                                                .testTag("font_size_${size.toInt()}")
                                        ) {
                                            Text(
                                                text = "${size.toInt()}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Line Selection Toggle Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Select lines separately",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "প্রতিটি লাইন আলাদাভাবে স্পর্শ করে বুকমার্কে সংরক্ষণ করুন",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Switch(
                                    checked = answerLineSelectionMode,
                                    onCheckedChange = { viewModel.setAnswerLineSelectionMode(it) },
                                    modifier = Modifier.testTag("line_selection_toggle")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Helpful banner when line selection mode is active
                if (answerLineSelectionMode) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, Color(0xFF07894A).copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color(0xFF07894A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "যে কোনো লাইন স্পর্শ করুন — সরাসরি বুকমার্ক মেনুতে সংরক্ষিত হবে",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF07894A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Answer Content
                if (currentSection.modelAnswer.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (answerLineSelectionMode) {
                            // Touch-to-select line mode
                            val answerLines = remember(currentSection.modelAnswer) {
                                val raw = currentSection.modelAnswer.trim()
                                val paragraphs = raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                                val result = mutableListOf<String>()
                                for (p in paragraphs) {
                                    if (p.contains(Regex("[।!?.]\\s+"))) {
                                        val sentences = p.split(Regex("(?<=[।!?.])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
                                        result.addAll(sentences)
                                    } else {
                                        result.add(p)
                                    }
                                }
                                if (result.isEmpty()) listOf(raw) else result
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                answerLines.forEachIndexed { index, lineText ->
                                    val isBookmarked = bookmarkedLines.any {
                                        it.sectionId == currentSection.id && it.lineText.trim() == lineText.trim()
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isBookmarked) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = if (isBookmarked) {
                                            BorderStroke(1.5.dp, Color(0xFF07894A))
                                        } else {
                                            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.toggleLineBookmark(
                                                    lessonId = currentLesson.id,
                                                    sectionId = currentSection.id,
                                                    lessonTitle = currentLesson.title,
                                                    sectionTitle = currentSection.title.cleanDisplayTitle(),
                                                    lineText = lineText.trim()
                                                ) { added ->
                                                    Toast.makeText(
                                                        context,
                                                        if (added) "লাইনটি বুকমার্ক মেনুতে সংরক্ষিত হয়েছে ✓" else "লাইনটি বুকমার্ক থেকে সরানো হয়েছে",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            .testTag("answer_line_$index")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            BanglaAwareText(
                                                text = lineText,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = answerFontSize.sp,
                                                    lineHeight = (answerFontSize * 1.5f).sp
                                                ),
                                                color = if (isBookmarked) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = if (isBookmarked) "Saved to bookmarks" else "Touch to bookmark line",
                                                tint = if (isBookmarked) Color(0xFF07894A) else extColors.textMuted.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Rich standard display
                            com.example.ui.components.RichStudyContentView(
                                content = currentSection.modelAnswer,
                                fontSize = answerFontSize.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No model answer provided for this section.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extColors.textMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        showModelAnswer = false
                        showAnswerSettings = false
                    },
                    modifier = Modifier.fillMaxWidth().testTag("close_model_answer_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
