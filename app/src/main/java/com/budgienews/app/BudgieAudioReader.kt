package com.budgienews.app

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

internal object BudgieAudioReader {
    private var tts: TextToSpeech? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private val _currentArticleId = MutableStateFlow<String?>(null)
    val currentArticleId = _currentArticleId.asStateFlow()
    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate = _speechRate.asStateFlow()

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.UK
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isPlaying.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isPlaying.value = false
                        _currentArticleId.value = null
                    }
                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                        _currentArticleId.value = null
                    }
                })
            }
        }
    }

    fun togglePlay(context: Context, item: FeedItem) {
        init(context)
        if (_isPlaying.value && _currentArticleId.value == item.id) {
            stop()
        } else {
            _currentArticleId.value = item.id
            _isPlaying.value = true
            val points = item.quickReadPoints().joinToString(". ")
            val fullText = "${item.title}. $points"
            tts?.setSpeechRate(_speechRate.value)
            tts?.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, "article_${item.id}")
        }
    }

    fun cycleSpeed() {
        val nextRate = when (_speechRate.value) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            else -> 1.0f
        }
        _speechRate.value = nextRate
        tts?.setSpeechRate(nextRate)
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentArticleId.value = null
    }
}
