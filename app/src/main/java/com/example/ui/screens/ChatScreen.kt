package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.VoiceInputController
import com.example.data.ChatMessageEntity
import com.example.ui.components.ChatComposerBar
import com.example.ui.components.ChatEmptyState
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.GeneratingImageCard
import com.example.ui.theme.ManoTheme
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    messages: List<ChatMessageEntity>,
    streamingReply: String,
    isSendingText: Boolean,
    isGeneratingImage: Boolean,
    isImageMode: Boolean,
    attachedImagePath: String?,
    chatError: String?,
    onToggleImageMode: () -> Unit,
    onSetImageMode: (Boolean) -> Unit,
    onAttachImageUri: (Uri) -> Unit,
    onClearAttachedImage: () -> Unit,
    onSendMessage: (String) -> Unit,
    onNewChat: () -> Unit,
    onDismissError: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onGoToMyths: () -> Unit,
    onOpenVoiceMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var inputText by rememberSaveable { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToBottom by remember {
        derivedStateOf { listState.canScrollForward }
    }

    val voiceController = remember(context) {
        VoiceInputController(
            context = context,
            onListeningChanged = { listening -> isListening = listening },
            onPartialText = { partial -> inputText = partial },
            onFinalText = { final -> inputText = final },
            onError = { err -> voiceError = err }
        )
    }

    DisposableEffect(voiceController) {
        onDispose { voiceController.destroy() }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                voiceError = null
                voiceController.startListening()
            } else {
                voiceError = "Libera o microfone nas permissões pra mandar voz pro mno."
            }
        }
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onAttachImageUri(uri)
            }
        }
    )

    val totalItems = messages.size +
        (if (isSendingText) 1 else 0) +
        (if (isGeneratingImage) 1 else 0)

    LaunchedEffect(messages.size, isGeneratingImage) {
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    LaunchedEffect(streamingReply.length) {
        if (totalItems > 0 && !listState.canScrollForward) {
            listState.scrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .imePadding()
    ) {
        ChatHeaderBar(
            onOpenVoiceMode = onOpenVoiceMode,
            onOpenKnowledgeBase = onOpenKnowledgeBase,
            onNewChat = onNewChat
        )

        // Scrollable Message List or Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && streamingReply.isEmpty() && !isSendingText && !isGeneratingImage) {
                ChatEmptyState(
                    onPickStarter = { starter ->
                        onSetImageMode(starter.enableImageMode)
                        inputText = starter.prompt
                    },
                    onGoToMyths = onGoToMyths
                )
            } else {
                val streamingFallback = stringResource(R.string.streaming_placeholder)
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .align(Alignment.TopCenter)
                        .testTag("chat_message_list")
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        ChatMessageBubble(message = msg)
                    }

                    if (isSendingText) {
                        item(key = "streaming_item") {
                            ChatMessageBubble(
                                message = ChatMessageEntity(
                                    id = -1L,
                                    sessionId = -1L,
                                    isUser = false,
                                    text = streamingReply.ifEmpty { streamingFallback }
                                ),
                                isStreaming = true
                            )
                        }
                    }

                    if (isGeneratingImage) {
                        item(key = "generating_image_item") {
                            GeneratingImageCard()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollToBottom,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                if (totalItems > 0) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(totalItems - 1)
                                    }
                                }
                            },
                            containerColor = colorScheme.surfaceVariant,
                            contentColor = colorScheme.primary,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("scroll_to_bottom_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.scroll_to_bottom_cd)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Text Input & Controls
        ChatComposerBar(
            inputText = inputText,
            onInputTextChange = { inputText = it },
            isImageMode = isImageMode,
            isListening = isListening,
            isSendingText = isSendingText,
            isGeneratingImage = isGeneratingImage,
            attachedImagePath = attachedImagePath,
            activeError = chatError ?: voiceError,
            onDismissError = {
                onDismissError()
                voiceError = null
            },
            onStopVoice = { voiceController.stopListening() },
            onClearAttachedImage = onClearAttachedImage,
            onPickImage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onToggleImageMode = onToggleImageMode,
            onToggleVoiceInput = {
                voiceError = null
                if (isListening) {
                    voiceController.stopListening()
                } else {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        voiceController.startListening()
                    } else {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            onSend = {
                if ((inputText.isNotBlank() || attachedImagePath != null) && !isSendingText && !isGeneratingImage) {
                    val toSend = inputText
                    inputText = ""
                    onSendMessage(toSend)
                }
            }
        )
    }
}

@Composable
private fun ChatHeaderBar(
    onOpenVoiceMode: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onNewChat: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mano_avatar_1795262249289),
                    contentDescription = stringResource(R.string.avatar_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "+17",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.mano_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenVoiceMode,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_voice_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RecordVoiceOver,
                        contentDescription = stringResource(R.string.voice_mode_cd),
                        tint = colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onOpenKnowledgeBase,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("knowledge_base_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.knowledge_base_title),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, colorScheme.outline),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onNewChat)
                        .testTag("new_chat_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.new_chat),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    ManoTheme {
        ChatScreen(
            messages = listOf(
                ChatMessageEntity(
                    id = 1L,
                    sessionId = 1L,
                    isUser = true,
                    text = "E aí mno, me ajuda a estruturar uma função em Kotlin?"
                ),
                ChatMessageEntity(
                    id = 2L,
                    sessionId = 1L,
                    isUser = false,
                    text = "Bora lá, mno! Manda o que essa função precisa fazer que eu já monto o código na hora."
                )
            ),
            streamingReply = "",
            isSendingText = false,
            isGeneratingImage = false,
            isImageMode = false,
            attachedImagePath = null,
            chatError = null,
            onToggleImageMode = {},
            onSetImageMode = {},
            onAttachImageUri = {},
            onClearAttachedImage = {},
            onSendMessage = {},
            onNewChat = {},
            onDismissError = {},
            onOpenKnowledgeBase = {},
            onGoToMyths = {},
            onOpenVoiceMode = {}
        )
    }
}
