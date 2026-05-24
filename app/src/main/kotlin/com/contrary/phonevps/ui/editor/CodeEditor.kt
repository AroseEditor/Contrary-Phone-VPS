package com.contrary.phonevps.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contrary.phonevps.ui.theme.*

@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val lineCount = remember(value.text) { value.text.lines().size }

    val highlightedText = remember(value.text) {
        SyntaxHighlighter.highlight(value.text)
    }

    // Merge syntax highlighting with cursor/selection from TextFieldValue
    val displayValue = remember(highlightedText, value) {
        value.copy(annotatedString = highlightedText)
    }

    Row(
        modifier = modifier
            .background(TerminalBg)
    ) {
        // ── Line numbers ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
                .background(Color(0xFF161B22))
                .verticalScroll(verticalScrollState)
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Column {
                repeat(lineCount) { line ->
                    Text(
                        text = "${line + 1}",
                        color = Color(0xFF4A5568),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF21262D))
        )

        // ── Code area ────────────────────────────────────────────────
        BasicTextField(
            value = displayValue,
            onValueChange = { newVal ->
                // Strip syntax highlighting from the input event — we re-apply it
                onValueChange(newVal.copy(annotatedString = newVal.annotatedString))
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            textStyle = TextStyle(
                color = TerminalText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
            ),
            cursorBrush = SolidColor(NeonCyan),
            readOnly = readOnly,
            decorationBox = { innerTextField ->
                if (value.text.isEmpty()) {
                    Text(
                        text = "# Write your Discord bot here...\n# BOT_TOKEN is injected automatically\n\nimport discord\n\nclient = discord.Client(intents=discord.Intents.default())\n\n@client.event\nasync def on_ready():\n    print(f'Logged in as {client.user}')\n\nclient.run(BOT_TOKEN)",
                        color = Color(0xFF3A4A5A),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                    )
                }
                innerTextField()
            }
        )
    }
}
