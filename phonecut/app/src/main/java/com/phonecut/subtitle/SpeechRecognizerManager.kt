package com.phonecut.subtitle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognizerManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var onResultListener: ((List<SubtitleItem>) -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null
    private var onProgressListener: ((Float) -> Unit)? = null
    
    private val recognizedSubtitles = mutableListOf<SubtitleItem>()
    private var startTimeMs: Long = 0

    fun setOnResultListener(listener: (List<SubtitleItem>) -> Unit) {
        onResultListener = listener
    }

    fun setOnErrorListener(listener: (String) -> Unit) {
        onErrorListener = listener
    }

    fun setOnProgressListener(listener: (Float) -> Unit) {
        onProgressListener = listener
    }

    fun startRecognition(language: String = Locale.getDefault().language) {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorListener?.invoke("语音识别不可用")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                startTimeMs = System.currentTimeMillis()
                recognizedSubtitles.clear()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频错误"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "未匹配到语音"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
                    else -> "未知错误"
                }
                onErrorListener?.invoke(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    processRecognizedText(matches[0])
                }
                onResultListener?.invoke(recognizedSubtitles.toList())
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val elapsedMs = System.currentTimeMillis() - startTimeMs
                    onProgressListener?.invoke(elapsedMs / 1000f)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        speechRecognizer?.startListening(intent)
    }

    fun stopRecognition() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    fun destroy() {
        stopRecognition()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun processRecognizedText(text: String) {
        val sentences = text.split("[。！？.!?\\n]+".toRegex()).filter { it.isNotBlank() }
        var currentTime = 0L
        val durationPerChar = 200L

        for (sentence in sentences) {
            val trimmed = sentence.trim()
            if (trimmed.isNotEmpty()) {
                val duration = (trimmed.length * durationPerChar).coerceAtLeast(2000L)
                val subtitle = SubtitleItem(
                    text = trimmed,
                    startTimeMs = currentTime,
                    endTimeMs = currentTime + duration
                )
                recognizedSubtitles.add(subtitle)
                currentTime += duration
            }
        }
    }

    fun isListening(): Boolean = isListening
}
