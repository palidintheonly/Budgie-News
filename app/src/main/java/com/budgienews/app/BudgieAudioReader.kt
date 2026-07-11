package com.budgienews.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder
import java.util.Locale

internal object BudgieAudioReader {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingTtsAction: (() -> Unit)? = null
    private var appContext: Context? = null

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private val _currentArticleId = MutableStateFlow<String?>(null)
    val currentArticleId = _currentArticleId.asStateFlow()
    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate = _speechRate.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        if (tts != null && isTtsReady) return
        if (tts != null && !isTtsReady) {
            // Currently initializing, let it finish or reset if stuck
            return
        }
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    try {
                        val availableVoices = tts?.voices
                        if (!availableVoices.isNullOrEmpty()) {
                            val bestVoice = availableVoices
                                .filter { voice ->
                                    val lang = voice.locale?.language ?: ""
                                    lang == "en" && voice.latency != Voice.LATENCY_VERY_HIGH && !voice.name.contains("espeak", ignoreCase = true)
                                }
                                .maxByOrNull { voice ->
                                    var score = 0
                                    val name = voice.name.lowercase()
                                    val country = voice.locale?.country?.uppercase().orEmpty()

                                    if (voice.isNetworkConnectionRequired || name.contains("network") || name.contains("wavenet") || name.contains("neural")) {
                                        score += 100
                                    }
                                    if (voice.quality >= Voice.QUALITY_HIGH) {
                                        score += 50
                                    }
                                    when (country) {
                                        "GB" -> score += 40
                                        "AU" -> score += 35
                                        "US" -> score += 20
                                    }
                                    if (name.contains("-rjs-") || name.contains("-gbd-") || name.contains("-gbe-") || name.contains("-afh-") || name.contains("-iom-")) {
                                        score += 30
                                    }
                                    if (name.contains("local") && voice.quality <= Voice.QUALITY_NORMAL) {
                                        score -= 30
                                    }
                                    score
                                }
                            if (bestVoice != null) {
                                tts?.voice = bestVoice
                            } else {
                                tts?.language = Locale.UK
                            }
                        } else {
                            tts?.language = Locale.UK
                        }
                    } catch (_: Exception) {
                        tts?.language = Locale.UK
                    }

                    tts?.setPitch(0.96f)

                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isPlaying.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            if (utteranceId != null && utteranceId.startsWith("article_")) {
                                _isPlaying.value = false
                                _currentArticleId.value = null
                            }
                        }

                        @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
                        override fun onError(utteranceId: String?) {
                            if (utteranceId != null && utteranceId.startsWith("article_")) {
                                _isPlaying.value = false
                                _currentArticleId.value = null
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            if (utteranceId != null && utteranceId.startsWith("article_")) {
                                _isPlaying.value = false
                                _currentArticleId.value = null
                            }
                        }
                    })

                    // Execute any speech requests that were waiting for TTS engine to bind
                    pendingTtsAction?.let { action ->
                        pendingTtsAction = null
                        handler.post(action)
                    }
                } else {
                    isTtsReady = false
                    try { tts?.shutdown() } catch (_: Exception) {}
                    tts = null
                    _isPlaying.value = false
                    _currentArticleId.value = null
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
            tts = null
            _isPlaying.value = false
            _currentArticleId.value = null
        }
    }

    fun togglePlay(context: Context, item: FeedItem) {
        init(context)
        if (_isPlaying.value && _currentArticleId.value == item.id) {
            stop()
        } else {
            stop()
            _currentArticleId.value = item.id
            _isPlaying.value = true
            val points = item.quickReadPoints().joinToString(". ")
            val fullText = "${item.title}. $points"
            playWithNeuralCloud(context, item, fullText)
        }
    }

    private fun playWithNeuralCloud(context: Context, item: FeedItem, fullText: String) {
        try {
            stopMediaPlayer()
            handler.removeCallbacksAndMessages(null)

            val cleanText = fullText.replace(" ... ", ". ").take(180)
            val encodedText = URLEncoder.encode(cleanText, "UTF-8")
            val url = "https://api.streamelements.com/kappa/v2/speech?voice=Brian&text=$encodedText"

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { player ->
                    handler.removeCallbacksAndMessages(null)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            player.playbackParams = player.playbackParams.setSpeed(_speechRate.value)
                        }
                        player.start()
                    } catch (e: Exception) {
                        playWithTts(fullText, item.id)
                    }
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentArticleId.value = null
                    stopMediaPlayer()
                }
                setOnErrorListener { _, _, _ ->
                    handler.removeCallbacksAndMessages(null)
                    stopMediaPlayer()
                    playWithTts(fullText, item.id)
                    true
                }
            }
            mediaPlayer = mp
            mp.prepareAsync()

            val runnable = Runnable {
                if (mediaPlayer == mp && _isPlaying.value && !mp.isPlaying) {
                    stopMediaPlayer()
                    playWithTts(fullText, item.id)
                }
            }
            timeoutRunnable = runnable
            handler.postDelayed(runnable, 1200L)
        } catch (e: Exception) {
            handler.removeCallbacksAndMessages(null)
            stopMediaPlayer()
            playWithTts(fullText, item.id)
        }
    }

    private fun playWithTts(fullText: String, itemId: String, retryCount: Int = 0) {
        if (tts == null || !isTtsReady) {
            if (retryCount >= 2) {
                _isPlaying.value = false
                _currentArticleId.value = null
                return
            }
            pendingTtsAction = { playWithTts(fullText, itemId, retryCount + 1) }
            if (tts == null) {
                appContext?.let { init(it) }
            }
            return
        }

        try {
            tts?.setSpeechRate(_speechRate.value)
            val sentences = fullText.split(Regex("""(?<=[.!?])\s+""")).filter { it.isNotBlank() }
            if (sentences.isEmpty()) {
                val res = tts?.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, "article_$itemId") ?: TextToSpeech.ERROR
                if (res == TextToSpeech.ERROR) {
                    handleSpeakError(fullText, itemId, retryCount)
                }
                return
            }
            var hasError = false
            sentences.forEachIndexed { index, sentence ->
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val utteranceId = if (index == sentences.lastIndex) "article_$itemId" else "chunk_${itemId}_$index"
                val res = tts?.speak(sentence.trim(), queueMode, null, utteranceId) ?: TextToSpeech.ERROR
                if (res == TextToSpeech.ERROR) {
                    hasError = true
                }
            }
            if (hasError) {
                handleSpeakError(fullText, itemId, retryCount)
            }
        } catch (_: Exception) {
            handleSpeakError(fullText, itemId, retryCount)
        }
    }

    private fun handleSpeakError(fullText: String, itemId: String, retryCount: Int) {
        if (retryCount >= 2) {
            _isPlaying.value = false
            _currentArticleId.value = null
            return
        }
        // Engine unbound or crashed, reset and re-initialize
        try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        tts = null
        isTtsReady = false
        pendingTtsAction = { playWithTts(fullText, itemId, retryCount + 1) }
        appContext?.let { init(it) }
    }

    fun cycleSpeed() {
        val nextRate = when (_speechRate.value) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            else -> 1.0f
        }
        _speechRate.value = nextRate
        tts?.setSpeechRate(nextRate)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = player.playbackParams.setSpeed(nextRate)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun stopMediaPlayer() {
        try {
            handler.removeCallbacksAndMessages(null)
            mediaPlayer?.run {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun stop() {
        stopMediaPlayer()
        try { tts?.stop() } catch (_: Exception) {}
        pendingTtsAction = null
        _isPlaying.value = false
        _currentArticleId.value = null
    }
}
