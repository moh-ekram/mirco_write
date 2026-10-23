package com.example.data.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ExportMode(val label: String) {
    USER_WRITING_ONLY("User Writing Only"),
    MODEL_ANSWERS_ONLY("Model Answers Only"),
    BOTH("Both Writing & Model Answers")
}

object ExportManager {

    fun generateFormattedText(lesson: Lesson, mode: ExportMode): String {
        val sb = StringBuilder()
        sb.append(lesson.title.uppercase()).append("\n")
        lesson.titleBn?.let { if (it.isNotBlank()) sb.append(it).append("\n") }
        sb.append("Type: ").append(lesson.type).append(" | Category: ").append(lesson.category).append("\n")
        sb.append("Completion: ").append(lesson.completedSectionCount).append("/").append(lesson.totalSectionCount)
            .append(" (").append(lesson.completionPercentage).append("%)\n")
        sb.append("--------------------------------------------------\n\n")

        for (section in lesson.sections) {
            sb.append("SECTION ").append(section.order).append(": ").append(section.title).append("\n")
            if (section.task.isNotBlank()) {
                sb.append("Task: ").append(section.task).append("\n")
            }
            sb.append("\n")

            if (mode == ExportMode.USER_WRITING_ONLY || mode == ExportMode.BOTH) {
                sb.append("[MY WRITING]\n")
                if (section.userDraft.isNotBlank()) {
                    sb.append(section.userDraft.trim()).append("\n\n")
                } else {
                    sb.append("(No draft recorded for this section)\n\n")
                }
            }

            if (mode == ExportMode.MODEL_ANSWERS_ONLY || mode == ExportMode.BOTH) {
                sb.append("[MODEL ANSWER]\n")
                if (section.modelAnswer.isNotBlank()) {
                    sb.append(section.modelAnswer.trim()).append("\n\n")
                } else {
                    sb.append("(No model answer provided)\n\n")
                }
            }
            sb.append("--------------------------------------------------\n\n")
        }

        return sb.toString()
    }

    fun copyToClipboard(context: Context, text: String, label: String = "MicroWrite Lesson") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareText(context: Context, text: String, subject: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Lesson Text"))
    }

