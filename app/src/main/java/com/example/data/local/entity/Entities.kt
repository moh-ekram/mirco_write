package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleBn: String?,
    val type: String,
    val category: String,
    val description: String,
    val estimatedMinutes: Int,
    val guidelinesJson: String,
    val finalReviewJson: String,
    val isBookmarked: Boolean,
    val lastOpenedTimestamp: Long,
    val importedAt: Long
)

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lessonId"]), Index(value = ["lessonId", "sectionOrder"])]
)
data class SectionEntity(
    @PrimaryKey val id: String, // e.g., "${lessonId}_${sectionOrder}"
    val lessonId: String,
    val sectionOrder: Int,
    val title: String,
    val type: String,
    val guideJson: String,
    val task: String,
    val timerSeconds: Int,
    val modelAnswer: String,
    val userDraft: String,
    val isCompleted: Boolean,
    val isSkipped: Boolean,
    val isBookmarked: Boolean,
    val lastDraftSavedAt: Long
)
