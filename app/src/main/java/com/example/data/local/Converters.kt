package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.FinalReview
import com.example.data.model.Guidelines
import com.example.data.model.SectionGuide
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val guidelinesAdapter = moshi.adapter(Guidelines::class.java)
    private val sectionGuideAdapter = moshi.adapter(SectionGuide::class.java)
    private val finalReviewAdapter = moshi.adapter(FinalReview::class.java)

    @TypeConverter
    fun fromGuidelines(value: Guidelines?): String {
        return value?.let { guidelinesAdapter.toJson(it) } ?: "{}"
    }

    @TypeConverter
    fun toGuidelines(value: String?): Guidelines {
        if (value.isNullOrBlank()) return Guidelines()
        return try {
            guidelinesAdapter.fromJson(value) ?: Guidelines()
        } catch (_: Exception) {
            Guidelines()
        }
    }

    @TypeConverter
    fun fromSectionGuide(value: SectionGuide?): String {
        return value?.let { sectionGuideAdapter.toJson(it) } ?: "{}"
    }

    @TypeConverter
    fun toSectionGuide(value: String?): SectionGuide {
        if (value.isNullOrBlank()) return SectionGuide()
        return try {
            sectionGuideAdapter.fromJson(value) ?: SectionGuide()
        } catch (_: Exception) {
            SectionGuide()
        }
    }

    @TypeConverter
    fun fromFinalReview(value: FinalReview?): String {
        return value?.let { finalReviewAdapter.toJson(it) } ?: "{}"
    }

    @TypeConverter
    fun toFinalReview(value: String?): FinalReview {
        if (value.isNullOrBlank()) return FinalReview()
        return try {
            finalReviewAdapter.fromJson(value) ?: FinalReview()
        } catch (_: Exception) {
            FinalReview()
        }
    }
}
