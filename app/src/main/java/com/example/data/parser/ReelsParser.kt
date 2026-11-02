package com.example.data.parser

import com.example.data.model.ReelDocument
import com.example.data.model.ReelPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

class ReelsParser(
    private val resourceParser: ResourceParser = ResourceParser(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    fun parseRawTextToReel(rawText: String, sourceUrl: String = ""): ReelDocument {
        val trimmed = rawText
            .replace("\uFEFF", "")
            .replace("\u200B", "")
            .replace("\u00A0", " ")
            .trim()

        if (trimmed.isEmpty()) {
            return getDefaultReelDocument()
        }

        val markdown = if (trimmed.contains("<html", true) || trimmed.contains("<body", true) ||
            trimmed.contains("<p>", true) || trimmed.contains("<div", true)
        ) {
            resourceParser.convertGoogleDocHtmlToMarkdown(trimmed)
        } else {
            resourceParser.cleanHtmlEntities(trimmed)
        }

        val allLines = markdown.lines().map { it.trimEnd() }
        var title = "Document Reel"
        val remainingLines = mutableListOf<String>()
        var foundTitle = false

        for (line in allLines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) {
                if (foundTitle) {
                    remainingLines.add("")
                }
                continue
            }

            if (!foundTitle) {
                val cleanedTitle = trimmedLine
                    .replace(Regex("^[#@$\\s*]+"), "")
                    .replace(Regex("[*#]+$"), "")
                    .trim()
                if (cleanedTitle.isNotEmpty()) {
                    title = cleanedTitle
                    foundTitle = true
                    continue
                }
            }

            remainingLines.add(line)
        }

        val fullRemainingText = remainingLines.joinToString("\n").trim()
        val rawParagraphs = fullRemainingText.split(Regex("\n\\s*\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val pages = mutableListOf<ReelPage>()
        var pageIndex = 1

        for (para in rawParagraphs) {
            val cleanPara = para.trim()
            if (cleanPara.isEmpty() || cleanPara == "---" || cleanPara == "***") {
                continue
            }

            pages.add(
                ReelPage(
                    id = UUID.randomUUID().toString(),
                    pageNumber = pageIndex,
                    content = cleanPara,
                    docTitle = title,
                    isTitlePage = false
                )
            )
            pageIndex++
        }

        if (pages.isEmpty()) {
            return getDefaultReelDocument()
        }

        return ReelDocument(
            title = title,
            sourceUrl = sourceUrl,
            pages = pages
        )
    }

    suspend fun fetchAndParseDocUrl(url: String): Pair<ReelDocument?, String?> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) {
            return@withContext Pair(null, "Document URL cannot be empty")
        }

        val docId = extractGoogleDocId(cleanUrl)
        if (docId != null) {
            val htmlUrl = "https://docs.google.com/document/d/$docId/export?format=html"
            try {
                val req = Request.Builder()
                    .url(htmlUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val resp = okHttpClient.newCall(req).execute()
                resp.use { r ->
                    if (r.isSuccessful && r.body != null) {
                        val body = r.body!!.string()
                        if (body.isNotBlank()) {
                            val reel = parseRawTextToReel(body, cleanUrl)
                            return@withContext Pair(reel, null)
                        }
                    } else if (r.code == 401 || r.code == 403) {
                        return@withContext Pair(null, "Access denied. Please ensure the Google Doc is shared as 'Anyone with the link can view'.")
                    }
                }
            } catch (e: Exception) {
                try {
                    val docxUrl = "https://docs.google.com/document/d/$docId/export?format=docx"
                    val docxReq = Request.Builder()
                        .url(docxUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()
                    val docxResp = okHttpClient.newCall(docxReq).execute()
                    docxResp.use { r ->
                        if (r.isSuccessful && r.body != null) {
                            val text = resourceParser.extractTextFromDocx(r.body!!.byteStream())
                            if (text.isNotBlank()) {
                                val reel = parseRawTextToReel(text, cleanUrl)
                                return@withContext Pair(reel, null)
                            }
                        }
                    }
                } catch (e2: Exception) {
                    return@withContext Pair(null, "Failed to load document: ${e2.localizedMessage ?: e.localizedMessage}")
                }
            }
        } else {
            try {
                val req = Request.Builder()
                    .url(cleanUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val resp = okHttpClient.newCall(req).execute()
                resp.use { r ->
                    if (r.isSuccessful && r.body != null) {
                        val body = r.body!!.string()
                        val reel = parseRawTextToReel(body, cleanUrl)
                        return@withContext Pair(reel, null)
                    } else {
                        return@withContext Pair(null, "Failed to download document (HTTP ${r.code})")
                    }
                }
            } catch (e: Exception) {
                return@withContext Pair(null, "Connection error: ${e.localizedMessage}")
            }
        }

        return@withContext Pair(null, "Unable to extract content from document link")
    }

    private fun extractGoogleDocId(url: String): String? {
        val patterns = listOf(
            Regex("/document/d/([a-zA-Z0-9_-]+)"),
            Regex("id=([a-zA-Z0-9_-]+)"),
            Regex("/d/([a-zA-Z0-9_-]+)")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun getDefaultReelDocument(): ReelDocument {
        val title = "বাংলাদেশের গ্যাসচিত্র: উৎপাদন, আমদানি ও জ্বালানি নিরাপত্তা"
        val sampleParagraphs = listOf(
            "বাংলাদেশের প্রাকৃতিক গ্যাস খাতে দেশীয় উৎপাদন হ্রাস, এলএনজি আমদানি বৃদ্ধি এবং দীর্ঘমেয়াদী জ্বালানি নিরাপত্তা বর্তমান অর্থনীতির প্রধান আলোচ্য বিষয়। ২০২৩-২৪ অর্থবছরে দৈনিক সামগ্রিক চাহিদা ছিল প্রায় **৩,৮০০ MMcfd**।",
            "**২১ জুলাই:** মহেশখালীর একটি ভাসমান এলএনজি টার্মিনালে অগ্নিকাণ্ডের পর সরবরাহ ব্যাহত হয়। এর ফলে আন্তর্জাতিক স্পট মার্কেট থেকে জরুরি ভিত্তিতে অতিরিক্ত জ্বালানি সংগ্রহের প্রয়োজনীয়তা দেখা দেয়।",
            "দ্য ডেইলি স্টারের ৩১ আগস্ট ২০২৬-এর প্রতিবেদন অনুযায়ী, আগস্টে দৈনিক গ্যাস সরবরাহের গড় নেমে আসে প্রায় **২,২৩৫ MMcfd**-এ, যা এক দশকের মধ্যে সর্বনিম্ন হিসেবে উল্লেখ করা হয়।",
            "**১৫ সেপ্টেম্বর:** সরবরাহ পুনরায় বৃদ্ধি পেয়ে প্রায় **২,৬১০ MMcfd**-এ ফিরে আসে। তবে দেশীয় শিল্প কারখানা ও সার কারখানায় স্বাভাবিক সরবরাহ নিশ্চিত করা এখনও বড় চ্যালেঞ্জ।",
            "**২০ সেপ্টেম্বর:** তিতাস-২৮ কুপ থেকে দৈনিক প্রায় **১২–১৩ মিলিয়ন** ঘনফুট গ্যাস জাতীয় গ্রিডে সরবরাহ শুরু হয়, যা সংকট উত্তরণে তাৎক্ষণিক স্বস্তি দেয়।",
            "**২৩ অক্টোবর:** দ্য ফিন্যান্সিয়াল এক্সপ্রেসের প্রতিবেদন অনুযায়ী, ২০২৬ সালের নভেম্বর মাসের জন্য মোট **৯টি** এলএনজি কার্গো আমদানির পরিকল্পনা অনুমোদন করা হয়েছে।",
            "আন্তর্জাতিক বাজারে এলএনজি-র মূল্য পরিমাপে প্রতি **MMBtu** ভিত্তি ধরা হয়। দেশীয় বিদ্যুৎ খাতের প্রায় **৫৫%** এবং শিল্প খাতের **১৮%** জ্বালানি প্রত্যক্ষভাবে গ্যাস সরবরাহের উপর নির্ভরশীল।",
            "বিশেষজ্ঞদের মতে, কেবল আমদানি বৃদ্ধির উপর নির্ভর না করে বঙ্গোপসাগরের গভীর সমুদ্রে তেল-গ্যাস অনুসন্ধানে দ্রুত পিএসসি (PSC) চুক্তি চূড়ান্ত করা আবশ্যক।"
        )

        val pages = sampleParagraphs.mapIndexed { index, content ->
            ReelPage(
                id = "default_page_${index + 1}",
                pageNumber = index + 1,
                content = content,
                docTitle = title
            )
        }

        return ReelDocument(
            title = title,
            sourceUrl = "",
            pages = pages
        )
    }

    fun getPresetDocuments(): List<ReelDocument> {
        val gasDoc = getDefaultReelDocument()

        val macroTitle = "বাংলাদেশের সামষ্টিক অর্থনীতি ও বাণিজ্য নীতি"
        val macroParagraphs = listOf(
            "বাংলাদেশের বৈদেশিক মুদ্রার রিজার্ভ ও রপ্তানি প্রবৃদ্ধি ধরে রাখতে বহুমুখী বাণিজ্য চুক্তি সম্পাদন ও রেমিট্যান্স প্রবাহ ত্বরান্বিত করা অন্যতম প্রধান লক্ষ্য হিসেবে নির্ধারিত হয়েছে।",
            "**২০২৪-২৫ অর্থবছর:** রপ্তানি আয়ের সিংহভাগ আসে তৈরি পোশাক (RMG) খাত থেকে। মোট রপ্তানির প্রায় **৮১%** এই একক খাতের ওপর নির্ভরশীল।",
            "বাংলাদেশ ব্যাংকের তথ্যানুযায়ী, সামগ্রিক আমদানি ব্যয় নিয়ন্ত্রণের মাধ্যমে চলতি হিসাবের ঘাটতি উল্লেখযোগ্য হারে হ্রাস পেয়েছে এবং মোট রিজার্ভ **২৫ বিলিয়ন** ডলারের স্থিতিশীল অবস্থানে রয়েছে।",
            "মূল্যস্ফীতি নিয়ন্ত্রণে বাংলাদেশ ব্যাংক নীতি সুদহার বৃদ্ধি করে **১০.০%**-এ উন্নীত করেছে, যা ব্যাংক ঋণের সুদহারের ওপর সরাসরি প্রভাব ফেলেছে।",
            "আন্তর্জাতিক মুদ্রা তহবিল (IMF)-এর **৪.৭ বিলিয়ন** ডলারের ঋণ প্যাকেজের আওতায় রাজস্ব সংস্কার ও কর-জিডিপি অনুপাত বৃদ্ধির শর্ত পূরণে জাতীয় রাজস্ব বোর্ড বিশেষ উদ্যোগ গ্রহণ করেছে।"
        )
        val macroDoc = ReelDocument(
            title = macroTitle,
            sourceUrl = "",
            pages = macroParagraphs.mapIndexed { index, content ->
                ReelPage(
                    id = "macro_page_${index + 1}",
                    pageNumber = index + 1,
                    content = content,
                    docTitle = macroTitle
                )
            }
        )

        val writingTitle = "ফোকাস রাইটিং ও রিভিশন কৌশল"
        val writingParagraphs = listOf(
            "ফোকাস রাইটিংয়ের মূল উদ্দেশ্য হলো নির্ধারিত সময়ের মধ্যে কোনো নির্দিষ্ট বিষয়ের ওপর কাঠামোগত, স্পষ্ট ও তথ্যসমৃদ্ধ অনুচ্ছেদ রচনা করা।",
            "**প্রথম ধাপ:** প্রশ্নের মূল বক্তব্য অনুধাবন করে প্রাসঙ্গিক তথ্য, সাম্প্রতিক পরিসংখ্যান ও যৌক্তিক মতামত ক্রমানুসারে সাজানো।",
            "লেখার শুরুতে একটি শক্তিশালী ভূমিকা ও উপসংহারে সুস্পষ্ট সুপারিশ থাকলে যেকোনো প্রতিযোগিতামূলক মূল্যায়নে সর্বোচ্চ নম্বর প্রাপ্তি সহজতর হয়।",
            "প্রতিটি অনুচ্ছেদে নতুন একটি দিক উন্মোচন করতে হবে এবং অপ্রয়োজনীয় বাক্যাংশ বর্জন করে স্পষ্ট ও নির্ভুল ব্যাকরণ বজায় রাখা অপরিহার্য।"
        )
        val writingDoc = ReelDocument(
            title = writingTitle,
            sourceUrl = "",
            pages = writingParagraphs.mapIndexed { index, content ->
                ReelPage(
                    id = "writing_page_${index + 1}",
                    pageNumber = index + 1,
                    content = content,
                    docTitle = writingTitle
                )
            }
        )

        return listOf(gasDoc, macroDoc, writingDoc)
    }
}

