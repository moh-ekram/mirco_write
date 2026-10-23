package com.example.ui.screens.home

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.Lesson
import com.example.ui.MainViewModel
import com.example.ui.components.BanglaAwareText
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.LessonProgressIndicator
import com.example.ui.components.cleanDisplayTitle
import com.example.ui.theme.extendedColors

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToLesson: (String) -> Unit,
    onNavigateToSection: (String, String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allLessons by viewModel.allLessons.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val totalLessons = allLessons.size
    val totalSections = allLessons.sumOf { it.totalSectionCount }
    val completedSections = allLessons.sumOf { it.completedSectionCount }

    // Last opened or first incomplete lesson
    val continueLesson = allLessons.maxByOrNull { it.lastOpenedTimestamp } ?: allLessons.firstOrNull()
    val continueSection = continueLesson?.let { viewModel.continueLesson(it) }

    val extColors = MaterialTheme.extendedColors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header: Minimal avatar + greeting + Quick Import Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MicroWrite",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Focused writing & practice",
                            style = MaterialTheme.typography.bodySmall,
                            color = extColors.textMuted
                        )
                    }
                }

                Button(
                    onClick = onNavigateToImport,
                    modifier = Modifier.testTag("home_import_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import", fontWeight = FontWeight.Bold)
                }
            }
        }


        // Bold Gradient Hero Card: Continue Learning with Circular Progress Ring
        if (continueLesson != null && continueSection != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continue_learning_card")
                        .clickable { onNavigateToSection(continueLesson.id, continueSection.id) },
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(extColors.heroGradient)
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = continueLesson.type,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "CONTINUE PRACTICE",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    BanglaAwareText(
                                        text = continueLesson.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Next Focus Subtitle (NO "Section1" prefix)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        BanglaAwareText(
                                            text = continueSection.title.cleanDisplayTitle(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Circular Progress Ring inside Hero
                                CircularProgressRing(
                                    progress = continueLesson.completionPercentage / 100f,
                                    size = 64.dp,
                                    strokeWidth = 6.dp,
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "${continueLesson.completionPercentage}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action Pill Button inside Hero Card
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onNavigateToSection(continueLesson.id, continueSection.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Resume Practice",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Icon-Tiles Grid (4 Rounded Squircle Tiles)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Default.MenuBook,
                    label = "Library",
                    sub = "${allLessons.size} topics",
                    onClick = onNavigateToLibrary,
                    modifier = Modifier.weight(1f)
                )

                QuickActionTile(
                    icon = Icons.Default.CloudUpload,
                    label = "Import",
                    sub = "Docs & Text",
                    onClick = onNavigateToImport,
                    modifier = Modifier.weight(1f)
                )

                QuickActionTile(
                    icon = Icons.Default.Bookmark,
                    label = "Saved",
                    sub = "Bookmarked",
                    onClick = { /* handled by bottom nav */ onNavigateToLibrary() },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Overall Learning Metric Card
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
                        Text(
                            text = "Overall Study Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$totalLessons Lessons",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LessonProgressIndicator(
                        completed = completedSections,
                        total = totalSections
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$completedSections of $totalSections sections completed across your imported resources.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extColors.textMuted
                    )
                }
            }
        }


    }
}

@Composable
private fun QuickActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extColors = MaterialTheme.extendedColors
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = extColors.textMuted
            )
        }
    }
}

@Composable
fun LessonListItemCard(
    lesson: Lesson,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val extColors = MaterialTheme.extendedColors
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Lesson?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${lesson.title}\"? All drafts and progress will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = extColors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lesson_item_${lesson.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = lesson.type,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lesson.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = extColors.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    BanglaAwareText(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )


                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (lesson.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (lesson.isBookmarked) MaterialTheme.colorScheme.primary else extColors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = extColors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LessonProgressIndicator(
                completed = lesson.completedSectionCount,
                total = lesson.totalSectionCount
            )
        }
    }
}
