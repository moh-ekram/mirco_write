package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY importedAt DESC")
    fun getAllLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    fun getLessonById(id: String): Flow<LessonEntity?>

    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun getLessonByIdDirect(id: String): LessonEntity?

    @Query("SELECT * FROM lessons WHERE isBookmarked = 1 ORDER BY importedAt DESC")
    fun getBookmarkedLessons(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)

    @Update
    suspend fun updateLesson(lesson: LessonEntity)

    @Query("UPDATE lessons SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmark(id: String, isBookmarked: Boolean)

    @Query("UPDATE lessons SET lastOpenedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: String, timestamp: Long)

    @Query("DELETE FROM lessons WHERE id = :id")
    suspend fun deleteLessonById(id: String)

    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
}
