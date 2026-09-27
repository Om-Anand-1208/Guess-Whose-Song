package com.guesswhosesong.app.ui.screens.join

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.guesswhosesong.app.ui.components.AvatarCharacter
import com.guesswhosesong.app.ui.components.AvatarPicker
import com.guesswhosesong.app.ui.theme.GwsPalette

@Composable
fun JoinScreen(
    viewModel: JoinViewModel = hiltViewModel(),
    onNavigateToLobby: (joinCode: String, displayName: String, avatarId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> result.data?.let(viewModel::linkGoogle) }
    val recoveryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> result.data?.let(viewModel::recoverWithGoogle) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JoinEvent.NavigateToLobby -> onNavigateToLobby(event.joinCode, event.displayName, event.avatarId)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            JoinHero()
            Spacer(Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Make your entrance", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Choose a name and a little character. Then start a room or hop into a friend’s.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = uiState.displayName,
                        onValueChange = viewModel::onDisplayNameChanged,
                        label = { Text("Your name") },
                        placeholder = { Text("e.g. Maya") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))
                    AvatarPicker(
                        selectedId = uiState.avatarId,
                        onSelected = viewModel::onAvatarSelected
                    )

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            keyboard?.hide()
                            viewModel.createRoom()
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create a room  →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text("  or  ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(
                        value = uiState.joinCode,
                        onValueChange = viewModel::onJoinCodeChanged,
                        label = { Text("Room code") },
                        placeholder = { Text("ABCD12") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = {
                            keyboard?.hide()
                            viewModel.joinRoom()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            keyboard?.hide()
                            viewModel.joinRoom()
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Join a room  →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SpotifyConnection(
                connected = uiState.isSpotifyConnected,
                onConnect = viewModel::connectSpotify,
                onDisconnect = viewModel::disconnectSpotify
            )
            Spacer(Modifier.height(10.dp))
            AccountRecovery(
                linked = uiState.isAccountLinked,
                onLink = {
                    val activity = context as? Activity
                    if (activity != null) googleLauncher.launch(viewModel.googleSignInIntent(activity))
                },
                onRecover = {
                    val activity = context as? Activity
                    if (activity != null) recoveryLauncher.launch(viewModel.googleSignInIntent(activity))
                }
            )
            Text(
                "Guests can play instantly. Linking Google just helps you recover your player later.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun JoinHero() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = GwsPalette.Lavender,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(30.dp))
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .align(Alignment.BottomEnd)
                    .background(GwsPalette.Paper.copy(alpha = 0.22f), RoundedCornerShape(80.dp))
            )
            Box(
                modifier = Modifier
                    .size(122.dp)
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
            ) {
                AvatarCharacter("violet", modifier = Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                AvatarCharacter("sunny", modifier = Modifier.fillMaxSize())
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp, end = 112.dp)
            ) {
                Surface(
                    color = GwsPalette.Ink,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "SOCIAL MUSIC GAME",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
                Text(
                    "Guess\nwhose song?",
                    style = MaterialTheme.typography.displaySmall,
                    color = GwsPalette.Ink,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "A little mystery.\nA lot of music.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GwsPalette.Ink.copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SpotifyConnection(
    connected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (connected) Color(0xFF1DB954).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (connected) Color(0xFF1DB954) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("♫", fontSize = 24.sp, color = if (connected) Color(0xFF159447) else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (connected) "Spotify connected" else "Bring your top songs",
                    fontWeight = FontWeight.Bold,
                    color = if (connected) Color(0xFF159447) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (connected) "Your picks are ready for a round" else "Optional · auto-fill your song picks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = if (connected) onDisconnect else onConnect) {
                Text(if (connected) "Disconnect" else "Connect")
            }
        }
    }
}

@Composable
private fun AccountRecovery(
    linked: Boolean,
    onLink: () -> Unit,
    onRecover: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                if (linked) "Google account linked ✓" else "Want to keep this character?",
                fontWeight = FontWeight.Bold
            )
            Text(
                if (linked) "You can recover your player after reinstalling."
                else "Link Google so your player identity can be recovered.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (!linked) OutlinedButton(onClick = onLink) { Text("Link Google") }
                TextButton(onClick = onRecover) { Text("Already linked?") }
            }
        }
    }
}
