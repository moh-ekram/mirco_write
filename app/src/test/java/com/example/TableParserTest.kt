package com.example

import com.example.data.parser.ResourceParser
import com.example.data.parser.TableParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableParserTest {

    @Test
    fun testParseStandardMarkdownTable() {
        val markdown = listOf(
            "| খাত | বর্তমান অবস্থা | প্রস্তাবনা |",
            "| --- | --- | --- |",
            "| তৈরি পোশাক | রপ্তানি ৮২% | পণ্যের বৈচিত্র্যকরণ |",
            "| চামড়া | অনুন্নত ইটিপি | সাভারে আধুনিকায়ন |"
        )

        val table = TableParser.parseTable(markdown)
        assertNotNull(table)
        assertEquals(3, table!!.headers.size)
        assertEquals("খাত", table.headers[0])
        assertEquals("বর্তমান অবস্থা", table.headers[1])
        assertEquals("প্রস্তাবনা", table.headers[2])

        assertEquals(2, table.rows.size)
        assertEquals("তৈরি পোশাক", table.rows[0][0])
        assertEquals("রপ্তানি ৮২%", table.rows[0][1])
        assertEquals("পণ্যের বৈচিত্র্যকরণ", table.rows[0][2])
        assertEquals("চামড়া", table.rows[1][0])
    }

    @Test
    fun testParseGoogleDocsTabSeparatedTable() {
        // Google Docs plain text export and copy-paste formats use \t
        val gdocLines = listOf(
            "Category\tKey Drivers\tPolicy Measure",
            "Food Inflation\tImport supply bottlenecks\tTariff rationalization",
            "Non-Food Inflation\tCurrency depreciation\tInterest rate tightening"
        )

        val table = TableParser.parseTable(gdocLines)
        assertNotNull(table)
        assertEquals(3, table!!.headers.size)
        assertEquals("Category", table.headers[0])
        assertEquals("Key Drivers", table.headers[1])
        assertEquals("Policy Measure", table.headers[2])

        assertEquals(2, table.rows.size)
        assertEquals("Food Inflation", table.rows[0][0])
        assertEquals("Import supply bottlenecks", table.rows[0][1])
        assertEquals("Tariff rationalization", table.rows[0][2])
    }

    @Test
    fun testParsePipeTableWithoutEdgePipes() {
        val lines = listOf(
            "Parameter | Value | Remark",
            "--- | --- | ---",
            "CPI | 9.8% | Elevated",
            "Forex Reserve | $21B | Moderate"
        )

        val table = TableParser.parseTable(lines)
        assertNotNull(table)
        assertEquals(3, table!!.headers.size)
        assertEquals("Parameter", table.headers[0])
        assertEquals("CPI", table.rows[0][0])
        assertEquals("9.8%", table.rows[0][1])
    }

    @Test
    fun testParseAsciiBorderTable() {
        val lines = listOf(
            "+---------------+---------------+",
            "| Concept       | Definition    |",
            "+---------------+---------------+",
            "| Repo Rate     | Policy rate   |",
            "+---------------+---------------+"
        )

        val table = TableParser.parseTable(lines)
        assertNotNull(table)
        assertEquals(2, table!!.headers.size)
        assertEquals("Concept", table.headers[0])
        assertEquals("Definition", table.headers[1])
        assertEquals(1, table.rows.size)
        assertEquals("Repo Rate", table.rows[0][0])
        assertEquals("Policy rate", table.rows[0][1])
    }

    @Test
    fun testParseUnicodeBoxTable() {
        val lines = listOf(
            "┌───────────────┬───────────────┐",
            "│ Subject       │ Score         │",
            "├───────────────┼───────────────┤",
            "│ Economics     │ 92            │",
            "└───────────────┴───────────────┘"
        )

        val table = TableParser.parseTable(lines)
        assertNotNull(table)
        assertEquals(2, table!!.headers.size)
        assertEquals("Subject", table.headers[0])
        assertEquals("Score", table.headers[1])
        assertEquals(1, table.rows.size)
        assertEquals("Economics", table.rows[0][0])
        assertEquals("92", table.rows[0][1])
    }

    @Test
    fun testConvertTextWithTablesToMarkdown() {
        val rawDocText = """
            @Macroeconomic Stability
            [Overview: Analysis of macroeconomic challenges]
            
            1. Sectoral Analysis
            [Key sector data]
            Sector	Growth	Contribution
            Industry	7.5%	High
            Agriculture	3.8%	Stable
            
            Detailed answer text goes here.
        """.trimIndent()

        val converted = TableParser.convertTextWithTablesToMarkdown(rawDocText)
        assertTrue(converted.contains("| Sector | Growth | Contribution |"))
        assertTrue(converted.contains("| --- | --- | --- |"))
        assertTrue(converted.contains("| Industry | 7.5% | High |"))
    }

    @Test
    fun testResourceParserExtractsTableFromTabDelimitedGoogleDocText() {
        val docText = """
            @Economic Prospects of Bangladesh
            -Guidelines:
            General guidelines for analytical writing.
            
            1. Sector Performance Matrix
            Guide:
            Review the following sectoral matrix:
            Sector	Growth Rate	Employment Share
            RMG & Textile	8.2%	45%
            Pharmaceuticals	12.1%	10%
            Leather Goods	6.5%	8%
            
            Answer:
            Bangladesh has demonstrated resilient growth across secondary sectors.
        """.trimIndent()

        val parser = ResourceParser()
        val parsed = parser.parseContent(docText)

        assertEquals(1, parsed.size)
        val lesson = parsed[0].lesson
        assertEquals("Economic Prospects of Bangladesh", lesson.title)
        assertEquals(1, lesson.sections.size)

        val section = lesson.sections[0]
        val tables = section.guide.tables
        assertEquals(1, tables.size)
        val table = tables[0]
        assertEquals(3, table.headers.size)
        assertEquals("Sector", table.headers[0])
        assertEquals("Growth Rate", table.headers[1])
        assertEquals("Employment Share", table.headers[2])
        assertEquals(3, table.rows.size)
        assertEquals("RMG & Textile", table.rows[0][0])
        assertEquals("8.2%", table.rows[0][1])
        assertEquals("45%", table.rows[0][2])
    }
}
