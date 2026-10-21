package com.guesswhosesong.app.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guesswhosesong.app.ui.screens.game.GameScreen
import com.guesswhosesong.app.ui.screens.join.JoinScreen
import com.guesswhosesong.app.ui.screens.lobby.LobbyScreen
import com.guesswhosesong.app.ui.screens.results.ResultsScreen
import com.guesswhosesong.app.ui.screens.submission.SubmissionScreen
import com.guesswhosesong.app.ui.components.RoomChatOverlay
import com.guesswhosesong.app.ui.theme.AppearanceMode
import com.guesswhosesong.app.ui.theme.AppearanceSettings
import com.guesswhosesong.app.ui.theme.LocalAppearanceSettings
import com.guesswhosesong.app.ui.theme.PostJoinTheme
import com.guesswhosesong.app.ui.theme.GuessWhoseSongTheme
import com.guesswhosesong.shared.models.AvatarCustomization

object Routes {
    const val JOIN = "join"
    const val LOBBY = "lobby/{joinCode}/{displayName}/{shapeId}/{colorId}/{eyesId}/{mouthId}/{accessoryId}"
    const val SUBMISSION = "submission"
    const val GAME = "game"
    const val RESULTS = "results"

    fun lobby(joinCode: String, displayName: String, avatarCustomization: AvatarCustomization) =
        "lobby/$joinCode/${displayName.encodeUrl()}/${avatarCustomization.shapeId}/${avatarCustomization.colorId}/${avatarCustomization.eyesId}/${avatarCustomization.mouthId}/${avatarCustomization.accessoryId}"

    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")
}

@Composable
fun GWSNavHost(pendingInviteCode: String? = null, onInviteHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val chatViewModel: RoomChatViewModel = hiltViewModel()
    val chatState by chatViewModel.uiState.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(pendingInviteCode, backStackEntry?.destination?.route) {
        if (pendingInviteCode != null && backStackEntry != null &&
            backStackEntry?.destination?.route != Routes.JOIN) {
            navController.navigate(Routes.JOIN) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    var hadActiveRoom by remember { mutableStateOf(false) }
    LaunchedEffect(chatState.roomCode) {
        if (chatState.roomCode != null) {
            hadActiveRoom = true
        } else if (hadActiveRoom) {
            hadActiveRoom = false
            val route = navController.currentDestination?.route
            if (route != Routes.JOIN && route != Routes.RESULTS) {
                navController.navigate(Routes.JOIN) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    val context = LocalContext.current
    val appearancePreferences = remember {
        context.getSharedPreferences("post_join_appearance", android.content.Context.MODE_PRIVATE)
    }
    var appearanceMode by remember {
        mutableStateOf(AppearanceMode.fromStoredValue(appearancePreferences.getString("mode", null)))
    }
    val appearanceSettings = remember(appearanceMode) {
        AppearanceSettings(appearanceMode) { mode ->
            appearancePreferences.edit().putString("mode", mode.name).apply()
            appearanceMode = mode
        }
    }
    val darkTheme = appearanceMode.resolvesDark(isSystemInDarkTheme())
    val view = LocalView.current

    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppearanceSettings provides appearanceSettings) {
      GuessWhoseSongTheme(darkTheme = darkTheme) {
       Surface(
           modifier = androidx.compose.ui.Modifier.fillMaxSize(),
           color = MaterialTheme.colorScheme.background
       ) {
       Box(Modifier.fillMaxSize()) {
      NavHost(navController = navController, startDestination = Routes.JOIN) {
        composable(Routes.JOIN) {
            JoinScreen(
                incomingInviteCode = pendingInviteCode,
                onInviteHandled = onInviteHandled,
                onNavigateToLobby = { joinCode, displayName, avatarCustomization ->
                    navController.navigate(Routes.lobby(joinCode, displayName, avatarCustomization)) {
                        popUpTo(Routes.JOIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.LOBBY,
            arguments = listOf(
                navArgument("joinCode") { type = NavType.StringType },
                navArgument("displayName") { type = NavType.StringType },
                navArgument("shapeId") { type = NavType.StringType },
                navArgument("colorId") { type = NavType.StringType },
                navArgument("eyesId") { type = NavType.StringType },
                navArgument("mouthId") { type = NavType.StringType },
                navArgument("accessoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val joinCode = backStackEntry.arguments?.getString("joinCode") ?: ""
            val displayName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("displayName") ?: "", "UTF-8"
            )
            val avatarCustomization = AvatarCustomization.normalize(
                AvatarCustomization(
                    shapeId = backStackEntry.arguments?.getString("shapeId") ?: "sunny",
                    colorId = backStackEntry.arguments?.getString("colorId") ?: "sunny",
                    eyesId = backStackEntry.arguments?.getString("eyesId") ?: "dots",
                    mouthId = backStackEntry.arguments?.getString("mouthId") ?: "smile",
                    accessoryId = backStackEntry.arguments?.getString("accessoryId") ?: "none"
                ),
                "sunny"
            )
            PostJoinTheme {
              LobbyScreen(
                joinCode = joinCode,
                displayName = displayName,
                avatarCustomization = avatarCustomization,
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
        }

        composable(Routes.SUBMISSION) {
            val context = LocalContext.current
            PostJoinTheme {
              SubmissionScreen(
                onNavigateToGame = {
                    navController.navigate(Routes.GAME) {
                        popUpTo(Routes.SUBMISSION) { inclusive = true }
                    }
                },
                onReturnToLobby = { joinCode, displayName, avatarCustomization, explanation ->
                    Toast.makeText(context, explanation, Toast.LENGTH_LONG).show()
                    navController.navigate(Routes.lobby(joinCode, displayName, avatarCustomization)) {
                        popUpTo(0) { inclusive = true }
                    }
                }
              )
            }
        }

        composable(Routes.GAME) {
            PostJoinTheme {
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
        }

        composable(Routes.RESULTS) {
            ResultsScreen(
                onNavigateToLobby = { destination ->
                    navController.navigate(Routes.lobby(
                        destination.joinCode,
                        destination.displayName,
                        destination.avatarCustomization
                    )) {
                        popUpTo(Routes.RESULTS) { inclusive = true }
                    }
                },
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
          if (backStackEntry?.destination?.route != Routes.JOIN && chatState.roomCode != null) {
              RoomChatOverlay(
                  state = chatState,
                  onOpenChange = chatViewModel::setOpen,
                  onDraftChange = chatViewModel::setDraft,
                  onLoadOlder = chatViewModel::loadOlder,
                  onEndRoom = chatViewModel::endRoom,
                  onSend = chatViewModel::send
              )
          }
       }
       }
      }
    }
}
