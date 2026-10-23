package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.example.data.model.TableData
import com.example.data.model.VocabularyItem
import com.example.ui.theme.BanglaFontFamily
import com.example.ui.theme.PoppinsFontFamily
import com.example.ui.theme.extendedColors

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.remember
import java.util.regex.Pattern

fun String.cleanDisplayTitle(): String {
    val cleaned = this
        .replace(Regex("^(?:SECTION\\s*\\d+|SECTION\\d+|PART\\s*\\d+)[:\\s-]*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^([০-৯]+|\\d+)[\\.\\)][:\\s-]*"), "")
        .replace(Regex("^#{1,4}\\s*"), "")
        .replace(Regex("^(?:SECTION\\s*TITLE|SECTION\\s*HEADING|TITLE|HEADING)[:\\s-]*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^START[:\\s]*", RegexOption.IGNORE_CASE), "")
        .trim(' ', ':', '-', '\t', '[', ']')
        .trim()
    return if (cleaned.isNotBlank() && !Regex("^(?:SECTION|PART)\\s*\\d*$", RegexOption.IGNORE_CASE).matches(cleaned)) {
        cleaned
    } else {
        "Focus Area"
    }
}

fun hasBengaliChars(text: String): Boolean {
    return text.any { it in '\u0980'..'\u09FF' }
}

fun parseMarkdownToAnnotatedString(
    text: String
): AnnotatedString {
    return buildAnnotatedString {
        val pattern = Pattern.compile("(\\*\\*\\*(.+?)\\*\\*\\*)|(\\*\\*(.+?)\\*\\*)|(\\*(.+?)\\*)|(__([^_]+?)__)|(_([^_]+?)_)|(`([^`]+?)`)")
        val matcher = pattern.matcher(text)
        var lastIdx = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > lastIdx) {
                append(text.substring(lastIdx, start))
            }

            when {
                matcher.group(1) != null -> {
                    val content = matcher.group(2) ?: ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }
                matcher.group(3) != null -> {
                    val content = matcher.group(4) ?: ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(content)
                    }
                }
                matcher.group(5) != null -> {
                    val content = matcher.group(6) ?: ""
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }
                matcher.group(7) != null -> {
                    val content = matcher.group(8) ?: ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(content)
                    }
                }
                matcher.group(9) != null -> {
                    val content = matcher.group(10) ?: ""
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }
                matcher.group(11) != null -> {
                    val content = matcher.group(12) ?: ""
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(content)
                    }
                }
            }

            lastIdx = end
        }

        if (lastIdx < text.length) {
            append(text.substring(lastIdx))
        }
    }
}

@Composable
fun BanglaAwareText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val isBangla = hasBengaliChars(text)
    val chosenFamily = if (isBangla) BanglaFontFamily else PoppinsFontFamily
    val lineHeight = if (isBangla && style.fontSize.isSpecified) style.fontSize * 1.55f else style.lineHeight

    val annotated = remember(text) {
        parseMarkdownToAnnotatedString(text)
    }

    Text(
        text = annotated,
        modifier = modifier,
        style = style.copy(
            fontFamily = chosenFamily,
            fontWeight = fontWeight ?: style.fontWeight,
            letterSpacing = if (isBangla) 0.sp else style.letterSpacing,
            lineHeight = lineHeight
        ),
        color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}

sealed class RichBlock {
    data class Paragraph(val text: String) : RichBlock()
    data class BulletItem(val text: String) : RichBlock()
    data class Table(val table: TableData) : RichBlock()
}

