package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ManoAiRepository"

data class GeneratedMyth(
    val title: String,
    val entityName: String,
    val setting: String,
    val story: String,
    val catchphrase: String
)

class ManoAiRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(ManoPersona.CHAT_SYSTEM_INSTRUCTION)
            }
        )
    }

    private val mythSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Título curto e chamativo da lenda no estilo do mno"),
                "entityName" to Schema.string(description = "Nome do mito, criatura ou visagem amazônica"),
                "setting" to Schema.string(description = "Cenário curto no Norte do Brasil, ex: Igarapé do Tarumã, Rio Negro"),
                "story" to Schema.string(description = "Narrativa curta (3 a 5 parágrafos curtos), direta, sem enrolação, usando 'mno' e linguagem do Norte"),
                "catchphrase" to Schema.string(description = "Frase final ou moral curta no estilo direto do mno")
            )
        )
    }

    private val mythModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = mythSchema
            },
            systemInstruction = content {
                text(ManoPersona.MYTH_SYSTEM_INSTRUCTION)
            }
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun streamChatReply(
        history: List<Content>,
        prompt: String,
        attachedImage: Bitmap? = null
    ): Flow<String> = flow {
        val chat = chatModel.startChat(history = history)
        val userContent = content {
            if (attachedImage != null) {
                image(attachedImage)
            }
            if (prompt.isNotBlank()) {
                text(prompt)
            } else if (attachedImage != null) {
                text("Analisa essa imagem aí e manda o papo reto, mno.")
            }
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(userContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("O modelo retornou uma resposta vazia.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat streaming timed out", e)
            throw IllegalStateException("Demorou mais de 60s pra responder, mno. Tenta de novo.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat streaming failed", e)
            throw e
        }
    }

    suspend fun generateImage(
        prompt: String,
        inputBitmap: Bitmap? = null
    ): Bitmap {
        try {
            val response = withTimeout(60_000L) {
                if (inputBitmap != null) {
                    imageModel.generateContent(
                        content {
                            image(inputBitmap)
                            text(prompt)
                        }
                    )
                } else {
                    imageModel.generateContent(prompt)
                }
            }

            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()

            return imagePart?.image
                ?: throw IllegalStateException("Não veio nenhuma imagem na resposta do modelo.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("A geração da imagem passou de 60s, mno. Tenta um pedido mais direto.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw e
        }
    }

    suspend fun generateNorthMyth(topic: String): GeneratedMyth {
        val cleanTopic = topic.trim().ifEmpty {
            "uma lenda clássica ou visagem misteriosa da Amazônia contada na hora"
        }
        val prompt = """
            Cria uma lenda curta inspirada no Norte do Brasil sobre: $cleanTopic.
            Lembra: narração 100% na persona do Mano AI (mno do Norte, direto, sem enrolação, tom de parça +17 com humor e respeito às raízes amazônicas).
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                mythModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("Modelo não retornou o JSON da lenda.")
            val json = JSONObject(rawJson)
            return GeneratedMyth(
                title = json.getString("title"),
                entityName = json.getString("entityName"),
                setting = json.getString("setting"),
                story = json.getString("story"),
                catchphrase = json.getString("catchphrase")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Myth generation timed out", e)
            throw IllegalStateException("Passou de 60s pra puxar essa lenda, mno. Tenta de novo.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Myth generation failed", e)
            throw e
        }
    }

    fun detectImageGenerationIntent(prompt: String): Boolean {
        val lower = prompt.lowercase().trim()
        val verbs = listOf(
            "gera", "gerar", "gere", "cria", "criar", "crie",
            "faz", "fazer", "faça", "desenha", "desenhar", "desenhe",
            "monta", "montar", "ilustra", "ilustrar", "ilustre",
            "edita", "editar", "transforma", "transformar"
        )
        val nouns = listOf(
            "imagem", "foto", "arte", "logo", "logotipo", "capa",
            "banner", "avatar", "desenho", "anime", "ilustração",
            "ilustracao", "wallpaper", "papel de parede", "poster", "pôster",
            "thumbnail", "icone", "ícone", "mascote", "figurinha", "sticker"
        )
        val hasVerb = verbs.any { lower.contains(it) }
        val hasNoun = nouns.any { lower.contains(it) }
        if (hasVerb && hasNoun) return true

        val directPrefixes = listOf(
            "/img", "/imagem", "imagem de ", "foto de ", "logo de ",
            "logo para ", "capa de ", "capa para ", "arte de ", "estilo anime "
        )
        return directPrefixes.any { lower.startsWith(it) }
    }
}
