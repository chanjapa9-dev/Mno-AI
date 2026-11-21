package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ManoAiRepository
import com.example.data.ChatMessageEntity
import com.example.data.ChatSessionEntity
import com.example.data.ImageStorageHelper
import com.example.data.ManoDatabase
import com.example.data.ManoLocalRepository
import com.example.data.NorthMythEntity
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ManoViewModel"

enum class ManoTab {
    CHAT, MYTHS, HISTORY
}

sealed interface MythGenUiState {
    data object Idle : MythGenUiState
    data object Loading : MythGenUiState
    data class Success(val myth: NorthMythEntity) : MythGenUiState
    data class Error(val message: String) : MythGenUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ManoViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepo = ManoLocalRepository(ManoDatabase.getInstance(application).manoDao())
    private val aiRepo = ManoAiRepository()

    private val _activeTab = MutableStateFlow(ManoTab.CHAT)
    val activeTab: StateFlow<ManoTab> = _activeTab.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = localRepo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList())
            else localRepo.observeMessages(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val myths: StateFlow<List<NorthMythEntity>> = localRepo.observeMyths()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isImageMode = MutableStateFlow(false)
    val isImageMode: StateFlow<Boolean> = _isImageMode.asStateFlow()

    private val _attachedImagePath = MutableStateFlow<String?>(null)
    val attachedImagePath: StateFlow<String?> = _attachedImagePath.asStateFlow()

    private val _isSendingText = MutableStateFlow(false)
    val isSendingText: StateFlow<Boolean> = _isSendingText.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _streamingReply = MutableStateFlow("")
    val streamingReply: StateFlow<String> = _streamingReply.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _mythState = MutableStateFlow<MythGenUiState>(MythGenUiState.Idle)
    val mythState: StateFlow<MythGenUiState> = _mythState.asStateFlow()

    private val _illustratingMythId = MutableStateFlow<Long?>(null)
    val illustratingMythId: StateFlow<Long?> = _illustratingMythId.asStateFlow()

    private val _mythImageError = MutableStateFlow<String?>(null)
    val mythImageError: StateFlow<String?> = _mythImageError.asStateFlow()

    init {
        seedInitialMythsIfNeeded()
    }

    private fun seedInitialMythsIfNeeded() {
        val prefs = getApplication<Application>().getSharedPreferences("mano_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded_initial_myths_v1", false)) return

        viewModelScope.launch {
            val baseTime = System.currentTimeMillis()
            val initialMyths = listOf(
                NorthMythEntity(
                    title = "1. O Curupira do Ramal",
                    entityName = "Curupira",
                    setting = "Ramal na mata",
                    story = "Mno, tinha um cara que vivia caçando na mata sem respeito. Um dia ouviu assovio e viu um menino de pé virado. Era o Curupira, mno. O cara correu, se perdeu, e voltou 3 dias depois sem lembrar de nada.",
                    catchphrase = "Moral, mno: mata não é bagunça.",
                    createdAt = baseTime - 1_000L
                ),
                NorthMythEntity(
                    title = "2. O Boto da Festa",
                    entityName = "Boto-Cor-de-Rosa",
                    setting = "Festa na beira do rio",
                    story = "Mno, na festa da beira chegou um cara de branco, bonito, dançando tudo. Quando a moça foi ver, ele tinha sumido e só tinha pegada molhada até o rio. Era o boto, mno.",
                    catchphrase = "Moral, mno: nem todo bonito é gente.",
                    createdAt = baseTime - 2_000L
                ),
                NorthMythEntity(
                    title = "3. A Cobra Grande",
                    entityName = "Cobra Grande",
                    setting = "Beiradão do rio à noite",
                    story = "Mno, os pescador diziam que o rio tremia de noite. Era a Cobra Grande passando por baixo do barco. Quem olha no olho dela, trava.",
                    catchphrase = "Moral, mno: rio tem dono, respeita.",
                    createdAt = baseTime - 3_000L
                ),
                NorthMythEntity(
                    title = "4. A Matinta Pereira",
                    entityName = "Matinta Pereira",
                    setting = "Quintal de madrugada",
                    story = "Mno, de noite ouviu um assovio \"matinta... matinta\". Era a velha que vira pássaro. Quem não dá tabaco, ela fica assoviando até de manhã.",
                    catchphrase = "Moral, mno: não mexe com quem assovia de noite.",
                    createdAt = baseTime - 4_000L
                ),
                NorthMythEntity(
                    title = "5. A Iara do Igarapé",
                    entityName = "Iara",
                    setting = "Igarapé",
                    story = "Mno, o menino foi tomar banho no igarapé e ouviu uma voz bonita chamando. Era a Iara, mno, cabelo comprido, cantando. Ele quase foi, mas o avô puxou ele.",
                    catchphrase = "Moral, mno: voz bonita no rio é cilada.",
                    createdAt = baseTime - 5_000L
                ),
                NorthMythEntity(
                    title = "6. O Mapinguari",
                    entityName = "Mapinguari",
                    setting = "Mata fechada",
                    story = "Mno, o caçador entrou fundo na mata e sentiu um cheiro podre. Era o Mapinguari, peludo, com boca na barriga. Ele correu tanto que deixou até a espingarda.",
                    catchphrase = "Moral, mno: mata funda não é pra curioso.",
                    createdAt = baseTime - 6_000L
                ),
                NorthMythEntity(
                    title = "7. A Vitória-Régia",
                    entityName = "Vitória-Régia",
                    setting = "Rio sob a lua",
                    story = "Mno, tinha uma índia que amava a lua, a Jaci. Ela correu pra abraçar a lua no reflexo do rio e se afogou. A Jaci com pena, virou ela em Vitória-Régia.",
                    catchphrase = "Moral, mno: amor doido também vira lenda.",
                    createdAt = baseTime - 7_000L
                ),
                NorthMythEntity(
                    title = "8. O Boto e a Menina",
                    entityName = "Boto-Cor-de-Rosa",
                    setting = "Beira do rio",
                    story = "Mno, a menina ia toda noite pra beira esperar o boto. Um dia ela sumiu e só acharam o chapéu branco boiando. Dizem que ela virou sereia do rio.",
                    catchphrase = "Moral, mno: quem se apaixona por boto, some.",
                    createdAt = baseTime - 8_000L
                ),
                NorthMythEntity(
                    title = "9. O Curupira e o Caçador Mentiroso",
                    entityName = "Curupira",
                    setting = "Trilha fechada",
                    story = "Mno, o caçador dizia que matava onça com a mão. O Curupira ouviu e fez ele andar em círculo a noite toda. De manhã ele tava no mesmo lugar, com o pé todo arranhado.",
                    catchphrase = "Moral, mno: mentiroso na mata se perde sozinho.",
                    createdAt = baseTime - 9_000L
                ),
                NorthMythEntity(
                    title = "10. A Cobra Grande de Manaus",
                    entityName = "Cobra Grande",
                    setting = "Subsolo e rio de Manaus",
                    story = "Mno, dizem que embaixo de Manaus dorme uma Cobra Grande gigante. Quando ela se mexe, a terra treme um pouco. Por isso o povo respeita o rio.",
                    catchphrase = "Moral, mno: Manaus tá em cima de lenda, não de asfalto só.",
                    createdAt = baseTime - 10_000L
                )
            )
            initialMyths.forEach { localRepo.saveMyth(it) }
            prefs.edit().putBoolean("seeded_initial_myths_v1", true).apply()
        }
    }

    fun selectTab(tab: ManoTab) {
        _activeTab.value = tab
    }

    fun toggleImageMode() {
        _isImageMode.value = !_isImageMode.value
    }

    fun setImageMode(enabled: Boolean) {
        _isImageMode.value = enabled
    }

    fun attachImageFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val (path, bitmap) = ImageStorageHelper.copyUriToAppFilesAndDecode(
                    getApplication(),
                    uri
                )
                if (bitmap != null && path.isNotEmpty()) {
                    _attachedImagePath.value = path
                } else {
                    _chatError.value = "Não deu pra abrir essa foto, mno. Tenta outra."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to attach image", e)
                _chatError.value = e.localizedMessage ?: "Erro ao anexar imagem."
            }
        }
    }

    fun clearAttachedImage() {
        _attachedImagePath.value = null
    }

    fun startNewChat() {
        _currentSessionId.value = null
        _streamingReply.value = ""
        _chatError.value = null
        _attachedImagePath.value = null
        _activeTab.value = ManoTab.CHAT
    }

    fun openSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        _streamingReply.value = ""
        _chatError.value = null
        _activeTab.value = ManoTab.CHAT
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            localRepo.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            localRepo.clearAllSessions()
            _currentSessionId.value = null
        }
    }

    fun dismissChatError() {
        _chatError.value = null
    }

    fun dismissMythImageError() {
        _mythImageError.value = null
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        val imagePath = _attachedImagePath.value
        if (prompt.isEmpty() && imagePath == null) return
        if (_isSendingText.value || _isGeneratingImage.value) return

        _chatError.value = null
        _attachedImagePath.value = null

        val shouldGenerateImage = _isImageMode.value ||
            (prompt.isNotEmpty() && aiRepo.detectImageGenerationIntent(prompt))

        viewModelScope.launch {
            val appContext = getApplication<Application>()
            val existingSessionId = _currentSessionId.value
            val sessionId = if (existingSessionId != null) {
                existingSessionId
            } else {
                val title = prompt.ifEmpty { "Análise de imagem" }.take(40)
                val newId = localRepo.createSession(title = title, preview = prompt)
                _currentSessionId.value = newId
                newId
            }

            localRepo.addMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    isUser = true,
                    text = prompt.ifEmpty { "Analisa essa imagem aí, mno." },
                    imagePath = imagePath
                )
            )
            localRepo.updateSessionPreview(
                sessionId = sessionId,
                preview = prompt.ifEmpty { "📷 Imagem enviada" }.take(80)
            )

            val attachedBitmap = imagePath?.let { ImageStorageHelper.loadBitmapFromPath(it) }

            if (shouldGenerateImage) {
                _isGeneratingImage.value = true
                try {
                    val imagePrompt = prompt.removePrefix("/img").removePrefix("/imagem").trim()
                        .ifEmpty { "Transforma essa imagem com estilo vibrante e acabamento profissional" }
                    val generatedBitmap = aiRepo.generateImage(
                        prompt = imagePrompt,
                        inputBitmap = attachedBitmap
                    )
                    val savedPath = ImageStorageHelper.saveBitmapToAppFiles(
                        context = appContext,
                        bitmap = generatedBitmap,
                        prefix = "mano_art"
                    )
                    val replyText = "Tá na mão, mno! Olha a arte que eu mandei ver:"
                    localRepo.addMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            isUser = false,
                            text = replyText,
                            imagePath = savedPath,
                            isImageGeneration = true
                        )
                    )
                    localRepo.updateSessionPreview(sessionId, "🎨 Imagem gerada")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Image generation error", e)
                    val errorMsg = e.localizedMessage ?: "Falha ao gerar imagem."
                    _chatError.value = errorMsg
                    localRepo.addMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            isUser = false,
                            text = "Deu ruim pra gerar a imagem, mno: $errorMsg",
                            isError = true
                        )
                    )
                } finally {
                    _isGeneratingImage.value = false
                    _isImageMode.value = false
                }
            } else {
                _isSendingText.value = true
                _streamingReply.value = ""
                try {
                    val snapshot = localRepo.getMessagesSnapshot(sessionId)
                    // Exclude the newly inserted final user message and any error bubbles
                    val historyMessages = snapshot
                        .dropLast(1)
                        .filter { !it.isError && it.text.isNotBlank() }
                        .takeLast(20)
                        .map { msg ->
                            content(if (msg.isUser) "user" else "model") {
                                text(msg.text)
                            }
                        }

                    var finalReply = ""
                    aiRepo.streamChatReply(
                        history = historyMessages,
                        prompt = prompt,
                        attachedImage = attachedBitmap
                    ).collect { partial ->
                        finalReply = partial
                        _streamingReply.value = partial
                    }

                    if (finalReply.isNotBlank()) {
                        localRepo.addMessage(
                            ChatMessageEntity(
                                sessionId = sessionId,
                                isUser = false,
                                text = finalReply
                            )
                        )
                        localRepo.updateSessionPreview(sessionId, finalReply.take(80))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Chat error", e)
                    val errorMsg = e.localizedMessage ?: "Erro ao conversar com o Mano AI."
                    _chatError.value = errorMsg
                    localRepo.addMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            isUser = false,
                            text = "Eita mno, travou aqui: $errorMsg",
                            isError = true
                        )
                    )
                } finally {
                    _streamingReply.value = ""
                    _isSendingText.value = false
                }
            }
        }
    }

    fun sendVoicePrompt(
        transcript: String,
        onReplyReady: (String) -> Unit,
        onReplyError: (String) -> Unit
    ) {
        val prompt = transcript.trim()
        if (prompt.isEmpty()) {
            onReplyError("Não peguei o que tu falou, mno. Tenta de novo.")
            return
        }
        if (_isSendingText.value || _isGeneratingImage.value) {
            onReplyError("Mano ainda tá respondendo outra parada, espera um segundo.")
            return
        }

        _chatError.value = null
        viewModelScope.launch {
            val existingSessionId = _currentSessionId.value
            val sessionId = if (existingSessionId != null) {
                existingSessionId
            } else {
                val newId = localRepo.createSession(
                    title = "Voz: ${prompt.take(34)}",
                    preview = prompt
                )
                _currentSessionId.value = newId
                newId
            }

            localRepo.addMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    isUser = true,
                    text = prompt
                )
            )
            localRepo.updateSessionPreview(sessionId, prompt.take(80))

            _isSendingText.value = true
            _streamingReply.value = ""
            try {
                val snapshot = localRepo.getMessagesSnapshot(sessionId)
                val historyMessages = snapshot
                    .dropLast(1)
                    .filter { !it.isError && it.text.isNotBlank() }
                    .takeLast(20)
                    .map { msg ->
                        content(if (msg.isUser) "user" else "model") {
                            text(msg.text)
                        }
                    }

                var finalReply = ""
                aiRepo.streamChatReply(
                    history = historyMessages,
                    prompt = prompt,
                    attachedImage = null
                ).collect { partial ->
                    finalReply = partial
                    _streamingReply.value = partial
                }

                if (finalReply.isNotBlank()) {
                    localRepo.addMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            isUser = false,
                            text = finalReply
                        )
                    )
                    localRepo.updateSessionPreview(sessionId, finalReply.take(80))
                    onReplyReady(finalReply)
                } else {
                    onReplyError("Mano ficou sem resposta agora, mno.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice chat error", e)
                val errorMsg = e.localizedMessage ?: "Erro ao responder por voz."
                _chatError.value = errorMsg
                onReplyError(errorMsg)
            } finally {
                _streamingReply.value = ""
                _isSendingText.value = false
            }
        }
    }

    fun generateMyth(topic: String) {
        if (_mythState.value is MythGenUiState.Loading) return
        _mythImageError.value = null

        viewModelScope.launch {
            _mythState.value = MythGenUiState.Loading
            try {
                val generated = aiRepo.generateNorthMyth(topic)
                val entity = NorthMythEntity(
                    title = generated.title,
                    entityName = generated.entityName,
                    setting = generated.setting,
                    story = generated.story,
                    catchphrase = generated.catchphrase
                )
                val insertedId = localRepo.saveMyth(entity)
                _mythState.value = MythGenUiState.Success(entity.copy(id = insertedId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Myth generation error", e)
                _mythState.value = MythGenUiState.Error(
                    e.localizedMessage ?: "Não deu pra gerar a lenda agora, mno."
                )
            }
        }
    }

    fun illustrateMyth(myth: NorthMythEntity) {
        if (_illustratingMythId.value != null) return
        _mythImageError.value = null

        viewModelScope.launch {
            _illustratingMythId.value = myth.id
            try {
                val prompt = "Ilustração atmosférica e cinematográfica de folclore amazônico brasileiro: " +
                    "${myth.entityName} em ${myth.setting}. Luz noturna verde esmeralda na floresta e rio, " +
                    "estilo arte digital detalhada sem texto na imagem."
                val bitmap = aiRepo.generateImage(prompt = prompt)
                val path = ImageStorageHelper.saveBitmapToAppFiles(
                    context = getApplication(),
                    bitmap = bitmap,
                    prefix = "mito_norte"
                )
                localRepo.updateMythImage(myth.id, path)

                val currentSuccess = _mythState.value as? MythGenUiState.Success
                if (currentSuccess?.myth?.id == myth.id) {
                    _mythState.value = MythGenUiState.Success(currentSuccess.myth.copy(imagePath = path))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Myth illustration error", e)
                _mythImageError.value = e.localizedMessage ?: "Erro ao ilustrar a lenda."
            } finally {
                _illustratingMythId.value = null
            }
        }
    }

    fun deleteMyth(mythId: Long) {
        viewModelScope.launch {
            localRepo.deleteMyth(mythId)
            val current = _mythState.value as? MythGenUiState.Success
            if (current?.myth?.id == mythId) {
                _mythState.value = MythGenUiState.Idle
            }
        }
    }

    fun sendMythToChat(myth: NorthMythEntity) {
        viewModelScope.launch {
            val sessionId = localRepo.createSession(
                title = "Lenda: ${myth.entityName}".take(40),
                preview = myth.title
            )
            localRepo.addMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    isUser = false,
                    text = "**${myth.title}** (${myth.entityName} • ${myth.setting})\n\n${myth.story}\n\n_${myth.catchphrase}_",
                    imagePath = myth.imagePath
                )
            )
            _currentSessionId.value = sessionId
            _activeTab.value = ManoTab.CHAT
        }
    }
}