fun splitContentIntoRichBlocks(lines: List<String>): List<RichBlock> {
    val blocks = mutableListOf<RichBlock>()
    var currentParagraph = StringBuilder()
    var currentTableLines = mutableListOf<String>()

    fun flushParagraph() {
        val t = currentParagraph.toString().trim()
        if (t.isNotEmpty()) {
            blocks.add(RichBlock.Paragraph(t))
            currentParagraph.clear()
        }
    }

    fun flushTable() {
        if (currentTableLines.isNotEmpty()) {
            parseMarkdownTableToData(currentTableLines)?.let {
                blocks.add(RichBlock.Table(it))
            }
            currentTableLines.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            flushParagraph()
            flushTable()
            continue
        }

        if (com.example.data.parser.TableParser.isTableLine(trimmed)) {
            flushParagraph()
            currentTableLines.add(trimmed)
        } else if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ") || Regex("^([০-৯]+|\\d+)[.)]\\s+").containsMatchIn(trimmed)) {
            flushParagraph()
            flushTable()
            val bulletText = trimmed.replaceFirst(Regex("^[-*•]\\s+"), "")
                .replaceFirst(Regex("^([০-৯]+|\\d+)[.)]\\s+"), "")
                .trim()
            blocks.add(RichBlock.BulletItem(bulletText))
        } else {
            flushTable()
            if (currentParagraph.isNotEmpty()) currentParagraph.append("\n")
            currentParagraph.append(trimmed)
        }
    }

    flushParagraph()
    flushTable()
    return blocks
}

fun parseMarkdownTableToData(lines: List<String>): TableData? {
    return com.example.data.parser.TableParser.parseTable(lines)
}

@Composable
fun RichStudyContentView(
    content: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified
) {
    if (content.isBlank()) return

    val lines = remember(content) { content.lines() }
    val blocks = remember(lines) { splitContentIntoRichBlocks(lines) }

    val baseStyle = if (fontSize.isSpecified) {
        MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize, lineHeight = fontSize * 1.5f)
    } else {
        MaterialTheme.typography.bodyLarge
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is RichBlock.Paragraph -> {
                    BanglaAwareText(
                        text = block.text,
                        style = baseStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is RichBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = baseStyle,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        BanglaAwareText(
                            text = block.text,
                            style = baseStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is RichBlock.Table -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    LessonTableView(table = block.table)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun CircularProgressRing(
    progress: Float,
    size: Dp = 60.dp,
    strokeWidth: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(size),
            color = trackColor,
            strokeWidth = strokeWidth,
            strokeCap = StrokeCap.Round
        )
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = strokeWidth,
            strokeCap = StrokeCap.Round
        )
        content()
    }
}

@Composable
fun SegmentedSectionProgressBar(
    totalSections: Int,
    currentIndex: Int,
    completedIndices: Set<Int>,
    skippedIndices: Set<Int>,
    modifier: Modifier = Modifier
) {
    if (totalSections <= 0) return

    val extColors = MaterialTheme.extendedColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSections) {
            val barColor = when {
                i == currentIndex -> MaterialTheme.colorScheme.primary
                completedIndices.contains(i) -> extColors.success
                skippedIndices.contains(i) -> extColors.warning
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
fun LessonProgressIndicator(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (total == 0) 0f else (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val percentage = (fraction * 100).toInt()
    val extColors = MaterialTheme.extendedColors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "$completed/$total completed",
            style = MaterialTheme.typography.labelSmall,
            color = extColors.textSecondary,
            fontWeight = FontWeight.Medium
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun VocabularySection(
    items: List<VocabularyItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val extColors = MaterialTheme.extendedColors

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Vocabulary",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Key Vocabulary & Meanings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = item.term,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        BanglaAwareText(
                            text = item.meaningBn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        item.note?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = extColors.textMuted
                            )
                        }
                    }
                }
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun LessonTableView(
    table: TableData,
    modifier: Modifier = Modifier
) {
    if (table.headers.isEmpty()) return

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            Column {
                // Header row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 10.dp, horizontal = 14.dp)
                ) {
                    table.headers.forEach { header ->
                        BanglaAwareText(
                            text = header,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .width(160.dp)
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                // Data rows
                table.rows.forEachIndexed { index, row ->
                    val bgColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    Row(
                        modifier = Modifier
                            .background(bgColor)
                            .padding(vertical = 10.dp, horizontal = 14.dp)
                    ) {
                        row.forEachIndexed { colIdx, cell ->
                            BanglaAwareText(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .width(160.dp)
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
