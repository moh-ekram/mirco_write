package com.example.ui.screens.bookmarks

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExportManager
import com.example.data.model.BookmarkedLine
import com.example.data.model.LessonSection
import com.example.ui.MainViewModel
import com.example.ui.components.BanglaAwareText
import com.example.ui.components.cleanDisplayTitle
import com.example.ui.screens.home.LessonListItemCard
import com.example.ui.theme.extendedColors

@Composable
fun BookmarksScreen(
    viewModel: MainViewModel,
    onNavigateToLesson: (String) -> Unit,
    onNavigateToSection: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bookmarkedLines by viewModel.bookmarkedLines.collectAsState()
    val bookmarkedLessons by viewModel.bookmarkedLessons.collectAsState()
    val bookmarkedSections by viewModel.bookmarkedSections.collectAsState()
    val allLessons by viewModel.allLessons.collectAsState()
    val extColors = MaterialTheme.extendedColors

    var showClearDialog by remember { mutableStateOf(false) }

    // If user has other legacy bookmarks, give them access via tabs with Selected Lines as the primary tab
    val hasOtherBookmarks = bookmarkedLessons.isNotEmpty() || bookmarkedSections.isNotEmpty()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bookmarks",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${bookmarkedLines.size} selected line${if (bookmarkedLines.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = extColors.textMuted
                )
            }

            if (selectedTabIndex == 0 && bookmarkedLines.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val combined = bookmarkedLines.joinToString("\n\n") { "• ${it.lineText} (${it.sectionTitle})" }
                            ExportManager.copyToClipboard(context, combined, "Selected Lines")
                            Toast.makeText(context, "সবগুলো লাইন কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_all_lines_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy all lines",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("clear_all_lines_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear all lines",
                            tint = extColors.textMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab selection (Primary is Selected Lines; secondary tabs only if user has bookmarked lessons/sections)
        if (hasOtherBookmarks) {
            val tabs = listOf(
                "Selected Lines (${bookmarkedLines.size})",
                "Lessons (${bookmarkedLessons.size})",
                "Sections (${bookmarkedSections.size})"
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        when (selectedTabIndex) {
            0 -> {
                // Dedicated Selected Lines View (The primary bookmark menu content)
                if (bookmarkedLines.isEmpty()) {
                    EmptySelectedLinesView()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("bookmarked_lines_list"),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bookmarkedLines, key = { it.id }) { line ->
                            BookmarkedLineCard(
                                line = line,
                                onCopy = {
                                    ExportManager.copyToClipboard(context, line.lineText, "Bookmarked Line")
                                    Toast.makeText(context, "লাইনটি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                onRemove = {
                                    viewModel.removeBookmarkedLine(line.id)
                                    Toast.makeText(context, "বুকমার্ক থেকে সরানো হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                onNavigateToSection = {
                                    onNavigateToSection(line.lessonId, line.sectionId)
                                }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Bookmarked Lessons
                if (bookmarkedLessons.isEmpty()) {
                    EmptyBookmarksView(message = "No bookmarked lessons yet.\nTap the bookmark icon on any lesson to save it here.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(bookmarkedLessons, key = { it.id }) { lesson ->
                            LessonListItemCard(
                                lesson = lesson,
                                onClick = { onNavigateToLesson(lesson.id) },
                                onBookmarkToggle = { viewModel.toggleLessonBookmark(lesson.id) },
                                onDelete = { viewModel.deleteLesson(lesson.id) }
                            )
                        }
                    }
                }
            }
            2 -> {
                // Bookmarked Sections
                if (bookmarkedSections.isEmpty()) {
                    EmptyBookmarksView(message = "No bookmarked sections yet.\nTap the bookmark icon inside any section to quickly revisit it here.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(bookmarkedSections, key = { it.id }) { section ->
                            val parentLesson = allLessons.find { it.id == section.lessonId }
                            BookmarkedSectionCard(
                                section = section,
                                parentLessonTitle = parentLesson?.title ?: "Lesson",
                                onClick = { onNavigateToSection(section.lessonId, section.id) },
                                onBookmarkToggle = { viewModel.toggleSectionBookmark(section.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("সবগুলো সিলেক্ট করা লাইন মুছবেন?") },
            text = { Text("এই তালিকার সব বুকমার্ক করা লাইন মুছে ফেলা হবে। আপনি কি নিশ্চিত?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllBookmarkedLines()
                        showClearDialog = false
                        Toast.makeText(context, "সকল সংরক্ষিত লাইন মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("মুছে ফেলুন", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun BookmarkedLineCard(
    line: BookmarkedLine,
    onCopy: () -> Unit,
    onRemove: () -> Unit,
    onNavigateToSection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extColors = MaterialTheme.extendedColors

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bookmarked_line_${line.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Source Tag (Lesson & Section)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.clickable(onClick = onNavigateToSection)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${line.lessonTitle} • ${line.sectionTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Line",
                            tint = extColors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove from bookmarks",
                            tint = extColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // The Selected Line Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = Color(0xFF07894A),
                    modifier = Modifier.size(20.dp).padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                BanglaAwareText(
                    text = line.lineText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer jump to section action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSection),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Go to section",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun EmptySelectedLinesView() {
    val extColors = MaterialTheme.extendedColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = Color(0xFF07894A),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Selected Lines Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "যেকোনো উত্তরের (Ans.) পপ-আপে গিয়ার আইকন চাপুন। এরপর 'Select lines separately' অন করে উত্তরের যেকোনো লাইনে স্পর্শ করলেই সেই লাইনটি এখানে আলাদাভাবে জমা হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = extColors.textMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BookmarkedSectionCard(
    section: LessonSection,
    parentLessonTitle: String,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extColors = MaterialTheme.extendedColors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("bookmarked_section_${section.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                            text = "Part ${section.order}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = parentLessonTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = extColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                BanglaAwareText(
                    text = section.title.cleanDisplayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (section.task.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = section.task,
                        style = MaterialTheme.typography.bodySmall,
                        color = extColors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onBookmarkToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Unbookmark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyBookmarksView(message: String) {
    val extColors = MaterialTheme.extendedColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Bookmarks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = extColors.textMuted
                )
            }
        }
    }
}
