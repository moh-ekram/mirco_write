package com.example.data.seed

import com.example.data.model.FinalReview
import com.example.data.model.Guidelines
import com.example.data.model.Lesson
import com.example.data.model.LessonSection
import com.example.data.model.SectionGuide
import com.example.data.model.TableData
import com.example.data.model.VocabularyItem

object SeedLessons {

    val nplLesson: Lesson = Lesson(
        id = "BD-BANK-NPL-001",
        title = "Resolution of Non-Performing Loans",
        titleBn = "খেলাপি ঋণ সমস্যার সমাধান ও শৃঙ্খলা",
        type = "Focus Writing",
        category = "Banking & Economics",
        description = "Learn about NPL dynamics in Bangladesh and practise structured focus writing with verified statistics, regulatory insights, and vocabulary.",
        estimatedMinutes = 20,
        guidelines = Guidelines(
            paragraphs = listOf(
                "Non-Performing Loans (NPLs) represent one of the most critical structural challenges confronting the banking system in developing economies, notably Bangladesh. Addressing NPLs requires rigorous focus writing that synthesizes financial data, institutional reforms, and legal mechanisms.",
                "In focus writing, avoid colloquial language and maintain an authoritative, objective tone. Always anchor your arguments with structural causes rather than superficial symptoms."
            ),
            bullets = listOf(
                "Maintain concise, factual arguments with relevant macroeconomic context.",
                "Highlight institutional frameworks such as the Artha Rin Adalat Ain (Money Loan Court Act) and Bankruptcy Act.",
                "Structure your focus writing into logical segments: Introduction, Causes, Consequences, and Reform Measures."
            ),
            tables = listOf(
                TableData(
                    headers = listOf("Financial Indicator", "Current Estimate", "Prudential Target"),
                    rows = listOf(
                        listOf("Gross NPL Ratio", "9.5% – 11.2%", "< 5.0%"),
                        listOf("Capital Adequacy Ratio (CAR)", "10.8%", "> 12.5%"),
                        listOf("Provision Coverage Shortfall", "BDT ~15,000 Cr", "Zero Shortfall"),
                        listOf("Adjudication Duration in Money Loan Courts", "4–7 Years", "< 1 Year")
                    )
                )
            )
        ),
        sections = listOf(
            LessonSection(
                id = "BD-BANK-NPL-001_sec_1",
                lessonId = "BD-BANK-NPL-001",
                order = 1,
                title = "Introduction to NPLs",
                type = "Writing",
                guide = SectionGuide(
                    paragraphs = listOf(
                        "An introduction in financial focus writing must define the core concept precisely, highlight the economic stakes, and outline the scope of your essay.",
                        "Under Bangladesh Bank prudential guidelines, loans are classified as Continuous, Demand, or Fixed Term loans, progressing through Substandard (SS), Doubtful (DF), and Bad/Loss (B/L) stages as delinquency deepens."
                    ),
                    bullets = listOf(
                        "Define NPLs clearly in the first 2 sentences.",
                        "Mention the macroeconomic ripple effects on liquidity and interest rate spreads.",
                        "Keep your word count strictly between 100 and 130 words."
                    ),
                    vocabulary = listOf(
                        VocabularyItem("Asset quality deterioration", "ঋণের গুণগত মানের অবনতি"),
                        VocabularyItem("Substandard loan", "নিম্নমান ঋণ (তিন মাস অনাদায়ী)"),
                        VocabularyItem("Doubtful loan", "সন্দেহজনক ঋণ (ছয় মাস অনাদায়ী)"),
                        VocabularyItem("Bad/Loss loan", "মন্দ বা ক্ষতিজনক ঋণ (নয় মাস বা তদূর্ধ্ব অনাদায়ী)")
                    )
                ),
                task = "Write an introduction in 100–130 words defining Non-Performing Loans and their current prevalence in the banking sector.",
                timerSeconds = 300,
                modelAnswer = "A Non-Performing Loan (NPL) is a credit facility wherein the borrower has defaulted on scheduled principal or interest payments for 90 days or longer. In modern financial architectures, high NPL volumes indicate severe asset quality deterioration and liquidity constriction. In Bangladesh, spiraling non-performing assets have persistently constrained commercial credit growth and inflated lending rates. The systemic accumulation of bad loans undermines capital adequacy, distorts fiscal discipline, and impedes productive investment across key sectors of the real economy. Consequently, understanding the roots and resolution of bad debt is essential for preserving monetary stability and sustaining economic trajectory."
            ),
            LessonSection(
                id = "BD-BANK-NPL-001_sec_2",
                lessonId = "BD-BANK-NPL-001",
                order = 2,
                title = "Key Drivers and Systemic Consequences",
                type = "Writing",
                guide = SectionGuide(
                    paragraphs = listOf(
                        "In this section, distinguish between systemic factors (e.g., regulatory forbearance, legal gridlock) and borrower/lender behavior (e.g., willful default, inflated collateral appraisals).",
                        "Explain how provision shortfalls force banks to divert operating profits into balance sheet buffers, leading to higher lending rates for legitimate businesses."
                    ),
                    bullets = listOf(
                        "Explore willful default vs genuine business distress.",
                        "Discuss the impact of serial rescheduling on credit discipline.",
                        "Analyze how higher risk premiums stifle small and medium enterprises (SMEs)."
                    ),
                    vocabulary = listOf(
                        VocabularyItem("Willful defaulter", "ইচ্ছাকৃত ঋণখেলাপি"),
                        VocabularyItem("Collateral overvaluation", "জামানতের কৃত্রিম অতিরিক্ত মূল্যায়ন"),
                        VocabularyItem("Moral hazard", "নৈতিক অবক্ষয়জনিত ঝুঁকি"),
                        VocabularyItem("Regulatory forbearance", "নিয়ামক বা তদারকি সংস্থার অযাচিত শিথিলতা")
                    )
                ),
                task = "Write two connected paragraphs (140–180 words) examining the major causes of bad loans and their consequences on business borrowing.",
                timerSeconds = 420,
                modelAnswer = "The proliferation of NPLs stems from a confluence of flawed credit appraisal, aggressive lending without adequate collateral verification, and pervasive moral hazard. A critical vulnerability is the prevalence of willful defaulters who capitalize on regulatory forbearance and protracted litigation to evade repayment obligations. Furthermore, repeated loan rescheduling without stringent down-payment compliance has institutionalized non-compliance and eroded credit culture.\n\nThe repercussions of this escalating delinquency are severe and far-reaching. As banks are compelled to allocate significant operating surpluses toward mandatory provisioning buffers, their net interest margins contract sharply. To offset these impairments, financial institutions elevate lending interest rates, thereby imposing an unwarranted risk premium on compliant borrowers. This credit squeeze disproportionately penalizes small and medium-sized enterprises, stifling innovation, depressing industrial capital expenditure, and exacerbating macroeconomic vulnerabilities."
            ),
            LessonSection(
                id = "BD-BANK-NPL-001_sec_3",
                lessonId = "BD-BANK-NPL-001",
                order = 3,
                title = "Judicial & Regulatory Reforms",
                type = "Writing",
                guide = SectionGuide(
                    paragraphs = listOf(
                        "Provide concrete, actionable solutions. Avoid vague generalities like 'everyone should be honest'. Focus on institutional instruments.",
                        "Mention Asset Reconstruction Companies (ARCs), tightening loan classification criteria to match Basel III standards, and introducing special commercial benches to expedite Money Loan Court appeals."
                    ),
                    bullets = listOf(
                        "Detail judicial acceleration through dedicated benches.",
                        "Explain the role of public credit registries and forensic auditing.",
                        "Conclude with a balanced perspective on sustainable banking governance."
                    ),
                    vocabulary = listOf(
                        VocabularyItem("Asset Reconstruction Company (ARC)", "সম্পদ পুনর্গঠন বা আদায়কারী কোম্পানি"),
                        VocabularyItem("Expeditious adjudication", "দ্রুত বিচার নিষ্পত্তি"),
                        VocabularyItem("Prudential oversight", "দূরদর্শী বা সুবিবেচক তদারকি"),
                        VocabularyItem("Corporate governance", "প্রাতিষ্ঠানিক সুশাসন ও স্বচ্ছতা")
                    )
                ),
                task = "Write recommendations and a conclusion in 120–150 words detailing actionable reforms to resolve bad debt permanently.",
                timerSeconds = 360,
                modelAnswer = "Overcoming the NPL deadlock necessitates uncompromising institutional discipline and comprehensive legal reform. Foremost, the Artha Rin Adalat Ain must be modernized with strict caps on interim writ stays, accompanied by dedicated commercial tribunals to expedite debt recovery. The central bank must enforce international classification norms without arbitrary forbearance, mandating forensic audits for high-value delinquent exposures. Concurrently, establishing an independent Asset Reconstruction Company would facilitate the securitization and liquidation of distressed collateral, revitalizing strained bank balances. Ultimately, safeguarding the banking apparatus requires insulating regulatory oversight from external interference and restoring corporate accountability across credit governance."
            )
        ),
        finalReview = FinalReview(
            items = listOf(
                "Check for smooth transitions between definition, analysis, and recommendations.",
                "Ensure word counts align with the section prompts without unnecessary repetition.",
                "Verify correct usage of specialized financial vocabulary and Bangla terms.",
                "Review against the model answer to identify missed nuances."
            )
        )
    )

