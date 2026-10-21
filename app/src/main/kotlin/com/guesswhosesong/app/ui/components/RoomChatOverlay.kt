package com.guesswhosesong.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guesswhosesong.app.ui.RoomChatUiState
import com.guesswhosesong.shared.models.RoomChatMessage

@Composable
fun RoomChatOverlay(
    state: RoomChatUiState,
    onOpenChange: (Boolean) -> Unit,
    onDraftChange: (String) -> Unit,
    onLoadOlder: () -> Unit,
    onEndRoom: () -> Unit,
    onSend: () -> Unit
) {
    val roomCode = state.roomCode ?: return
    var confirmEndRoom by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        val panelWidth = minOf(maxWidth - 36.dp, 360.dp)
        val panelHeight = minOf(maxHeight - 170.dp, 370.dp).coerceAtLeast(210.dp)
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (state.isOpen) {
                Surface(
                    modifier = Modifier.width(panelWidth).height(panelHeight),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 12.dp
                ) {
                    Column(Modifier.fillMaxSize().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Room chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Text("Room $roomCode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (state.isHost) {
                                TextButton(onClick = { confirmEndRoom = true }, enabled = !state.endingRoom) {
                                    Text("End room", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { onOpenChange(false) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close room chat")
                            }
                        }
                        val listState = rememberLazyListState()
                        LaunchedEffect(state.isOpen) {
                            if (state.isOpen && state.messages.isNotEmpty()) {
                                listState.scrollToItem(state.messages.lastIndex + if (state.hasOlder) 1 else 0)
                            }
                        }
                        LaunchedEffect(state.messages.lastOrNull()?.id) {
                            if (state.isOpen && state.messages.isNotEmpty()) {
                                val lastIndex = state.messages.lastIndex + if (state.hasOlder) 1 else 0
                                val visibleLast = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                                if (visibleLast >= lastIndex - 1) listState.scrollToItem(lastIndex)
                            }
                        }
                        if (state.messages.isEmpty()) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No messages yet. Say hi!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (state.hasOlder) {
                                    item(key = "older_messages") {
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            TextButton(onClick = onLoadOlder, enabled = !state.loadingOlder) {
                                                Text(if (state.loadingOlder) "Loading…" else "Load earlier messages")
                                            }
                                        }
                                    }
                                }
                                items(state.messages, key = { it.id }) { message ->
                                    ChatMessageBubble(message, message.senderId == state.selfPlayerId)
                                }
                            }
                        }
                        state.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = state.draft,
                                onValueChange = onDraftChange,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Message…") },
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { onSend() })
                            )
                            IconButton(
                                onClick = onSend,
                                enabled = state.draft.isNotBlank() && !state.sending,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send message", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Box {
                FloatingActionButton(
                    onClick = { onOpenChange(!state.isOpen) },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Room chat")
                }
                if (state.unreadCount > 0 && !state.isOpen) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(start = 36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            if (state.unreadCount > 9) "9+" else state.unreadCount.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    if (confirmEndRoom) {
        AlertDialog(
            onDismissRequest = { confirmEndRoom = false },
            title = { Text("End this room?") },
            text = { Text("This closes the room for everyone and deletes its chat history.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEndRoom = false
                    onEndRoom()
                }) { Text("End room", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEndRoom = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(message: RoomChatMessage, isSelf: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = Modifier.widthIn(max = 290.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isSelf) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
                Text(
                    if (isSelf) "You" else message.senderName,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(message.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            }
        }
    }
}
