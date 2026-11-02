package com.example.ui.screens.reels

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.example.ui.theme.BanglaFontFamily
import com.example.ui.theme.PoppinsFontFamily
import java.util.regex.Pattern

object ReelsTextRenderer {

    // Regex patterns for numbers, dates, statistics, and units
    private val numberAndDatePattern = Pattern.compile(
        "(" +
            // Bengali Dates (e.g. ২১ জুলাই, ৩১ আগস্ট ২০২৬, ১৫ সেপ্টেম্বর)
            "[০-৯]{1,2}\\s+(?:জানুয়ারি|ফেব্রুয়ারি|মার্চ|এপ্রিল|মে|জুন|জুলাই|আগস্ট|সেপ্টেম্বর|অক্টোবর|নভেম্বর|ডিসেম্বর)(?:\\s+[০-৯]{4})?" +
            // English Dates (e.g. 21 July, 31 August 2026)
            "|\\b\\d{1,2}\\s+(?:January|February|March|April|May|June|July|August|September|October|November|December)(?:\\s+\\d{4})?\\b" +
            // Years with optional Bengali suffixes (e.g. ২০২৬-এর, ২০২৪-এ, 2026)
            "|(?:২০[০-৯]{2}|[১-২][০-৯]{3}|20[0-9]{2})(?:-(?:এর|এ|পরবর্তী))?" +
            // Bengali numbers with decimals, commas, percentages, hyphens
            "|[০-৯]+(?:[\\.,–-][০-৯]+)*(?:%|টি|টি কার্গো)?" +
            // English numbers with decimals, commas, percentages
            "|\\b\\d+(?:[\\.,–-]\\d+)*(?:%|st|nd|rd|th)?\\b" +
            // Common statistics & energy units
            "|\\b(?:MMcfd|BCF|MMBtu|MW|GW|PSC|BDT|USD|টাকা|ঘনফুট|মিলিয়ন|বিলিয়ন|কোটি)\\b" +
        ")",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    fun hasBengaliChars(text: String): Boolean {
        for (c in text) {
            if (c in '\u0980'..'\u09FF') return true
        }
        return false
    }

    fun getBaseFontFamily(text: String): FontFamily {
        return if (hasBengaliChars(text)) BanglaFontFamily else PoppinsFontFamily
    }

    /**
     * Builds an AnnotatedString preserving:
     * - Bold: **text**
     * - Italic: *text*
     * - Underline: <u>text</u>
     * - Strikethrough: ~~text~~
     * - Numbers, Dates, Units: Highlighted in custom vivid accent color (e.g. Amber / Cyan)
     * - Bengali text rendered in Kalpurush/Noto Sans Bengali, English in Poppins
     */
    fun buildReelAnnotatedString(
        content: String,
        highlightNumbers: Boolean = true,
        numberColor: Color = Color(0xFFFFB703),
        defaultTextColor: Color = Color.White
    ): AnnotatedString {
        val baseFont = getBaseFontFamily(content)

        // 1. First parse markdown markers (**bold**, *italic*, <u>underline</u>, ~~strikethrough~~)
        var text = content

        data class StyleSpan(
            val start: Int,
            val end: Int,
            val isBold: Boolean = false,
            val isItalic: Boolean = false,
            val isUnderline: Boolean = false,
            val isStrikethrough: Boolean = false
        )

        val accumulatedSpans = mutableListOf<StyleSpan>()

        // Replace <u>...</u>
        val underlinePattern = Pattern.compile("<u>(.*?)</u>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        while (true) {
            val m = underlinePattern.matcher(text)
            if (m.find()) {
                val start = m.start()
                val inner = m.group(1) ?: ""
                text = text.substring(0, start) + inner + text.substring(m.end())
                accumulatedSpans.add(StyleSpan(start, start + inner.length, isUnderline = true))
            } else break
        }

        // Replace ~~...~~ (strikethrough)
        val strikePattern = Pattern.compile("~~(.*?)~~", Pattern.DOTALL)
        while (true) {
            val m = strikePattern.matcher(text)
            if (m.find()) {
                val start = m.start()
                val inner = m.group(1) ?: ""
                text = text.substring(0, start) + inner + text.substring(m.end())
                accumulatedSpans.add(StyleSpan(start, start + inner.length, isStrikethrough = true))
            } else break
        }

        // Replace **...** (bold)
        val boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*", Pattern.DOTALL)
        while (true) {
            val m = boldPattern.matcher(text)
            if (m.find()) {
                val start = m.start()
                val inner = m.group(1) ?: ""
                text = text.substring(0, start) + inner + text.substring(m.end())
                accumulatedSpans.add(StyleSpan(start, start + inner.length, isBold = true))
            } else break
        }

        // Replace *...* (italic)
        val italicPattern = Pattern.compile("(?<!\\*)\\*(?!\\s)(.*?)(?<!\\s)\\*(?!\\*)", Pattern.DOTALL)
        while (true) {
            val m = italicPattern.matcher(text)
            if (m.find()) {
                val start = m.start()
                val inner = m.group(1) ?: ""
                text = text.substring(0, start) + inner + text.substring(m.end())
                accumulatedSpans.add(StyleSpan(start, start + inner.length, isItalic = true))
            } else break
        }

        // 2. Identify numbers and dates in the final unescaped text
        val numberDateSpans = mutableListOf<Pair<Int, Int>>()
        if (highlightNumbers) {
            val matcher = numberAndDatePattern.matcher(text)
            while (matcher.find()) {
                numberDateSpans.add(Pair(matcher.start(), matcher.end()))
            }
        }

        // 3. Construct AnnotatedString
        return buildAnnotatedString {
            append(text)

            // Base style: default text color and base font family
            addStyle(
                SpanStyle(
                    color = defaultTextColor,
                    fontFamily = baseFont
                ),
                0,
                text.length
            )

            // If the document is predominantly Bengali, apply Poppins to English words/tokens
            if (baseFont == BanglaFontFamily) {
                val engPattern = Pattern.compile("[a-zA-Z]{2,}")
                val engMatcher = engPattern.matcher(text)
                while (engMatcher.find()) {
                    addStyle(
                        SpanStyle(fontFamily = PoppinsFontFamily),
                        engMatcher.start(),
                        engMatcher.end()
                    )
                }
            }

            // Apply markdown styles
            for (span in accumulatedSpans) {
                val safeStart = span.start.coerceIn(0, text.length)
                val safeEnd = span.end.coerceIn(0, text.length)
                if (safeStart < safeEnd) {
                    val fontWt = if (span.isBold) FontWeight.Bold else null
                    val fontSt = if (span.isItalic) FontStyle.Italic else null
                    val decoration = when {
                        span.isUnderline && span.isStrikethrough -> TextDecoration.combine(
                            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                        )
                        span.isUnderline -> TextDecoration.Underline
                        span.isStrikethrough -> TextDecoration.LineThrough
                        else -> null
                    }

                    addStyle(
                        SpanStyle(
                            fontWeight = fontWt,
                            fontStyle = fontSt,
                            textDecoration = decoration
                        ),
                        safeStart,
                        safeEnd
                    )
                }
            }

            // Apply number and date highlights
            for ((numStart, numEnd) in numberDateSpans) {
                val safeStart = numStart.coerceIn(0, text.length)
                val safeEnd = numEnd.coerceIn(0, text.length)
                if (safeStart < safeEnd) {
                    addStyle(
                        SpanStyle(
                            color = numberColor,
                            fontWeight = FontWeight.Bold
                        ),
                        safeStart,
                        safeEnd
                    )
                }
            }
        }
    }
}
