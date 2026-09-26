package com.guesswhosesong.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guesswhosesong.app.ui.screens.game.GameScreen
import com.guesswhosesong.app.ui.screens.join.JoinScreen
import com.guesswhosesong.app.ui.screens.lobby.LobbyScreen
import com.guesswhosesong.app.ui.screens.results.ResultsScreen
import com.guesswhosesong.app.ui.screens.submission.SubmissionScreen

object Routes {
    const val JOIN = "join"
    const val LOBBY = "lobby/{joinCode}/{displayName}"
    const val SUBMISSION = "submission"
    const val GAME = "game"
    const val RESULTS = "results"

    fun lobby(joinCode: String, displayName: String) =
        "lobby/$joinCode/${displayName.encodeUrl()}"

    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")
}

@Composable
fun GWSNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.JOIN) {
        composable(Routes.JOIN) {
            JoinScreen(
                onNavigateToLobby = { joinCode, displayName ->
                    navController.navigate(Routes.lobby(joinCode, displayName)) {
                        popUpTo(Routes.JOIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.LOBBY,
            arguments = listOf(
                navArgument("joinCode") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val joinCode = backStackEntry.arguments?.getString("joinCode") ?: ""
            val displayName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("displayName") ?: "", "UTF-8"
            )
            LobbyScreen(
                joinCode = joinCode,
                displayName = displayName,
                onNavigateToSubmission = {
                    navController.navigate(Routes.SUBMISSION) {
                        popUpTo(Routes.LOBBY) { inclusive = true }
                    }
                },
                onKicked = {
                    navController.navigate(Routes.JOIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Routes.SUBMISSION) {
            SubmissionScreen(
                onNavigateToGame = {
                    navController.navigate(Routes.GAME) {
                        popUpTo(Routes.SUBMISSION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.GAME) {
            GameScreen(
                onNavigateToResults = {
                    navController.navigate(Routes.RESULTS) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                },
                onNavigateToSubmission = {
                    navController.navigate(Routes.SUBMISSION) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.RESULTS) {
            ResultsScreen(
                onNavigateToSubmission = {
                    navController.navigate(Routes.SUBMISSION) {
                        popUpTo(Routes.RESULTS) { inclusive = true }
                    }
                },
                onNavigateToJoin = {
                    navController.navigate(Routes.JOIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}
