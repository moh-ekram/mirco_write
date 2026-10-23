package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.screens.bookmarks.BookmarksScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.import_screen.ImportScreen
import com.example.ui.screens.lesson_detail.LessonDetailScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.review.FinalReviewScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.study.SectionStudyScreen
import com.example.ui.theme.extendedColors

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val bottomNavLeftItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    BottomNavItem(Screen.Library.route, "Library", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_library")
)

val bottomNavRightItems = listOf(
    BottomNavItem(Screen.Bookmarks.route, "Bookmarks", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_bookmarks"),
    BottomNavItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

val allBottomNavItems = bottomNavLeftItems + bottomNavRightItems

@Composable
fun AppNavHost(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show floating pill bottom bar on the 4 top-level screens
    val isTopLevelDestination = allBottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Floating Pill Navigation Bar with Soft Drop Shadow
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left 2 items: Home & Library
                            bottomNavLeftItems.forEach { item ->
                                val selected = currentRoute == item.route
                                FloatingNavItem(
                                    item = item,
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }

                            // Center Round Elevated Floating Button (Import)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("nav_import")
                                    .clickable {
                                        navController.navigate(Screen.Import.route)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Import Lesson",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Right 2 items: Bookmarks & Settings
                            bottomNavRightItems.forEach { item ->
                                val selected = currentRoute == item.route
                                FloatingNavItem(
                                    item = item,
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home Destination
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToLesson = { lessonId ->
                        navController.navigate(Screen.LessonDetail.createRoute(lessonId))
                    },
                    onNavigateToSection = { lessonId, sectionId ->
                        navController.navigate(Screen.SectionStudy.createRoute(lessonId, sectionId))
                    },
                    onNavigateToLibrary = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToImport = {
                        navController.navigate(Screen.Import.route)
                    }
                )
            }

            // Library Destination
            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    onNavigateToLesson = { lessonId ->
                        navController.navigate(Screen.LessonDetail.createRoute(lessonId))
                    },
                    onNavigateToImport = {
                        navController.navigate(Screen.Import.route)
                    }
                )
            }

            // Bookmarks Destination
            composable(Screen.Bookmarks.route) {
                BookmarksScreen(
                    viewModel = viewModel,
                    onNavigateToLesson = { lessonId ->
                        navController.navigate(Screen.LessonDetail.createRoute(lessonId))
                    },
                    onNavigateToSection = { lessonId, sectionId ->
                        navController.navigate(Screen.SectionStudy.createRoute(lessonId, sectionId))
                    }
                )
            }

            // Settings Destination
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }

            // Import Screen
            composable(Screen.Import.route) {
                ImportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onImportSuccess = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // Lesson Detail Screen
            composable(
                route = Screen.LessonDetail.route,
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
            ) { backStackEntry ->
                val lessonId = backStackEntry.arguments?.getString("lessonId") ?: return@composable
                LessonDetailScreen(
                    lessonId = lessonId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSection = { lId, sId ->
                        navController.navigate(Screen.SectionStudy.createRoute(lId, sId))
                    },
                    onNavigateToReview = { lId ->
                        navController.navigate(Screen.FinalReview.createRoute(lId))
                    }
                )
            }

            // Section Study Screen
            composable(
                route = Screen.SectionStudy.route,
                arguments = listOf(
                    navArgument("lessonId") { type = NavType.StringType },
                    navArgument("sectionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val lessonId = backStackEntry.arguments?.getString("lessonId") ?: return@composable
                val sectionId = backStackEntry.arguments?.getString("sectionId") ?: return@composable
                SectionStudyScreen(
                    lessonId = lessonId,
                    sectionId = sectionId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSection = { lId, sId ->
                        navController.navigate(Screen.SectionStudy.createRoute(lId, sId)) {
                            popUpTo(Screen.LessonDetail.createRoute(lId))
                        }
                    },
                    onNavigateToReview = { lId ->
                        navController.navigate(Screen.FinalReview.createRoute(lId)) {
                            popUpTo(Screen.LessonDetail.createRoute(lId))
                        }
                    }
                )
            }

            // Final Review Screen
            composable(
                route = Screen.FinalReview.route,
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
            ) { backStackEntry ->
                val lessonId = backStackEntry.arguments?.getString("lessonId") ?: return@composable
                FinalReviewScreen(
                    lessonId = lessonId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSection = { lId, sId ->
                        navController.navigate(Screen.SectionStudy.createRoute(lId, sId))
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val extColors = MaterialTheme.extendedColors

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = if (selected) MaterialTheme.colorScheme.primary else extColors.textMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else extColors.textMuted
        )
    }
}
