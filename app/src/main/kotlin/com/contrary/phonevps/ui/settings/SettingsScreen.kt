package com.contrary.phonevps.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contrary.phonevps.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isBatteryOptIgnored = remember { pm.isIgnoringBatteryOptimizations(context.packageName) }
    val hasOverlay = remember { Settings.canDrawOverlays(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TerminalGray)
            }
            Text(
                "Settings",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        HorizontalDivider(color = CyberPurple.copy(0.2f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsSection("Permissions") {
                SettingsPermissionRow(
                    title = "Battery Optimization",
                    description = "Keeps bots running with screen off",
                    icon = Icons.Default.BatteryFull,
                    isGranted = isBatteryOptIgnored,
                    onFix = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    }
                )
                SettingsPermissionRow(
                    title = "Display Over Apps",
                    description = "Floating terminal widget",
                    icon = Icons.Default.PictureInPicture,
                    isGranted = hasOverlay,
                    onFix = {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        )
                    }
                )
            }

            SettingsSection("OEM Battery Killers") {
                SettingsInfoRow(
                    title = "Xiaomi / MIUI",
                    description = "Settings → Apps → Contrary VPS → Battery → No restrictions",
                    icon = Icons.Default.PhoneAndroid,
                )
                SettingsInfoRow(
                    title = "Samsung / One UI",
                    description = "Battery → Background usage limits → Never sleeping apps",
                    icon = Icons.Default.PhoneAndroid,
                )
                SettingsInfoRow(
                    title = "Oppo / ColorOS / Vivo",
                    description = "Battery → Energy consumption list → No restrictions",
                    icon = Icons.Default.PhoneAndroid,
                )
            }

            SettingsSection("About") {
                SettingsInfoRow(
                    title = "Version",
                    description = "1.0.0 — Contrary Phone VPS",
                    icon = Icons.Default.Info,
                )
                SettingsInfoRow(
                    title = "Python Runtime",
                    description = "Python 3.11 via Chaquopy",
                    icon = Icons.Default.Code,
                )
                SettingsClickRow(
                    title = "GitHub Repository",
                    description = "github.com/AroseEditor/Contrary-Phone-VPS",
                    icon = Icons.Default.OpenInNew,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AroseEditor/Contrary-Phone-VPS"))
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            color = CyberPurple,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, CardSurfaceElevated),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsPermissionRow(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onFix: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (isGranted) StatusGreen else StatusRed, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = TerminalGray, fontSize = 11.sp)
        }
        if (!isGranted) {
            TextButton(onClick = onFix, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Fix", color = StatusYellow, fontSize = 11.sp)
            }
        } else {
            Icon(Icons.Default.CheckCircle, null, tint = StatusGreen, modifier = Modifier.size(16.dp))
        }
    }
    HorizontalDivider(color = CardSurfaceElevated, thickness = 0.5.dp)
}

@Composable
private fun SettingsInfoRow(title: String, description: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = TerminalGray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = TerminalGray, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
    HorizontalDivider(color = CardSurfaceElevated, thickness = 0.5.dp)
}

@Composable
private fun SettingsClickRow(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(description, color = TerminalGray, fontSize = 11.sp)
            }
            Icon(Icons.Default.ArrowForward, null, tint = TerminalGray, modifier = Modifier.size(14.dp))
        }
    }
}
