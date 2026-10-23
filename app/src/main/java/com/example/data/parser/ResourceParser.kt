package com.example.data.parser

import com.example.data.model.FinalReview
import com.example.data.model.Guidelines
import com.example.data.model.ImportValidationIssue
import com.example.data.model.IssueSeverity
import com.example.data.model.Lesson
import com.example.data.model.LessonSection
import com.example.data.model.ParsedLessonPreview
import com.example.data.model.SectionGuide
import com.example.data.model.TableData
import com.example.data.model.VocabularyItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

class ResourceParser(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Parses text content (which could be marker-based V2 format or JSON format).
     * Returns a list of preview lessons with validation issues.
     */
    fun parseContent(rawContent: String): List<ParsedLessonPreview> {
        val trimmed = rawContent
            .replace("\uFEFF", "")
            .replace("\u200B", "")
            .replace("\u00A0", " ")
            .trim()
        if (trimmed.isEmpty()) {
            return emptyList()
        }

        // Check if JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val jsonLessons = tryParseJson(trimmed)
            if (jsonLessons.isNotEmpty()) {
                return jsonLessons
            }
        }

        // Normalize any tab-delimited or non-standard tables to markdown tables
        val normalized = TableParser.convertTextWithTablesToMarkdown(trimmed)

        // Default to Marker Parser V2 & Shorthand Parser
        return parseMarkerFormat(normalized)
    }

    /**
     * Parses a .docx document from InputStream by reading word/document.xml
     */
    suspend fun parseDocx(inputStream: InputStream): List<ParsedLessonPreview> = withContext(Dispatchers.IO) {
        val extractedText = extractTextFromDocx(inputStream)
        parseContent(extractedText)
    }

    /**
     * Fetches public Google Doc plain text via export URL, automatically discovering all tabs if present.
     */
    suspend fun fetchAndParseGoogleDocs(url: String): Pair<List<ParsedLessonPreview>, String?> = withContext(Dispatchers.IO) {
        val cleanUrl = url.replace("*", ".").trim()
        val docId = extractGoogleDocId(cleanUrl)
            ?: return@withContext Pair(emptyList(), "Invalid Google Docs URL. Make sure it follows: https://docs.google.com/document/d/YOUR_DOC_ID/...")

        val tabId = extractGoogleDocTab(cleanUrl)
        val allTabs = mutableListOf<String>()
        if (tabId != null) {
            allTabs.add(tabId)
        }
        val discoveredTabs = discoverDocTabs(docId)
        for (tab in discoveredTabs) {
            if (!allTabs.contains(tab)) {
                allTabs.add(tab)
            }
        }
        if (allTabs.isEmpty()) {
            allTabs.add("t.0")
        }

        val allPreviews = mutableListOf<ParsedLessonPreview>()
        var lastError: String? = null

        for (tab in allTabs) {
            val tabParsed = mutableListOf<ParsedLessonPreview>()

            // 1. Try DOCX export first (full preservation of Google Doc tables, columns, rows, formatting)
            val docxUrl = "https://docs.google.com/document/d/$docId/export?format=docx&tab=$tab"
            try {
                val docxReq = Request.Builder()
                    .url(docxUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val docxResp = okHttpClient.newCall(docxReq).execute()
                docxResp.use { r ->
                    if (r.isSuccessful && r.body != null) {
                        val contentType = r.header("Content-Type") ?: ""
                        if (!contentType.contains("text/html")) {
                            val stream = r.body!!.byteStream()
                            val textFromDocx = extractTextFromDocx(stream)
                            if (textFromDocx.isNotBlank()) {
                                val parsed = parseContent(textFromDocx)
                                if (parsed.isNotEmpty()) {
                                    tabParsed.addAll(parsed)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to TXT export
            }

            // 2. If DOCX export yielded no lessons, fallback to TXT export
            if (tabParsed.isEmpty()) {
                val exportUrl = "https://docs.google.com/document/d/$docId/export?format=txt&tab=$tab"
                val request = Request.Builder()
                    .url(exportUrl)
                    .header("User-Agent", "MicroWrite-App/1.0")
                    .build()

                try {
                    val response = okHttpClient.newCall(request).execute()
                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            val code = resp.code
                            lastError = when (code) {
                                401, 403 -> "Google Doc is private. Please set link sharing to 'Anyone with the link can view'."
                                404 -> "Document not found (404). Check if the document ID is correct and public."
                                else -> "Failed to download Google Doc tab $tab. HTTP $code: ${resp.message}"
                            }
                            return@use
                        }

                        val bodyText = (resp.body?.string() ?: "")
                            .replace("\uFEFF", "")
                            .replace("\u200B", "")
                            .replace("\u00A0", " ")
                        if (bodyText.isNotBlank()) {
                            val parsed = parseContent(bodyText)
                            tabParsed.addAll(parsed)
                        }
                    }
                } catch (e: Exception) {
                    lastError = "Network error on tab $tab: ${e.localizedMessage ?: "Unable to reach Google Docs"}"
                }
            }

            for (p in tabParsed) {
                if (allPreviews.none { it.lesson.title.equals(p.lesson.title, ignoreCase = true) }) {
                    allPreviews.add(p)
                }
            }
        }

        if (allPreviews.isEmpty()) {
            Pair(emptyList(), lastError ?: "No valid lessons found in the Google Doc. Please check format.")
        } else {
            Pair(allPreviews, null)
        }
    }

    private fun discoverDocTabs(docId: String): List<String> {
        try {
            val htmlUrl = "https://docs.google.com/document/d/$docId/edit"
            val req = Request.Builder()
                .url(htmlUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            val resp = okHttpClient.newCall(req).execute()
            resp.use {
                if (it.isSuccessful) {
                    val html = it.body?.string() ?: ""
                    val pattern = Pattern.compile("\\[\"ksm\",\"(t\\.[a-zA-Z0-9_-]+)\"\\]")
                    val matcher = pattern.matcher(html)
                    val discovered = mutableListOf<String>()
                    while (matcher.find()) {
                        val tab = matcher.group(1)
                        if (tab != null && !discovered.contains(tab)) {
                            discovered.add(tab)
                        }
                    }
                    if (discovered.isNotEmpty()) {
                        val result = mutableListOf<String>()
                        if (!discovered.contains("t.0")) result.add("t.0")
                        result.addAll(discovered)
                        return result
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore error, fallback to default export
        }
        return emptyList()
    }

    /**
     * Extracts text and tables from DOCX archive's word/document.xml
     */
    fun extractTextFromDocx(inputStream: InputStream): String {
        val zip = ZipInputStream(inputStream)
        val sb = StringBuilder()
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                parseWordDocumentXml(xmlContent, sb)
                break
            }
            entry = zip.nextEntry
        }
        return sb.toString()
    }

    private fun parseWordDocumentXml(xml: String, sb: StringBuilder) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inTableCell = false
            var inTableRow = false
            var isFirstRowOfTable = false
            val rowCells = mutableListOf<String>()
            val currentCellText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val rawName = parser.name ?: ""
                val tag = rawName.substringAfterLast(":").lowercase()

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (tag) {
                            "tbl" -> {
                                isFirstRowOfTable = true
                                sb.append("\n")
                            }
                            "tr" -> {
                                inTableRow = true
                                rowCells.clear()
                            }
                            "tc" -> {
                                inTableCell = true
                                currentCellText.clear()
                            }
                            "p" -> {
                                if (inTableCell && currentCellText.isNotEmpty()) {
                                    currentCellText.append(" ")
                                }
                            }
                            "tab" -> {
                                if (inTableCell) {
                                    currentCellText.append(" ")
                                } else {
                                    sb.append("\t")
                                }
                            }
                            "br" -> {
                                if (inTableCell) {
                                    currentCellText.append(" ")
                                } else {
                                    sb.append("\n")
                                }
                            }
                            "t" -> {
                                val text = parser.nextText()
                                if (inTableCell) {
                                    currentCellText.append(text)
                                } else {
                                    sb.append(text)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (tag) {
                            "tc" -> {
                                inTableCell = false
                                val cellVal = currentCellText.toString()
                                    .replace("|", "/")
                                    .replace("\n", " ")
                                    .trim()
                                rowCells.add(cellVal)
                                currentCellText.clear()
                            }
                            "tr" -> {
                                inTableRow = false
                                if (rowCells.isNotEmpty() && rowCells.any { it.isNotBlank() }) {
                                    sb.append("| ").append(rowCells.joinToString(" | ")).append(" |\n")
                                    if (isFirstRowOfTable) {
                                        val sep = rowCells.map { "---" }.joinToString(" | ", prefix = "| ", postfix = " |\n")
                                        sb.append(sep)
                                        isFirstRowOfTable = false
                                    }
                                }
                            }
                            "tbl" -> {
                                isFirstRowOfTable = false
                                sb.append("\n")
                            }
                            "p" -> {
                                if (!inTableCell) {
                                    sb.append("\n")
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            val fallback = xml.replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
            sb.append(fallback)
        }
    }

    private fun extractGoogleDocId(url: String): String? {
        val normalized = url.replace("*", ".")
        val pattern = Pattern.compile("/document/d/([a-zA-Z0-9_-]+)")
        val matcher = pattern.matcher(normalized)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }

    private fun extractGoogleDocTab(url: String): String? {
        val normalized = url.replace("*", ".")
        val pattern = Pattern.compile("[?&#]tab=([a-zA-Z0-9_.-]+)")
        val matcher = pattern.matcher(normalized)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }

    /**
     * Parses Standard Resource Format Version 2 and Shorthand Format
     */
    fun parseMarkerFormat(text: String): List<ParsedLessonPreview> {
        val lines = text.lines()
        val lessonBlocks = splitIntoLessonBlocks(lines)
        val previews = mutableListOf<ParsedLessonPreview>()

        for (block in lessonBlocks) {
            val preview = parseSingleLessonBlock(block)
            val isTemplatePlaceholder = (preview.lesson.title.equals("[Lesson Title]", true) ||
                    preview.lesson.title.equals("Lesson Title", true)) &&
                    (preview.lesson.sections.isEmpty() ||
                            preview.lesson.sections.all { it.title.equals("[Title]", true) || it.title.equals("Title", true) })
            if (!isTemplatePlaceholder && preview.lesson.title.isNotBlank()) {
                previews.add(preview)
            }
        }

        return previews
    }

    private fun isLessonStartLine(line: String): Boolean {
        val clean = line.replace("\uFEFF", "").replace("\u200B", "").trim()
        val upper = clean.uppercase()
        return upper.startsWith("LESSON TITLE:") ||
                upper.startsWith("LESSON TITLE :") ||
                upper.startsWith("LESSON:") ||
                upper.startsWith("@TYPE:") ||
                upper.startsWith("@TYPE :") ||
                upper.startsWith("@TYPE,") ||
                upper.startsWith("\$TYPE,") ||
                upper.startsWith("\$TYPE:") ||
                upper.startsWith("@TITLE:") ||
                upper.startsWith("\$TITLE:") ||
                upper.startsWith("@FOCUS") ||
                upper.startsWith("\$FOCUS") ||
                upper.startsWith("@ARGUMENTATIVE") ||
                upper.startsWith("\$ARGUMENTATIVE") ||
                clean.startsWith("@") ||
                clean.startsWith("$")
    }

    private fun splitIntoLessonBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        var currentBlock = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.replace("\uFEFF", "").replace("\u200B", "").trim()
            val upper = trimmed.uppercase()

            if (isLessonStartLine(trimmed)) {
                if (currentBlock.any { it.isNotBlank() }) {
                    blocks.add(currentBlock)
                    currentBlock = mutableListOf()
                }
            }

            currentBlock.add(line)

            if (upper == "LESSON END" || upper == "END LESSON") {
                if (currentBlock.any { it.isNotBlank() }) {
                    blocks.add(currentBlock)
                    currentBlock = mutableListOf()
                }
            }
        }

        if (currentBlock.any { it.isNotBlank() }) {
            blocks.add(currentBlock)
        }

        return blocks
    }

    private fun parseSingleLessonBlock(lines: List<String>): ParsedLessonPreview {
        val issues = mutableListOf<ImportValidationIssue>()

        var title = ""
        var lessonId = ""
        var lessonType = "Focus Writing"
        var category = "General"

        val guidelinesLines = mutableListOf<String>()
        val finalReviewLines = mutableListOf<String>()

        val firstLine = lines.firstOrNull { it.isNotBlank() }?.replace("\uFEFF", "")?.replace("\u200B", "")?.trim() ?: ""
        if (firstLine.startsWith("@") || firstLine.startsWith("$")) {
            val clean = firstLine.removePrefix("@").removePrefix("$").trim()
            if (clean.contains("Title:", ignoreCase = true)) {
                val parts = clean.split(Regex("Title:", RegexOption.IGNORE_CASE), limit = 2)
                val tCand = parts[0].replace(Regex("^Type[:\\s,]*", RegexOption.IGNORE_CASE), "").replace(Regex("[,@$]+"), "").trim()
                if (tCand.isNotBlank()) lessonType = tCandidateClean(tCand)
                title = parts[1].trim(' ', ':', '-', '[', ']')
            } else if (clean.startsWith("Focus Writing:", ignoreCase = true) || clean.startsWith("Argumentative Writing:", ignoreCase = true)) {
                lessonType = if (clean.startsWith("Argumentative", ignoreCase = true)) "Argumentative Writing" else "Focus Writing"
                title = clean.substringAfter(":").trim(' ', ':', '-', '[', ']')
            } else if (clean.contains(",") && (clean.substringBefore(",").contains("Writing", ignoreCase = true) || clean.substringBefore(",").contains("Focus", ignoreCase = true))) {
                val parts = clean.split(",", limit = 2)
                lessonType = tCandidateClean(parts[0].trim())
                title = parts[1].trim().removePrefix("@").removePrefix("$").replace(Regex("^(?:Title|Topic)[:\\s]*", RegexOption.IGNORE_CASE), "").trim(' ', ':', '-', '[', ']')
            } else {
                title = clean.trim(' ', ':', '-', '[', ']')
            }
        } else if (firstLine.uppercase().startsWith("LESSON TITLE:") || firstLine.uppercase().startsWith("TITLE:")) {
            title = firstLine.substringAfter(":").trim(' ', ':', '-', '[', ']')
        } else {
            title = firstLine.trim(' ', ':', '-', '[', ']')
        }

        if (title.isBlank()) title = "Untitled Lesson"
        val titleBn = if (hasBengaliChars(title)) title else null

        var description = ""
        val sectionBlocks = mutableListOf<MutableList<String>>()
        var currentSectionLines: MutableList<String>? = null
        var inGuidelines = false
        var inFinalReview = false
        var expectedOrder = 1

        for (line in lines) {
            val trimmed = line.replace("\uFEFF", "").replace("\u200B", "").trim()
            val upper = trimmed.uppercase()

            if (trimmed == firstLine || upper == "LESSON END" || upper == "END LESSON") {
                continue
            }

            if (upper.startsWith("LESSON ID:")) {
                lessonId = trimmed.substringAfter(":").trim()
                continue
            }

            if (upper.startsWith("LESSON TYPE:")) {
                lessonType = trimmed.substringAfter(":").trim()
                continue
            }

            if (upper.startsWith("LESSON DESCRIPTION:") || upper.startsWith("-DESCRIPTION:") || upper.startsWith("DESCRIPTION:")) {
                val inline = trimmed.substringAfter(":").trim()
                if (inline.isNotBlank()) {
                    description = inline
                }
                continue
            }

            if (upper == "FINAL REVIEW START" || upper == "REVIEW START" || upper == "CHECKLIST START" ||
                upper.startsWith("-FINAL REVIEW:") || upper.startsWith("FINAL REVIEW:")) {
                inFinalReview = true
                inGuidelines = false
                currentSectionLines?.let { sectionBlocks.add(it) }
                currentSectionLines = null
                val inline = if (trimmed.contains(":")) trimmed.substringAfter(":").trim() else ""
                if (inline.isNotBlank() && !inline.equals("START", true)) finalReviewLines.add(inline)
                continue
            }
            if (upper == "FINAL REVIEW END" || upper == "REVIEW END" || upper == "CHECKLIST END") {
                inFinalReview = false
                continue
            }

            if (isSectionEndMarker(upper)) {
                currentSectionLines?.add(line)
                currentSectionLines?.let { sectionBlocks.add(it) }
                currentSectionLines = null
                continue
            }

            val secHeading = detectSectionHeading(trimmed, expectedOrder)
            if (secHeading != null) {
                inGuidelines = false
                inFinalReview = false
                currentSectionLines?.let { sectionBlocks.add(it) }
                currentSectionLines = mutableListOf(line)
                expectedOrder = secHeading.second + 1
                continue
            }

            if (upper.startsWith("-GUIDELINES:") || upper.startsWith("GUIDELINES:") ||
                upper.startsWith("GENERAL GUIDELINES:") ||
                upper == "GENERAL GUIDELINES START" || upper == "GUIDELINES START") {
                inGuidelines = true
                val inline = if (trimmed.contains(":")) trimmed.substringAfter(":").trim() else ""
                if (inline.isNotBlank() && !inline.equals("START", true)) {
                    guidelinesLines.add(inline)
                }
                continue
            }
            if (upper == "GENERAL GUIDELINES END" || upper == "GUIDELINES END") {
                inGuidelines = false
                continue
            }

            if (inFinalReview) {
                finalReviewLines.add(line)
            } else if (currentSectionLines != null) {
                currentSectionLines.add(line)
            } else {
                if (trimmed.isNotBlank()) {
                    val cleanLead = if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        trimmed.removePrefix("[").removeSuffix("]").trim()
                    } else trimmed
                    guidelinesLines.add(cleanLead)
                }
            }
        }

        currentSectionLines?.let { sectionBlocks.add(it) }

        if (lessonId.isBlank()) {
            lessonId = generateLessonId(title)
        }

        val guidelines = parseGuidelines(guidelinesLines)

        val sections = mutableListOf<LessonSection>()
        for ((index, secLines) in sectionBlocks.withIndex()) {
            val sectionOrder = index + 1
            val parsedSection = parseSingleSection(lessonId, sectionOrder, secLines, issues)
            sections.add(parsedSection)
        }

        val finalReview = parseFinalReview(finalReviewLines)
        val totalMinutes = sections.sumOf { if (it.timerSeconds > 0) it.timerSeconds / 60 else 5 }
        val estimatedMinutes = if (totalMinutes > 0) totalMinutes else 15

        val lesson = Lesson(
            id = lessonId,
            title = title,
            titleBn = titleBn,
            type = if (lessonType.isNotBlank()) lessonType else "Focus Writing",
            category = category,
            description = description,
            estimatedMinutes = estimatedMinutes,
            guidelines = guidelines,
            sections = sections,
            finalReview = finalReview
        )

        return ParsedLessonPreview(lesson, issues)
    }

    private fun detectSectionHeading(trimmed: String, expectedOrder: Int): Pair<String, Int>? {
        val clean = trimmed
            .replace(Regex("^[*_#\\-*•]+\\s*"), "")
            .replace(Regex("[*_]+$"), "")
            .trim()
        val upper = clean.uppercase()
        if (isSectionEndMarker(upper)) return null

        val explicitMatch = Regex("^(?:SECTION|PART)\\s*(\\d+)[:\\s-]*(.*)$", RegexOption.IGNORE_CASE).find(clean)
        if (explicitMatch != null) {
            val num = explicitMatch.groupValues[1].toIntOrNull() ?: expectedOrder
            val titlePart = explicitMatch.groupValues[2].trim()
            return Pair(titlePart, num)
        }

        val numMatch = Regex("^([০-৯]+|\\d+)[\\.\\):/\\-–—]\\s*(.*)$").find(clean)
        if (numMatch != null) {
            val numStr = numMatch.groupValues[1]
            val num = parseBengaliOrAsciiInt(numStr)
            val titlePart = numMatch.groupValues[2].trim()
            if (num != null && titlePart.isNotBlank()) {
                val isLongSentence = (titlePart.endsWith("।") || titlePart.endsWith(".")) && titlePart.length > 70
                if (!isLongSentence) {
                    if (num == expectedOrder || (expectedOrder == 1 && num == 1) || Math.abs(num - expectedOrder) <= 2 || expectedOrder <= 1) {
                        return Pair(titlePart, num)
                    }
                }
            }
        }

        val mdMatch = Regex("^#{2,3}\\s+(?:(\\d+|[০-৯]+)[\\.\\):/\\-–—]\\s*)?(.*)$").find(trimmed)
        if (mdMatch != null) {
            val numStr = mdMatch.groupValues[1]
            val num = parseBengaliOrAsciiInt(numStr) ?: expectedOrder
            if (num == expectedOrder || (expectedOrder == 1 && num == 1) || Math.abs(num - expectedOrder) <= 2) {
                val titlePart = mdMatch.groupValues[2].trim()
                return Pair(titlePart, num)
            }
        }

        return null
    }

    private fun parseBengaliOrAsciiInt(str: String): Int? {
        val bengaliDigits = mapOf('০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4', '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9')
        val ascii = str.map { bengaliDigits[it] ?: it }.joinToString("")
        return ascii.toIntOrNull()
    }

    private fun tCandidateClean(cand: String): String {
        return when {
            cand.contains("Argument", true) -> "Argumentative Writing"
            cand.contains("Focus", true) -> "Focus Writing"
            else -> cand.ifBlank { "Focus Writing" }
        }
    }

    private fun isSectionEndMarker(upper: String): Boolean {
        val cleaned = upper.replace(Regex("^[#\\-*•]+\\s*"), "").trim()
        return cleaned == "SECTION END" || cleaned == "END SECTION" ||
                Regex("^SECTION\\s*\\d+\\s+END$", RegexOption.IGNORE_CASE).matches(cleaned)
    }

    private fun cleanSectionTitle(raw: String): String {
        return raw
            .replace(Regex("^(?:SECTION\\s*\\d+|SECTION\\d+|PART\\s*\\d+)[:\\s-]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^([০-৯]+|\\d+)[\\.\\)][:\\s-]*"), "")
            .replace(Regex("^#{1,4}\\s*"), "")
            .replace(Regex("^(?:SECTION\\s*TITLE|SECTION\\s*HEADING|TITLE|HEADING)[:\\s-]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^START[:\\s]*", RegexOption.IGNORE_CASE), "")
            .trim(' ', ':', '-', '\t', '[', ']')
            .trim()
    }

    private fun parseSingleSection(
        lessonId: String,
        order: Int,
        lines: List<String>,
        issues: MutableList<ImportValidationIssue>
    ): LessonSection {
        val firstLine = lines.firstOrNull()?.trim() ?: ""
        var secTitle = cleanSectionTitle(firstLine)
        var startIndex = 1

        if (secTitle.isBlank() && lines.size > 1) {
            secTitle = cleanSectionTitle(lines[1])
            startIndex = 2
        }

        var secType = "Writing"
        var task = ""
        var timerSeconds = 0
        val guideLines = mutableListOf<String>()
        val answerLines = mutableListOf<String>()
        var mode = "auto"
        var inBracketGuide = false

        for (i in startIndex until lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            val upper = trimmed.uppercase()

            if (upper.startsWith("SECTION TITLE:") || upper.startsWith("TITLE:") || upper.startsWith("SECTION HEADING:")) {
                val candidateTitle = trimmed.substringAfter(":").trim()
                if (candidateTitle.isNotBlank()) secTitle = candidateTitle
            } else if (upper.startsWith("SECTION TYPE:") || upper.startsWith("TYPE:")) {
                secType = trimmed.substringAfter(":").trim()
            } else if (upper.startsWith("SECTION TASK:") || upper.startsWith("TASK:") ||
                upper.startsWith("-TASK:") || upper.startsWith("PROMPT:")) {
                task = trimmed.substringAfter(":").trim()
            } else if (upper.startsWith("SECTION TIMER:") || upper.startsWith("TIMER:") ||
                upper.startsWith("-TIMER:") || upper.startsWith("TIME:")) {
                timerSeconds = parseTimerSeconds(trimmed.substringAfter(":").trim())
            } else if (upper == "SECTION GUIDE START" || upper == "GUIDE START" ||
                upper == "GUIDE:" || upper == "-GUIDE:" || upper.startsWith("GUIDE:") ||
                upper == "GUIDE" || upper == "INSTRUCTIONS START" || upper == "RESOURCES START") {
                mode = "guide"
                val inlineText = if (trimmed.contains(":")) trimmed.substringAfter(":").trim() else ""
                if (inlineText.isNotBlank() && !inlineText.equals("START", true)) {
                    guideLines.add(inlineText)
                }
            } else if (upper == "SECTION GUIDE END" || upper == "GUIDE END" ||
                upper == "INSTRUCTIONS END" || upper == "RESOURCES END") {
                mode = "auto"
            } else if (upper == "SECTION ANSWER START" || upper == "ANSWER START" ||
                upper == "MODEL ANSWER START" || upper == "SOLUTION START" ||
                upper == "ANSWER:" || upper == "-ANSWER:" || upper.startsWith("ANSWER:") ||
                upper == "ANSWER" || upper.startsWith("MODEL ANSWER:")) {
                mode = "answer"
                val inlineText = if (trimmed.contains(":")) trimmed.substringAfter(":").trim() else ""
                if (inlineText.isNotBlank() && !inlineText.equals("START", true)) {
                    answerLines.add(inlineText)
                }
            } else if (upper == "SECTION ANSWER END" || upper == "ANSWER END" ||
                upper == "MODEL ANSWER END" || upper == "SOLUTION END") {
                mode = "auto"
            } else if (isSectionEndMarker(upper)) {
                mode = "auto"
            } else {
                if (mode == "guide") {
                    guideLines.add(line)
                } else if (mode == "answer") {
                    answerLines.add(line)
                } else {
                    if (inBracketGuide) {
                        if (trimmed.contains("]")) {
                            inBracketGuide = false
                            val before = trimmed.substringBefore("]").removePrefix("[").trim()
                            val after = trimmed.substringAfter("]").trim()
                            if (before.isNotEmpty()) guideLines.add(before)
                            if (after.isNotEmpty()) answerLines.add(after)
                        } else {
                            guideLines.add(trimmed)
                        }
                    } else if (trimmed.startsWith("[")) {
                        if (trimmed.contains("]")) {
                            val before = trimmed.substringBefore("]").removePrefix("[").trim()
                            val after = trimmed.substringAfter("]").trim()
                            if (before.isNotEmpty()) guideLines.add(before)
                            if (after.isNotEmpty()) answerLines.add(after)
                        } else {
                            inBracketGuide = true
                            val content = trimmed.removePrefix("[").trim()
                            if (content.isNotEmpty()) guideLines.add(content)
                        }
                    } else {
                        answerLines.add(line)
                    }
                }
            }
        }

        if (secTitle.isBlank()) {
            secTitle = "Topic $order"
        }

        val guide = parseSectionGuide(guideLines)
        val modelAnswer = answerLines.joinToString("\n").trim()

        return LessonSection(
            id = "${lessonId}_sec_$order",
            lessonId = lessonId,
            order = order,
            title = secTitle,
            type = secType,
            guide = guide,
            task = task,
            timerSeconds = timerSeconds,
            modelAnswer = modelAnswer
        )
    }

    private fun parseTimerSeconds(str: String): Int {
        val trimmed = str.lowercase().trim()
        if (trimmed.endsWith("m") || trimmed.contains("min")) {
            val num = trimmed.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            return num * 60
        }
        val cleanSec = trimmed.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        return cleanSec
    }

    private fun parseGuidelines(lines: List<String>): Guidelines {
        val paragraphs = mutableListOf<String>()
        val bullets = mutableListOf<String>()
        val tables = mutableListOf<TableData>()

        var currentTableLines = mutableListOf<String>()
        val currentParagraph = StringBuilder()

        fun flushParagraph() {
            val text = currentParagraph.toString().trim()
            if (text.isNotEmpty()) {
                paragraphs.add(text)
                currentParagraph.clear()
            }
        }

        fun flushTable() {
            if (currentTableLines.isNotEmpty()) {
                parseMarkdownTable(currentTableLines)?.let { tables.add(it) }
                currentTableLines = mutableListOf()
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                flushParagraph()
                flushTable()
                continue
            }

            if (TableParser.isTableLine(trimmed)) {
                flushParagraph()
                currentTableLines.add(trimmed)
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") || isNumberedList(trimmed)) {
                flushParagraph()
                flushTable()
                val item = cleanBullet(trimmed)
                if (item.isNotEmpty()) {
                    bullets.add(item)
                }
            } else {
                flushTable()
                if (currentParagraph.isNotEmpty()) currentParagraph.append(" ")
                currentParagraph.append(trimmed)
            }
        }

        flushParagraph()
        flushTable()

        return Guidelines(paragraphs, bullets, tables)
    }

    private fun parseSectionGuide(lines: List<String>): SectionGuide {
        val paragraphs = mutableListOf<String>()
        val bullets = mutableListOf<String>()
        val vocabulary = mutableListOf<VocabularyItem>()
        val tables = mutableListOf<TableData>()

        var currentTableLines = mutableListOf<String>()
        val currentParagraph = StringBuilder()

        fun flushParagraph() {
            val text = currentParagraph.toString().trim()
            if (text.isNotEmpty()) {
                paragraphs.add(text)
                currentParagraph.clear()
            }
        }

        fun flushTable() {
            if (currentTableLines.isNotEmpty()) {
                parseMarkdownTable(currentTableLines)?.let { tables.add(it) }
                currentTableLines = mutableListOf()
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                flushParagraph()
                flushTable()
                continue
            }

            // Check table
            if (TableParser.isTableLine(trimmed)) {
                flushParagraph()
                currentTableLines.add(trimmed)
                continue
            }

            // Check vocabulary item (e.g., "Term: Meaning" or "- Term -> Meaning" or "Term (Meaning)")
            val vocab = tryParseVocabularyItem(trimmed)
            if (vocab != null) {
                flushParagraph()
                flushTable()
                vocabulary.add(vocab)
                continue
            }

            // Check bullet
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") || isNumberedList(trimmed)) {
                flushParagraph()
                flushTable()
                val item = cleanBullet(trimmed)
                if (item.isNotEmpty()) {
                    bullets.add(item)
                }
                continue
            }

            // Otherwise paragraph
            flushTable()
            if (currentParagraph.isNotEmpty()) currentParagraph.append(" ")
            currentParagraph.append(trimmed)
        }

        flushParagraph()
        flushTable()

        return SectionGuide(paragraphs, bullets, vocabulary, tables)
    }

    private fun parseFinalReview(lines: List<String>): FinalReview {
        val items = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                val cleaned = cleanBullet(trimmed)
                if (cleaned.isNotEmpty()) {
                    items.add(cleaned)
                }
            }
        }
        return FinalReview(items)
    }

    private fun cleanBullet(line: String): String {
        return line.replaceFirst(Regex("^[-*•]\\s+"), "")
            .replaceFirst(Regex("^([০-৯]+|\\d+)[.)]\\s+"), "")
            .trim()
    }

    private fun isNumberedList(line: String): Boolean {
        return Regex("^([০-৯]+|\\d+)[.)]\\s+").containsMatchIn(line)
    }

    private fun tryParseVocabularyItem(line: String): VocabularyItem? {
        val cleaned = cleanBullet(line)

        // Check for Bengali Unicode characters range (\u0980-\u09FF)
        val hasBengali = cleaned.any { it in '\u0980'..'\u09FF' }

        // Formats:
        // 1. "Term -> MeaningBn" or "Term — MeaningBn"
        // 2. "Term: MeaningBn"
        // 3. "Term (MeaningBn)"
        if (cleaned.contains("->")) {
            val parts = cleaned.split("->", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                return VocabularyItem(parts[0].trim(), parts[1].trim())
            }
        }

        if (cleaned.contains("—")) {
            val parts = cleaned.split("—", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                return VocabularyItem(parts[0].trim(), parts[1].trim())
            }
        }

        if (hasBengali && cleaned.contains(":")) {
            val parts = cleaned.split(":", limit = 2)
            val term = parts[0].trim()
            val meaning = parts[1].trim()
            val termWordCount = term.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            if (term.isNotBlank() && meaning.isNotBlank() &&
                termWordCount in 1..4 && term.length <= 35 &&
                meaning.length <= 80 && !meaning.contains("।") && !meaning.contains(".")
            ) {
                return VocabularyItem(term, meaning)
            }
        }

        val parenMatch = Regex("^([^()]+)\\s*\\(([\\u0980-\\u09FF\\s,]+)\\)$").find(cleaned)
        if (parenMatch != null) {
            val term = parenMatch.groupValues[1].trim()
            val meaning = parenMatch.groupValues[2].trim()
            return VocabularyItem(term, meaning)
        }

        return null
    }

    private fun parseMarkdownTable(lines: List<String>): TableData? {
        return TableParser.parseTable(lines)
    }

    /**
     * Parses JSON topic packs (both single object and array of objects)
     */
    private fun tryParseJson(json: String): List<ParsedLessonPreview> {
        val previews = mutableListOf<ParsedLessonPreview>()
        try {
            // Check if array
            if (json.startsWith("[")) {
                val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                val list = adapter.fromJson(json) ?: return emptyList()
                for (item in list) {
                    val preview = parseJsonObjectToPreview(item)
                    if (preview != null) previews.add(preview)
                }
            } else {
                val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                val adapter = moshi.adapter<Map<String, Any>>(mapType)
                val map = adapter.fromJson(json) ?: return emptyList()
                val preview = parseJsonObjectToPreview(map)
                if (preview != null) previews.add(preview)
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return previews
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJsonObjectToPreview(map: Map<String, Any>): ParsedLessonPreview? {
        val issues = mutableListOf<ImportValidationIssue>()

        val title = (map["title"] as? String) ?: run {
            issues.add(ImportValidationIssue(IssueSeverity.ERROR, "JSON is missing 'title' field."))
            "Untitled Lesson"
        }

        val topicId = (map["topicId"] as? String) ?: (map["id"] as? String) ?: generateLessonId(title)
        val titleBn = map["titleBn"] as? String
        val category = (map["category"] as? String) ?: "General"
        val description = (map["description"] as? String) ?: ""
        val type = (map["type"] as? String) ?: (map["writingTypes"] as? List<*>)?.firstOrNull()?.toString() ?: "Focus Writing"

        // Guidelines
        val guidelinesMap = map["guidelines"] as? Map<String, Any>
        val gParagraphs = (guidelinesMap?.get("paragraphs") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val gBullets = (guidelinesMap?.get("bullets") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val guidelines = Guidelines(paragraphs = gParagraphs, bullets = gBullets)

        // Sections
        val sectionsList = map["sections"] as? List<Map<String, Any>>
        val sections = mutableListOf<LessonSection>()

        if (sectionsList != null) {
            for ((index, secMap) in sectionsList.withIndex()) {
                val secOrder = (secMap["order"] as? Number)?.toInt() ?: (index + 1)
                val secTitle = (secMap["title"] as? String) ?: "Section $secOrder"
                val secType = (secMap["type"] as? String) ?: "Writing"
                val task = (secMap["task"] as? String) ?: ""
                val timerSeconds = (secMap["timerSeconds"] as? Number)?.toInt() ?: 0

                val answerMap = secMap["answer"] as? Map<String, Any>
                val modelAnswer = (answerMap?.get("text") as? String) ?: (secMap["answer"] as? String) ?: ""

                val guideMap = secMap["guide"] as? Map<String, Any>
                val sParagraphs = (guideMap?.get("paragraphs") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val sBullets = (guideMap?.get("bullets") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                val vocabList = guideMap?.get("vocabulary") as? List<Map<String, Any>>
                val vocabulary = mutableListOf<VocabularyItem>()
                if (vocabList != null) {
                    for (v in vocabList) {
                        val term = v["term"] as? String ?: continue
                        val meaningBn = v["meaningBn"] as? String ?: ""
                        val note = v["note"] as? String
                        vocabulary.add(VocabularyItem(term, meaningBn, note))
                    }
                }

                val guide = SectionGuide(
                    paragraphs = sParagraphs,
                    bullets = sBullets,
                    vocabulary = vocabulary
                )

                sections.add(
                    LessonSection(
                        id = "${topicId}_sec_$secOrder",
                        lessonId = topicId,
                        order = secOrder,
                        title = secTitle,
                        type = secType,
                        guide = guide,
                        task = task,
                        timerSeconds = timerSeconds,
                        modelAnswer = modelAnswer
                    )
                )
            }
        }

        if (sections.isEmpty()) {
            issues.add(ImportValidationIssue(IssueSeverity.WARNING, "No sections found in JSON topic pack."))
        }

        // Final Review
        val finalReviewMap = map["finalReview"] as? Map<String, Any>
        val frItems = (finalReviewMap?.get("items") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val finalReview = FinalReview(frItems)

        val totalMinutes = sections.sumOf { if (it.timerSeconds > 0) it.timerSeconds / 60 else 5 }

        val lesson = Lesson(
            id = topicId,
            title = title,
            titleBn = titleBn,
            type = type,
            category = category,
            description = description,
            estimatedMinutes = if (totalMinutes > 0) totalMinutes else 15,
            guidelines = guidelines,
            sections = sections,
            finalReview = finalReview
        )

        return ParsedLessonPreview(lesson, issues)
    }

    private fun generateLessonId(title: String): String {
        val slug = title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(24)
        return if (slug.isNotBlank()) "MW-$slug" else "MW-${UUID.randomUUID().toString().take(8)}"
    }

    private fun hasBengaliChars(text: String): Boolean {
        return text.any { it in '\u0980'..'\u09FF' }
    }

    /**
     * Fetches an online newspaper article (e.g. Prothom Alo, Daily Star, Samakal, Jugantor, etc.)
     * and automatically extracts and structures it into lesson sections with guidelines and model answers.
     */
    suspend fun fetchAndParseNewspaperArticle(url: String): Pair<List<ParsedLessonPreview>, String?> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return@withContext Pair(emptyList(), "অনুগ্রহ করে একটি সঠিক ওয়েব লিঙ্ক দিন (যেমন: https://...)")
        }

        try {
            val req = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "bn-BD,bn;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()

            val response = okHttpClient.newCall(req).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Pair(emptyList(), "আর্টিকেল লোড করা যায়নি (HTTP ${resp.code}: ${resp.message})")
                }

                val html = resp.body?.string() ?: ""
                if (html.isBlank()) {
                    return@withContext Pair(emptyList(), "ওয়েবপেজে কোনো টেক্সট পাওয়া যায়নি।")
                }

                val extracted = extractArticleFromHtml(html, cleanUrl)
                if (extracted.isBlank()) {
                    return@withContext Pair(emptyList(), "ওয়েবপেজ থেকে আর্টিকেল বের করা যায়নি। আপনি সরাসরি টেক্সট কপি করে পেস্ট করতে পারেন।")
                }

                val structured = autoFormatNewspaperText(extracted)
                val previews = parseContent(structured)
                if (previews.isEmpty()) {
                    Pair(emptyList(), "আর্টিকেল থেকে সেকশন তৈরি করা সম্ভব হয়নি। টেক্সট অপশনে পেস্ট করে ট্রাই করুন।")
                } else {
                    Pair(previews, null)
                }
            }
        } catch (e: Exception) {
            Pair(emptyList(), "নেটওয়ার্ক এরর: ${e.localizedMessage ?: "সংযোগ বিচ্ছিন্ন"}")
        }
    }

    /**
     * Extracts title and clean paragraphs/headings from newspaper HTML.
     */
    fun extractArticleFromHtml(html: String, sourceUrl: String): String {
        // Extract title
        var title = ""
        val ogTitleMatch = Regex("<meta\\s+property=[\"']og:title[\"']\\s+content=[\"'](.*?)[\"']", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("<meta\\s+content=[\"'](.*?)[\"']\\s+property=[\"']og:title[\"']", RegexOption.IGNORE_CASE).find(html)
        if (ogTitleMatch != null) {
            title = cleanHtmlEntities(ogTitleMatch.groupValues[1])
        }

        if (title.isBlank()) {
            val titleTagMatch = Regex("<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)
            if (titleTagMatch != null) {
                title = cleanHtmlEntities(titleTagMatch.groupValues[1])
            }
        }

        if (title.isBlank()) {
            val h1Match = Regex("<h1[^>]*>(.*?)</h1>", RegexOption.IGNORE_CASE).find(html)
            if (h1Match != null) {
                title = cleanHtmlEntities(stripHtmlTags(h1Match.groupValues[1]))
            }
        }

        // Clean site suffix from title: " | প্রথম আলো", " - The Daily Star", " | Samakal", etc.
        title = title.split(Regex("[\\|–—]|\\s-\\s")).firstOrNull()?.trim() ?: title
        if (title.isBlank()) title = "Newspaper Article"

        // Strip scripts, styles, navs, headers, footers
        val bodyHtml = html
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<nav[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<header[\\s\\S]*?</header>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<footer[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<svg[\\s\\S]*?</svg>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<!--[\\s\\S]*?-->"), "")

        // Look for article container if available
        val articleMatch = Regex("<article[^>]*>([\\s\\S]*?)</article>", RegexOption.IGNORE_CASE).find(bodyHtml)
        val contentHtml = articleMatch?.groupValues?.get(1) ?: bodyHtml

        // Extract paragraphs and headings
        val tagPattern = Pattern.compile("<(p|h2|h3|h4|blockquote)[^>]*>([\\s\\S]*?)</\\1>", Pattern.CASE_INSENSITIVE)
        val matcher = tagPattern.matcher(contentHtml)

        val items = mutableListOf<String>()
        while (matcher.find()) {
            val tagName = matcher.group(1)?.lowercase() ?: ""
            val innerHtml = matcher.group(2) ?: ""
            val cleanText = cleanHtmlEntities(stripHtmlTags(innerHtml)).trim()
            if (cleanText.length >= 20 && !isJunkContent(cleanText)) {
                if (tagName.startsWith("h")) {
                    items.add("## $cleanText")
                } else {
                    items.add(cleanText)
                }
            }
        }

        if (items.isEmpty()) {
            return title
        }

        return "$title\n\n" + items.joinToString("\n\n")
    }

    private fun stripHtmlTags(html: String): String {
        return html.replace(Regex("<[^>]+>"), " ").trim()
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&rsquo;", "'")
            .replace("&lsquo;", "'")
            .replace("&rdquo;", "\"")
            .replace("&ldquo;", "\"")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isJunkContent(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("copyright") ||
                lower.contains("all rights reserved") ||
                lower.contains("সব অধিকার সংরক্ষিত") ||
                lower.contains("বিজ্ঞাপন") ||
                lower.startsWith("ছবি:") ||
                lower.startsWith("ফাইল ছবি") ||
                lower.startsWith("ভিডিও:")
    }

    /**
     * Automatically converts raw newspaper text into structured sections
     * with numbering (১., ২., etc.) and summary guidelines in brackets [...].
     */
    fun autoFormatNewspaperText(rawText: String): String {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return ""

        val lines = trimmed.lines().map { it.trim() }
        val nonBlankLines = lines.filter { it.isNotBlank() }
        if (nonBlankLines.isEmpty()) return ""

        // If it already has multiple numbered sections, return as is
        val numberedCount = nonBlankLines.count {
            Regex("^([০-৯]+|\\d+)[\\.\\):/\\-–—]\\s+").containsMatchIn(it)
        }
        if (numberedCount >= 2) {
            return rawText
        }

        val firstLine = nonBlankLines[0]
        val title = firstLine
            .removePrefix("@")
            .removePrefix("#")
            .replace(Regex("^(?:শিরোনাম|TITLE|HEADING)[:\\s-]*", RegexOption.IGNORE_CASE), "")
            .trim(' ', ':', '-', '[', ']', '*')

        val isBengali = hasBengaliChars(trimmed)
        val bengaliNums = listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "১০", "১১", "১২", "১৩", "১৪", "১৫")

        // Split remaining content into paragraphs (separated by blank lines)
        val rawParagraphs = trimmed.split(Regex("\n\\s*\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val bodyParagraphs = if (rawParagraphs.size > 1 && rawParagraphs[0].contains(title)) {
            rawParagraphs.drop(1)
        } else if (rawParagraphs.isNotEmpty()) {
            val firstPara = rawParagraphs[0]
            val linesInFirst = firstPara.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (linesInFirst.size > 1) {
                listOf(linesInFirst.drop(1).joinToString("\n")) + rawParagraphs.drop(1)
            } else {
                rawParagraphs.drop(1)
            }
        } else {
            emptyList()
        }

        if (bodyParagraphs.isEmpty()) {
            return "@$title\n\n১. সারসংক্ষেপ\n[মূল বিষয়: $title]\n$trimmed"
        }

        // Identify candidate subheadings: short paragraph without terminal punctuation
        val isCandidateSubheading = { p: String ->
            val pTrim = p.trim(' ', '*', '#', '_')
            pTrim.length in 4..65 &&
                    !pTrim.endsWith("।") &&
                    !pTrim.endsWith(".") &&
                    !pTrim.endsWith("?") &&
                    !pTrim.endsWith("!") &&
                    !pTrim.contains("\n") &&
                    !pTrim.startsWith("- ") &&
                    !pTrim.startsWith("* ") &&
                    !pTrim.startsWith("• ")
        }

        val subheadings = mutableListOf<Pair<Int, String>>()
        for (i in bodyParagraphs.indices) {
            if (isCandidateSubheading(bodyParagraphs[i])) {
                subheadings.add(Pair(i, bodyParagraphs[i].trim(' ', '*', '#', '_')))
            }
        }

        val sb = StringBuilder()
        sb.append("@").append(title).append("\n\n")

        if (subheadings.size >= 2) {
            // Intro before first subheading
            val firstSubIdx = subheadings[0].first
            if (firstSubIdx > 0) {
                val introParas = bodyParagraphs.subList(0, firstSubIdx)
                val introText = introParas.joinToString("\n\n")
                val leadSentence = extractLeadSentence(introText)
                if (leadSentence.isNotBlank()) {
                    val label = if (isBengali) "ভূমিকা ও প্রেক্ষাপট" else "Overview & Context"
                    sb.append("[$label: $leadSentence]\n")
                }
                sb.append(introText).append("\n\n")
            }

            for (sIdx in subheadings.indices) {
                val (pIdx, subTitle) = subheadings[sIdx]
                val nextPIdx = if (sIdx + 1 < subheadings.size) subheadings[sIdx + 1].first else bodyParagraphs.size
                val sectionParas = bodyParagraphs.subList(pIdx + 1, nextPIdx)
                val numStr = if (isBengali && sIdx < bengaliNums.size) bengaliNums[sIdx] else "${sIdx + 1}"

                sb.append("$numStr. $subTitle\n")
                if (sectionParas.isNotEmpty()) {
                    val secText = sectionParas.joinToString("\n\n")
                    val lead = extractLeadSentence(secText)
                    if (lead.isNotBlank()) {
                        val guideLabel = if (isBengali) "মূল ভাবনা" else "Key Point"
                        val term = if (isBengali) "।" else "."
                        sb.append("[$guideLabel: $lead$term]\n")
                    }
                    sb.append(secText).append("\n\n")
                } else {
                    sb.append("\n")
                }
            }
        } else {
            // Group paragraphs into 2-paragraph chunks
            val chunkSize = if (bodyParagraphs.size >= 4) 2 else 1
            val chunks = bodyParagraphs.chunked(chunkSize)
            val bnSectionTitles = listOf(
                "প্রেক্ষাপট ও সূচনা",
                "মূল সংকট ও বিশ্লেষণ",
                "প্রভাব ও বর্তমান অবস্থা",
                "উত্তরণের উপায় ও সুপারিশ",
                "ভবিষ্যৎ রূপরেখা ও উপসংহার"
            )
            val enSectionTitles = listOf(
                "Context & Overview",
                "Core Analysis",
                "Key Impact & Realities",
                "Recommendations & Path Forward",
                "Conclusion"
            )

            for (cIdx in chunks.indices) {
                val chunk = chunks[cIdx]
                val numStr = if (isBengali && cIdx < bengaliNums.size) bengaliNums[cIdx] else "${cIdx + 1}"
                val secTitle = if (isBengali) {
                    bnSectionTitles.getOrElse(cIdx) { "পর্ব $numStr" }
                } else {
                    enSectionTitles.getOrElse(cIdx) { "Section $numStr" }
                }

                sb.append("$numStr. $secTitle\n")
                val chunkText = chunk.joinToString("\n\n")
                val lead = extractLeadSentence(chunkText)
                if (lead.isNotBlank()) {
                    val guideLabel = if (isBengali) "মূল ভাবনা" else "Key Point"
                    val term = if (isBengali) "।" else "."
                    sb.append("[$guideLabel: $lead$term]\n")
                }
                sb.append(chunkText).append("\n\n")
            }
        }

        return sb.toString().trim()
    }

    private fun extractLeadSentence(text: String): String {
        val clean = text.replace(Regex("^[#\\-*•\\s]+"), "").trim()
        val sentences = clean.split(Regex("[।\\.\\n]"))
        val first = sentences.firstOrNull { it.trim().length >= 10 }?.trim() ?: ""
        return if (first.length > 120) first.take(117) + "..." else first
    }
}
