package com.example

import com.example.data.export.ExportManager
import com.example.data.export.ExportMode
import com.example.data.model.Lesson
import com.example.data.model.LessonSection
import com.example.data.parser.ResourceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val parser = ResourceParser()

    @Test
    fun testMarkerFormatV2Parsing() {
        val rawContent = """
LESSON TITLE: Resolution of Non-Performing Loans
LESSON TYPE: Focus Writing
LESSON ID: BD-BANK-NPL-001
LESSON DESCRIPTION: Learn about NPLs and practise focus writing.

GENERAL GUIDELINES START
General information about macro-prudential regulations.
- Important concept 1
- Important concept 2
GENERAL GUIDELINES END

SECTION 1
SECTION TITLE: Introduction to NPLs
SECTION TYPE: Writing

SECTION GUIDE START
Instructions and facts about loan classification.
Vocabulary:
- Asset quality deterioration -> ঋণের গুণগত মানের অবনতি
- Substandard loan -> নিম্নমান ঋণ
SECTION GUIDE END

SECTION TASK: Write an introduction in 100–130 words.
SECTION TIMER: 300

SECTION ANSWER START
A Non-Performing Loan (NPL) represents a delinquent debt instrument.
SECTION ANSWER END

SECTION END

FINAL REVIEW START
- Check accuracy of classification terms.
- Confirm logical transition.
FINAL REVIEW END

LESSON END
        """.trimIndent()

        val previews = parser.parseContent(rawContent)
        assertEquals(1, previews.size)

        val preview = previews[0]
        assertTrue(preview.isValid)

        val lesson = preview.lesson
        assertEquals("Resolution of Non-Performing Loans", lesson.title)
        assertEquals("Focus Writing", lesson.type)
        assertEquals("BD-BANK-NPL-001", lesson.id)
        assertEquals(1, lesson.guidelines.paragraphs.size)
        assertEquals(2, lesson.guidelines.bullets.size)

        assertEquals(1, lesson.sections.size)
        val section = lesson.sections[0]
        assertEquals("Introduction to NPLs", section.title)
        assertEquals("Write an introduction in 100–130 words.", section.task)
        assertEquals(300, section.timerSeconds)
        assertEquals("A Non-Performing Loan (NPL) represents a delinquent debt instrument.", section.modelAnswer)

        assertEquals(2, section.guide.vocabulary.size)
        assertEquals("Asset quality deterioration", section.guide.vocabulary[0].term)
        assertEquals("ঋণের গুণগত মানের অবনতি", section.guide.vocabulary[0].meaningBn)

        assertEquals(2, lesson.finalReview.items.size)
    }

    @Test
    fun testMultipleLessonsInSingleDocument() {
        val raw = """
LESSON TITLE: Lesson One
SECTION 1
SECTION TITLE: Sec 1
SECTION END
LESSON END

LESSON TITLE: Lesson Two
SECTION 1
SECTION TITLE: Sec 2
SECTION END
LESSON END
        """.trimIndent()

        val previews = parser.parseContent(raw)
        assertEquals(2, previews.size)
        assertEquals("Lesson One", previews[0].lesson.title)
        assertEquals("Lesson Two", previews[1].lesson.title)
    }

    @Test
    fun testJsonTopicPackParsing() {
        val json = """
{
  "schemaVersion": 1,
  "topicId": "BD-BANK-NPL-001",
  "title": "Resolution of Non-Performing Loans",
  "titleBn": "খেলাপি ঋণ সমস্যার সমাধান",
  "category": "Banking",
  "writingTypes": ["Focus Writing"],
  "guidelines": {
    "paragraphs": ["General guideline."],
    "bullets": ["Bullet point 1"]
  },
  "sections": [
    {
      "order": 1,
      "title": "Introduction to NPLs",
      "type": "writing_task",
      "task": "Write introduction.",
      "timerSeconds": 300,
      "guide": {
        "paragraphs": ["Guide text."],
        "vocabulary": [
          { "term": "Moral hazard", "meaningBn": "নৈতিক অবক্ষয়জনিত ঝুঁকি" }
        ]
      },
      "answer": {
        "text": "Model answer text."
      }
    }
  ],
  "finalReview": {
    "items": ["Review checklist."]
  }
}
        """.trimIndent()

        val previews = parser.parseContent(json)
        assertEquals(1, previews.size)
        val lesson = previews[0].lesson
        assertEquals("Resolution of Non-Performing Loans", lesson.title)
        assertEquals("খেলাপি ঋণ সমস্যার সমাধান", lesson.titleBn)
        assertEquals("Banking", lesson.category)
        assertEquals(1, lesson.sections.size)
        assertEquals("নৈতিক অবক্ষয়জনিত ঝুঁকি", lesson.sections[0].guide.vocabulary[0].meaningBn)
    }

    @Test
    fun testCompletionCalculation() {
        val sections = listOf(
            LessonSection(id = "s1", lessonId = "l1", order = 1, title = "S1", isCompleted = true),
            LessonSection(id = "s2", lessonId = "l1", order = 2, title = "S2", isCompleted = false, isSkipped = true),
            LessonSection(id = "s3", lessonId = "l1", order = 3, title = "S3", isCompleted = true),
            LessonSection(id = "s4", lessonId = "l1", order = 4, title = "S4", isCompleted = false)
        )
        val lesson = Lesson(id = "l1", title = "L1", sections = sections)

        assertEquals(2, lesson.completedSectionCount)
        assertEquals(4, lesson.totalSectionCount)
        assertEquals(50, lesson.completionPercentage)
    }

    @Test
    fun testExportFormattedTextGeneration() {
        val lesson = Lesson(
            id = "l1",
            title = "Test Lesson",
            type = "Focus Writing",
            category = "Economics",
            sections = listOf(
                LessonSection(
                    id = "s1",
                    lessonId = "l1",
                    order = 1,
                    title = "Intro",
                    userDraft = "This is my essay draft.",
                    modelAnswer = "This is model answer."
                )
            )
        )

        val userOnlyText = ExportManager.generateFormattedText(lesson, ExportMode.USER_WRITING_ONLY)
        assertTrue(userOnlyText.contains("[MY WRITING]"))
        assertTrue(userOnlyText.contains("This is my essay draft."))
        assertTrue(!userOnlyText.contains("[MODEL ANSWER]"))

        val bothText = ExportManager.generateFormattedText(lesson, ExportMode.BOTH)
        assertTrue(bothText.contains("[MY WRITING]"))
        assertTrue(bothText.contains("[MODEL ANSWER]"))
    }

    @Test
    fun testShorthandFormatParsing() {
        val shorthandText = """
@Type: Title: Renewable Energy in Bangladesh
-Description: Assessing green energy transition.
-Guidelines:
Focus on solar and wind potential.
- Need realistic targets
- Grid modernization

Section1: Present Energy Matrix
Guide:
Detail current fossil fuel dependence.
Vocabulary:
- Energy transition -> জ্বালানি রূপান্তর
Answer:
Bangladesh currently relies heavily on natural gas and imported LNG.

Section2:
Solar and Wind Potential
Guide:
Examine geographical limitations.
Answer:
Land scarcity presents a constraint for large-scale solar farms.
        """.trimIndent()

        val previews = parser.parseContent(shorthandText)
        assertEquals(1, previews.size)
        val lesson = previews[0].lesson
        assertEquals("Renewable Energy in Bangladesh", lesson.title)
        assertEquals("Assessing green energy transition.", lesson.description)
        assertEquals(2, lesson.sections.size)

        assertEquals("Present Energy Matrix", lesson.sections[0].title)
        assertTrue(lesson.sections[0].modelAnswer.contains("Bangladesh currently relies"))
        assertEquals(1, lesson.sections[0].guide.vocabulary.size)
        assertEquals("Energy transition", lesson.sections[0].guide.vocabulary[0].term)
        assertEquals("জ্বালানি রূপান্তর", lesson.sections[0].guide.vocabulary[0].meaningBn)

        assertEquals("Solar and Wind Potential", lesson.sections[1].title)
        assertTrue(lesson.sections[1].modelAnswer.contains("Land scarcity presents"))
    }

    @Test
    fun testEscapingEconomicTrapFormatAndCleanSectionTitles() {
        val raw = """
${'$'}Focus Writing, @Escaping the Economic Trap: An Analysis of Bangladesh's Slowdown, Inflation Crisis, and Governance Reform Imperatives
-description: ১. সূচনা: প্রথমে মূল সমস্যাটি উল্লেখ করুন। ২. মূল অংশ: খেলাপি ঋণ নিয়ে আলোচনা করুন।
-Guidelines:
* Bangladesh growth projection (FY27): 3.2%
* Average inflation projected: 10.9%

Section1: Executive Summary
Guide: 
Answer: The selected source provides a comprehensive diagnostic of Bangladesh's macroeconomic crisis.

Section1: Key Areas of Analysis
Guide: 
Answer: Grim Growth Projections and Inflation Trends across South Asia.

Section1: 2. Root Causes: Policy Mistakes vs. Institutional Failures
Guide: 
Answer: Domestic policy flaws and governance indicators ranking.

Section1: 3. Financial Sector Distress and Banking Weaknesses
Guide: 
Answer: High non-performing loans in banking sector.

Section1: 4. Strategic Roadmaps and Policy Recommendations
Guide: 
Answer: Shift to inflation control and targeted digital safety nets.
        """.trimIndent()

        val previews = parser.parseContent(raw)
        assertEquals(1, previews.size)
        val lesson = previews[0].lesson
        assertEquals("Escaping the Economic Trap: An Analysis of Bangladesh's Slowdown, Inflation Crisis, and Governance Reform Imperatives", lesson.title)
        assertEquals("Focus Writing", lesson.type)
        assertEquals(5, lesson.sections.size)

        // Strict validation: Section titles must NOT contain Section1, Section 1, or numbers
        assertEquals("Executive Summary", lesson.sections[0].title)
        assertTrue(lesson.sections[0].modelAnswer.contains("comprehensive diagnostic"))

        assertEquals("Key Areas of Analysis", lesson.sections[1].title)
        assertTrue(lesson.sections[1].modelAnswer.contains("Grim Growth Projections"))

        assertEquals("Root Causes: Policy Mistakes vs. Institutional Failures", lesson.sections[2].title)
        assertTrue(lesson.sections[2].modelAnswer.contains("Domestic policy flaws"))

        assertEquals("Financial Sector Distress and Banking Weaknesses", lesson.sections[3].title)
        assertTrue(lesson.sections[3].modelAnswer.contains("High non-performing loans"))

        assertEquals("Strategic Roadmaps and Policy Recommendations", lesson.sections[4].title)
        assertTrue(lesson.sections[4].modelAnswer.contains("Shift to inflation control"))
    }

    @Test
    fun testTwoArticlesInOneTabGoogleDocFormat() {
        val raw = """
@Export Diversification of Bangladesh: Challenges, Opportunities and the Path to a Competitive Economy
[Central argument: Bangladesh has achieved remarkable export growth through the Ready-Made Garments industry.]

1. Introduction: Why Export Diversification Matters
[Key idea: Export diversification reduces vulnerability to external shocks.]
Exports are a major source of foreign exchange.

2. Latest Export Performance
[Latest data: FY2025-26 merchandise exports.]
Growth without sufficient diversification.

@জেরেমি বেন্থাম: জীবন, চিন্তাধারা, কর্ম ও আধুনিক সমাজে তাঁর অবদান
[এই আর্টিকেলের উদ্দেশ্য হলো Jeremy Bentham-এর সঙ্গে পরিচয়।]

১. জেরেমি বেন্থাম কে ছিলেন?
[জন্ম: ১৫ ফেব্রুয়ারি ১৭৪৮]
বেন্থাম ছিলেন একজন প্রভাবশালী চিন্তাবিদ।

২. শৈশব, শিক্ষা ও বুদ্ধিবৃত্তিক বিকাশ
[পিতা জেরেমিয়া বেন্থাম ছিলেন আইনজীবী]
আইনব্যবস্থা নিয়ে তাঁর বিশেষ উপলব্ধি তৈরি হয়।
        """.trimIndent()

        val previews = parser.parseContent(raw)
        assertEquals(2, previews.size)
        assertTrue(previews[0].lesson.title.contains("Export Diversification of Bangladesh"))
        assertEquals(2, previews[0].lesson.sections.size)
        assertEquals("Introduction: Why Export Diversification Matters", previews[0].lesson.sections[0].title)

        assertTrue(previews[1].lesson.title.contains("জেরেমি বেন্থাম"))
        assertEquals(2, previews[1].lesson.sections.size)
        assertEquals("জেরেমি বেন্থাম কে ছিলেন?", previews[1].lesson.sections[0].title)
    }

    @Test
    fun testNewspaperArticleSectionExtractionAndFormatting() {
        val newspaperText = """
@মূল্যস্ফীতি নিয়ন্ত্রণ ও মুদ্রানীতির কার্যকারিতা
[কেন্দ্রীয় যুক্তি: সমসাময়িক অর্থনীতিতে মূল্যস্ফীতি রোধে কার্যকর মুদ্রানীতির বাস্তবায়ন জরুরি।]

১. মূল্যস্ফীতির বর্তমান রূপ
[মূল তথ্য: বাংলাদেশ পরিসংখ্যান ব্যুরোর তথ্য অনুযায়ী খাদ্য ও খাদ্যবহির্ভূত মূল্যস্ফীতি উচ্চস্তরে রয়েছে।]
দেশের অর্থনীতিতে সামগ্রিক মূল্যস্ফীতি দীর্ঘ সময় ধরে **৯ শতাংশের উপরে** অবস্থান করছে। 
- খাদ্য মূল্যস্ফীতি: ১০% ছাড়িয়েছে
- খাদ্যবহির্ভূত মূল্যস্ফীতি: ৯.২%

| সূচক | ২০২৩ | ২০২৪ |
| খাদ্য | ৮.৫% | ১০.২% |
| খাদ্যবহির্ভূত | ৮.০% | ৯.২% |

২. কেন্দ্রীয় ব্যাংকের নীতিগত পদক্ষেপ
[প্রধান লক্ষ্য: সংকোচনমূলক মুদ্রানীতি এবং সুদের হারের সমন্বয়।]
বাংলাদেশ ব্যাংক নীতি সুদহার বৃদ্ধি করে মূল্যস্ফীতি নিয়ন্ত্রণে আনার চেষ্টা চালাচ্ছে।
* রেপো রেট বৃদ্ধি
* ব্যাংক ঋণ সুদের বাজারভিত্তিক নির্ধারণ
        """.trimIndent()

        val previews = parser.parseContent(newspaperText)
        assertEquals(1, previews.size)
        val lesson = previews[0].lesson
        assertEquals("মূল্যস্ফীতি নিয়ন্ত্রণ ও মুদ্রানীতির কার্যকারিতা", lesson.title)
        assertEquals(2, lesson.sections.size)

        val sec1 = lesson.sections[0]
        assertEquals("মূল্যস্ফীতির বর্তমান রূপ", sec1.title)
        assertTrue(sec1.guide.paragraphs.any { it.contains("পরিসংখ্যান ব্যুরোর তথ্য") })
        assertTrue(sec1.modelAnswer.contains("**৯ শতাংশের উপরে**"))
        assertTrue(sec1.modelAnswer.contains("| সূচক |"))

        val sec2 = lesson.sections[1]
        assertEquals("কেন্দ্রীয় ব্যাংকের নীতিগত পদক্ষেপ", sec2.title)
        assertTrue(sec2.guide.paragraphs.any { it.contains("সংকোচনমূলক মুদ্রানীতি") })
        assertTrue(sec2.modelAnswer.contains("বাংলাদেশ ব্যাংক নীতি সুদহার বৃদ্ধি"))
    }

    @Test
    fun testAutoFormatNewspaperTextWithSubheadings() {
        val rawText = """
ডলার সংকট ও রেমিট্যান্স প্রবাহ
মো. কামরুল হাসান

দেশের বৈদেশিক মুদ্রার রিজার্ভ দীর্ঘদিন ধরে চাপের মুখে রয়েছে। আমদানি দায় মেটাতে গিয়ে প্রতিনিয়ত ডলার বিক্রি করতে হচ্ছে।

মূল্যস্ফীতি ও ডলারের বিনিময় হার
মুদ্রার বিনিময় হার বাজারভিত্তিক না করার কারণে সমস্যা বেড়েছে। খোলা বাজারে ডলারের দাম অনেক বেড়ে যাওয়ায় আমদানিকৃত পণ্যের ব্যয় বেড়েছে।

হুন্ডি প্রতিরোধ ও প্রণোদনা
প্রবাসী আয় বৈধ পথে আনার জন্য সরকার আড়াই শতাংশ প্রণোদনা দিচ্ছে। তবে খোলা বাজার ও ব্যাংকিং চ্যানেলের মধ্যে ডলারের দামের ব্যবধান থাকায় অনেকে এখনো হুন্ডির আশ্রয় নিচ্ছে।
        """.trimIndent()

        val structured = parser.autoFormatNewspaperText(rawText)
        assertTrue(structured.contains("১. মূল্যস্ফীতি ও ডলারের বিনিময় হার") || structured.contains("১."))
        assertTrue(structured.contains("[মূল ভাবনা:") || structured.contains("[ভূমিকা"))

        val previews = parser.parseContent(structured)
        assertEquals(1, previews.size)
        val lesson = previews[0].lesson
        assertEquals("ডলার সংকট ও রেমিট্যান্স প্রবাহ", lesson.title)
        assertTrue(lesson.sections.size >= 2)
    }

    @Test
    fun testAutoFormatNewspaperTextWithoutSubheadings() {
        val rawText = """
ব্যাংক খাতের সুশাসন ও অর্থনৈতিক প্রবৃদ্ধি

দেশের ব্যাংক খাতে সুশাসন প্রতিষ্ঠা করা এখন সময়ের সবচেয়ে বড় দাবি। অনিয়ম ও দুর্নীতির কারণে খেলাপি ঋণের পরিমাণ দিন দিন বাড়ছে।

ঋণ আদায়ের ক্ষেত্রে কার্যকর আইনি পদক্ষেপ গ্রহণ করা প্রয়োজন। এছাড়া বিশেষ ট্রাইব্যুনাল গঠন করে দ্রুত মামলার নিষ্পত্তি করতে হবে।

বিনিয়োগকারীদের আস্থা ফিরিয়ে আনতে কেন্দ্রীয় ব্যাংকের স্বায়ত্বশাসন ও শক্তিশালী তদারকি অত্যন্ত গুরুত্বপূর্ণ।
        """.trimIndent()

        val structured = parser.autoFormatNewspaperText(rawText)
        val previews = parser.parseContent(structured)
        assertEquals(1, previews.size)
        val lesson = previews[0].lesson
        assertEquals("ব্যাংক খাতের সুশাসন ও অর্থনৈতিক প্রবৃদ্ধি", lesson.title)
        assertTrue(lesson.sections.isNotEmpty())
        assertTrue(lesson.sections[0].guide.paragraphs.isNotEmpty())
    }

    @Test
    fun testHtmlArticleExtraction() {
        val sampleHtml = """
<html>
<head>
<title>বাজেট ২০২৪-২৫: মূল্যস্ফীতি নিয়ন্ত্রণই প্রধান চ্যালেঞ্জ | প্রথম আলো</title>
<meta property="og:title" content="বাজেট ২০২৪-২৫: মূল্যস্ফীতি নিয়ন্ত্রণই প্রধান চ্যালেঞ্জ | প্রথম আলো">
</head>
<body>
<nav>মেনু লিঙ্ক</nav>
<article>
<h1>বাজেট ২০২৪-২৫: মূল্যস্ফীতি নিয়ন্ত্রণই প্রধান চ্যালেঞ্জ</h1>
<p>সদ্য ঘোষিত বাজেট বাস্তবায়ন করতে হলে সামষ্টিক অর্থনৈতিক স্থিতিশীলতা রক্ষা করতে হবে এবং সাধারণ মানুষের স্বস্তি নিশ্চিত করতে হবে।</p>
<h2>নিত্যপণ্যের মূল্যবৃদ্ধি ও জনজীবন</h2>
<p>খাদ্য মূল্যস্ফীতি ১০ শতাংশের ওপরে চলে যাওয়ায় সাধারণ মানুষের জীবনে মারাত্মক সংকট তৈরি হয়েছে এবং পরিবারের ব্যয় বেড়েছে।</p>
<h2>মুদ্রানীতির ভূমিকা ও করণীয়</h2>
<p>বাংলাদেশ ব্যাংক নীতি সুদহার বৃদ্ধি করে সংকোচনমূলক নীতি গ্রহণ করে বাজার নিয়ন্ত্রণে আনার চেষ্টা চালাচ্ছে।</p>
</article>
<footer>কপিরাইট ২০২৪</footer>
</body>
</html>
        """.trimIndent()

        val extracted = parser.extractArticleFromHtml(sampleHtml, "https://prothomalo.com/article/123")
        assertTrue(extracted.contains("বাজেট ২০২৪-২৫: মূল্যস্ফীতি নিয়ন্ত্রণই প্রধান চ্যালেঞ্জ"))
        assertTrue(extracted.contains("নিত্যপণ্যের মূল্যবৃদ্ধি ও জনজীবন"))

        val structured = parser.autoFormatNewspaperText(extracted)
        val previews = parser.parseContent(structured)
        assertEquals(1, previews.size)
        val lesson = previews[0].lesson
        assertEquals("বাজেট ২০২৪-২৫: মূল্যস্ফীতি নিয়ন্ত্রণই প্রধান চ্যালেঞ্জ", lesson.title)
        assertEquals(2, lesson.sections.size)
        assertEquals("নিত্যপণ্যের মূল্যবৃদ্ধি ও জনজীবন", lesson.sections[0].title)
        assertEquals("মুদ্রানীতির ভূমিকা ও করণীয়", lesson.sections[1].title)
    }
}

