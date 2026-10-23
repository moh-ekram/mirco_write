package com.example.ui.screens.library

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.screens.home.LessonListItemCard
import com.example.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onNavigateToLesson: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allLessons by viewModel.allLessons.collectAsState()
    val filteredLessons by viewModel.filteredLessons.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    val extColors = MaterialTheme.extendedColors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToImport,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("library_add_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Import Lesson")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Minimal Search Icon Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_search_input"),
                        placeholder = {
                            Text("Search lessons...", style = MaterialTheme.typography.bodyMedium, color = extColors.textMuted)
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = extColors.textMuted, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.searchQuery.value = ""
                                isSearchActive = false
                            }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = extColors.textMuted, modifier = Modifier.size(20.dp))
                            }
                        },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                } else {
                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("library_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Lesson List
            if (filteredLessons.isEmpty()) {
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
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = extColors.textMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (allLessons.isEmpty()) "No saved lessons yet" else "No matching lessons found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (allLessons.isEmpty()) "Import lesson resources to start practising." else "Try adjusting your search terms or filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = extColors.textMuted
                            )
                            if (allLessons.isEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onNavigateToImport, shape = CircleShape) {
                                    Text("Import Lesson", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredLessons, key = { it.id }) { lesson ->
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
    }
}
