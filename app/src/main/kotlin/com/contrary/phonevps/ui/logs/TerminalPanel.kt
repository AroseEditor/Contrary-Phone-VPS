package com.contrary.phonevps.ui.logs

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.contrary.phonevps.ui.theme.*
import com.contrary.phonevps.viewmodel.TerminalLineType
import com.contrary.phonevps.viewmodel.TerminalViewModel
import kotlinx.coroutines.launch

@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    botId: String? = null,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    terminalViewModel: TerminalViewModel = hiltViewModel(),
) {
    val lines by terminalViewModel.lines.collectAsState()
    val isExecuting by terminalViewModel.isExecuting.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var filterErrors by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty() && isExpanded) {
            coroutineScope.launch {
                listState.animateScrollToItem(lines.size - 1)
            }
        }
    }

    LaunchedEffect(botId) {
        terminalViewModel.setActiveBotId(botId)
    }

    val displayedLines = remember(lines, filterErrors, searchQuery) {
        lines
            .filter { line ->
                if (filterErrors) line.type == TerminalLineType.ERROR else true
            }
            .filter { line ->
                if (searchQuery.isBlank()) true
                else line.text.contains(searchQuery, ignoreCase = true)
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBg)
    ) {
        // ── Header ───────────────────────────────────────────────────
        TerminalHeader(
            isExpanded = isExpanded,
            isExecuting = isExecuting,
            filterErrors = filterErrors,
            showSearch = showSearch,
            onToggleExpand = onToggleExpand,
            onClear = { terminalViewModel.executeCommand("clear") },
            onCopyAll = {
                val text = lines.joinToString("\n") { it.text }
                clipboardManager.setText(AnnotatedString(text))
            },
            onFilterErrors = { filterErrors = !filterErrors },
            onToggleSearch = { showSearch = !showSearch; if (!showSearch) searchQuery = "" },
            onExport = { terminalViewModel.executeCommand("export") },
        )

        // ── Search bar ───────────────────────────────────────────────
        AnimatedVisibility(visible = showSearch && isExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text("Search logs...", color = TerminalGray, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberPurple,
                    unfocusedBorderColor = TerminalGray.copy(alpha = 0.3f),
                    focusedTextColor = TerminalText,
                    unfocusedTextColor = TerminalText,
                    cursorColor = CyberPurple,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                ),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TerminalGray, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, null, tint = TerminalGray, modifier = Modifier.size(14.dp))
                        }
                    }
                },
            )
        }

        // ── Log output ───────────────────────────────────────────────
        AnimatedVisibility(visible = isExpanded) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(displayedLines, key = { "${it.timestamp}_${it.text.hashCode()}" }) { line ->
                    TerminalLogLine(line = line)
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }

        // ── Command Input ────────────────────────────────────────────
        TerminalInputBar(
            value = inputText,
            isExecuting = isExecuting,
            onValueChange = { inputText = it },
            onSubmit = {
                val cmd = inputText.trim()
                if (cmd.isNotBlank()) {
                    terminalViewModel.executeCommand(cmd, botId)
                    inputText = ""
                }
            },
            onHistoryUp = {
                inputText = terminalViewModel.historyUp(inputText)
            },
            onHistoryDown = {
                inputText = terminalViewModel.historyDown()
            },
            focusRequester = focusRequester,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Terminal Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TerminalHeader(
    isExpanded: Boolean,
    isExecuting: Boolean,
    filterErrors: Boolean,
    showSearch: Boolean,
    onToggleExpand: () -> Unit,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
    onFilterErrors: () -> Unit,
    onToggleSearch: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CyberPurpleDark.copy(alpha = 0.8f),
                        CardSurface,
                    )
                )
            )
            .clickable { onToggleExpand() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Terminal icon + title
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "TERMINAL",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        )

        // Executing indicator
        AnimatedVisibility(visible = isExecuting) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    color = StatusYellow,
                    strokeWidth = 1.5.dp,
                )
                Spacer(Modifier.width(4.dp))
                Text("executing...", color = StatusYellow, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.weight(1f))

        // Actions
        val iconMod = Modifier.size(18.dp)
        val iconTint = TerminalGray

        IconButton(onClick = onToggleSearch, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Search, "Search", tint = if (showSearch) NeonCyan else iconTint, modifier = iconMod)
        }
        IconButton(onClick = onFilterErrors, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ErrorOutline, "Errors only", tint = if (filterErrors) StatusRed else iconTint, modifier = iconMod)
        }
        IconButton(onClick = onCopyAll, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ContentCopy, "Copy all", tint = iconTint, modifier = iconMod)
        }
        IconButton(onClick = onExport, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.FileDownload, "Export", tint = iconTint, modifier = iconMod)
        }
        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.DeleteSweep, "Clear", tint = iconTint, modifier = iconMod)
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = iconTint,
            modifier = iconMod,
        )
    }

    HorizontalDivider(color = CyberPurple.copy(alpha = 0.3f), thickness = 1.dp)
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual log line
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TerminalLogLine(line: com.contrary.phonevps.viewmodel.TerminalLine) {
    val (color, prefix) = when (line.type) {
        TerminalLineType.INPUT -> TerminalCyan to ""
        TerminalLineType.OUTPUT -> TerminalText to ""
        TerminalLineType.ERROR -> TerminalRed to ""
        TerminalLineType.SUCCESS -> TerminalGreen to ""
        TerminalLineType.SYSTEM -> TerminalBlue to ""
        TerminalLineType.PIP -> TerminalPurple to ""
        TerminalLineType.WARN -> TerminalYellow to ""
    }

    Text(
        text = "$prefix${line.text}",
        color = color,
        fontSize = 11.5.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp),
        softWrap = true,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Input bar — THE interactive terminal input
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TerminalInputBar(
    value: String,
    isExecuting: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    focusRequester: FocusRequester,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    HorizontalDivider(color = CyberPurple.copy(alpha = 0.4f), thickness = 1.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Prompt indicator
        Text(
            text = "❯",
            color = TerminalPrompt,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp),
        )

        // Input field
        BasicTerminalTextField(
            value = value,
            onValueChange = onValueChange,
            onSubmit = onSubmit,
            onHistoryUp = onHistoryUp,
            onHistoryDown = onHistoryDown,
            enabled = !isExecuting,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )

        // Run button
        AnimatedVisibility(visible = value.isNotBlank() && !isExecuting) {
            IconButton(
                onClick = onSubmit,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Run",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Executing spinner
        AnimatedVisibility(visible = isExecuting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = CyberPurple,
                strokeWidth = 2.dp,
            )
        }
    }

    // Quick command chips
    QuickCommandChips(
        onChip = { onValueChange(it) }
    )
}

@Composable
private fun BasicTerminalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onKeyEvent { event ->
                when {
                    event.key == Key.Enter && event.type == KeyEventType.KeyDown -> {
                        onSubmit()
                        true
                    }
                    event.key == Key.DirectionUp && event.type == KeyEventType.KeyDown -> {
                        onHistoryUp()
                        true
                    }
                    event.key == Key.DirectionDown && event.type == KeyEventType.KeyDown -> {
                        onHistoryDown()
                        true
                    }
                    else -> false
                }
            },
        textStyle = TextStyle(
            color = TerminalText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions(
            onSend = { onSubmit() }
        ),
        singleLine = true,
        enabled = enabled,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(NeonCyan),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "Type a command... (pip install, start, stop, exit, help)",
                        color = TerminalGray.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun QuickCommandChips(onChip: (String) -> Unit) {
    val quickCmds = listOf(
        "pip list" to Icons.Default.List,
        "pip install " to Icons.Default.Add,
        "status" to Icons.Default.Info,
        "logs 50" to Icons.Default.Article,
        "clear" to Icons.Default.DeleteSweep,
        "help" to Icons.Default.Help,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(TerminalBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        quickCmds.forEach { (cmd, icon) ->
            QuickChip(label = cmd.trim(), icon = icon, onClick = { onChip(cmd) })
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = CardSurface,
        border = BorderStroke(0.5.dp, CyberPurple.copy(alpha = 0.4f)),
        modifier = Modifier.height(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, tint = CyberPurple, modifier = Modifier.size(10.dp))
            Text(
                text = label,
                color = TerminalGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
