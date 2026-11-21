package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.VoiceInputController
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.DarkEmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MintAccent
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextOnGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

private enum class VoicePhase {
    IDLE, LISTENING, THINKING, SPEAKING
}

private fun cleanReplyForSpeech(raw: String): String {
    val withoutCodeBlocks = raw.replace(
        Regex("```[\\s\\S]*?```"),
        "Deixei o código pronto lá no chat pra tu copiar, mno."
    )
    return withoutCodeBlocks
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .replace(Regex("#+\\s*"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

@Composable
fun ManoVoiceMode(
    streamingReply: String,
    onSendVoicePrompt: (
        transcript: String,
        onReplyReady: (String) -> Unit,
        onReplyError: (String) -> Unit
    ) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(VoicePhase.IDLE) }
    var autoContinue by remember { mutableStateOf(true) }
    var lastHeard by remember { mutableStateOf("") }
    var lastSpokenReply by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var isDisposed by remember { mutableStateOf(false) }

    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }
    val voiceControllerRef = remember { mutableStateOf<VoiceInputController?>(null) }

    fun startListeningSafely() {
        errorMessage = null
        ttsRef.value?.stop()
        voiceControllerRef.value?.startListening()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                startListeningSafely()
            } else {
                phase = VoicePhase.IDLE
                errorMessage = "Libera o microfone nas permissões pra conversar por voz, mno."
            }
        }
    )

    fun triggerMicAction() {
        when (phase) {
            VoicePhase.LISTENING -> {
                voiceControllerRef.value?.stopListening()
                phase = VoicePhase.IDLE
            }
            VoicePhase.SPEAKING -> {
                ttsRef.value?.stop()
                phase = VoicePhase.IDLE
            }
            VoicePhase.THINKING -> Unit
            VoicePhase.IDLE -> {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    startListeningSafely()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    // Initialize TextToSpeech in pt-BR
    DisposableEffect(context) {
        isDisposed = false
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (isDisposed) return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                val ptBr = Locale.forLanguageTag("pt-BR")
                val res = engine?.setLanguage(ptBr)
                if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine?.setSpeechRate(1.06f)
                }
                ttsReady = true
            }
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    scope.launch(Dispatchers.Main) {
                        if (!isDisposed) {
                            phase = VoicePhase.SPEAKING
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    scope.launch(Dispatchers.Main) {
                        if (isDisposed) return@launch
                        phase = VoicePhase.IDLE
                        if (autoContinue) {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                startListeningSafely()
                            }
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    scope.launch(Dispatchers.Main) {
                        if (!isDisposed) {
                            phase = VoicePhase.IDLE
                        }
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    scope.launch(Dispatchers.Main) {
                        if (!isDisposed) {
                            phase = VoicePhase.IDLE
                        }
                    }
                }
            })
        }
        ttsRef.value = engine

        onDispose {
            isDisposed = true
            engine.stop()
            engine.shutdown()
            ttsRef.value = null
            ttsReady = false
        }
    }

    // Initialize SpeechRecognizer controller
    val voiceController = remember(context) {
        VoiceInputController(
            context = context,
            onListeningChanged = { listening ->
                if (isDisposed) return@VoiceInputController
                if (listening) {
                    phase = VoicePhase.LISTENING
                } else if (phase == VoicePhase.LISTENING) {
                    phase = VoicePhase.IDLE
                }
            },
            onPartialText = { partial ->
                if (!isDisposed) {
                    lastHeard = partial
                }
            },
            onFinalText = { finalText ->
                if (isDisposed) return@VoiceInputController
                lastHeard = finalText
                phase = VoicePhase.THINKING
                onSendVoicePrompt(
                    finalText,
                    { reply ->
                        if (isDisposed) return@onSendVoicePrompt
                        lastSpokenReply = reply
                        val speechText = cleanReplyForSpeech(reply)
                        val tts = ttsRef.value
                        if (ttsReady && tts != null && speechText.isNotBlank()) {
                            phase = VoicePhase.SPEAKING
                            val speakStatus = tts.speak(
                                speechText,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "mano_voice_${System.currentTimeMillis()}"
                            )
                            if (speakStatus != TextToSpeech.SUCCESS) {
                                phase = VoicePhase.IDLE
                            }
                        } else {
                            phase = VoicePhase.IDLE
                        }
                    },
                    { err ->
                        if (isDisposed) return@onSendVoicePrompt
                        errorMessage = err
                        phase = VoicePhase.IDLE
                    }
                )
            },
            onError = { err ->
                if (isDisposed) return@VoiceInputController
                errorMessage = err
                if (phase == VoicePhase.LISTENING) {
                    phase = VoicePhase.IDLE
                }
            }
        )
    }

    voiceControllerRef.value = voiceController

    DisposableEffect(voiceController) {
        onDispose {
            voiceController.destroy()
        }
    }

    // Auto-start listening if permission is already granted on open
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startListeningSafely()
        }
    }

    BackHandler {
        ttsRef.value?.stop()
        voiceController.destroy()
        onClose()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkEmeraldContainer,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.voice_mode_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MintAccent
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    ttsRef.value?.stop()
                    voiceController.destroy()
                    onClose()
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(CarbonElevated, CircleShape)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .testTag("close_voice_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.voice_mode_close_cd),
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Auto-continue pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (autoContinue) DarkEmeraldContainer else CarbonSurface,
            border = BorderStroke(1.dp, if (autoContinue) EmeraldPrimary else BorderSubtle),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { autoContinue = !autoContinue }
                .testTag("voice_auto_continue_toggle")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Loop,
                    contentDescription = null,
                    tint = if (autoContinue) EmeraldPrimary else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (autoContinue) "Conversa contínua: LIGADA" else "Conversa contínua: DESLIGADA",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (autoContinue) MintAccent else TextSecondary
                )
            }
        }

        // Center Pulsing Orb & Mano Avatar
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val compactHeight = maxHeight < 420.dp
            val orbBoxSize = if (compactHeight) 156.dp else 204.dp
            val avatarSize = if (compactHeight) 96.dp else 124.dp
            val transcriptScrollState = rememberScrollState()

            val activeReplyText = if (phase == VoicePhase.THINKING && streamingReply.isNotBlank()) {
                streamingReply
            } else {
                lastSpokenReply
            }

            LaunchedEffect(lastHeard, activeReplyText) {
                transcriptScrollState.animateScrollTo(transcriptScrollState.maxValue)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(orbBoxSize)
                ) {
                    val activePulse = phase == VoicePhase.LISTENING || phase == VoicePhase.SPEAKING
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseRadius = size.minDimension * 0.34f
                        val scale = if (activePulse) pulseScale else 1.02f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    EmeraldPrimary.copy(alpha = if (activePulse) 0.28f else 0.10f),
                                    Color.Transparent
                                )
                            ),
                            radius = baseRadius * scale * 1.38f
                        )
                        drawCircle(
                            color = EmeraldPrimary.copy(alpha = if (activePulse) 0.55f else 0.22f),
                            radius = baseRadius * scale,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.img_mano_avatar_1795262249289),
                        contentDescription = stringResource(R.string.avatar_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                            .border(2.5.dp, EmeraldPrimary, CircleShape)
                            .clickable(enabled = phase != VoicePhase.THINKING) {
                                triggerMicAction()
                            }
                    )
                }

                Spacer(modifier = Modifier.height(if (compactHeight) 6.dp else 10.dp))

                val statusTitle = when (phase) {
                    VoicePhase.IDLE -> "Toca no mic e fala, mno"
                    VoicePhase.LISTENING -> "Tô te ouvindo, mno…"
                    VoicePhase.THINKING -> "Mano tá pensando…"
                    VoicePhase.SPEAKING -> "Mano tá falando…"
                }
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = EmeraldPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(if (compactHeight) 8.dp else 14.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 84.dp, max = 180.dp)
                        .verticalScroll(transcriptScrollState)
                ) {
                    if (lastHeard.isBlank() && activeReplyText.isBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CarbonSurface,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Manda tua dúvida ou papo em português. Quando eu responder, volto a ouvir se a conversa contínua tiver ligada.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    if (lastHeard.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CarbonElevated,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tu: “$lastHeard”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (activeReplyText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CarbonSurface,
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeReplyText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ErrorContainerDark,
                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Bottom Voice Action Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            val buttonColor = when (phase) {
                VoicePhase.LISTENING -> ErrorRed
                VoicePhase.SPEAKING -> CarbonElevated
                VoicePhase.THINKING -> CarbonElevated
                VoicePhase.IDLE -> EmeraldPrimary
            }

            Surface(
                shape = CircleShape,
                color = buttonColor,
                border = BorderStroke(
                    2.dp,
                    if (phase == VoicePhase.SPEAKING) EmeraldPrimary else buttonColor
                ),
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable(enabled = phase != VoicePhase.THINKING) {
                        triggerMicAction()
                    }
                    .testTag("voice_mode_main_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (phase) {
                        VoicePhase.THINKING -> CircularProgressIndicator(
                            color = EmeraldPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        VoicePhase.LISTENING -> Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Parar de ouvir",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        VoicePhase.SPEAKING -> Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "Calar o Mano",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                        VoicePhase.IDLE -> Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Falar com o Mano",
                            tint = TextOnGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
