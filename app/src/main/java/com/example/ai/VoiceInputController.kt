package com.example.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceInputController(
    private val context: Context,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit = {},
    private val onNoResults: () -> Unit = {},
    private val onErrorCode: (Int, String) -> Unit = { _, _ -> }
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val msg = "Reconhecimento de voz não disponível nesse aparelho, mno."
            onError(msg)
            onErrorCode(SpeechRecognizer.ERROR_CLIENT, msg)
            return
        }

        destroy()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningChanged(true)
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) {
                this@VoiceInputController.onRmsChanged(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                onListeningChanged(false)
            }

            override fun onError(error: Int) {
                onListeningChanged(false)
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erro no áudio do microfone, mno."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Precisa liberar o microfone nas permissões, mno."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sem conexão pro reconhecimento de voz agora."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi o que tu falou, mno. Tenta de novo."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Não ouvi nada, mno. Toca no mic e fala perto."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Microfone ocupado, tenta em 1 segundo."
                    else -> "Deu erro no reconhecimento de voz ($error)."
                }
                if (error != SpeechRecognizer.ERROR_CLIENT) {
                    onError(message)
                    onErrorCode(error, message)
                }
            }

            override fun onResults(results: Bundle?) {
                onListeningChanged(false)
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (!text.isNullOrEmpty()) {
                    onFinalText(text)
                } else {
                    onNoResults()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (!text.isNullOrEmpty()) {
                    onPartialText(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        onListeningChanged(true)
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        onListeningChanged(false)
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        onListeningChanged(false)
    }
}
