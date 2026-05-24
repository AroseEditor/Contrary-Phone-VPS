package com.contrary.phonevps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.contrary.phonevps.ui.dashboard.DashboardScreen
import com.contrary.phonevps.ui.editor.BotEditorScreen
import com.contrary.phonevps.ui.onboarding.OnboardingScreen
import com.contrary.phonevps.ui.settings.SettingsScreen
import com.contrary.phonevps.ui.theme.ContraryPhoneVPSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContraryPhoneVPSTheme {
                ContraryNavGraph()
            }
        }
    }
}

@Composable
fun ContraryNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "onboarding",
        enterTransition = { slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(220)) },
        exitTransition = { slideOutHorizontally(tween(220)) { -it / 3 } + fadeOut(tween(220)) },
        popEnterTransition = { slideInHorizontally(tween(220)) { -it / 3 } + fadeIn(tween(220)) },
        popExitTransition = { slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(220)) },
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onNavigateToEditor = { botId ->
                    navController.navigate(
                        if (botId != null) "editor/$botId" else "editor/new"
                    )
                },
                onNavigateToSettings = { navController.navigate("settings") },
            )
        }

        composable(
            route = "editor/{botId}",
            arguments = listOf(navArgument("botId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val botId = backStackEntry.arguments?.getString("botId")?.takeIf { it != "new" }
            BotEditorScreen(
                botId = botId,
                onBack = { navController.popBackStack() },
            )
        }

        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
