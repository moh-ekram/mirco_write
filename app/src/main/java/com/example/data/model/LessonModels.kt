package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TableData(
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList()
)

@JsonClass(generateAdapter = true)
data class VocabularyItem(
    val term: String,
    val meaningBn: String,
    val note: String? = null
)

@JsonClass(generateAdapter = true)
data class Guidelines(
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
    val tables: List<TableData> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SectionGuide(
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
    val vocabulary: List<VocabularyItem> = emptyList(),
    val tables: List<TableData> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FinalReview(
    val items: List<String> = emptyList()
)

data class Lesson(
    val id: String,
    val title: String,
    val titleBn: String? = null,
    val type: String = "Focus Writing",
    val category: String = "General",
    val description: String = "",
    val estimatedMinutes: Int = 15,
    val guidelines: Guidelines = Guidelines(),
    val sections: List<LessonSection> = emptyList(),
    val finalReview: FinalReview = FinalReview(),
    val isBookmarked: Boolean = false,
    val lastOpenedTimestamp: Long = 0L,
    val importedAt: Long = System.currentTimeMillis()
) {
    val completedSectionCount: Int
        get() = sections.count { it.isCompleted }

    val totalSectionCount: Int
        get() = sections.size

    val completionPercentage: Int
        get() = if (totalSectionCount == 0) 0 else ((completedSectionCount * 100) / totalSectionCount)
}

data class LessonSection(
    val id: String,
    val lessonId: String,
    val order: Int,
    val title: String,
    val type: String = "Writing",
    val guide: SectionGuide = SectionGuide(),
    val task: String = "",
    val timerSeconds: Int = 0,
    val modelAnswer: String = "",
    val userDraft: String = "",
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val isBookmarked: Boolean = false,
    val lastDraftSavedAt: Long = 0L
) {
    val requiresWriting: Boolean
        get() = type.equals("Writing", ignoreCase = true) ||
                type.equals("writing_task", ignoreCase = true) ||
                task.isNotBlank()
}

data class ImportValidationIssue(
    val severity: IssueSeverity,
    val message: String
)

enum class IssueSeverity {
    WARNING, ERROR
}

data class ParsedLessonPreview(
    val lesson: Lesson,
    val validationIssues: List<ImportValidationIssue> = emptyList(),
    val isSelectedForImport: Boolean = true
) {
    val isValid: Boolean
        get() = validationIssues.none { it.severity == IssueSeverity.ERROR }
}

@JsonClass(generateAdapter = true)
data class BookmarkedLine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val lessonId: String,
    val sectionId: String,
    val lessonTitle: String,
    val sectionTitle: String,
    val lineText: String,
    val timestamp: Long = System.currentTimeMillis()
)
