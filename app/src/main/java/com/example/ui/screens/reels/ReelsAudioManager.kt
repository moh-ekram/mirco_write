package com.example.ui.screens.reels

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.data.model.ReelAudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ReelsAudioManager(private val context: Context) {

    private val tag = "ReelsAudioManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<ReelAudioTrack?>(null)
    val currentTrack: StateFlow<ReelAudioTrack?> = _currentTrack.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _volume = MutableStateFlow(0.6f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _tracks = MutableStateFlow<List<ReelAudioTrack>>(emptyList())
    val tracks: StateFlow<List<ReelAudioTrack>> = _tracks.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    init {
        // Initialize default preset ambient / lofi tracks and sample Drive links
        _tracks.value = listOf(
            ReelAudioTrack(
                id = "preset_ambient_1",
                title = "Calm Ambient Study",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3",
                originalDriveUrl = "",
                isPreset = true
            ),
            ReelAudioTrack(
                id = "preset_ambient_2",
                title = "Peaceful Acoustic Flow",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3",
                originalDriveUrl = "",
                isPreset = true
            ),
            ReelAudioTrack(
                id = "preset_ambient_3",
                title = "Deep Focus Piano",
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f7772f.mp3",
                originalDriveUrl = "",
                isPreset = true
            )
        )
    }

    /**
     * Resolves Google Drive link or direct audio link to a playable streaming URL.
     */
    fun convertDriveUrlToStreamUrl(url: String): String {
        val clean = url.trim()
        val fileIdRegex = Regex("(?:drive\\.google\\.com\\/(?:file\\/d\\/|open\\?id=)|[?&]id=)([a-zA-Z0-9_-]+)")
        val match = fileIdRegex.find(clean)
        return if (match != null) {
            val fileId = match.groupValues[1]
            "https://docs.google.com/uc?export=download&id=$fileId"
        } else {
            clean
        }
    }

    fun addDriveTrack(rawUrl: String, title: String = "") {
        val clean = rawUrl.trim()
        if (clean.isEmpty()) return
        val streamUrl = convertDriveUrlToStreamUrl(clean)
        val trackTitle = if (title.isNotBlank()) title else "Drive Track #${_tracks.value.size + 1}"
        val newTrack = ReelAudioTrack(
            id = UUID.randomUUID().toString(),
            title = trackTitle,
            streamUrl = streamUrl,
            originalDriveUrl = clean,
            isPreset = false
        )
        _tracks.value = _tracks.value + newTrack
        _statusMessage.value = "Added: $trackTitle"
    }

    fun removeTrack(trackId: String) {
        val current = _currentTrack.value
        if (current?.id == trackId) {
            stop()
        }
        _tracks.value = _tracks.value.filter { it.id != trackId }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (e: Exception) {
            Log.e(tag, "Error setting volume", e)
        }
    }

    fun playRandomTrack() {
        val available = _tracks.value
        if (available.isEmpty()) return
        val randomTrack = available.random()
        playTrack(randomTrack)
    }

    fun playTrack(track: ReelAudioTrack) {
        scope.launch {
            try {
                _isLoading.value = true
                _statusMessage.value = "Connecting: ${track.title}..."

                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null

                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(track.streamUrl)
                    val vol = _volume.value
                    setVolume(vol, vol)
                    isLooping = true

                    setOnPreparedListener {
                        _isLoading.value = false
                        _isPlaying.value = true
                        _currentTrack.value = track
                        _statusMessage.value = "Playing: ${track.title}"
                        it.start()
                    }

                    setOnErrorListener { _, what, extra ->
                        _isLoading.value = false
                        _isPlaying.value = false
                        _statusMessage.value = "Playback error ($what, $extra)"
                        Log.e(tag, "MediaPlayer error: what=$what, extra=$extra")
                        true
                    }

                    setOnCompletionListener {
                        if (!isLooping) {
                            playRandomTrack()
                        }
                    }
                }

                mediaPlayer = mp
                mp.prepareAsync()
            } catch (e: Exception) {
                _isLoading.value = false
                _isPlaying.value = false
                _statusMessage.value = "Audio error: ${e.localizedMessage ?: "Unknown"}"
                Log.e(tag, "Exception in playTrack", e)
            }
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer
        if (mp != null) {
            try {
                if (mp.isPlaying) {
                    mp.pause()
                    _isPlaying.value = false
                    _statusMessage.value = "Paused"
                } else {
                    mp.start()
                    _isPlaying.value = true
                    _statusMessage.value = "Playing: ${_currentTrack.value?.title ?: "Audio"}"
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in togglePlayPause", e)
            }
        } else {
            playRandomTrack()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(tag, "Error stopping media player", e)
        }
        _isPlaying.value = false
        _currentTrack.value = null
        _statusMessage.value = "Stopped"
    }

    fun release() {
        stop()
    }
}
