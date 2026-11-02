package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
    text: String,
    baseFontSize: TextUnit = 16.sp
): AnnotatedString {
    return buildAnnotatedString {
        val pattern = Pattern.compile(
            "(\\*\\*\\*(.+?)\\*\\*\\*)|" +         // 1, 2: Bold Italic
            "(\\*\\*(.+?)\\*\\*)|" +             // 3, 4: Bold
            "(__([^_]+?)__)|" +                   // 5, 6: Bold
            "(_\\{([^}]+)\\})|" +                 // 7, 8: Subscript _{...}
            "(\\^\\{([^}]+)\\})|" +               // 9, 10: Superscript ^{...}
            "(<sub>([\\s\\S]+?)</sub>)|" +        // 11, 12: Subscript <sub>...</sub>
            "(<sup>([\\s\\S]+?)</sup>)|" +        // 13, 14: Superscript <sup>...</sup>
            "(\\*(.+?)\\*)|" +                   // 15, 16: Italic
            "(_([^_]+?)_)|" +                     // 17, 18: Italic
            "(`([^`]+?)`)|" +                     // 19, 20: Code
            "(\\$([^$]+?)\\$)|" +                 // 21, 22: Inline Math $...$
            "(\\\\\\(([^)]+?)\\\\\\))|" +         // 23, 24: Inline Math \(...\)
            "(<u>([\\s\\S]+?)</u>)|" +            // 25, 26: Underline
            "(<ins>([\\s\\S]+?)</ins>)|" +        // 27, 28: Underline
            "(\\+\\+([^+]+?)\\+\\+)|" +           // 29, 30: Underline
            "(<b>([\\s\\S]+?)</b>)|" +            // 31, 32: Bold
            "(<strong>([\\s\\S]+?)</strong>)|" +  // 33, 34: Bold
            "(<i>([\\s\\S]+?)</i>)|" +            // 35, 36: Italic
            "(<em>([\\s\\S]+?)</em>)|" +          // 37, 38: Italic
            "(~~([\\s\\S]+?)~~)"                  // 39, 40: Strikethrough
        )
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
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(content)
                    }
                }
                matcher.group(7) != null -> {
                    val subContent = matcher.group(8) ?: ""
                    val subSize = if (baseFontSize.isSpecified) baseFontSize * 0.75f else 11.sp
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = subSize)) {
                        append(subContent)
                    }
                }
                matcher.group(9) != null -> {
                    val supContent = matcher.group(10) ?: ""
                    val supSize = if (baseFontSize.isSpecified) baseFontSize * 0.75f else 11.sp
                    withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = supSize)) {
                        append(supContent)
                    }
                }
                matcher.group(11) != null -> {
                    val subContent = matcher.group(12) ?: ""
                    val subSize = if (baseFontSize.isSpecified) baseFontSize * 0.75f else 11.sp
                    withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = subSize)) {
                        append(subContent)
                    }
                }
                matcher.group(13) != null -> {
                    val supContent = matcher.group(14) ?: ""
                    val supSize = if (baseFontSize.isSpecified) baseFontSize * 0.75f else 11.sp
                    withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = supSize)) {
                        append(supContent)
                    }
                }
                matcher.group(15) != null -> {
                    val content = matcher.group(16) ?: ""
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }
                matcher.group(17) != null -> {
                    val content = matcher.group(18) ?: ""
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }
                matcher.group(19) != null -> {
                    val content = matcher.group(20) ?: ""
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(content)
                    }
                }
                matcher.group(21) != null -> {
                    val mathContent = matcher.group(22) ?: ""
                    append(LatexParser.parseLatexToAnnotatedString(mathContent, baseFontSize))
                }
                matcher.group(23) != null -> {
                    val mathContent = matcher.group(24) ?: ""
                    append(LatexParser.parseLatexToAnnotatedString(mathContent, baseFontSize))
                }
                matcher.group(25) != null || matcher.group(27) != null || matcher.group(29) != null -> {
                    val uContent = matcher.group(26) ?: matcher.group(28) ?: matcher.group(30) ?: ""
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(uContent)
                    }
                }
                matcher.group(31) != null || matcher.group(33) != null -> {
                    val bContent = matcher.group(32) ?: matcher.group(34) ?: ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(bContent)
                    }
                }
                matcher.group(35) != null || matcher.group(37) != null -> {
                    val iContent = matcher.group(36) ?: matcher.group(38) ?: ""
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(iContent)
                    }
                }
                matcher.group(39) != null -> {
                    val sContent = matcher.group(40) ?: ""
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(sContent)
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

    val annotated = remember(text, style.fontSize) {
        parseMarkdownToAnnotatedString(text, if (style.fontSize.isSpecified) style.fontSize else 16.sp)
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
    data class Subheading(val text: String, val level: Int = 3) : RichBlock()
    data class BulletItem(val text: String, val level: Int = 0, val prefix: String = "•") : RichBlock()
    data class Table(val table: TableData) : RichBlock()
    data class Latex(val latex: String) : RichBlock()
    data class Image(val url: String, val altText: String = "") : RichBlock()
}

fun splitContentIntoRichBlocks(lines: List<String>): List<RichBlock> {
    val blocks = mutableListOf<RichBlock>()
    var currentParagraph = StringBuilder()
    var currentTableLines = mutableListOf<String>()
    var currentLatexLines = mutableListOf<String>()
    var isInLatexBlock = false

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

    fun flushLatex() {
        if (currentLatexLines.isNotEmpty()) {
            val content = currentLatexLines.joinToString("\n").trim()
            if (content.isNotEmpty()) {
                blocks.add(RichBlock.Latex(content))
            }
            currentLatexLines.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()

        if (isInLatexBlock) {
            currentLatexLines.add(line)
            if (trimmed.endsWith("$$") || trimmed.endsWith("\\]") || trimmed.contains("\\end{equation}") || trimmed.contains("\\end{align}")) {
                isInLatexBlock = false
                flushLatex()
            }
            continue
        }

        if ((trimmed.startsWith("$$") && !trimmed.endsWith("$$")) || (trimmed.startsWith("\\[") && !trimmed.endsWith("\\]")) || trimmed.startsWith("\\begin{equation}") || trimmed.startsWith("\\begin{align}")) {
            flushParagraph()
            flushTable()
            isInLatexBlock = true
            currentLatexLines.add(line)
            continue
        }

        if (LatexParser.isBlockLatex(trimmed)) {
            flushParagraph()
            flushTable()
            blocks.add(RichBlock.Latex(trimmed))
            continue
        }

        // Detect Markdown image: ![alt](url)
        val imgMdMatch = Regex("^!\\[(.*?)\\]\\((.*?)\\)").find(trimmed)
        // Detect HTML image: <img ... src="..." ...>
        val imgHtmlMatch = Regex("<img[^>]+src=[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE).find(trimmed)

        if (imgMdMatch != null || imgHtmlMatch != null) {
            flushParagraph()
            flushTable()
            flushLatex()
            val alt = imgMdMatch?.groupValues?.get(1)?.trim() ?: ""
            val url = imgMdMatch?.groupValues?.get(2)?.trim() ?: (imgHtmlMatch?.groupValues?.get(1)?.trim() ?: "")
            if (url.isNotBlank()) {
                blocks.add(RichBlock.Image(url, alt))
            }
            continue
        }

        if (trimmed.isEmpty()) {
            flushParagraph()
            flushTable()
            continue
        }

        // Subheadings inside section: ###, ####, etc.
        val subHeadingMatch = Regex("^(#{3,6})\\s+(.+)$").find(trimmed)
        if (subHeadingMatch != null) {
            flushParagraph()
            flushTable()
            flushLatex()
            val hLevel = subHeadingMatch.groupValues[1].length
            val headingText = subHeadingMatch.groupValues[2].trim()
            blocks.add(RichBlock.Subheading(headingText, hLevel))
            continue
        }

        val indentCount = line.takeWhile { it == ' ' || it == '\t' }.let { s ->
            s.count { it == '\t' } * 2 + s.count { it == ' ' } / 2
        }
        val level = indentCount.coerceIn(0, 4)
        val numberListMatch = Regex("^([০-৯]+|\\d+|[a-zA-Z]|[ivxlcdmIVXLCDM]+)[.)]\\s+(.+)$").find(trimmed)
        val isBullet = trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ") || trimmed.startsWith("◦ ") || trimmed.startsWith("▪ ")

        if (com.example.data.parser.TableParser.isTableLine(trimmed)) {
            flushParagraph()
            currentTableLines.add(trimmed)
        } else if (numberListMatch != null) {
            flushParagraph()
            flushTable()
            val prefix = numberListMatch.groupValues[1] + "."
            val itemText = numberListMatch.groupValues[2].trim()
            blocks.add(RichBlock.BulletItem(itemText, level, prefix))
        } else if (isBullet) {
            flushParagraph()
            flushTable()
            val bulletChar = when (level) {
                0 -> "•"
                1 -> "◦"
                else -> "▪"
            }
            val itemText = trimmed.replaceFirst(Regex("^[-*•◦▪]\\s+"), "").trim()
            blocks.add(RichBlock.BulletItem(itemText, level, bulletChar))
        } else {
            flushTable()
            if (currentParagraph.isNotEmpty()) currentParagraph.append("\n")
            currentParagraph.append(trimmed)
        }
    }

    flushParagraph()
    flushTable()
    flushLatex()
    return blocks
}

fun parseMarkdownTableToData(lines: List<String>): TableData? {
    return com.example.data.parser.TableParser.parseTable(lines)
}

@Composable
fun ImageZoomDialog(
    imageUrl: String,
    altText: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            // Main Zoomable Image Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.7f, 6f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = altText.ifBlank { "Zoomed Image" },
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    },
                    error = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "Load error",
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (altText.isNotBlank()) altText else "ছবিটি লোড করা যায়নি",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                )
            }

            // Top bar with close and altText
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (altText.isNotBlank()) {
                    Text(
                        text = altText,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Bottom toolbar with Zoom Controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zoom Out
                IconButton(
                    onClick = { scale = (scale / 1.25f).coerceAtLeast(0.8f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Reset
                IconButton(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Zoom",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom In
                IconButton(
                    onClick = { scale = (scale * 1.25f).coerceAtMost(5f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
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
    var zoomImage by remember { mutableStateOf<RichBlock.Image?>(null) }

    zoomImage?.let { img ->
        ImageZoomDialog(
            imageUrl = img.url,
            altText = img.altText,
            onDismiss = { zoomImage = null }
        )
    }

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = (block.level * 18).dp,
                                top = 2.dp,
                                bottom = 2.dp
                            ),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.prefix,
                            style = baseStyle,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        BanglaAwareText(
                            text = block.text,
                            style = baseStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is RichBlock.Table -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    LessonTableView(table = block.table)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is RichBlock.Subheading -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    BanglaAwareText(
                        text = block.text,
                        style = if (block.level == 3) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                is RichBlock.Latex -> {
                    Spacer(modifier = Modifier.height(2.dp))
                    LatexBlockView(latex = block.latex, fontSize = if (fontSize.isSpecified) fontSize else 17.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                }
                is RichBlock.Image -> {
                    val isAltMath = remember(block.altText) {
                        block.altText.isNotBlank() && (
                            block.altText.startsWith("$$") || block.altText.startsWith("\\[") ||
                            block.altText.contains("\\frac") || block.altText.contains("_{") || block.altText.contains("^{") ||
                            block.altText.contains("\\sqrt") || block.altText.contains("\\sum") ||
                            (block.altText.contains("=") && (block.altText.contains("/") || block.altText.contains("_")))
                        )
                    }

                    if (isAltMath) {
                        Spacer(modifier = Modifier.height(2.dp))
                        LatexBlockView(latex = block.altText, fontSize = if (fontSize.isSpecified) fontSize else 17.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                    } else {
                        var hasError by remember(block.url) { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .then(if (!hasError) Modifier.clickable { zoomImage = block } else Modifier),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SubcomposeAsyncImage(
                                    model = block.url,
                                    contentDescription = block.altText.ifBlank { "Chart / Image" },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 340.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Fit,
                                    loading = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(160.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                                        }
                                    },
                                    error = {
                                        hasError = true
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BrokenImage,
                                                contentDescription = "Image load error",
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "ছবিটি লোড করা সম্ভব হয়নি",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                )
                                if (block.altText.isNotBlank() && !hasError && !block.altText.equals("Image", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = block.altText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (!hasError) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ট্যাপ করে বড় করুন 🔍",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
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
