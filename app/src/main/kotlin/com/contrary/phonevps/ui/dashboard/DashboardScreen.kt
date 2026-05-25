package com.contrary.phonevps.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.hilt.navigation.compose.hiltViewModel
import com.contrary.phonevps.data.model.BotStatus
import com.contrary.phonevps.ui.logs.TerminalPanel
import com.contrary.phonevps.ui.theme.*
import com.contrary.phonevps.viewmodel.BotViewModel
import com.contrary.phonevps.viewmodel.TerminalViewModel

@Composable
fun DashboardScreen(
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    botViewModel: BotViewModel = hiltViewModel(),
    terminalViewModel: TerminalViewModel = hiltViewModel(),
) {
    val uiState by botViewModel.uiState.collectAsState()
    var sidebarExpanded by remember { mutableStateOf(false) }
    var terminalExpanded by remember { mutableStateOf(false) }
    var terminalHeightFraction by remember { mutableStateOf(0.35f) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarExpanded) 200.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sidebar_width",
    )

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            botViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepNavy,
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Sidebar ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = sidebarExpanded,
                enter = slideInHorizontally() + expandHorizontally(),
                exit = slideOutHorizontally() + shrinkHorizontally(),
            ) {
                BotSidebar(
                    modifier = Modifier.width(200.dp),
                    selectedBotId = uiState.selectedBotId,
                    onBotSelected = { botId ->
                        botViewModel.selectBot(botId)
                        terminalViewModel.setActiveBotId(botId)
                    },
                    onCreateBot = { onNavigateToEditor(null) },
                    onSettings = onNavigateToSettings,
                    onExportLogs = { terminalViewModel.executeCommand("export") },
                )
            }

            // ── Main area ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top bar
                DashboardTopBar(
                    selectedBotId = uiState.selectedBotId,
                    sidebarExpanded = sidebarExpanded,
                    onToggleSidebar = { sidebarExpanded = !sidebarExpanded },
                    onEdit = { uiState.selectedBotId?.let { onNavigateToEditor(it) } },
                )

                // Bot detail / empty state
                val selectedBot = uiState.bots.find { it.id == uiState.selectedBotId }
                if (selectedBot != null) {
                    val runtimeState = uiState.runtimeStates[selectedBot.id]
                    val status = runtimeState?.status ?: BotStatus.STOPPED

                    // Bot control panel
                    BotControlPanel(
                        botName = selectedBot.name,
                        status = status,
                        uptimeSeconds = runtimeState?.uptimeSeconds ?: 0L,
                        ramMb = runtimeState?.ramUsageMb ?: 0f,
                        cpuPercent = runtimeState?.cpuPercent ?: 0f,
                        restartCount = runtimeState?.restartCount ?: 0,
                        onStart = { botViewModel.startBot(selectedBot.id) },
                        onStop = {
                            botViewModel.stopBot(selectedBot.id)
                            terminalViewModel.executeCommand("stop ${selectedBot.id}", selectedBot.id)
                        },
                        onRestart = { botViewModel.restartBot(selectedBot.id) },
                        onEdit = { onNavigateToEditor(selectedBot.id) },
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
                    NoBotSelectedPlaceholder(
                        onCreateBot = { onNavigateToEditor(null) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }

                Spacer(Modifier.weight(1f))

                // ── Terminal panel at bottom ──────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (terminalExpanded) terminalHeightFraction else 0.06f)
                ) {
                    TerminalPanel(
                        modifier = Modifier.fillMaxSize(),
                        botId = uiState.selectedBotId,
                        isExpanded = terminalExpanded,
                        onToggleExpand = { terminalExpanded = !terminalExpanded },
                        terminalViewModel = terminalViewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTopBar(
    selectedBotId: String?,
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleSidebar, modifier = Modifier.size(36.dp)) {
            Icon(
                if (sidebarExpanded) Icons.Default.MenuOpen else Icons.Default.Menu,
                contentDescription = "Toggle sidebar",
                tint = TerminalGray,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = "Contrary Phone VPS",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        if (selectedBotId != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "Edit bot", tint = CyberPurpleLight, modifier = Modifier.size(18.dp))
            }
        }
    }
    HorizontalDivider(color = CyberPurple.copy(0.2f), thickness = 1.dp)
}

@Composable
private fun BotControlPanel(
    botName: String,
    status: BotStatus,
    uptimeSeconds: Long,
    ramMb: Float,
    cpuPercent: Float,
    restartCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Status header
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = when (status) {
                BotStatus.RUNNING -> StatusGreen
                BotStatus.STARTING -> StatusYellow
                BotStatus.ERROR, BotStatus.CRASHING -> StatusRed
                BotStatus.INSTALLING_DEPS -> StatusBlue
                BotStatus.STOPPED -> StatusGray
            }

            Text(
                text = botName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusColor.copy(0.15f),
                border = BorderStroke(1.dp, statusColor.copy(0.5f)),
            ) {
                Text(
                    text = status.displayName(),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Stats row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip("Uptime", formatUptime(uptimeSeconds))
            StatChip("RAM", "${ramMb.toInt()} MB")
            StatChip("CPU", "${cpuPercent.toInt()}%")
            StatChip("Restarts", "$restartCount")
        }

        Spacer(Modifier.height(16.dp))

        // Control buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                status.isActive() -> {
                    GlowButton(
                        text = "Stop",
                        icon = Icons.Default.Stop,
                        color = StatusRed,
                        onClick = onStop,
                    )
                    GlowButton(
                        text = "Restart",
                        icon = Icons.Default.Refresh,
                        color = StatusYellow,
                        onClick = onRestart,
                    )
                }
                else -> {
                    GlowButton(
                        text = "Start",
                        icon = Icons.Default.PlayArrow,
                        color = StatusGreen,
                        onClick = onStart,
                    )
                }
            }
            OutlinedButton(
                onClick = onEdit,
                border = BorderStroke(1.dp, CyberPurple.copy(0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Default.Edit, null, tint = CyberPurpleLight, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit", color = CyberPurpleLight, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GlowButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(0.15f),
            contentColor = color,
        ),
        border = BorderStroke(1.dp, color.copy(0.6f)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CardSurface)
            .border(1.dp, CardSurfaceElevated, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, color = TerminalGray, fontSize = 9.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun NoBotSelectedPlaceholder(
    onCreateBot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(">_", color = CyberPurple.copy(0.3f), fontSize = 48.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(12.dp))
        Text("Select or create a bot", color = TerminalGray, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onCreateBot,
            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Create Bot")
        }
    }
}

private fun formatUptime(s: Long) = "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
