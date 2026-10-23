package com.example.data.parser

import com.example.data.model.TableData

/**
 * Robust Table Parser designed to handle Google Docs tables in all their variations:
 * 1. Standard Markdown tables (with or without edge pipes: | A | B | or A | B)
 * 2. Google Docs Tab-delimited tables (from plain text export or copy-paste: A\tB\tC)
 * 3. ASCII Border tables (+---+---+ or |===|===|)
 * 4. Unicode Box Drawing tables (┌─┬┐, │ │, └─┴┘)
 * 5. Multi-space aligned tables
 */
object TableParser {

    /**
     * Determines whether a line is part of a table.
     */
    fun isTableLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false

        // Unicode box borders and vertical bars
        if (trimmed.contains('│') || trimmed.contains('┌') || trimmed.contains('├') || trimmed.contains('└')) {
            return true
        }

        // ASCII borders like +---+---+ or +===+===+
        if (trimmed.startsWith("+") && (trimmed.contains("-+") || trimmed.contains("=+")) && trimmed.endsWith("+")) {
            return true
        }

        // Markdown pipe lines starting or ending with |
        if (trimmed.startsWith("|") || trimmed.endsWith("|")) {
            return true
        }

        // Line with at least 1 pipe separator between words (e.g. "Col 1 | Col 2" or "Col 1 | Col 2 | Col 3")
        if (trimmed.count { it == '|' } >= 1 &&
            !trimmed.startsWith("- ") &&
            !trimmed.startsWith("* ") &&
            !trimmed.startsWith("• ") &&
            !trimmed.matches(Regex("^([০-৯]+|\\d+)[.)]\\s+.*"))
        ) {
            val parts = trimmed.split("|").map { it.trim() }
            if (parts.size >= 2 && parts.all { it.isNotEmpty() || trimmed.startsWith("|") || trimmed.endsWith("|") }) {
                return true
            }
        }

        // Tab-separated lines (Google Docs plain text export or clipboard paste)
        if (trimmed.contains('\t')) {
            val tabCells = trimmed.split('\t').map { it.trim() }
            if (tabCells.count { it.isNotEmpty() } >= 2) {
                return true
            }
        }

        // Markdown separator line: "--- | ---" or "| --- | --- |" or ":--- | :---:"
        if (trimmed.matches(Regex("^[\\-:\\s|+=]+$")) &&
            (trimmed.contains("-") || trimmed.contains("=")) &&
            (trimmed.contains("|") || trimmed.contains("+"))
        ) {
            return true
        }

        return false
    }

    /**
     * Checks if a line is a purely decorative horizontal separator.
     */
    fun isSeparatorLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false

        // Markdown separator: | --- | --- | or |:---|---:| or --- | ---
        if (trimmed.matches(Regex("^[|\\s\\-:+]+$")) && trimmed.contains("-")) {
            return true
        }
        // ASCII border: +---+---+ or +===+===+
        if (trimmed.startsWith("+") && trimmed.endsWith("+") && trimmed.matches(Regex("^\\+[\\-+=]+\\+$"))) {
            return true
        }
        // Unicode borders: ┌───┬───┐ or ├───┼───┤ or └───┴───┘
        if (trimmed.all { it in "┌┬┐├┼┤└┴┘─═│║ \t" }) {
            return true
        }
        return false
    }

    /**
     * Splits a single line into cell values according to its format.
     */
    fun extractCells(line: String): List<String> {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || isSeparatorLine(trimmed)) {
            return emptyList()
        }

        // 1. Tab separated (Google Docs export format & clipboard paste)
        if (trimmed.contains('\t')) {
            val cells = trimmed.split('\t').map { cleanCell(it) }
            val cleanCells = cells.toMutableList()
            while (cleanCells.isNotEmpty() && cleanCells.last().isEmpty()) {
                cleanCells.removeAt(cleanCells.size - 1)
            }
            while (cleanCells.isNotEmpty() && cleanCells.first().isEmpty()) {
                cleanCells.removeAt(0)
            }
            if (cleanCells.isNotEmpty()) return cleanCells
        }

        // 2. Unicode box border table: │ Col 1 │ Col 2 │
        if (trimmed.contains('│')) {
            val raw = trimmed.trim('│', ' ', '\t')
            return raw.split('│').map { cleanCell(it) }
        }

        // 3. Pipe separated table: | Col 1 | Col 2 | or Col 1 | Col 2
        if (trimmed.contains('|')) {
            var content = trimmed
            if (content.startsWith("|")) content = content.substring(1)
            if (content.endsWith("|")) content = content.substring(0, content.length - 1)
            return content.split('|').map { cleanCell(it) }
        }

        // 4. Multi-space separation (3 or more spaces between columns)
        val spaceParts = trimmed.split(Regex("\\s{3,}")).map { cleanCell(it) }
        if (spaceParts.size >= 2) {
            return spaceParts
        }

        return listOf(cleanCell(trimmed))
    }

    private fun cleanCell(cell: String): String {
        return cell.trim()
            .replace("\uFEFF", "")
            .replace("\u200B", "")
            .replace("\u00A0", " ")
            .replace("\\|", "|")
    }

    /**
     * Parses a group of raw table lines into a clean TableData object.
     */
    fun parseTable(lines: List<String>): TableData? {
        if (lines.isEmpty()) return null

        val rows = mutableListOf<List<String>>()
        for (line in lines) {
            if (isSeparatorLine(line)) continue
            val cells = extractCells(line)
            if (cells.isNotEmpty() && cells.any { it.isNotBlank() }) {
                rows.add(cells)
            }
        }

        if (rows.isEmpty()) return null

        // Determine maximum column count across all rows
        val maxCols = rows.maxOf { it.size }
        if (maxCols < 1) return null

        // First row is header row
        val rawHeaders = rows.first()
        val headers = (0 until maxCols).map { idx ->
            val h = rawHeaders.getOrNull(idx)?.trim() ?: ""
            if (h.isNotBlank()) h else "Column ${idx + 1}"
        }

        val contentRows = if (rows.size > 1) {
            rows.drop(1).map { row ->
                (0 until maxCols).map { idx ->
                    row.getOrNull(idx) ?: ""
                }
            }
        } else {
            emptyList()
        }

        return TableData(headers = headers, rows = contentRows)
    }

    /**
     * Converts raw text containing tab-delimited or non-standard tables into
     * standard markdown tables so that all parts of the app can consistently process them.
     */
    fun convertTextWithTablesToMarkdown(rawText: String): String {
        val lines = rawText.lines()
        val out = StringBuilder()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // Check if this line starts a table
            if (isTableLine(line)) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && (isTableLine(lines[i]) || (tableLines.isNotEmpty() && lines[i].trim().isEmpty() && i + 1 < lines.size && isTableLine(lines[i + 1])))) {
                    if (lines[i].trim().isNotEmpty()) {
                        tableLines.add(lines[i])
                    }
                    i++
                }

                val tableData = parseTable(tableLines)
                if (tableData != null && tableData.headers.isNotEmpty()) {
                    out.append("| ").append(tableData.headers.joinToString(" | ") { it.replace("|", "/") }).append(" |\n")
                    out.append("| ").append(tableData.headers.map { "---" }.joinToString(" | ")).append(" |\n")
                    for (row in tableData.rows) {
                        out.append("| ").append(row.joinToString(" | ") { it.replace("|", "/") }).append(" |\n")
                    }
                    out.append("\n")
                } else {
                    for (tl in tableLines) {
                        out.append(tl).append("\n")
                    }
                }
            } else {
                out.append(line).append("\n")
                i++
            }
        }

        return out.toString().trimEnd()
    }
}
