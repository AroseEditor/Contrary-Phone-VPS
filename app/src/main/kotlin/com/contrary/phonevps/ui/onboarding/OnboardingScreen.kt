package com.contrary.phonevps.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.contrary.phonevps.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    val notificationPermState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else null

    val storagePermState = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        rememberPermissionState(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    } else null

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Terminal,
            title = "Contrary Phone VPS",
            subtitle = "Run Discord bots directly on your Android phone — no server needed.",
            iconColor = NeonCyan,
            action = null,
        ),
        OnboardingPage(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = "Required to show bot status and receive crash alerts in the background.",
            iconColor = StatusYellow,
            action = OnboardingAction("Grant Permission") {
                notificationPermState?.launchPermissionRequest()
            },
        ),
        OnboardingPage(
            icon = Icons.Default.BatteryFull,
            title = "Battery Optimization",
            subtitle = "Disable battery optimization so bots keep running when your screen is off.",
            iconColor = StatusGreen,
            action = OnboardingAction("Disable Battery Limits") {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            },
        ),
        OnboardingPage(
            icon = Icons.Default.PictureInPicture,
            title = "Overlay Permission",
            subtitle = "Allows a floating mini-terminal to stay visible over other apps.",
            iconColor = CyberPurpleLight,
            action = OnboardingAction("Grant Overlay") {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            },
        ),
        OnboardingPage(
            icon = Icons.Default.RocketLaunch,
            title = "All Set!",
            subtitle = "You're ready to deploy bots. Create your first bot and paste your Discord token.",
            iconColor = StatusGreen,
            action = OnboardingAction("Get Started") { onComplete() },
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(DarkSurface, DeepNavy),
                    radius = 1200f,
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(4f),
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            // Page indicators
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(pages.size) { index ->
                    val isActive = pagerState.currentPage == index
                    val width by animateDpAsState(if (isActive) 24.dp else 8.dp, label = "dot")
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (isActive) CyberPurple else TerminalGray.copy(0.3f))
                    )
                }
            }

            // Nav buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Back", color = TerminalGray)
                    }
                } else {
                    Spacer(Modifier.width(80.dp))
                }

                if (pagerState.currentPage < pages.size - 1) {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.weight(0.5f))
        }

        // Skip button
        TextButton(
            onClick = onComplete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Text("Skip", color = TerminalGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Glowing icon
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(page.iconColor.copy(0.25f), Color.Transparent)
                    )
                )
                .border(1.5.dp, page.iconColor.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                tint = page.iconColor,
                modifier = Modifier.size(44.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = page.title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.subtitle,
            color = TerminalGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        page.action?.let { action ->
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = action.onClick,
                colors = ButtonDefaults.buttonColors(containerColor = page.iconColor.copy(0.2f), contentColor = page.iconColor),
                border = BorderStroke(1.dp, page.iconColor.copy(0.5f)),
            ) {
                Text(action.label, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val iconColor: Color,
    val action: OnboardingAction?,
)

private data class OnboardingAction(
    val label: String,
    val onClick: () -> Unit,
)
