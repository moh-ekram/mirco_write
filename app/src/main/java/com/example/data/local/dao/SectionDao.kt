package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE lessonId = :lessonId ORDER BY sectionOrder ASC")
    fun getSectionsForLesson(lessonId: String): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE lessonId = :lessonId ORDER BY sectionOrder ASC")
    suspend fun getSectionsForLessonDirect(lessonId: String): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE id = :id LIMIT 1")
    fun getSectionById(id: String): Flow<SectionEntity?>

    @Query("SELECT * FROM sections WHERE id = :id LIMIT 1")
    suspend fun getSectionByIdDirect(id: String): SectionEntity?

    @Query("SELECT * FROM sections WHERE isBookmarked = 1 ORDER BY lessonId, sectionOrder ASC")
    fun getBookmarkedSections(): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<SectionEntity>)

    @Update
    suspend fun updateSection(section: SectionEntity)

    @Query("UPDATE sections SET userDraft = :draft, lastDraftSavedAt = :timestamp WHERE id = :id")
    suspend fun updateDraft(id: String, draft: String, timestamp: Long)

    @Query("UPDATE sections SET isCompleted = :isCompleted, isSkipped = :isSkipped WHERE id = :id")
    suspend fun updateStatus(id: String, isCompleted: Boolean, isSkipped: Boolean)

    @Query("UPDATE sections SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmark(id: String, isBookmarked: Boolean)

    @Query("UPDATE sections SET isCompleted = 0, isSkipped = 0 WHERE lessonId = :lessonId")
    suspend fun resetProgressForLesson(lessonId: String)

    @Query("UPDATE sections SET userDraft = '', lastDraftSavedAt = 0 WHERE lessonId = :lessonId")
    suspend fun clearDraftsForLesson(lessonId: String)

    @Query("DELETE FROM sections WHERE lessonId = :lessonId")
    suspend fun deleteSectionsForLesson(lessonId: String)

    @Query("DELETE FROM sections")
    suspend fun deleteAllSections()
}