    suspend fun generatePdf(context: Context, lesson: Lesson, mode: ExportMode): Uri = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = lesson.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(30)
        val pdfFile = File(exportDir, "${safeName}_review.pdf")

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard pt
        val pageHeight = 842 // A4 standard pt
        val margin = 40
        val contentWidth = pageWidth - (margin * 2)

        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val metaPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isAntiAlias = true
        }

        val headingPaint = TextPaint().apply {
            color = Color.rgb(26, 54, 93) // Navy
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val labelPaint = TextPaint().apply {
            color = Color.rgb(43, 108, 176) // Accent blue
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var currentPageNumber = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create())
        var canvas = page.canvas
        var currentY = margin.toFloat()

        fun checkNewPage(neededHeight: Float) {
            if (currentY + neededHeight > pageHeight - margin) {
                pdfDocument.finishPage(page)
                currentPageNumber++
                page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create())
                canvas = page.canvas
                currentY = margin.toFloat()
            }
        }

        // Draw Header
        val titleLayout = createStaticLayout(lesson.title, titlePaint, contentWidth)
        titleLayout.draw(canvas)
        currentY += titleLayout.height + 6

        lesson.titleBn?.let { bn ->
            if (bn.isNotBlank()) {
                val bnLayout = createStaticLayout(bn, headingPaint, contentWidth)
                bnLayout.draw(canvas)
                currentY += bnLayout.height + 4
            }
        }

        val metaText = "Type: ${lesson.type}  |  Category: ${lesson.category}  |  Progress: ${lesson.completedSectionCount}/${lesson.totalSectionCount} (${lesson.completionPercentage}%)"
        val metaLayout = createStaticLayout(metaText, metaPaint, contentWidth)
        metaLayout.draw(canvas)
        currentY += metaLayout.height + 12

        canvas.drawLine(margin.toFloat(), currentY, (pageWidth - margin).toFloat(), currentY, dividerPaint)
        currentY += 14

        // Draw Sections
        for (sec in lesson.sections) {
            checkNewPage(60f)

            val secTitle = "SECTION ${sec.order}: ${sec.title}"
            val secTitleLayout = createStaticLayout(secTitle, headingPaint, contentWidth)
            canvas.save()
            canvas.translate(margin.toFloat(), currentY)
            secTitleLayout.draw(canvas)
            canvas.restore()
            currentY += secTitleLayout.height + 4

            if (sec.task.isNotBlank()) {
                val taskLayout = createStaticLayout("Task: ${sec.task}", metaPaint, contentWidth)
                checkNewPage(taskLayout.height.toFloat() + 4)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                taskLayout.draw(canvas)
                canvas.restore()
                currentY += taskLayout.height + 8
            }

            if (mode == ExportMode.USER_WRITING_ONLY || mode == ExportMode.BOTH) {
                val label = "[MY WRITING]"
                val labelLayout = createStaticLayout(label, labelPaint, contentWidth)
                checkNewPage(labelLayout.height.toFloat() + 4)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                labelLayout.draw(canvas)
                canvas.restore()
                currentY += labelLayout.height + 4

                val draftText = if (sec.userDraft.isNotBlank()) sec.userDraft.trim() else "(No draft recorded for this section)"
                val draftLayout = createStaticLayout(draftText, bodyPaint, contentWidth)
                checkNewPage(draftLayout.height.toFloat() + 8)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                draftLayout.draw(canvas)
                canvas.restore()
                currentY += draftLayout.height + 10
            }

            if (mode == ExportMode.MODEL_ANSWERS_ONLY || mode == ExportMode.BOTH) {
                val label = "[MODEL ANSWER]"
                val labelLayout = createStaticLayout(label, labelPaint, contentWidth)
                checkNewPage(labelLayout.height.toFloat() + 4)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                labelLayout.draw(canvas)
                canvas.restore()
                currentY += labelLayout.height + 4

                val answerText = if (sec.modelAnswer.isNotBlank()) sec.modelAnswer.trim() else "(No model answer provided)"
                val answerLayout = createStaticLayout(answerText, bodyPaint, contentWidth)
                checkNewPage(answerLayout.height.toFloat() + 8)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                answerLayout.draw(canvas)
                canvas.restore()
                currentY += answerLayout.height + 12
            }

            checkNewPage(10f)
            canvas.drawLine(margin.toFloat(), currentY, (pageWidth - margin).toFloat(), currentY, dividerPaint)
            currentY += 14
        }

        pdfDocument.finishPage(page)
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    }

    suspend fun generateJpegPages(context: Context, lesson: Lesson, mode: ExportMode): List<Uri> = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = lesson.title.replace(Regex("[^a-zA-Z0-9]"), "_").take(30)

        val imageWidth = 1200
        val imageHeight = 1700
        val margin = 80
        val contentWidth = imageWidth - (margin * 2)

        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val metaPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 20f
            isAntiAlias = true
        }

        val headingPaint = TextPaint().apply {
            color = Color.rgb(26, 54, 93) // Navy
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val labelPaint = TextPaint().apply {
            color = Color.rgb(43, 108, 176) // Accent blue
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 20f
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }

        val pageUris = mutableListOf<Uri>()
        var currentPageIndex = 1

        var currentBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(currentBitmap).apply { drawColor(Color.WHITE) }
        var currentY = margin.toFloat()

        fun flushCurrentPage() {
            val file = File(exportDir, "${safeName}_page_${currentPageIndex}.jpg")
            FileOutputStream(file).use { out ->
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pageUris.add(uri)
            currentPageIndex++
            currentBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
            canvas = Canvas(currentBitmap).apply { drawColor(Color.WHITE) }
            currentY = margin.toFloat()
        }

        fun checkNewPage(neededHeight: Float) {
            if (currentY + neededHeight > imageHeight - margin) {
                flushCurrentPage()
            }
        }

        // Header
        val titleLayout = createStaticLayout(lesson.title, titlePaint, contentWidth)
        canvas.save()
        canvas.translate(margin.toFloat(), currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        currentY += titleLayout.height + 12

        lesson.titleBn?.let { bn ->
            if (bn.isNotBlank()) {
                val bnLayout = createStaticLayout(bn, headingPaint, contentWidth)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                bnLayout.draw(canvas)
                canvas.restore()
                currentY += bnLayout.height + 8
            }
        }

        val metaText = "Type: ${lesson.type}  |  Category: ${lesson.category}  |  Progress: ${lesson.completedSectionCount}/${lesson.totalSectionCount} (${lesson.completionPercentage}%)"
        val metaLayout = createStaticLayout(metaText, metaPaint, contentWidth)
        canvas.save()
        canvas.translate(margin.toFloat(), currentY)
        metaLayout.draw(canvas)
        canvas.restore()
        currentY += metaLayout.height + 24

        canvas.drawLine(margin.toFloat(), currentY, (imageWidth - margin).toFloat(), currentY, dividerPaint)
        currentY += 28

        // Sections
        for (sec in lesson.sections) {
            checkNewPage(120f)

            val secTitle = "SECTION ${sec.order}: ${sec.title}"
            val secTitleLayout = createStaticLayout(secTitle, headingPaint, contentWidth)
            canvas.save()
            canvas.translate(margin.toFloat(), currentY)
            secTitleLayout.draw(canvas)
            canvas.restore()
            currentY += secTitleLayout.height + 8

            if (sec.task.isNotBlank()) {
                val taskLayout = createStaticLayout("Task: ${sec.task}", metaPaint, contentWidth)
                checkNewPage(taskLayout.height.toFloat() + 8)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                taskLayout.draw(canvas)
                canvas.restore()
                currentY += taskLayout.height + 16
            }

            if (mode == ExportMode.USER_WRITING_ONLY || mode == ExportMode.BOTH) {
                val label = "[MY WRITING]"
                val labelLayout = createStaticLayout(label, labelPaint, contentWidth)
                checkNewPage(labelLayout.height.toFloat() + 8)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                labelLayout.draw(canvas)
                canvas.restore()
                currentY += labelLayout.height + 8

                val draftText = if (sec.userDraft.isNotBlank()) sec.userDraft.trim() else "(No draft recorded for this section)"
                val draftLayout = createStaticLayout(draftText, bodyPaint, contentWidth)
                checkNewPage(draftLayout.height.toFloat() + 16)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                draftLayout.draw(canvas)
                canvas.restore()
                currentY += draftLayout.height + 20
            }

            if (mode == ExportMode.MODEL_ANSWERS_ONLY || mode == ExportMode.BOTH) {
                val label = "[MODEL ANSWER]"
                val labelLayout = createStaticLayout(label, labelPaint, contentWidth)
                checkNewPage(labelLayout.height.toFloat() + 8)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                labelLayout.draw(canvas)
                canvas.restore()
                currentY += labelLayout.height + 8

                val answerText = if (sec.modelAnswer.isNotBlank()) sec.modelAnswer.trim() else "(No model answer provided)"
                val answerLayout = createStaticLayout(answerText, bodyPaint, contentWidth)
                checkNewPage(answerLayout.height.toFloat() + 16)
                canvas.save()
                canvas.translate(margin.toFloat(), currentY)
                answerLayout.draw(canvas)
                canvas.restore()
                currentY += answerLayout.height + 24
            }

            checkNewPage(20f)
            canvas.drawLine(margin.toFloat(), currentY, (imageWidth - margin).toFloat(), currentY, dividerPaint)
            currentY += 28
        }

        flushCurrentPage()
        pageUris
    }

    fun shareExportedFile(context: Context, uri: Uri, mimeType: String, title: String = "Share Lesson") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    private fun createStaticLayout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.25f)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, 1.25f, 0f, true)
        }
    }
}
