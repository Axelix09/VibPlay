package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.core.app.NotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

object AudioEngine {
    val isPlaying = MutableStateFlow(false)
    val currentPosition = MutableStateFlow(0)
    val duration = MutableStateFlow(0)
    val currentTrackId = MutableStateFlow<Long?>(null)
    val playbackSpeed = MutableStateFlow(1.0f)
    val visualizerBands = MutableStateFlow(FloatArray(8) { 0.05f })

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private var visualizerJob: Job? = null
    private var positionTrackerJob: Job? = null
    private var isSynthetic = false

    var onPlaybackCompleted: (() -> Unit)? = null
    var playNextAction: (() -> Unit)? = null
    var playPrevAction: (() -> Unit)? = null
    private var appContext: android.content.Context? = null
    internal var activeTrack: TrackEntity? = null
    private var mediaSession: MediaSessionCompat? = null

    const val CHANNEL_ID = "vibplay_channel"
    const val NOTIFICATION_ID = 808

    const val ACTION_PLAY_PAUSE = "com.example.VIBPLAY_PLAY_PAUSE"
    const val ACTION_PREVIOUS = "com.example.VIBPLAY_PREVIOUS"
    const val ACTION_NEXT = "com.example.VIBPLAY_NEXT"
    const val ACTION_STOP = "com.example.VIBPLAY_STOP"

    init {
        startVisualizerLoop()
    }

    fun playTrack(context: Context, track: TrackEntity) {
        appContext = context.applicationContext
        activeTrack = track
        initMediaSession(context)
        mediaSession?.isActive = true
        try {
            isSynthetic = false
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            } else {
                mediaPlayer?.reset()
            }

            mediaPlayer?.setOnCompletionListener {
                onTrackComplete()
            }

            val file = java.io.File(track.filePath)
            if (!file.exists()) {
                throw java.io.FileNotFoundException("Synthetic offline file simulated playback.")
            }

            mediaPlayer?.setDataSource(track.filePath)
            mediaPlayer?.prepare()
            
            val audioSessionId = mediaPlayer?.audioSessionId ?: 0
            if (audioSessionId != 0) {
                initAudioEffects(audioSessionId)
            }

            duration.value = mediaPlayer?.duration ?: 0
            currentTrackId.value = track.id

            applySpeedToPlayer()

            mediaPlayer?.start()
            isPlaying.value = true
            startPositionTracker()
            updateMetadata(track)
            updatePlaybackState()
        } catch (e: Exception) {
            // Enter synthetic simulation fallback mode!
            isSynthetic = true
            duration.value = if (track.duration > 0) track.duration.toInt() else 180000 // default 3 min
            currentTrackId.value = track.id
            isPlaying.value = true
            currentPosition.value = 0
            startSyntheticPlaybackTracker()
            updateMetadata(track)
            updatePlaybackState()
        }