    val automationLesson: Lesson = Lesson(
        id = "TECH-AI-AUTO-002",
        title = "Automation and the Future of Employment",
        titleBn = "স্বয়ংক্রিয়করণ ও ভবিষ্যৎ কর্মসংস্থান",
        type = "Argumentative Writing",
        category = "Technology & Society",
        description = "Practise structured argumentative writing balancing technological displacement against historical labor resilience and skill adaptation.",
        estimatedMinutes = 18,
        guidelines = Guidelines(
            paragraphs = listOf(
                "Argumentative writing requires taking a clear stance, developing persuasive arguments supported by evidence, anticipating counterarguments, and offering a nuanced synthesis.",
                "When discussing technological automation, avoid hyperbolic claims. Differentiate between task automation and whole-job obsolescence."
            ),
            bullets = listOf(
                "State your thesis statement clearly in the opening section.",
                "Dedicate distinct sections to supporting evidence and counterarguments.",
                "Synthesize opposing viewpoints in your conclusion."
            )
        ),
        sections = listOf(
            LessonSection(
                id = "TECH-AI-AUTO-002_sec_1",
                lessonId = "TECH-AI-AUTO-002",
                order = 1,
                title = "Introduction and Thesis Statement",
                type = "Writing",
                guide = SectionGuide(
                    paragraphs = listOf(
                        "Introduce the debate surrounding artificial intelligence and robotics replacing human labor. State your thesis clearly."
                    ),
                    bullets = listOf(
                        "Hook the reader with the rapid pace of generative AI and robotic automation.",
                        "Clearly state whether automation is primarily a threat or a transformative opportunity.",
                        "Target 100–120 words."
                    ),
                    vocabulary = listOf(
                        VocabularyItem("Technological unemployment", "প্রযুক্তিগত বেকারত্ব"),
                        VocabularyItem("Creative destruction", "সৃজনশীল ধ্বংসসাধন (অর্থনৈতিক রূপান্তর)"),
                        VocabularyItem("Labor market polarization", "শ্রমবাজারের দ্বিমুখী মেরুকরণ")
                    )
                ),
                task = "Write an introductory paragraph of 100–120 words presenting the debate and stating your clear argumentative thesis.",
                timerSeconds = 300,
                modelAnswer = "The rapid proliferation of artificial intelligence, robotic process automation, and machine learning has reignited fierce debate regarding the destiny of human labor. While alarmists predict unprecedented structural unemployment, historical economic cycles suggest technological disruption fundamentally reallocates rather than permanently diminishes aggregate employment. Automation undoubtedly renders routine cognitive and physical tasks redundant; nevertheless, it simultaneously engenders novel industries, enhances worker productivity, and fosters new categories of enterprise. This essay contends that while automation poses severe transitional frictions for vulnerable workforces, deliberate public investment in lifelong reskilling and adaptive safety nets will transform automation into a catalyst for human flourishing."
            ),
            LessonSection(
                id = "TECH-AI-AUTO-002_sec_2",
                lessonId = "TECH-AI-AUTO-002",
                order = 2,
                title = "Arguments in Favour of Displacement Concerns",
                type = "Writing",
                guide = SectionGuide(
                    paragraphs = listOf(
                        "Present the strongest arguments that automation threatens jobs: velocity of change, cognitive automation, and wage stagnation for low-skill labor."
                    ),
                    bullets = listOf(
                        "Discuss the difference between physical industrial robotics and cognitive AI.",
                        "Cite risk of middle-skill job hollowing.",
                        "Target 120–150 words."
                    ),
                    vocabulary = listOf(
                        VocabularyItem("Algorithmic displacement", "অ্যালগরিদমীয় কর্মী প্রতিস্থাপন"),
                        VocabularyItem("Skill obsolescence", "দক্ষতার অপ্রাসঙ্গিকতা"),
                        VocabularyItem("Precarity", "অনিশ্চয়তা ও দুর্বল সামাজিক সুরক্ষা")
                    )
                ),
                task = "Write 120–150 words developing the argument that automation disrupts job stability and exacerbates economic disparity.",
                timerSeconds = 360,
                modelAnswer = "Proponents of the displacement thesis emphasize that modern cognitive automation differs qualitatively from previous industrial revolutions. Past mechanization predominantly replaced muscle power while generating routine mental occupations. In sharp contrast, modern algorithms automate complex analytical, diagnostic, and creative workflows at negligible marginal cost. This shift threatens to hollow out middle-skill employment across accounting, logistics, legal research, and customer operations. Because algorithm efficiency outpaces the human capacity to retrain, displaced workers frequently transition into low-wage, precarious gig-economy roles. Without robust regulatory safeguards, capital owners reap disproportionate productivity dividends while labor shares of national income stagnate, widening socioeconomic inequality."
            ),
            LessonSection(
                id = "TECH-AI-AUTO-002_sec_3",
                lessonId = "TECH-AI-AUTO-002",
                order = 3,
                title = "Counterarguments and Synthesis",
                type = "Writing",
                guide = SectionGuide(
                    paragraphs = listOf(
                        "Demonstrate intellectual balance by examining job creation, productivity gains, and the imperative for active policy intervention."
                    ),
                    bullets = listOf(
                        "Discuss how higher productivity lowers consumer costs and expands disposable income.",
                        "Propose policy measures: lifelong learning, transition subsidies, STEM + humanities education.",
                        "Target 120–150 words."
                    ),
                    vocabulary = listOf(
                        VocabularyItem("Complementary technology", "পরিপূরক প্রযুক্তি (যা মানুষের উৎপাদনশীলতা বাড়ায়)"),
                        VocabularyItem("Human-in-the-loop", "সিদ্ধান্ত প্রক্রিয়ায় মানুষের প্রত্যক্ষ নিয়ন্ত্রণ"),
                        VocabularyItem("Adaptive pedagogy", "অভিযোজিত শিক্ষাপদ্ধতি")
                    )
                ),
                task = "Write 120–150 words evaluating counterarguments and synthesizing policy strategies for the future workforce.",
                timerSeconds = 360,
                modelAnswer = "Conversely, historical precedent demonstrates that technology frequently acts as a complement rather than a substitute. By automating repetitive drudgery, intelligent tools amplify human ingenuity, enabling professionals to focus on high-order synthesis, empathetic communication, and ethical judgment. Furthermore, lowered production costs stimulate consumer surplus, igniting demand for newly emergent sectors in renewable energy, cybersecurity, and personalized care. Harnessing this transition requires proactive governance. Governments and academic institutions must overhaul educational curricula toward critical reasoning and digital literacy, while establishing portable benefits for transitioning workers. When guided by visionary policy, automation enriches the human vocation."
            )
        ),
        finalReview = FinalReview(
            items = listOf(
                "Ensure both sides of the argument are handled with equal intellectual rigor.",
                "Check that the synthesis resolves the tension rather than merely ignoring the counterargument.",
                "Verify accurate word economy and formal academic phrasing."
            )
        )
    )
}
