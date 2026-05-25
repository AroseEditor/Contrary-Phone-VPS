package com.contrary.phonevps.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.contrary.phonevps.ui.logs.TerminalPanel
import com.contrary.phonevps.ui.theme.*
import kotlinx.coroutines.launch
import com.contrary.phonevps.viewmodel.BotViewModel
import com.contrary.phonevps.viewmodel.TerminalViewModel
import java.util.UUID

@Composable
fun BotEditorScreen(
    botId: String?,
    onBack: () -> Unit,
    botViewModel: BotViewModel = hiltViewModel(),
    terminalViewModel: TerminalViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var botName by remember { mutableStateOf("") }
    var botDescription by remember { mutableStateOf("") }
    var tokenText by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    var scriptPath by remember { mutableStateOf("") }
    var requirementsPath by remember { mutableStateOf("") }
    var codeValue by remember { mutableStateOf(TextFieldValue("")) }
    var autoRestart by remember { mutableStateOf(true) }
    var autoStart by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }
    val currentBotId = remember(botId) { botId ?: UUID.randomUUID().toString() }

    // Load data asynchronously from Room
    LaunchedEffect(botId) {
        if (botId != null) {
            val bot = botViewModel.getBotById(botId)
            if (bot != null) {
                botName = bot.name
                botDescription = bot.description
                scriptPath = bot.scriptPath
                requirementsPath = bot.requirementsPath
                codeValue = TextFieldValue(bot.scriptContent)
                autoRestart = bot.autoRestart
                autoStart = bot.autoStart
                tokenText = botViewModel.getToken(botId) ?: ""
            }
        }
    }

    val existingBot = remember(botId, botViewModel.uiState.collectAsState().value.bots) {
        botId?.let { botViewModel.uiState.value.bots.find { b -> b.id == it } }
    }

    // Unified Save Function — ensures code is NEVER lost
    val saveBot: suspend () -> Unit = {
        val id = currentBotId
        val finalScriptPath = scriptPath.ifBlank { "${context.filesDir.absolutePath}/bots/${id}.py" }
        if (existingBot != null) {
            botViewModel.updateBot(
                existingBot.copy(
                    name = botName.ifBlank { "Bot-${id.take(6)}" },
                    description = botDescription,
                    scriptPath = finalScriptPath,
                    scriptContent = codeValue.text,
                    requirementsPath = requirementsPath,
                    autoRestart = autoRestart,
                    autoStart = autoStart,
                ),
                newToken = if (tokenText.isNotBlank()) tokenText else null,
            )
        } else {
            botViewModel.createBot(
                id = id,
                name = botName.ifBlank { "Bot-${id.take(6)}" },
                description = botDescription,
                token = tokenText,
                scriptContent = codeValue.text,
                scriptPath = finalScriptPath,
            )
        }
    }

    // File pickers (always write/copy to standard filesystem path)
    val scriptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = "${context.filesDir.absolutePath}/bots/${currentBotId}.py"
            scriptPath = localPath
            try {
                val stream = context.contentResolver.openInputStream(it)
                val content = stream?.bufferedReader()?.readText() ?: ""
                codeValue = TextFieldValue(content)
                stream?.close()
                // Proactively save copy to filesystem
                java.io.File(localPath).apply {
                    parentFile?.mkdirs()
                    writeText(content)
                }
            } catch (e: Exception) {
                terminalViewModel.executeCommand("echo Error reading file: ${e.message}")
            }
        }
    }

    val requirementsPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = "${context.filesDir.absolutePath}/bots/${currentBotId}_requirements.txt"
            try {
                val stream = context.contentResolver.openInputStream(it)
                val content = stream?.bufferedReader()?.readText() ?: ""
                stream?.close()
                java.io.File(localPath).apply {
                    parentFile?.mkdirs()
                    writeText(content)
                }
                requirementsPath = localPath
            } catch (e: Exception) {
                terminalViewModel.executeCommand("echo Error reading requirements: ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // ── Top bar ──────────────────────────────────────────────────
        EditorTopBar(
            isEditing = existingBot != null,
            botName = botName.ifBlank { "New Bot" },
            onBack = onBack,
            onSave = {
                scope.launch {
                    saveBot()
                    onBack()
                }
            },
        )

        // ── Sliding Navigation Tab Row ───────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardSurface,
            contentColor = CyberPurpleLight,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberPurple
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp)) },
                selectedContentColor = CyberPurpleLight,
                unselectedContentColor = TerminalGray
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Code", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp)) },
                selectedContentColor = CyberPurpleLight,
                unselectedContentColor = TerminalGray
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Console", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp)) },
                selectedContentColor = CyberPurpleLight,
                unselectedContentColor = TerminalGray
            )
        }

        // ── Tab Contents ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> {
                    // CONFIG TAB
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditorField(
                                value = botName,
                                onValueChange = { botName = it },
                                label = "Bot Name",
                                icon = Icons.Default.SmartToy,
                                modifier = Modifier.weight(1f),
                            )
                            EditorField(
                                value = botDescription,
                                onValueChange = { botDescription = it },
                                label = "Description",
                                icon = Icons.Default.Description,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Token field
                        OutlinedTextField(
                            value = tokenText,
                            onValueChange = { tokenText = it },
                            label = { Text("Discord Token", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Key, null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { tokenVisible = !tokenVisible }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null, modifier = Modifier.size(16.dp), tint = TerminalGray
                                    )
                                }
                            },
                            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = editorFieldColors(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )

                        // File selectors
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilePickerField(
                                label = "Python Script",
                                value = scriptPath.substringAfterLast('/').ifBlank { "Select .py file" },
                                icon = Icons.Default.Code,
                                onClick = { scriptPicker.launch("*/*") },
                                modifier = Modifier.weight(1f),
                            )
                            FilePickerField(
                                label = "requirements.txt",
                                value = requirementsPath.substringAfterLast('/').ifBlank { "Select requirements.txt" },
                                icon = Icons.Default.List,
                                onClick = { requirementsPicker.launch("*/*") },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Options
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = autoRestart,
                                    onCheckedChange = { autoRestart = it },
                                    modifier = Modifier.size(36.dp, 20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Auto restart", color = TerminalGray, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = autoStart,
                                    onCheckedChange = { autoStart = it },
                                    modifier = Modifier.size(36.dp, 20.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Start on boot", color = TerminalGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
                1 -> {
                    // CODE EDITOR TAB
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "  CODE EDITOR (AUTOSAVES ON RUN)",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier
                                .background(Color(0xFF0D1117))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                        )
                        CodeEditor(
                            value = codeValue,
                            onValueChange = { codeValue = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                2 -> {
                    // CONSOLE & TERMINAL TAB
                    Column(modifier = Modifier.fillMaxSize()) {
                        EditorBottomBar(
                            botId = currentBotId,
                            isRunning = botViewModel.getRuntimeState(currentBotId).status.isActive(),
                            onRun = {
                                scope.launch {
                                    saveBot()
                                    botViewModel.startBot(currentBotId)
                                    terminalViewModel.setActiveBotId(currentBotId)
                                }
                            },
                            onStop = {
                                botViewModel.stopBot(currentBotId)
                                terminalViewModel.executeCommand("stop $currentBotId")
                            },
                            onRestart = { botViewModel.restartBot(currentBotId) },
                            onInstallDeps = {
                                if (requirementsPath.isNotBlank()) {
                                    terminalViewModel.executeCommand("pip install -r $requirementsPath")
                                } else {
                                    terminalViewModel.executeCommand("pip list")
                                }
                            },
                        )
                        HorizontalDivider(color = CyberPurple.copy(0.2f))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            TerminalPanel(
                                modifier = Modifier.fillMaxSize(),
                                botId = currentBotId,
                                isExpanded = true,
                                onToggleExpand = {},
                                terminalViewModel = terminalViewModel,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    isEditing: Boolean,
    botName: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ArrowBack, "Back", tint = TerminalGray, modifier = Modifier.size(20.dp))
        }
        Text(
            text = if (isEditing) "Edit: $botName" else "New Bot",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Icon(Icons.Default.Save, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save", fontSize = 12.sp)
        }
    }
    HorizontalDivider(color = CyberPurple.copy(0.2f))
}

@Composable
private fun EditorBottomBar(
    botId: String,
    isRunning: Boolean,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onInstallDeps: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isRunning) {
            GlowActionButton("Run", Icons.Default.PlayArrow, StatusGreen, onRun)
        } else {
            GlowActionButton("Stop", Icons.Default.Stop, StatusRed, onStop)
            GlowActionButton("Restart", Icons.Default.Refresh, StatusYellow, onRestart)
        }
        GlowActionButton("Deps", Icons.Default.Download, NeonCyan, onInstallDeps)
    }
}

@Composable
private fun GlowActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, color.copy(0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
        modifier = modifier,
        colors = editorFieldColors(),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
        singleLine = true,
    )
}

@Composable
private fun FilePickerField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label, fontSize = 11.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.FolderOpen, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
        },
        modifier = modifier.clickable(onClick = onClick),
        colors = editorFieldColors(),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
        singleLine = true,
        readOnly = true,
    )
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberPurple,
    unfocusedBorderColor = CardSurfaceElevated,
    focusedTextColor = TerminalText,
    unfocusedTextColor = TerminalText,
    cursorColor = NeonCyan,
    focusedContainerColor = CardSurface,
    unfocusedContainerColor = CardSurface,
    focusedLabelColor = CyberPurple,
    unfocusedLabelColor = TerminalGray,
    focusedLeadingIconColor = CyberPurple,
    unfocusedLeadingIconColor = TerminalGray,
)