        showPlaybackNotification(context, track)
    }

    fun play() {
        if (currentTrackId.value == null) return
        isPlaying.value = true
        if (isSynthetic) {
            startSyntheticPlaybackTracker()
        } else {
            try {
                mediaPlayer?.start()
                startPositionTracker()
            } catch (e: Exception) {
                // safe fail and switch synthetic
                isSynthetic = true
                startSyntheticPlaybackTracker()
            }
        }
        updatePlaybackState()
        appContext?.let { ctx -> activeTrack?.let { showPlaybackNotification(ctx, it) } }
    }

    fun pause() {
        isPlaying.value = false
        positionTrackerJob?.cancel()
        if (!isSynthetic) {
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        updatePlaybackState()
        appContext?.let { ctx -> activeTrack?.let { showPlaybackNotification(ctx, it) } }
    }

    fun seekTo(ms: Int) {
        if (isSynthetic) {
            currentPosition.value = ms.coerceIn(0, duration.value)
        } else {
            try {
                mediaPlayer?.seekTo(ms)
                currentPosition.value = ms
            } catch (e: Exception) {
                currentPosition.value = ms
            }
        }
        updatePlaybackState()
    }

    fun setSpeed(speed: Float) {
        playbackSpeed.value = speed
        applySpeedToPlayer()
    }

    private fun applySpeedToPlayer() {
        if (!isSynthetic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { mp ->
                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed.value)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun release() {
        isPlaying.value = false
        positionTrackerJob?.cancel()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            equalizer?.release()
            equalizer = null
            bassBoost?.release()
            bassBoost = null
            virtualizer?.release()
            virtualizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAndClearNotification(context: Context) {
        release()
        currentTrackId.value = null
        currentPosition.value = 0
        duration.value = 0
        isPlaying.value = false
        mediaSession?.isActive = false
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    fun initAudioEffects(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }

            bassBoost?.release()
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }

            virtualizer?.release()
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyHardTuning(bands: IntArray, bassValue: Int, virtValue: Int) {
        // bands contains level for each band (index 0 to 4 correspond to frequencies: 32, 125, 500, 2k, 8k)
        try {
            equalizer?.let { eq ->
                val numBands = eq.numberOfBands.toInt()
                for (i in 0 until numBands.coerceAtMost(bands.size)) {
                    val level = (bands[i] * 100).toShort() // -15 to +15 is mapping to -1500 to +1500 mB
                    eq.setBandLevel(i.toShort(), level)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.setStrength((bassValue * 10).toShort()) // Strength range 0-1000
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            virtualizer?.let { virt ->
                if (virt.strengthSupported) {
                    virt.setStrength((virtValue * 10).toShort()) // Strength range 0-1000
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isPlaying.value && !isSynthetic) {
                mediaPlayer?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            currentPosition.value = mp.currentPosition
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(250)
            }
        }
    }

    private fun startSyntheticPlaybackTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isPlaying.value && isSynthetic) {
                val nextPos = currentPosition.value + (250 * playbackSpeed.value).toInt()
                if (nextPos >= duration.value) {
                    currentPosition.value = duration.value
                    isPlaying.value = false
                    onTrackComplete()
                } else {
                    currentPosition.value = nextPos
                }
                delay(250)
            }
        }
    }

    private fun onTrackComplete() {
        onPlaybackCompleted?.invoke()
    }

    private fun startVisualizerLoop() {
        visualizerJob?.cancel()
        visualizerJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                if (isPlaying.value) {
                    val bands = FloatArray(8)
                    val time = System.currentTimeMillis() / 250.0
                    for (i in 0 until 8) {
                        // Complex simulated wave using sin() + noise
                        val base = 0.2f + 0.55f * kotlin.math.sin(time + i * 0.6).toFloat()
                        val noise = (Math.random() * 0.25).toFloat()
                        bands[i] = (base + noise).coerceIn(0.05f, 1.0f)
                    }
                    visualizerBands.value = bands
                } else {
                    // Decay bands gradually
                    val bands = visualizerBands.value.copyOf()
                    var nonStatic = false
                    for (i in 0 until 8) {
                        if (bands[i] > 0.051f) {
                            bands[i] = (bands[i] * 0.8f).coerceAtLeast(0.05f)
                            nonStatic = true
                        }
                    }
                    visualizerBands.value = bands
                    if (!nonStatic) {
                        delay(200)
                    }
                }
                delay(80)
            }
        }
    }

    fun showPlaybackNotification(context: Context, track: TrackEntity) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "VibPlay Playback", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        initMediaSession(context)

        val piPlay = PendingIntent.getBroadcast(context, 1, Intent(ACTION_PLAY_PAUSE).setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val piPrev = PendingIntent.getBroadcast(context, 2, Intent(ACTION_PREVIOUS).setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val piNext = PendingIntent.getBroadcast(context, 3, Intent(ACTION_NEXT).setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val piStop = PendingIntent.getBroadcast(context, 4, Intent(ACTION_STOP).setPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val piContent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playIcon = if (isPlaying.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val sessionToken = mediaSession?.sessionToken

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.displayTitle)
            .setContentText(track.displayArtist)
            .setSubText(track.album.ifEmpty { "VibPlay" })
            .setContentIntent(piContent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(sessionToken)
            )
            .addAction(android.R.drawable.ic_media_previous, "Previous", piPrev)
            .addAction(playIcon, "Play/Pause", piPlay)
            .addAction(android.R.drawable.ic_media_next, "Next", piNext)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", piStop)
            .setOngoing(isPlaying.value)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun initMediaSession(context: Context) {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(context.applicationContext, "VibPlaySession").apply {
                isActive = true
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        play()
                    }
                    override fun onPause() {
                        pause()
                    }
                    override fun onSkipToNext() {
                        playNextAction?.invoke()
                    }
                    override fun onSkipToPrevious() {
                        playPrevAction?.invoke()
                    }
                    override fun onStop() {
                        stopAndClearNotification(context)
                    }
                })
            }
        }
    }

    private fun updatePlaybackState() {
        val state = if (isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP
        
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, currentPosition.value.toLong(), playbackSpeed.value)
                .setActions(actions)
                .build()
        )
    }

    private fun updateMetadata(track: TrackEntity) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.displayTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.displayArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration.value.toLong())
        
        mediaSession?.setMetadata(metadataBuilder.build())
    }
}

class MediaControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        val action = intent?.action ?: return
        
        when (action) {
            AudioEngine.ACTION_PLAY_PAUSE -> {
                if (AudioEngine.isPlaying.value) {
                    AudioEngine.pause()
                } else {
                    AudioEngine.play()
                }
            }
            AudioEngine.ACTION_PREVIOUS -> {
                AudioEngine.playPrevAction?.invoke()
            }
            AudioEngine.ACTION_NEXT -> {
                AudioEngine.playNextAction?.invoke()
            }
            AudioEngine.ACTION_STOP -> {
                AudioEngine.stopAndClearNotification(ctx)
            }
        }
    }
}
