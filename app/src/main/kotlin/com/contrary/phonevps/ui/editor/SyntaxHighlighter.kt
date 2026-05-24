package com.contrary.phonevps.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.contrary.phonevps.ui.theme.*

/**
 * Regex-based Python syntax highlighter.
 * Returns an AnnotatedString with color spans applied.
 * No external library — pure Compose.
 */
object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await",
        "break", "class", "continue", "def", "del", "elif", "else", "except",
        "finally", "for", "from", "global", "if", "import", "in", "is",
        "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
        "try", "while", "with", "yield",
    )

    private val BUILTINS = setOf(
        "print", "len", "range", "type", "int", "str", "float", "bool",
        "list", "dict", "set", "tuple", "input", "open", "super",
        "enumerate", "zip", "map", "filter", "sorted", "reversed",
        "hasattr", "getattr", "setattr", "isinstance", "issubclass",
        "abs", "max", "min", "sum", "round", "chr", "ord", "hex", "bin", "oct",
    )

    private val DECORATORS = Regex("""@\w+""")
    private val SINGLE_LINE_COMMENT = Regex("""#.*""")
    private val STRING_DQ = Regex(""""(?:[^"\\]|\\.)*"""")
    private val STRING_SQ = Regex("""'(?:[^'\\]|\\.)*'""")
    private val TRIPLE_DQ = Regex("""\"\"\"[\s\S]*?\"\"\"""")
    private val TRIPLE_SQ = Regex("""\'\'\'[\s\S]*?\'\'\'""")
    private val NUMBER = Regex("""\b(?:0x[0-9a-fA-F]+|0o[0-7]+|0b[01]+|\d+\.?\d*(?:[eE][+-]?\d+)?)\b""")
    private val WORD = Regex("""\b\w+\b""")
    private val FUNC_DEF = Regex("""(?<=def\s)\w+""")
    private val CLASS_DEF = Regex("""(?<=class\s)\w+""")

    private val keywordColor = Color(0xFF569CD6)      // blue
    private val stringColor = Color(0xFFCE9178)       // orange-brown
    private val commentColor = Color(0xFF6A9955)      // green
    private val numberColor = Color(0xFFB5CEA8)       // light green
    private val builtinColor = Color(0xFFDCDCAA)      // yellow
    private val decoratorColor = Color(0xFFD7BA7D)    // gold
    private val funcNameColor = Color(0xFFDCDCAA)     // yellow
    private val classNameColor = Color(0xFF4EC9B0)    // teal

    fun highlight(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            fun applyStyle(color: Color, start: Int, end: Int, bold: Boolean = false, italic: Boolean = false) {
                addStyle(
                    SpanStyle(
                        color = color,
                        fontWeight = if (bold) FontWeight.SemiBold else null,
                        fontStyle = if (italic) FontStyle.Italic else null,
                    ),
                    start, end
                )
            }

            // Triple-quoted strings first (before single)
            for (m in TRIPLE_DQ.findAll(code)) applyStyle(stringColor, m.range.first, m.range.last + 1)
            for (m in TRIPLE_SQ.findAll(code)) applyStyle(stringColor, m.range.first, m.range.last + 1)

            // Single-line strings
            for (m in STRING_DQ.findAll(code)) applyStyle(stringColor, m.range.first, m.range.last + 1)
            for (m in STRING_SQ.findAll(code)) applyStyle(stringColor, m.range.first, m.range.last + 1)

            // Comments — override strings inside comments
            for (m in SINGLE_LINE_COMMENT.findAll(code)) applyStyle(commentColor, m.range.first, m.range.last + 1, italic = true)

            // Numbers
            for (m in NUMBER.findAll(code)) applyStyle(numberColor, m.range.first, m.range.last + 1)

            // Keywords + builtins
            for (m in WORD.findAll(code)) {
                val word = m.value
                when {
                    word in KEYWORDS -> applyStyle(keywordColor, m.range.first, m.range.last + 1, bold = true)
                    word in BUILTINS -> applyStyle(builtinColor, m.range.first, m.range.last + 1)
                }
            }

            // Function / class definitions
            for (m in FUNC_DEF.findAll(code)) applyStyle(funcNameColor, m.range.first, m.range.last + 1, bold = true)
            for (m in CLASS_DEF.findAll(code)) applyStyle(classNameColor, m.range.first, m.range.last + 1, bold = true)

            // Decorators
            for (m in DECORATORS.findAll(code)) applyStyle(decoratorColor, m.range.first, m.range.last + 1)
        }
    }
}
