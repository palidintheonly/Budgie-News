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
    private var progressRunnable: Runnable? = null
    private var currentEstimatedDurationSec = 15

    private var activeCloudChunks: List<String> = emptyList()
    private var activeCloudChunkIndex: Int = 0
    private var totalSummaryCharCount: Int = 1
    private var charsPlayedBeforeCloudChunk: Int = 0

    private var activeTtsSentences: List<String> = emptyList()
    private var activeTtsSentenceIndex: Int = 0
    private var activeTtsSentenceStartTime: Long = 0L

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _currentArticleId = MutableStateFlow<String?>(null)
    val currentArticleId = _currentArticleId.asStateFlow()
    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate = _speechRate.asStateFlow()
    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress = _playbackProgress.asStateFlow()
    private val _remainingTimeSeconds = MutableStateFlow(0)
    val remainingTimeSeconds = _remainingTimeSeconds.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        if (tts != null && isTtsReady) return
        if (tts != null && !isTtsReady) return
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
                            if (utteranceId != null && (utteranceId.startsWith("article_") || utteranceId.startsWith("chunk_"))) {
                                _isLoading.value = false
                                _isPlaying.value = true
                                if (utteranceId.startsWith("chunk_")) {
                                    val idx = utteranceId.substringAfterLast("_").toIntOrNull() ?: 0
                                    activeTtsSentenceIndex = idx
                                    activeTtsSentenceStartTime = System.currentTimeMillis()
                                } else if (utteranceId.startsWith("article_")) {
                                    activeTtsSentenceIndex = (activeTtsSentences.size - 1).coerceAtLeast(0)
                                    activeTtsSentenceStartTime = System.currentTimeMillis()
                                }
                                if (progressRunnable == null) {
                                    handler.post { startProgressTracker(false) }
                                }
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            if (utteranceId != null && utteranceId.startsWith("article_")) {
                                stop()
                            }
                        }

                        @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
                        override fun onError(utteranceId: String?) {
                            if (utteranceId != null && utteranceId.startsWith("article_")) {
                                stop()
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            if (utteranceId != null && utteranceId.startsWith("article_")) {
                                stop()
                            }
                        }
                    })

                    pendingTtsAction?.let { action ->
                        pendingTtsAction = null
                        handler.post(action)
                    }
                } else {
                    isTtsReady = false
                    try { tts?.shutdown() } catch (_: Exception) {}
                    tts = null
                    stop()
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
            tts = null
            stop()
        }
    }

    fun togglePlay(context: Context, item: FeedItem) {
        init(context)
        if ((_isPlaying.value || _isLoading.value) && _currentArticleId.value == item.id) {
            stop()
        } else {
            stop()
            _currentArticleId.value = item.id
            _isLoading.value = true
            _isPlaying.value = false
            _playbackProgress.value = 0f
            val points = item.quickReadPoints().joinToString(". ")
            val fullText = "${item.title}. $points"
            totalSummaryCharCount = fullText.length.coerceAtLeast(1)
            currentEstimatedDurationSec = ((totalSummaryCharCount.toFloat() / 15f) / _speechRate.value).toInt().coerceAtLeast(8)
            _remainingTimeSeconds.value = currentEstimatedDurationSec

            val chunks = splitTextIntoChunks(fullText, 170)
            activeCloudChunks = chunks
            activeCloudChunkIndex = 0
            charsPlayedBeforeCloudChunk = 0
            playNextCloudChunk(context, item, fullText)
        }
    }

    private fun splitTextIntoChunks(text: String, maxLen: Int = 170): List<String> {
        val sentences = text.split(Regex("""(?<=[.!?])\s+""")).filter { it.isNotBlank() }
        val chunks = mutableListOf<String>()
        var currentChunk = ""
        for (s in sentences) {
            if ((currentChunk + " " + s).trim().length <= maxLen) {
                currentChunk = if (currentChunk.isEmpty()) s else "$currentChunk $s"
            } else {
                if (currentChunk.isNotEmpty()) chunks.add(currentChunk.trim())
                if (s.length > maxLen) {
                    val words = s.split(" ")
                    var subChunk = ""
                    for (w in words) {
                        if ((subChunk + " " + w).trim().length <= maxLen) {
                            subChunk = if (subChunk.isEmpty()) w else "$subChunk $w"
                        } else {
                            if (subChunk.isNotEmpty()) chunks.add(subChunk.trim())
                            subChunk = w
                        }
                    }
                    currentChunk = subChunk
                } else {
                    currentChunk = s
                }
            }
        }
        if (currentChunk.isNotEmpty()) chunks.add(currentChunk.trim())
        return if (chunks.isEmpty()) listOf(text.take(maxLen)) else chunks
    }

    private fun startProgressTracker(isMediaPlayer: Boolean) {
        handler.removeCallbacks(progressRunnable ?: Runnable {})
        val run = object : Runnable {
            override fun run() {
                if (_isPlaying.value) {
                    if (isMediaPlayer && mediaPlayer != null && mediaPlayer!!.isPlaying) {
                        val pos = mediaPlayer!!.currentPosition
                        val dur = mediaPlayer!!.duration
                        if (dur > 0 && activeCloudChunks.isNotEmpty()) {
                            val chunkProgress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                            val currentChunkLen = activeCloudChunks.getOrNull(activeCloudChunkIndex)?.length ?: 150
                            val charsDone = charsPlayedBeforeCloudChunk + chunkProgress * currentChunkLen
                            val overallProgress = (charsDone / totalSummaryCharCount.toFloat()).coerceIn(0f, 0.995f)
                            _playbackProgress.value = overallProgress
                            val remSec = ((1f - overallProgress) * currentEstimatedDurationSec).toInt().coerceAtLeast(0)
                            _remainingTimeSeconds.value = remSec
                        }
                        handler.postDelayed(this, 100L)
                    } else if (!isMediaPlayer && activeTtsSentences.isNotEmpty()) {
                        val elapsedInSentenceSec = ((System.currentTimeMillis() - activeTtsSentenceStartTime) / 1000f) * _speechRate.value
                        val currentSentenceLen = activeTtsSentences.getOrNull(activeTtsSentenceIndex)?.length ?: 50
                        val sentenceDurationSec = (currentSentenceLen.toFloat() / 15f).coerceAtLeast(0.5f)
                        val progInSentence = (elapsedInSentenceSec / sentenceDurationSec).coerceIn(0f, 1f)
                        val charsBefore = activeTtsSentences.take(activeTtsSentenceIndex).sumOf { it.length }
                        val charsDone = charsBefore + progInSentence * currentSentenceLen
                        val overallProgress = (charsDone / totalSummaryCharCount.toFloat()).coerceIn(0f, 0.995f)
                        _playbackProgress.value = overallProgress
                        val remSec = ((1f - overallProgress) * currentEstimatedDurationSec).toInt().coerceAtLeast(0)
                        _remainingTimeSeconds.value = remSec
                        handler.postDelayed(this, 150L)
                    }
                }
            }
        }
        progressRunnable = run
        handler.post(run)
    }

    private fun playNextCloudChunk(context: Context, item: FeedItem, fullText: String) {
        if (activeCloudChunkIndex >= activeCloudChunks.size) {
            stop()
            return
        }
        val chunkText = activeCloudChunks[activeCloudChunkIndex]
        try {
            stopMediaPlayer()
            handler.removeCallbacksAndMessages(null)

            val encodedText = URLEncoder.encode(chunkText, "UTF-8")
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
                        if (activeCloudChunkIndex == 0) {
                            val firstChunkSec = player.duration / 1000f
                            val scaledSec = (firstChunkSec * (totalSummaryCharCount.toFloat() / chunkText.length.coerceAtLeast(1).toFloat())).toInt().coerceAtLeast(6)
                            currentEstimatedDurationSec = scaledSec
                            _remainingTimeSeconds.value = scaledSec
                        }
                        _isLoading.value = false
                        _isPlaying.value = true
                        player.start()
                        startProgressTracker(true)
                    } catch (e: Exception) {
                        playWithTts(fullText, item.id)
                    }
                }
                setOnCompletionListener {
                    charsPlayedBeforeCloudChunk += activeCloudChunks.getOrNull(activeCloudChunkIndex)?.length ?: 0
                    activeCloudChunkIndex++
                    if (activeCloudChunkIndex < activeCloudChunks.size) {
                        playNextCloudChunk(context, item, fullText)
                    } else {
                        stop()
                    }
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
                if (mediaPlayer == mp && (_isLoading.value || _isPlaying.value) && !mp.isPlaying) {
                    stopMediaPlayer()
                    playWithTts(fullText, item.id)
                }
            }
            timeoutRunnable = runnable
            handler.postDelayed(runnable, 2000L)
        } catch (e: Exception) {
            handler.removeCallbacksAndMessages(null)
            stopMediaPlayer()
            playWithTts(fullText, item.id)
        }
    }

    private fun playWithTts(fullText: String, itemId: String, retryCount: Int = 0) {
        if (tts == null || !isTtsReady) {
            if (retryCount >= 2) {
                stop()
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
            activeTtsSentences = sentences
            activeTtsSentenceIndex = 0
            activeTtsSentenceStartTime = System.currentTimeMillis()
            totalSummaryCharCount = fullText.length.coerceAtLeast(1)
            currentEstimatedDurationSec = ((totalSummaryCharCount.toFloat() / 15f) / _speechRate.value).toInt().coerceAtLeast(6)
            _remainingTimeSeconds.value = currentEstimatedDurationSec

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
            stop()
            return
        }
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
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
        pendingTtsAction = null
        activeCloudChunks = emptyList()
        activeCloudChunkIndex = 0
        activeTtsSentences = emptyList()
        activeTtsSentenceIndex = 0
        _isPlaying.value = false
        _isLoading.value = false
        _playbackProgress.value = 0f
        _remainingTimeSeconds.value = 0
        _currentArticleId.value = null
    }
}
