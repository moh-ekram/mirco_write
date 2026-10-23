package com.example.data.repository

import com.example.data.local.Converters
import com.example.data.local.dao.LessonDao
import com.example.data.local.dao.SectionDao
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.SectionEntity
import com.example.data.model.Lesson
import com.example.data.model.LessonSection
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class LessonRepository(
    private val lessonDao: LessonDao,
    private val sectionDao: SectionDao,
    private val converters: Converters = Converters()
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    val allLessons: Flow<List<Lesson>> = combine(
        lessonDao.getAllLessons(),
        sectionDao.getBookmarkedSections() // placeholder to trigger or we can map per lesson
    ) { lessons, _ ->
        lessons.map { entity ->
            val sections = sectionDao.getSectionsForLessonDirect(entity.id)
            mapToLesson(entity, sections)
        }
    }

    fun getLesson(lessonId: String): Flow<Lesson?> = combine(
        lessonDao.getLessonById(lessonId),
        sectionDao.getSectionsForLesson(lessonId)
    ) { entity, sections ->
        if (entity == null) null else mapToLesson(entity, sections)
    }

    suspend fun getLessonDirect(lessonId: String): Lesson? = withContext(Dispatchers.IO) {
        val entity = lessonDao.getLessonByIdDirect(lessonId) ?: return@withContext null
        val sections = sectionDao.getSectionsForLessonDirect(lessonId)
        mapToLesson(entity, sections)
    }

    val bookmarkedLessons: Flow<List<Lesson>> = lessonDao.getBookmarkedLessons().combine(
        sectionDao.getBookmarkedSections()
    ) { lessons, _ ->
        lessons.map { entity ->
            val sections = sectionDao.getSectionsForLessonDirect(entity.id)
            mapToLesson(entity, sections)
        }
    }

    val bookmarkedSections: Flow<List<LessonSection>> = sectionDao.getBookmarkedSections().combine(
        lessonDao.getAllLessons()
    ) { sections, _ ->
        sections.map { mapToSection(it) }
    }

    suspend fun saveLesson(lesson: Lesson) = withContext(Dispatchers.IO) {
        // Check existing sections to preserve drafts and completion
        val existingSections = sectionDao.getSectionsForLessonDirect(lesson.id)
            .associateBy { it.sectionOrder }

        val lessonEntity = LessonEntity(
            id = lesson.id,
            title = lesson.title,
            titleBn = lesson.titleBn,
            type = lesson.type,
            category = lesson.category,
            description = lesson.description,
            estimatedMinutes = lesson.estimatedMinutes,
            guidelinesJson = converters.fromGuidelines(lesson.guidelines),
            finalReviewJson = converters.fromFinalReview(lesson.finalReview),
            isBookmarked = lesson.isBookmarked,
            lastOpenedTimestamp = lesson.lastOpenedTimestamp,
            importedAt = lesson.importedAt
        )

        val sectionEntities = lesson.sections.map { sec ->
            val existing = existingSections[sec.order]
            SectionEntity(
                id = sec.id,
                lessonId = lesson.id,
                sectionOrder = sec.order,
                title = sec.title,
                type = sec.type,
                guideJson = converters.fromSectionGuide(sec.guide),
                task = sec.task,
                timerSeconds = sec.timerSeconds,
                modelAnswer = sec.modelAnswer,
                // Preserve user data if exists
                userDraft = if (existing != null && existing.userDraft.isNotBlank()) existing.userDraft else sec.userDraft,
                isCompleted = existing?.isCompleted ?: sec.isCompleted,
                isSkipped = existing?.isSkipped ?: sec.isSkipped,
                isBookmarked = existing?.isBookmarked ?: sec.isBookmarked,
                lastDraftSavedAt = existing?.lastDraftSavedAt ?: sec.lastDraftSavedAt
            )
        }

        lessonDao.insertLesson(lessonEntity)
        sectionDao.insertSections(sectionEntities)
    }

    suspend fun saveLessons(lessons: List<Lesson>) = withContext(Dispatchers.IO) {
        for (lesson in lessons) {
            saveLesson(lesson)
        }
    }

    suspend fun updateSectionDraft(sectionId: String, draft: String) = withContext(Dispatchers.IO) {
        sectionDao.updateDraft(sectionId, draft, System.currentTimeMillis())
    }

    suspend fun markSectionDone(sectionId: String) = withContext(Dispatchers.IO) {
        sectionDao.updateStatus(sectionId, isCompleted = true, isSkipped = false)
    }

    suspend fun markSectionSkipped(sectionId: String) = withContext(Dispatchers.IO) {
        sectionDao.updateStatus(sectionId, isCompleted = false, isSkipped = true)
    }

    suspend fun setSectionStatus(sectionId: String, isCompleted: Boolean, isSkipped: Boolean) = withContext(Dispatchers.IO) {
        sectionDao.updateStatus(sectionId, isCompleted = isCompleted, isSkipped = isSkipped)
    }

    suspend fun toggleLessonBookmark(lessonId: String, current: Boolean) = withContext(Dispatchers.IO) {
        lessonDao.setBookmark(lessonId, !current)
    }

    suspend fun toggleSectionBookmark(sectionId: String, current: Boolean) = withContext(Dispatchers.IO) {
        sectionDao.setBookmark(sectionId, !current)
    }

    suspend fun updateLastOpened(lessonId: String) = withContext(Dispatchers.IO) {
        lessonDao.updateLastOpened(lessonId, System.currentTimeMillis())
    }

    suspend fun resetLessonProgress(lessonId: String) = withContext(Dispatchers.IO) {
        sectionDao.resetProgressForLesson(lessonId)
    }

    suspend fun clearLessonDrafts(lessonId: String) = withContext(Dispatchers.IO) {
        sectionDao.clearDraftsForLesson(lessonId)
    }

    suspend fun deleteLesson(lessonId: String) = withContext(Dispatchers.IO) {
        sectionDao.deleteSectionsForLesson(lessonId)
        lessonDao.deleteLessonById(lessonId)
    }

    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        sectionDao.deleteAllSections()
        lessonDao.deleteAllLessons()
    }

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val lessons = allLessons.first()
        val adapter = moshi.adapter<List<Lesson>>(
            Types.newParameterizedType(List::class.java, Lesson::class.java)
        )
        adapter.indent("  ").toJson(lessons)
    }

    suspend fun restoreBackupJson(json: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter<List<Lesson>>(
                Types.newParameterizedType(List::class.java, Lesson::class.java)
            )
            val list = adapter.fromJson(json) ?: return@withContext Result.failure(Exception("Invalid backup format"))
            for (lesson in list) {
                saveLesson(lesson)
            }
            Result.success(list.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToLesson(entity: LessonEntity, sections: List<SectionEntity>): Lesson {
        return Lesson(
            id = entity.id,
            title = entity.title,
            titleBn = entity.titleBn,
            type = entity.type,
            category = entity.category,
            description = entity.description,
            estimatedMinutes = entity.estimatedMinutes,
            guidelines = converters.toGuidelines(entity.guidelinesJson),
            sections = sections.map { mapToSection(it) },
            finalReview = converters.toFinalReview(entity.finalReviewJson),
            isBookmarked = entity.isBookmarked,
            lastOpenedTimestamp = entity.lastOpenedTimestamp,
            importedAt = entity.importedAt
        )
    }

    private fun mapToSection(entity: SectionEntity): LessonSection {
        return LessonSection(
            id = entity.id,
            lessonId = entity.lessonId,
            order = entity.sectionOrder,
            title = entity.title,
            type = entity.type,
            guide = converters.toSectionGuide(entity.guideJson),
            task = entity.task,
            timerSeconds = entity.timerSeconds,
            modelAnswer = entity.modelAnswer,
            userDraft = entity.userDraft,
            isCompleted = entity.isCompleted,
            isSkipped = entity.isSkipped,
            isBookmarked = entity.isBookmarked,
            lastDraftSavedAt = entity.lastDraftSavedAt
        )
    }
}
