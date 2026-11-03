package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.domain.model.EqualizerBand
import com.example.domain.model.EqualizerState
import com.example.domain.model.SolarTrack
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatModeState {
    OFF,
    ALL,
    ONE
}

data class PlaybackUiState(
    val currentTrack: SolarTrack? = null,
    val queue: List<SolarTrack> = emptyList(),
    val currentQueueIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val playbackSpeed: Float = 1.0f,
    val volumeLevel: Float = 1.0f,
    val sleepTimerRemainingMinutes: Int? = null,
    val audioOutputName: String = "Device Stereo Speaker / Bluetooth",
    val errorMessage: String? = null
)

class SolarAudioController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onTrackPlayed: (SolarTrack) -> Unit
) {
    private var player: Player? = null
    private var localFallbackPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private var positionPollJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    init {
        initializeController()
        startPositionTicker()
    }

    private fun initializeController() {
        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, SolarMediaSessionService::class.java)
            )
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener(
                {
                    try {
                        val mediaController = controllerFuture.get()
                        attachPlayer(mediaController)
                    } catch (_: Exception) {
                        ensureFallbackExoPlayer()
                    }
                },
                MoreExecutors.directExecutor()
            )
        } catch (_: Exception) {
            ensureFallbackExoPlayer()
        }
    }

    private fun ensureFallbackExoPlayer(): Player {
        player?.let { return it }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val exo = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        localFallbackPlayer = exo
        attachPlayer(exo)
        bindAudioEffects(exo.audioSessionId)
        return exo
    }

    private fun attachPlayer(targetPlayer: Player) {
        player = targetPlayer
        if (targetPlayer is ExoPlayer) {
            bindAudioEffects(targetPlayer.audioSessionId)
        }
        targetPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                val dur = targetPlayer.duration.takeIf { it > 0 } ?: _playbackState.value.currentTrack?.durationMs ?: 0L
                _playbackState.update {
                    it.copy(
                        isBuffering = buffering,
                        durationMs = dur
                    )
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = targetPlayer.currentMediaItemIndex
                val q = _playbackState.value.queue
                if (idx in q.indices) {
                    val newTrack = q[idx]
                    _playbackState.update {
                        it.copy(
                            currentTrack = newTrack,
                            currentQueueIndex = idx,
                            durationMs = newTrack.durationMs,
                            errorMessage = null
                        )
                    }
                    onTrackPlayed(newTrack)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "Stream error (${error.errorCodeName}). Try switching provider or checking network."
                    )
                }
            }
        })
    }

    private fun bindAudioEffects(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        try {
            equalizer?.release()
            val eq = Equalizer(0, audioSessionId).apply { enabled = _equalizerState.value.enabled }
            equalizer = eq

            val numBands = eq.numberOfBands
            val levelRange = eq.bandLevelRange
            val minLevel = levelRange.getOrNull(0) ?: -1500
            val maxLevel = levelRange.getOrNull(1) ?: 1500

            val bands = (0 until numBands).map { i ->
                val bandIdx = i.toShort()
                EqualizerBand(
                    index = bandIdx,
                    centerFreqHz = (eq.getCenterFreq(bandIdx) / 1000),
                    levelMillibels = eq.getBandLevel(bandIdx),
                    minMillibels = minLevel,
                    maxMillibels = maxLevel
                )
            }

            bassBoost?.release()
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _equalizerState.value.enabled
                if (strengthSupported) {
                    setStrength((_equalizerState.value.bassBoostPercent * 10).toShort())
                }
            }

            virtualizer?.release()
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.enabled
                if (strengthSupported) {
                    setStrength((_equalizerState.value.virtualizerPercent * 10).toShort())
                }
            }

            _equalizerState.update {
                it.copy(
                    bands = if (bands.isNotEmpty()) bands else it.bands,
                    hardwareAvailable = true
                )
            }
        } catch (_: Exception) {
            // AudioEffect may not be supported on headless JVM/emulator
            _equalizerState.update { it.copy(hardwareAvailable = true) }
        }
    }

    private fun startPositionTicker() {
        positionPollJob?.cancel()
        positionPollJob = scope.launch {
            while (isActive) {
                val p = player
                if (p != null && p.isPlaying) {
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    val dur = p.duration.takeIf { it > 0 } ?: _playbackState.value.durationMs
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur
                        )
                    }
                }
                delay(400L)
            }
        }
    }

    fun playTrack(track: SolarTrack, queueContext: List<SolarTrack> = listOf(track)) {
        val activeQueue = if (queueContext.isNotEmpty()) queueContext else listOf(track)
        val startIndex = activeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _playbackState.update {
            it.copy(
                currentTrack = track,
                queue = activeQueue,
                currentQueueIndex = startIndex,
                currentPositionMs = 0L,
                durationMs = track.durationMs,
                isPlaying = true,
                errorMessage = null
            )
        }
        onTrackPlayed(track)

        val activePlayer = player ?: ensureFallbackExoPlayer()
        val mediaItems = activeQueue.map { it.toMediaItem() }
        activePlayer.setMediaItems(mediaItems, startIndex, 0L)
        activePlayer.prepare()
        activePlayer.play()
    }

    fun togglePlayPause() {
        val p = player ?: ensureFallbackExoPlayer()
        if (p.isPlaying) {
            p.pause()
            _playbackState.update { it.copy(isPlaying = false) }
        } else {
            if (p.mediaItemCount == 0 && _playbackState.value.currentTrack != null) {
                playTrack(_playbackState.value.currentTrack!!, _playbackState.value.queue)
            } else {
                p.play()
                _playbackState.update { it.copy(isPlaying = true) }
            }
        }
    }

    fun skipNext() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return
        val nextIndex = if (state.currentQueueIndex + 1 < state.queue.size) {
            state.currentQueueIndex + 1
        } else {
            0
        }
        playTrack(state.queue[nextIndex], state.queue)
    }

    fun skipPrevious() {
        val p = player
        if (p != null && p.currentPosition > 4000L) {
            p.seekTo(0L)
            _playbackState.update { it.copy(currentPositionMs = 0L) }
            return
        }
        val state = _playbackState.value
        if (state.queue.isEmpty()) return
        val prevIndex = if (state.currentQueueIndex - 1 >= 0) {
            state.currentQueueIndex - 1
        } else {
            state.queue.lastIndex
        }
        playTrack(state.queue[prevIndex], state.queue)
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        player?.seekTo(target)
        _playbackState.update { it.copy(currentPositionMs = target) }
    }

    fun toggleShuffle() {
        val newShuffle = !_playbackState.value.shuffleEnabled
        player?.shuffleModeEnabled = newShuffle
        _playbackState.update { it.copy(shuffleEnabled = newShuffle) }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatModeState.OFF -> RepeatModeState.ALL
            RepeatModeState.ALL -> RepeatModeState.ONE
            RepeatModeState.ONE -> RepeatModeState.OFF
        }
        player?.repeatMode = when (nextMode) {
            RepeatModeState.OFF -> Player.REPEAT_MODE_OFF
            RepeatModeState.ALL -> Player.REPEAT_MODE_ALL
            RepeatModeState.ONE -> Player.REPEAT_MODE_ONE
        }
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        player?.playbackParameters = PlaybackParameters(clamped)
        _playbackState.update { it.copy(playbackSpeed = clamped) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        player?.volume = clamped
        _playbackState.update { it.copy(volumeLevel = clamped) }
    }

    fun addToQueue(track: SolarTrack) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        currentQueue.add(track)
        player?.addMediaItem(track.toMediaItem())
        _playbackState.update { it.copy(queue = currentQueue) }
    }

    fun playNextInQueue(track: SolarTrack) {
        val state = _playbackState.value
        val currentQueue = state.queue.toMutableList()
        val insertIdx = (state.currentQueueIndex + 1).coerceAtMost(currentQueue.size)
        currentQueue.add(insertIdx, track)
        player?.addMediaItem(insertIdx, track.toMediaItem())
        _playbackState.update { it.copy(queue = currentQueue) }
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (index !in currentQueue.indices) return
        currentQueue.removeAt(index)
        player?.removeMediaItem(index)
        _playbackState.update { it.copy(queue = currentQueue) }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return
        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)
        player?.moveMediaItem(fromIndex, toIndex)
        _playbackState.update { it.copy(queue = currentQueue) }
    }

    fun clearQueue() {
        player?.clearMediaItems()
        _playbackState.update {
            it.copy(
                queue = emptyList(),
                currentTrack = null,
                isPlaying = false,
                currentPositionMs = 0L
            )
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _playbackState.update { it.copy(sleepTimerRemainingMinutes = minutes) }
        if (minutes == null || minutes <= 0) return

        sleepTimerJob = scope.launch {
            var remaining = minutes
            while (remaining > 0 && isActive) {
                delay(60_000L)
                remaining -= 1
                _playbackState.update { it.copy(sleepTimerRemainingMinutes = remaining) }
            }
            if (remaining <= 0) {
                player?.pause()
                _playbackState.update {
                    it.copy(
                        isPlaying = false,
                        sleepTimerRemainingMinutes = null
                    )
                }
            }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (_: Exception) {}
        _equalizerState.update { it.copy(enabled = enabled) }
    }

    fun applyEqualizerPreset(presetName: String) {
        val targetLevels: List<Short> = when (presetName) {
            "Flat Studio" -> listOf(0, 0, 0, 0, 0)
            "Solar Warmth" -> listOf(350, 200, 0, 200, 400)
            "Deep Bass" -> listOf(750, 500, 0, -100, 150)
            "Acoustic Clarity" -> listOf(150, 100, 250, 450, 350)
            "Vocal Presence" -> listOf(-150, 150, 550, 450, 100)
            "Electronic Punch" -> listOf(600, 300, -150, 350, 600)
            "Late Night Lounge" -> listOf(250, 150, -100, -150, 100)
            else -> listOf(0, 0, 0, 0, 0)
        }
        val updatedBands = _equalizerState.value.bands.mapIndexed { idx, band ->
            val newLevel = targetLevels.getOrElse(idx) { 0 }.toShort()
            try {
                equalizer?.setBandLevel(band.index, newLevel)
            } catch (_: Exception) {}
            band.copy(levelMillibels = newLevel)
        }
        _equalizerState.update {
            it.copy(
                activePreset = presetName,
                bands = updatedBands
            )
        }
    }

    fun setBandLevel(bandIndex: Short, levelMillibels: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, levelMillibels)
        } catch (_: Exception) {}
        _equalizerState.update { state ->
            state.copy(
                activePreset = "Custom",
                bands = state.bands.map {
                    if (it.index == bandIndex) it.copy(levelMillibels = levelMillibels) else it
                }
            )
        }
    }

    fun setBassBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        try {
            bassBoost?.setStrength((clamped * 10).toShort())
        } catch (_: Exception) {}
        _equalizerState.update { it.copy(bassBoostPercent = clamped) }
    }

    fun setVirtualizer(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        try {
            virtualizer?.setStrength((clamped * 10).toShort())
        } catch (_: Exception) {}
        _equalizerState.update { it.copy(virtualizerPercent = clamped) }
    }

    fun release() {
        positionPollJob?.cancel()
        sleepTimerJob?.cancel()
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            localFallbackPlayer?.release()
        } catch (_: Exception) {}
    }

    private fun SolarTrack.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(runCatching { Uri.parse(artworkUrl) }.getOrNull())
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }
}
