package com.example.ui.screens.reels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ReelDocument
import com.example.data.model.ReelFilterMode
import com.example.data.model.ReelGradients
import com.example.data.model.ReelPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val document by viewModel.document.collectAsState()
    val availableDocs by viewModel.availableDocs.collectAsState()
    val pages by viewModel.filteredPages.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val isPlaying by viewModel.audioManager.isPlaying.collectAsState()

    val totalPagesCount by viewModel.totalPagesCount.collectAsState()
    val unseenPagesCount by viewModel.unseenPagesCount.collectAsState()
    val seenPagesCount by viewModel.seenPagesCount.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDocPickerSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Page state for vertical scrolling
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size.coerceAtLeast(1) }
    )

    // Mark current page as seen and trigger audio update
    LaunchedEffect(pagerState.currentPage, pages) {
        if (pages.isNotEmpty() && pagerState.currentPage < pages.size) {
            val currentPage = pages[pagerState.currentPage]
            viewModel.onPageChanged(pagerState.currentPage, currentPage.id)
        }
    }

    // Auto-scroll effect
    LaunchedEffect(pagerState.currentPage, settings.autoScrollSeconds, pages.size) {
        if (settings.autoScrollSeconds > 0 && pages.size > 1) {
            delay(settings.autoScrollSeconds * 1000L)
            if (pagerState.currentPage < pages.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            } else {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    // Stop playback if leaving Reels
    DisposableEffect(Unit) {
        onDispose {
            viewModel.audioManager.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reels_screen")
    ) {
        if (pages.isEmpty()) {
            // Minimal Empty State for filters
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = when (filterMode) {
                            ReelFilterMode.UNSEEN -> "All slides in this document have been seen"
                            ReelFilterMode.SEEN -> "No seen slides yet. Start scrolling to mark slides as seen"
                            ReelFilterMode.ALL -> "No slides found"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.setFilterMode(ReelFilterMode.ALL) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View All")
                        }
                        if (filterMode == ReelFilterMode.UNSEEN) {
                            Button(
                                onClick = { viewModel.resetSeenPages() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reset Seen")
                            }
                        }
                    }
                }
            }
        } else {
            // Main Vertical Reel Pager
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val page = pages[pageIndex]
                ReelPageItem(
                    page = page,
                    pageIndex = pageIndex,
                    settings = settings,
                    isFullscreen = isFullscreen
                )
            }
        }

        // TOP BAR: Clean, Minimal Document & Filter Header (Hidden in Fullscreen)
        AnimatedVisibility(
            visible = !isFullscreen,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Top Row: Back / Doc Selector / Page Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Optional back & Document Title Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Document Selector Chip
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.12f)
                            ),
                            contentColor = Color.White,
                            modifier = Modifier
                                .clickable { showDocPickerSheet = true }
                                .testTag("reels_doc_selector_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = document.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Document",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right: Slide Counter
                    if (pages.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.12f)
                            ),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${pages.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Row: Seen / Unseen / All Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MinimalFilterPill(
                        label = "All ($totalPagesCount)",
                        isSelected = filterMode == ReelFilterMode.ALL,
                        onClick = { viewModel.setFilterMode(ReelFilterMode.ALL) },
                        testTag = "filter_all_pill"
                    )
                    MinimalFilterPill(
                        label = "Unseen ($unseenPagesCount)",
                        isSelected = filterMode == ReelFilterMode.UNSEEN,
                        onClick = { viewModel.setFilterMode(ReelFilterMode.UNSEEN) },
                        testTag = "filter_unseen_pill"
                    )
                    MinimalFilterPill(
                        label = "Seen ($seenPagesCount)",
                        isSelected = filterMode == ReelFilterMode.SEEN,
                        onClick = { viewModel.setFilterMode(ReelFilterMode.SEEN) },
                        testTag = "filter_seen_pill"
                    )
                }
            }
        }

        // FULLSCREEN EXIT BUTTON (Shown unobtrusively in top-right when in fullscreen)
        if (isFullscreen) {
            IconButton(
                onClick = { viewModel.toggleFullscreen() },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp)
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("reels_exit_fullscreen_top_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // MINIMAL RIGHT-CORNER FLOATING ACTIONS (Minimal, non-obtrusive, docked neatly at bottom-right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 14.dp, bottom = if (isFullscreen) 24.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Fullscreen Toggle
            MinimalCornerButton(
                onClick = { viewModel.toggleFullscreen() },
                icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                testTag = "reels_fullscreen_btn",
                isActive = isFullscreen
            )

            // 2. Music Toggle
            MinimalCornerButton(
                onClick = {
                    if (isPlaying) {
                        viewModel.audioManager.playRandomTrack()
                    } else {
                        viewModel.audioManager.togglePlayPause()
                    }
                },
                icon = if (isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                contentDescription = "Music",
                testTag = "reels_music_btn",
                isActive = isPlaying,
                isPulsing = isPlaying
            )

            // 3. Up Navigation
            MinimalCornerButton(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "Previous Slide",
                testTag = "reels_prev_btn",
                enabled = pagerState.currentPage > 0
            )

            // 4. Down Navigation
            MinimalCornerButton(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Next Slide",
                testTag = "reels_next_btn",
                enabled = pagerState.currentPage < pages.size - 1
            )

            // 5. Settings & Filters
            MinimalCornerButton(
                onClick = { showSettingsSheet = true },
                icon = Icons.Default.Tune,
                contentDescription = "Settings",
                testTag = "reels_settings_btn",
                highlight = true
            )
        }

        // Bottom Reading Progress Indicator
        if (pages.size > 1) {
            val progress = (pagerState.currentPage + 1).toFloat() / pages.size.toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }
    }

    // Document Picker Bottom Sheet
    if (showDocPickerSheet) {
        DocumentPickerSheet(
            currentDoc = document,
            availableDocs = availableDocs,
            onSelectDoc = { doc ->
                viewModel.selectDocument(doc)
                showDocPickerSheet = false
            },
            onOpenSettings = {
                showDocPickerSheet = false
                showSettingsSheet = true
            },
            onDismiss = { showDocPickerSheet = false }
        )
    }

    // Settings Modal Bottom Sheet (Minimal, concise, no unnecessary descriptions)
    if (showSettingsSheet) {
        ReelsSettingsSheet(
            viewModel = viewModel,
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
private fun ReelPageItem(
    page: ReelPage,
    pageIndex: Int,
    settings: com.example.data.model.ReelsSettingsState,
    isFullscreen: Boolean
) {
    val gradientBrush = ReelGradients.getGradientForTheme(settings.theme, pageIndex)
    val numberColor = remember(settings.numberHighlightColorHex) {
        try {
            Color(android.graphics.Color.parseColor(settings.numberHighlightColorHex))
        } catch (_: Exception) {
            Color(0xFFFFB703)
        }
    }

    val annotatedContent = remember(page.content, settings.highlightNumbers, numberColor) {
        ReelsTextRenderer.buildReelAnnotatedString(
            content = page.content,
            highlightNumbers = settings.highlightNumbers,
            numberColor = numberColor,
            defaultTextColor = Color(0xFFF1F5F9)
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(
                start = 20.dp,
                end = 68.dp, // Safe margin so corner buttons never overlap text
                top = if (isFullscreen) 40.dp else 90.dp,
                bottom = if (isFullscreen) 40.dp else 70.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        // Minimal Content Card Container
        Surface(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.32f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 26.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                // Formatted rich text content (Slide X and XX words badges removed for minimalism)
                Text(
                    text = annotatedContent,
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = (settings.fontSizeSp * 1.55f).sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MinimalCornerButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    enabled: Boolean = true,
    isActive: Boolean = false,
    highlight: Boolean = false,
    isPulsing: Boolean = false
) {
    val alpha = if (enabled) 1f else 0.35f
    val bgColor = when {
        highlight -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        else -> Color.Black.copy(alpha = 0.52f)
    }
    val iconColor = when {
        highlight -> MaterialTheme.colorScheme.onPrimary
        isActive -> Color.White
        else -> Color.White.copy(alpha = 0.9f)
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .testTag(testTag)
    ) {
        if (isPulsing) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor.copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MinimalFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        else Color.Black.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
        ),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentPickerSheet(
    currentDoc: ReelDocument,
    availableDocs: List<ReelDocument>,
    onSelectDoc: (ReelDocument) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Document",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add Link",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onOpenSettings)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableDocs) { doc ->
                    val isSelected = doc.title == currentDoc.title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .clickable { onSelectDoc(doc) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${doc.pages.size} slides",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
