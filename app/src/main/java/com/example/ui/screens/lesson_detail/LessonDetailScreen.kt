package com.example.ui.screens.lesson_detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.Lesson
import com.example.data.model.LessonSection
import com.example.ui.MainViewModel
import com.example.ui.components.BanglaAwareText
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.LessonProgressIndicator
import com.example.ui.components.LessonTableView
import com.example.ui.components.cleanDisplayTitle
import com.example.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    lessonId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSection: (String, String) -> Unit,
    onNavigateToReview: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(lessonId) {
        viewModel.openLesson(lessonId)
    }

    val lesson by viewModel.currentLesson.collectAsState()
    var isGuidelinesExpanded by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentLesson = lesson
    val extColors = MaterialTheme.extendedColors

    if (currentLesson == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading lesson...", color = extColors.textMuted)
        }
        return
    }

    val nextSection = viewModel.continueLesson(currentLesson)

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Lesson Progress?", fontWeight = FontWeight.Bold) },
            text = { Text("This will mark all sections in this lesson as uncompleted. Your written drafts will NOT be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetLessonProgress(currentLesson.id)
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = extColors.warning, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete This Lesson?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete \"${currentLesson.title}\" and all its drafts?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLesson(currentLesson.id)
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = extColors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
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
                        text = "Lesson Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("lesson_detail_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Hero Card: Lesson Title, Categories, Circular Progress Ring & Primary Action
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = currentLesson.type,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = currentLesson.category,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = extColors.textMuted
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = extColors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "~${currentLesson.estimatedMinutes} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extColors.textMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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

                            Spacer(modifier = Modifier.width(14.dp))

                            CircularProgressRing(
                                progress = currentLesson.completionPercentage / 100f,
                                size = 60.dp,
                                strokeWidth = 6.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${currentLesson.completionPercentage}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Continue (Pill), Review, Reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (nextSection != null) {
                                Button(
                                    onClick = { onNavigateToSection(currentLesson.id, nextSection.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_continue_button"),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentLesson.completedSectionCount == 0) "Start Learning"
                                        else "Continue (${nextSection.order}/${currentLesson.totalSectionCount})",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { onNavigateToReview(currentLesson.id) },
                                shape = CircleShape,
                                modifier = Modifier.testTag("detail_review_button"),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Review", fontWeight = FontWeight.SemiBold)
                            }

                            IconButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("detail_reset_button")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Progress", tint = extColors.textMuted)
                            }
                        }
                    }
                }
            }

            // General Guidelines Section (Expandable button & card)
            val hasGuidelines = currentLesson.guidelines.paragraphs.isNotEmpty() ||
                    currentLesson.guidelines.bullets.isNotEmpty() ||
                    currentLesson.guidelines.tables.isNotEmpty()
            if (hasGuidelines) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isGuidelinesExpanded = !isGuidelinesExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "General Guidelines",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (isGuidelinesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Guidelines",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(visible = isGuidelinesExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                currentLesson.guidelines.paragraphs.forEach { paragraph ->
                                    BanglaAwareText(
                                        text = paragraph,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                currentLesson.guidelines.bullets.forEach { bullet ->
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
                                            color = extColors.textSecondary
                                        )
                                    }
                                }

                                currentLesson.guidelines.tables.forEach { table ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LessonTableView(table = table)
                                }
                            }
                        }
                    }
                }
            }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Practice Sections (${currentLesson.sections.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Section Cards: clean titles, order numbers, status badges
            items(currentLesson.sections) { section ->
                SectionListItemCard(
                    section = section,
                    onClick = { onNavigateToSection(currentLesson.id, section.id) }
                )
            }
        }
    }
}

@Composable
fun SectionListItemCard(
    section: LessonSection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extColors = MaterialTheme.extendedColors
    val (statusColor, statusBg) = when {
        section.isCompleted -> Pair(extColors.success, extColors.successContainer)
        section.isSkipped -> Pair(extColors.warning, extColors.warningContainer)
        section.userDraft.isNotBlank() -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        else -> Pair(extColors.textMuted, MaterialTheme.colorScheme.surfaceVariant)
    }

    val statusText = when {
        section.isCompleted -> "Completed"
        section.isSkipped -> "Skipped"
        section.userDraft.isNotBlank() -> "In Progress"
        else -> "Not Started"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("section_card_${section.order}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon Circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusBg),
                contentAlignment = Alignment.Center
            ) {
                if (section.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = extColors.success,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "${section.order}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BanglaAwareText(
                        text = section.title.cleanDisplayTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = statusBg
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                if (section.task.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = section.task,
                        style = MaterialTheme.typography.bodySmall,
                        color = extColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (section.userDraft.isNotBlank()) {
                    val wordCount = section.userDraft.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Draft saved • $wordCount words",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
