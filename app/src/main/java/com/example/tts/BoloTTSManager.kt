package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class BoloTTSManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val onDoneCallbacks = java.util.concurrent.ConcurrentHashMap<String, () -> Unit>()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    data class VoiceInfo(
        val name: String,
        val locale: Locale,
        val isNetworkConnectionRequired: Boolean
    )

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.e("BoloTTSManager", "Failed to initialize TTS", e)
            _errorMessage.value = "Text-to-Speech initialization failed."
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                // Configure audio attributes for alarm voice
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        utteranceId?.let { id ->
                            onDoneCallbacks.remove(id)?.invoke()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    @Suppress("DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        utteranceId?.let { id -> onDoneCallbacks.remove(id) }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        utteranceId?.let { id -> onDoneCallbacks.remove(id) }
                        Log.w("BoloTTSManager", "Utterance error: $errorCode")
                    }
                })

                refreshVoices()
                _isReady.value = true
                _errorMessage.value = null
            }
        } else {
            _isReady.value = false
            _errorMessage.value = "Text-to-Speech is not available on this device."
            Log.e("BoloTTSManager", "TTS Init failed with code: $status")
        }
    }

    fun refreshVoices() {
        tts?.let { engine ->
            try {
                val voices = engine.voices?.map { voice ->
                    VoiceInfo(
                        name = voice.name,
                        locale = voice.locale,
                        isNetworkConnectionRequired = voice.isNetworkConnectionRequired
                    )
                } ?: emptyList()
                _availableVoices.value = voices
            } catch (e: Exception) {
                Log.w("BoloTTSManager", "Could not fetch voices", e)
            }
        }
    }

    fun speak(
        text: String,
        language: String = "hi",
        speechSpeed: Float = 1.0f,
        speechPitch: Float = 1.0f,
        voiceName: String = "",
        onDone: (() -> Unit)? = null
    ) {
        val engine = tts
        if (engine == null || !_isReady.value) {
            _errorMessage.value = "Text-to-Speech engine is preparing..."
            return
        }

        try {
            // Apply language
            val locale = when {
                language.equals("en", ignoreCase = true) -> Locale.ENGLISH
                language.equals("hi", ignoreCase = true) -> Locale.forLanguageTag("hi-IN")
                else -> Locale.getDefault()
            }

            val langResult = engine.setLanguage(locale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback gracefully to device default language or English if Hindi is missing
                Log.w("BoloTTSManager", "Language $locale not supported, falling back to English")
                engine.language = Locale.ENGLISH
            }

            // Apply specific voice if selected and available
            if (voiceName.isNotBlank()) {
                val matchedVoice = engine.voices?.find { it.name == voiceName }
                if (matchedVoice != null) {
                    engine.voice = matchedVoice
                }
            }

            // Apply speech rate & pitch
            engine.setSpeechRate(speechSpeed.coerceIn(0.5f, 2.0f))
            engine.setPitch(speechPitch.coerceIn(0.5f, 2.0f))

            val utteranceId = "BoloUtterance_${System.currentTimeMillis()}"
            if (onDone != null) {
                onDoneCallbacks[utteranceId] = onDone
            }

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            Log.e("BoloTTSManager", "Error in speak", e)
            _errorMessage.value = "Speech playback failed."
        }
    }

    fun stop() {
        try {
            onDoneCallbacks.clear()
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e("BoloTTSManager", "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            onDoneCallbacks.clear()
            tts?.stop()
            tts?.shutdown()
            tts = null
            _isReady.value = false
        } catch (e: Exception) {
            Log.e("BoloTTSManager", "Error shutting down TTS", e)
        }
    }
}
