package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Bookmarks : Screen("bookmarks")
    object Settings : Screen("settings")
    object Import : Screen("import")
    object LessonDetail : Screen("lesson_detail/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson_detail/$lessonId"
    }
    object SectionStudy : Screen("section_study/{lessonId}/{sectionId}") {
        fun createRoute(lessonId: String, sectionId: String) = "section_study/$lessonId/$sectionId"
    }
    object FinalReview : Screen("final_review/{lessonId}") {
        fun createRoute(lessonId: String) = "final_review/$lessonId"
    }
}
