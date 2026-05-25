package com.contrary.phonevps.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.contrary.phonevps.data.model.BotConfig
import com.contrary.phonevps.data.model.BotStatus
import com.contrary.phonevps.ui.theme.*
import com.contrary.phonevps.viewmodel.BotViewModel
import kotlinx.coroutines.delay

@Composable
fun BotSidebar(
    modifier: Modifier = Modifier,
    selectedBotId: String?,
    onBotSelected: (String) -> Unit,
    onCreateBot: () -> Unit,
    onSettings: () -> Unit,
    onExportLogs: () -> Unit,
    viewModel: BotViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkSurface,
                        DeepNavy,
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(CyberPurple.copy(0.3f), Color.Transparent)
                ),
                shape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
            )
    ) {
        // ── App Logo / Header ───────────────────────────────────────
        SidebarHeader()

        HorizontalDivider(
            color = CyberPurple.copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(8.dp))

        // ── Bot list ────────────────────────────────────────────────
        if (uiState.bots.isEmpty()) {
            EmptyBotsPlaceholder(onCreateBot = onCreateBot)
        } else {
            Text(
                text = "BOTS",
                color = TerminalGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(uiState.bots, key = { it.id }) { bot ->
                    val runtimeState = uiState.runtimeStates[bot.id]
                    SidebarBotItem(
                        bot = bot,
                        status = runtimeState?.status ?: BotStatus.STOPPED,
                        ramMb = runtimeState?.ramUsageMb ?: 0f,
                        cpuPercent = runtimeState?.cpuPercent ?: 0f,
                        isSelected = bot.id == selectedBotId,
                        onClick = { onBotSelected(bot.id) },
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(
            color = CyberPurple.copy(alpha = 0.15f),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        // ── Bottom actions ──────────────────────────────────────────
        SidebarBottomActions(
            onCreateBot = onCreateBot,
            onSettings = onSettings,
            onExportLogs = onExportLogs,
        )
    }
}

@Composable
private fun SidebarHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Glowing logo circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(CyberPurple.copy(0.6f), DeepNavy)
                    )
                )
                .border(1.5.dp, CyberPurple.copy(0.7f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = ">_",
                color = NeonCyan,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Contrary",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
        Text(
            text = "Phone VPS",
            color = NeonCyan,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun SidebarBotItem(
    bot: BotConfig,
    status: BotStatus,
    ramMb: Float,
    cpuPercent: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val statusColor = when (status) {
        BotStatus.RUNNING -> StatusGreen
        BotStatus.STARTING -> StatusYellow
        BotStatus.CRASHING -> StatusOrange
        BotStatus.ERROR -> StatusRed
        BotStatus.INSTALLING_DEPS -> StatusBlue
        BotStatus.STOPPED -> StatusGray
    }

    // Pulsing animation for running status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )
    val dotAlpha = if (status == BotStatus.RUNNING) pulseAlpha else 1f

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) CyberPurpleDark.copy(0.4f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, CyberPurple.copy(0.6f)) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = dotAlpha))
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bot.name,
                    color = if (isSelected) Color.White else TerminalText,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = status.displayName(),
                        color = statusColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (status.isActive() && (ramMb > 0f || cpuPercent > 0f)) {
                        Text("•", color = TerminalGray, fontSize = 8.sp)
                        Text(
                            text = "${ramMb.toInt()}MB",
                            color = TerminalGray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Mini CPU bar
            if (status.isActive()) {
                Spacer(Modifier.width(4.dp))
                MiniUsageBar(
                    percent = cpuPercent / 100f,
                    color = NeonCyan,
                    modifier = Modifier.width(24.dp).height(3.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniUsageBar(
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(color.copy(0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(percent.coerceIn(0f, 1f))
                .background(color)
        )
    }
}

@Composable
private fun ColumnScope.EmptyBotsPlaceholder(onCreateBot: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            tint = CyberPurple.copy(0.4f),
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No bots yet",
            color = TerminalGray,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onCreateBot,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = CyberPurple.copy(0.3f),
                contentColor = CyberPurpleLight,
            ),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Create Bot", fontSize = 12.sp)
        }
    }
}

@Composable
private fun SidebarBottomActions(
    onCreateBot: () -> Unit,
    onSettings: () -> Unit,
    onExportLogs: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SidebarAction(
            icon = Icons.Default.AddCircle,
            label = "Create Bot",
            color = CyberPurpleLight,
            onClick = onCreateBot,
        )
        SidebarAction(
            icon = Icons.Default.FileDownload,
            label = "Export Logs",
            color = TerminalGray,
            onClick = onExportLogs,
        )
        SidebarAction(
            icon = Icons.Default.Settings,
            label = "Settings",
            color = TerminalGray,
            onClick = onSettings,
        )
    }
}

@Composable
private fun SidebarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Text(label, color = color, fontSize = 12.sp)
        }
    }
}
