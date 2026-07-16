package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AiAssistantPanel(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Glowing heartbeat animation for the neural link status light
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .border(2.dp, Brush.verticalGradient(listOf(NeonPink, NeonCyan)), RoundedCornerShape(16.dp)),
        color = CyberBlack,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Neon Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonLime,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GEMINI NEURAL CO-PILOT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = GlowingWhite,
                        letterSpacing = 1.5.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(NeonLime.copy(alpha = alphaPulse))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isLoading) "PROCESSING STREAM..." else "UPLINK STABLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLoading) NeonPink else NeonCyan,
                            fontSize = 9.sp
                        )
                    }
                }

                // Clear Chat action
                IconButton(onClick = onClearChat) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Neural Log",
                        tint = CyberMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Close panel
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Sever Link",
                        tint = GlowingWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Message Stream Log
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { message ->
                    AiChatBubble(message = message)
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = NeonPink
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Synthesizing intelligence response...",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Terminal Input Row
            Surface(
                tonalElevation = 8.dp,
                color = CyberSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberOutline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_panel_chat_input"),
                        placeholder = { 
                            Text(
                                text = "Command neural uplink...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberMuted
                            ) 
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GlowingWhite,
                            unfocusedTextColor = GlowingWhite,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberOutline,
                            cursorColor = NeonCyan
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank() && !isLoading) {
                                    onSendMessage(textInput)
                                    textInput = ""
                                }
                            }
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank() && !isLoading) {
                                onSendMessage(textInput)
                                textInput = ""
                            }
                        },
                        enabled = textInput.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (textInput.isNotBlank() && !isLoading) {
                                    Brush.linearGradient(listOf(NeonPink, NeonCyan))
                                } else {
                                    Brush.linearGradient(listOf(CyberSurfaceVariant, CyberSurfaceVariant))
                                },
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Transmit",
                            tint = if (textInput.isNotBlank() && !isLoading) CyberBlack else CyberMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgBrush = if (message.isUser) {
        Brush.linearGradient(listOf(NeonPink.copy(alpha = 0.2f), CyberSurfaceVariant))
    } else {
        Brush.linearGradient(listOf(CyberSurfaceVariant, NeonCyan.copy(alpha = 0.15f)))
    }
    val outlineColor = if (message.isUser) NeonPink.copy(alpha = 0.4f) else NeonCyan.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        // Tag chip representing the node source
        Text(
            text = if (message.isUser) "USER_NODE" else "GEMINI_CORE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (message.isUser) NeonPink else NeonCyan,
            fontSize = 9.sp,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp),
            fontFamily = FontFamily.Monospace
        )

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bgBrush, shape = RoundedCornerShape(12.dp))
                .border(1.dp, outlineColor, shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = GlowingWhite,
                lineHeight = 20.sp,
                fontFamily = if (message.isUser) FontFamily.Default else FontFamily.Monospace,
                fontSize = if (message.isUser) 14.sp else 12.sp
            )
        }
    }
}
