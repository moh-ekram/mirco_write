package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.regex.Pattern

object LatexParser {

    private val GREEK_MAP = mapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\Delta" to "Δ", "\\epsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η",
        "\\theta" to "θ", "\\Theta" to "Θ", "\\iota" to "ι", "\\kappa" to "κ",
        "\\lambda" to "λ", "\\Lambda" to "Λ", "\\mu" to "μ", "\\nu" to "ν",
        "\\xi" to "ξ", "\\Xi" to "Ξ", "\\pi" to "π", "\\Pi" to "Π",
        "\\rho" to "ρ", "\\sigma" to "σ", "\\Sigma" to "Σ", "\\tau" to "τ",
        "\\upsilon" to "υ", "\\phi" to "φ", "\\Phi" to "Φ", "\\chi" to "χ",
        "\\psi" to "ψ", "\\Psi" to "Ψ", "\\omega" to "ω", "\\Omega" to "Ω"
    )

    private val SYMBOL_MAP = mapOf(
        "\\times" to " × ", "\\cdot" to " · ", "\\div" to " ÷ ", "\\pm" to " ± ", "\\mp" to " ∓ ",
        "\\leq" to " ≤ ", "\\le" to " ≤ ", "\\geq" to " ≥ ", "\\ge" to " ≥ ",
        "\\neq" to " ≠ ", "\\ne" to " ≠ ", "\\approx" to " ≈ ", "\\equiv" to " ≡ ",
        "\\infty" to "∞", "\\partial" to "∂", "\\nabla" to "∇",
        "\\forall" to "∀", "\\exists" to "∃", "\\in" to " ∈ ", "\\notin" to " ∉ ",
        "\\subset" to " ⊂ ", "\\subseteq" to " ⊆ ", "\\cup" to " ∪ ", "\\cap" to " ∩ ",
        "\\to" to " → ", "\\rightarrow" to " → ", "\\leftarrow" to " ← ",
        "\\Rightarrow" to " ⇒ ", "\\implies" to " ⇒ ", "\\Leftarrow" to " ⇐ ",
        "\\Leftrightarrow" to " ⇔ ", "\\iff" to " ⇔ ",
        "\\sum" to "∑", "\\prod" to "∏", "\\int" to "∫", "\\iint" to "∬", "\\iiint" to "∭",
        "\\degree" to "°", "\\circ" to "°",
        "\\mathbb{R}" to "ℝ", "\\mathbb{C}" to "ℂ", "\\mathbb{N}" to "ℕ", "\\mathbb{Z}" to "ℤ", "\\mathbb{Q}" to "ℚ",
        "\\sin" to "sin", "\\cos" to "cos", "\\tan" to "tan",
        "\\log" to "log", "\\ln" to "ln", "\\exp" to "exp",
        "\\lim" to "lim", "\\det" to "det"
    )

    fun hasLatex(text: String): Boolean {
        return text.contains(Regex("""\$\$|\$|\\\(|\\\[|\\frac|\\sqrt|\\alpha|\\beta|\\sum|\\int|\\begin\{"""))
    }

    fun isBlockLatex(text: String): Boolean {
        val trimmed = text.trim()
        return (trimmed.startsWith("$$") && trimmed.endsWith("$$")) ||
                (trimmed.startsWith("\\[") && trimmed.endsWith("\\]")) ||
                trimmed.startsWith("\\begin{equation}") ||
                trimmed.startsWith("\\begin{align}") ||
                trimmed.startsWith("\\begin{matrix}") ||
                trimmed.startsWith("\\begin{pmatrix}") ||
                trimmed.startsWith("\\begin{bmatrix}")
    }

    fun cleanBlockLatex(text: String): String {
        return text.trim()
            .removePrefix("$$").removeSuffix("$$")
            .removePrefix("\\[").removeSuffix("\\]")
            .removePrefix("\\begin{equation}").removeSuffix("\\end{equation}")
            .removePrefix("\\begin{align}").removeSuffix("\\end{align}")
            .trim()
    }

    /**
     * Parses a LaTeX math string into a styled AnnotatedString
     */
    fun parseLatexToAnnotatedString(latexRaw: String, baseFontSize: TextUnit = 16.sp): AnnotatedString {
        var clean = cleanBlockLatex(latexRaw)

        // Parse matrices: \begin{matrix/pmatrix/bmatrix} ... \end{matrix/pmatrix/bmatrix}
        val matrixPattern = Pattern.compile("\\\\begin\\{(matrix|pmatrix|bmatrix|vmatrix)\\}([\\s\\S]*?)\\\\end\\{\\1\\}")
        var matrixMatcher = matrixPattern.matcher(clean)
        while (matrixMatcher.find()) {
            val type = matrixMatcher.group(1) ?: "matrix"
            val body = matrixMatcher.group(2) ?: ""
            val rows = body.split("\\\\").map { row ->
                row.split("&").map { it.trim() }
            }
            val openBracket = when (type) {
                "pmatrix" -> "("
                "bmatrix" -> "["
                "vmatrix" -> "|"
                else -> "["
            }
            val closeBracket = when (type) {
                "pmatrix" -> ")"
                "bmatrix" -> "]"
                "vmatrix" -> "|"
                else -> "]"
            }

            val matrixStr = rows.joinToString("\n") { row ->
                "$openBracket " + row.joinToString("  ") + " $closeBracket"
            }
            clean = clean.replace(matrixMatcher.group(0) ?: "", matrixStr)
            matrixMatcher = matrixPattern.matcher(clean)
        }

        // Replace basic symbol keywords
        for ((k, v) in GREEK_MAP) {
            clean = clean.replace(k, v)
        }
        for ((k, v) in SYMBOL_MAP) {
            clean = clean.replace(k, v)
        }

        // Simplify fractions with balanced braces: \frac{a}{b} -> (a / b)
        clean = replaceFractions(clean)

        // Simplify sqrt: \sqrt{x} -> √(x), \sqrt[n]{x} -> ⁿ√(x)
        clean = clean.replace(Regex("\\\\sqrt\\{([^}]+)\\}"), "√($1)")
        clean = clean.replace(Regex("\\\\sqrt\\[(\\d+)\\]\\{([^}]+)\\}"), "$1√($2)")

        // Text removal \text{...} -> ...
        clean = clean.replace(Regex("\\\\text\\{([^}]+)\\}"), "$1")
        clean = clean.replace(Regex("\\\\mathbf\\{([^}]+)\\}"), "$1")
        clean = clean.replace(Regex("\\\\mathit\\{([^}]+)\\}"), "$1")
        clean = clean.replace(Regex("\\\\mathrm\\{([^}]+)\\}"), "$1")

        // Handle LaTeX linebreaks \\
        clean = clean.replace("\\\\", "\n")

        // Build formatted AnnotatedString handling superscripts ^ and subscripts _
        return buildAnnotatedString {
            var i = 0
            while (i < clean.length) {
                when {
                    clean[i] == '^' -> {
                        i++
                        val content = if (i < clean.length && clean[i] == '{') {
                            val end = clean.indexOf('}', i)
                            if (end != -1) {
                                val s = clean.substring(i + 1, end)
                                i = end + 1
                                s
                            } else {
                                val s = clean.substring(i + 1)
                                i = clean.length
                                s
                            }
                        } else if (i < clean.length) {
                            val s = clean[i].toString()
                            i++
                            s
                        } else ""

                        withStyle(
                            SpanStyle(
                                baselineShift = BaselineShift.Superscript,
                                fontSize = if (baseFontSize != TextUnit.Unspecified) baseFontSize * 0.72f else 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(content)
                        }
                    }
                    clean[i] == '_' -> {
                        i++
                        val content = if (i < clean.length && clean[i] == '{') {
                            val end = clean.indexOf('}', i)
                            if (end != -1) {
                                val s = clean.substring(i + 1, end)
                                i = end + 1
                                s
                            } else {
                                val s = clean.substring(i + 1)
                                i = clean.length
                                s
                            }
                        } else if (i < clean.length) {
                            val s = clean[i].toString()
                            i++
                            s
                        } else ""

                        withStyle(
                            SpanStyle(
                                baselineShift = BaselineShift.Subscript,
                                fontSize = if (baseFontSize != TextUnit.Unspecified) baseFontSize * 0.72f else 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        ) {
                            append(content)
                        }
                    }
                    clean[i] in 'a'..'z' || clean[i] in 'A'..'Z' -> {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif)) {
                            append(clean[i])
                        }
                        i++
                    }
                    else -> {
                        append(clean[i])
                        i++
                    }
                }
            }
        }
    }

    private fun extractBalancedBraces(str: String, startIdx: Int): Pair<String, Int>? {
        val openIdx = str.indexOf('{', startIdx)
        if (openIdx == -1) return null
        var depth = 1
        var i = openIdx + 1
        while (i < str.length) {
            when (str[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return Pair(str.substring(openIdx + 1, i), i + 1)
                    }
                }
            }
            i++
        }
        return null
    }

    private fun replaceFractions(str: String): String {
        var result = str
        var iterations = 0
        while (iterations < 20) {
            iterations++
            val fracIdx = result.indexOf("\\frac")
            if (fracIdx == -1) break
            val numRes = extractBalancedBraces(result, fracIdx + 5) ?: break
            val denRes = extractBalancedBraces(result, numRes.second) ?: break

            val num = replaceFractions(numRes.first)
            val den = replaceFractions(denRes.first)
            val replacement = "($num / $den)"
            result = result.substring(0, fracIdx) + replacement + result.substring(denRes.second)
        }
        return result
    }
}

@Composable
fun LatexBlockView(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 17.sp
) {
    val clean = remember(latex) { LatexParser.cleanBlockLatex(latex) }
    val formatted = remember(latex, fontSize) {
        LatexParser.parseLatexToAnnotatedString(clean, fontSize)
    }
    var showRawCode by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LaTeX Math",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Toggle Raw TeX
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (showRawCode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showRawCode = !showRawCode }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "View Code",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showRawCode) "Hide TeX" else "Show TeX",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Copy TeX code
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(latex))
                            Toast.makeText(context, "LaTeX কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy LaTeX",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontally scrollable equation container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.5f,
                        fontFamily = FontFamily.Serif
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Raw LaTeX expandable drawer
            AnimatedVisibility(visible = showRawCode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Raw LaTeX Code:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = latex,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
