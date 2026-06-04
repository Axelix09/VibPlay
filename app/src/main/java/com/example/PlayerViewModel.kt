package com.example

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ThemeStyle(val displayName: String) {
    CYBERPUNK("Cyberpunk"), LUXURY("Luxury"), LAVA("Lava"), TOXIC("Toxic")
}

enum class PlaybackMode(val displayName: String) {
    SHUFFLE("Aleatorio"), REPEAT_ONE("Repetir Una"),
    REPEAT_ALL("Repetir Todo"), SMART_REPEAT("Repetición Inteligente")
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(database.musicDao())
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences("vibplay_prefs", Context.MODE_PRIVATE)

    // Themes
    val currentTheme = MutableStateFlow(ThemeStyle.CYBERPUNK)

    // Playback modes
    val playbackMode = MutableStateFlow(PlaybackMode.SMART_REPEAT)

    // Loading overlay / Scanner
    val isScanning = MutableStateFlow(false)

    // Temporary session-only overrides
    private val tempTitles = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val tempArtists = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val tempArtUris = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val tempFolders = MutableStateFlow<Map<Long, String>>(emptyMap())

    // Tracks & Lists
    val allTracks: StateFlow<List<TrackEntity>> = combine(
        repository.allTracks,
        tempTitles,
        tempArtists,
        tempArtUris,
        tempFolders
    ) { dbTracks, titles, artists, arts, folders ->
        dbTracks.map { track ->
            track.copy(
                customTitle = titles[track.id] ?: track.customTitle,
                customArtist = artists[track.id] ?: track.customArtist,
                customArtUri = arts[track.id] ?: track.customArtUri,
                folder = folders[track.id] ?: track.folder
            )
        }.filter { !it.isLocal || it.filePath.startsWith("/simulated/") || java.io.File(it.filePath).exists() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<TrackEntity>> = allTracks.map { list ->
        list.filter { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableFolders: StateFlow<List<String>> = allTracks.map { list ->
        list.map { it.folder }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Equalizer sliders (-15 to +15) & effects (0-100)
    val band32Hz = MutableStateFlow(0)
    val band125Hz = MutableStateFlow(0)
    val band500Hz = MutableStateFlow(0)
    val band2kHz = MutableStateFlow(0)
    val band8kHz = MutableStateFlow(0)
    val bassBoost = MutableStateFlow(0)
    val virtualizer = MutableStateFlow(0)

    // Accumulated Insights Metrics
    val totalListeningTimeMinutes = MutableStateFlow(0L)
    private var currentTrackCountedId: Long? = null

    val isFloatingBubbleEnabled = MutableStateFlow(false)
    val isBackgroundPlaybackPremiumEnabled = MutableStateFlow(true)

    // Queue Manager
    val currentQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val currentQueueIndex = MutableStateFlow(-1)

    // Active track flow derived from queue index
    val currentTrack: StateFlow<TrackEntity?> = combine(currentQueue, currentQueueIndex) { list, idx ->
        if (idx in list.indices) list[idx] else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Seed database if empty - NO MORE DEFAULT HARDCODED MUSIC/VIDEOS AS REQUESTED BY USER
        viewModelScope.launch(Dispatchers.IO) {
            repository.allTracks.first().let { list ->
                // Keep the database clean without mock items so user can scan or add real tracks
            }
        }

        // Restore configs
        val themeOrdinal = sharedPrefs.getInt("theme_style", ThemeStyle.CYBERPUNK.ordinal)
        currentTheme.value = ThemeStyle.entries.getOrElse(themeOrdinal) { ThemeStyle.CYBERPUNK }

        val modeOrdinal = sharedPrefs.getInt("playback_mode", PlaybackMode.SMART_REPEAT.ordinal)
        playbackMode.value = PlaybackMode.entries.getOrElse(modeOrdinal) { PlaybackMode.SMART_REPEAT }

        val bubbleEnabled = sharedPrefs.getBoolean("bubble_enabled", false)
        isFloatingBubbleEnabled.value = bubbleEnabled

        val bgPremiumEnabled = sharedPrefs.getBoolean("bg_playback_premium_enabled", true)
        isBackgroundPlaybackPremiumEnabled.value = bgPremiumEnabled

        val secondsVal = sharedPrefs.getLong("listening_secs", 0L)
        totalListeningTimeMinutes.value = secondsVal / 60

        // Listen for AudioEngine completing tracks automatically to chain the queue!
        AudioEngine.onPlaybackCompleted = {
            playNext()
        }
        AudioEngine.playNextAction = {
            playNext()
        }
        AudioEngine.playPrevAction = {
            playPrevious()
        }

        startTimeTracker()
    }

    fun selectTheme(theme: ThemeStyle) {
        currentTheme.value = theme
        sharedPrefs.edit().putInt("theme_style", theme.ordinal).apply()
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val isNowFavorite = !track.isFavorite
            repository.updateTrack(track.copy(isFavorite = isNowFavorite))

            try {
                val playlistsList = repository.allPlaylists.first()
                var favoritos = playlistsList.find { it.name.trim().lowercase() == "favoritos" }
                val favId: Long
                if (isNowFavorite) {
                    if (favoritos == null) {
                        favId = repository.insertPlaylist(PlaylistEntity(
                            name = "Favoritos",
                            description = "Tus canciones favoritas",
                            coverUrl = null
                        ))
                    } else {
                        favId = favoritos.id
                    }
                    repository.insertPlaylistTrack(favId, track.id)
                    
                    // Update orderedTrackIds
                    val freshPlaylists = repository.allPlaylists.first()
                    freshPlaylists.find { it.id == favId }?.let { play ->
                        val currentIds = play.orderedTrackIds.split(",").filter { it.isNotBlank() }.toMutableList()
                        if (!currentIds.contains(track.id.toString())) {
                            currentIds.add(track.id.toString())
                            repository.updatePlaylist(play.copy(orderedTrackIds = currentIds.joinToString(",")))
                        }
                    }
                } else {
                    if (favoritos != null) {
                        favId = favoritos.id
                        repository.deletePlaylistTrack(favId, track.id)
                        
                        // Update orderedTrackIds
                        val freshPlaylists = repository.allPlaylists.first()
                        freshPlaylists.find { it.id == favId }?.let { play ->
                            val currentIds = play.orderedTrackIds.split(",").filter { it.isNotBlank() }.toMutableList()
                            if (currentIds.contains(track.id.toString())) {
                                currentIds.remove(track.id.toString())
                                repository.updatePlaylist(play.copy(orderedTrackIds = currentIds.joinToString(",")))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun scanDeviceFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            isScanning.value = true
            val foundTracks = mutableListOf<TrackEntity>()
            val context = getApplication<Application>()

             // Audio scanning
             try {
                 val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                 val projection = arrayOf(
                     android.provider.MediaStore.Audio.Media._ID,
                     android.provider.MediaStore.Audio.Media.TITLE,
                     android.provider.MediaStore.Audio.Media.ARTIST,
                     android.provider.MediaStore.Audio.Media.ALBUM,
                     android.provider.MediaStore.Audio.Media.DATA,
                     android.provider.MediaStore.Audio.Media.DURATION,
                     android.provider.MediaStore.Audio.Media.ALBUM_ID
                 )
                 // Filter songs of duration >= 30 seconds
                 val selection = "${android.provider.MediaStore.Audio.Media.DURATION} >= ?"
                 val selectionArgs = arrayOf("30000")
 
                 context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                     val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                     val artistCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                     val albumCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
                     val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                     val durCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                     val albumIdCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM_ID)
 
                     while (cursor.moveToNext()) {
                         val title = cursor.getString(titleCol) ?: "Pista Externa"
                         val artist = cursor.getString(artistCol) ?: "Artista Desconocido"
                         val album = cursor.getString(albumCol) ?: ""
                         val path = cursor.getString(dataCol) ?: ""
                         val dur = cursor.getLong(durCol)
                         val folderName = java.io.File(path).parentFile?.name ?: "All Beats"
                         val albumId = cursor.getLong(albumIdCol)
                         val artUri = "content://media/external/audio/albumart/$albumId"
 
                         foundTracks.add(
                             TrackEntity(
                                 title = title,
                                 artist = artist,
                                 album = album,
                                 filePath = path,
                                 duration = dur,
                                 folder = folderName,
                                 isLocal = true,
                                 isVideo = false,
                                 customArtUri = artUri
                             )
                         )
                     }
                 }
             } catch (e: Exception) {
                 e.printStackTrace()
             }

            // Video scanning
            try {
                val uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    android.provider.MediaStore.Video.Media._ID,
                    android.provider.MediaStore.Video.Media.TITLE,
                    android.provider.MediaStore.Video.Media.DATA,
                    android.provider.MediaStore.Video.Media.DURATION
                )
                val selection: String? = null
                val selectionArgs: Array<String>? = null

                context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.TITLE)
                    val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA)
                    val durCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DURATION)

                    while (cursor.moveToNext()) {
                        val title = cursor.getString(titleCol) ?: "Video Local"
                        val path = cursor.getString(dataCol) ?: ""
                        val dur = cursor.getLong(durCol)
                        val folderName = java.io.File(path).parentFile?.name ?: "Videos"

                        foundTracks.add(
                            TrackEntity(
                                title = title,
                                artist = "Video Studio",
                                album = "Clips",
                                filePath = path,
                                duration = dur,
                                folder = folderName,
                                isLocal = true,
                                isVideo = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Deep storage scanning for any exotic audio/video format not caught by MediaStore
            try {
                val existingPaths = allTracks.value.map { it.filePath }.toSet()
                val foundPaths = foundTracks.map { it.filePath }.toMutableSet()
                val rootsToScan = mutableListOf<java.io.File>()
                
                // Primary external storage root
                val externalDir = android.os.Environment.getExternalStorageDirectory()
                if (externalDir != null && externalDir.exists()) {
                    rootsToScan.add(externalDir)
                }
                
                // Secondary SD cards or external directories
                val files = context.getExternalFilesDirs(null)
                for (f in files) {
                    if (f != null) {
                        val p = f.absolutePath
                        val idx = p.indexOf("/Android")
                        if (idx != -1) {
                            val root = java.io.File(p.substring(0, idx))
                            if (root.exists() && !rootsToScan.contains(root)) {
                                rootsToScan.add(root)
                            }
                        }
                    }
                }

                // Scan common media and documents subfolders of root to be precise and highly performant
                val subDirNames = listOf(
                    android.os.Environment.DIRECTORY_MUSIC,
                    android.os.Environment.DIRECTORY_PODCASTS,
                    android.os.Environment.DIRECTORY_MOVIES,
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    android.os.Environment.DIRECTORY_DCIM,
                    android.os.Environment.DIRECTORY_DOCUMENTS,
                    "Bluetooth",
                    "Recordings"
                )

                for (root in rootsToScan) {
                    for (subName in subDirNames) {
                        val subDir = java.io.File(root, subName)
                        if (subDir.exists() && subDir.isDirectory) {
                            scanDirectoryForMedia(subDir, foundTracks, existingPaths, foundPaths)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (foundTracks.isNotEmpty()) {
                // Filter out duplicates by path to avoid DB UNIQUE constraint violations
                val uniqueNewTracks = foundTracks.distinctBy { it.filePath }
                repository.insertTracks(uniqueNewTracks)
            }
            delay(1500) // Aesthetic delay for progress indicator
            isScanning.value = false
        }
    }

    private fun scanDirectoryForMedia(
        directory: java.io.File,
        foundTracks: MutableList<TrackEntity>,
        existingPaths: Set<String>,
        foundPaths: MutableSet<String>,
        depth: Int = 0
    ) {
        if (depth > 3) return // Max recursion depth to prevent infinite loops and sluggish scanning
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (file.name != "Android" && !file.name.startsWith(".")) {
                    scanDirectoryForMedia(file, foundTracks, existingPaths, foundPaths, depth + 1)
                }
            } else if (file.isFile) {
                val path = file.absolutePath
                if (existingPaths.contains(path) || !foundPaths.add(path)) continue
                val ext = file.extension.lowercase()
                
                // Absolute global support of ALL standard & exotic audio/video format extensions
                val isAudio = ext in setOf(
                    "mp3", "wav", "m4a", "aac", "flac", "ogg", "wma", "opus", "mid", "midi", "amr", "mpga", "mka", "ra", "ape", "alac", "aif", "aiff"
                )
                val isVideo = ext in setOf(
                    "mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "ts", "mpeg", "mpg", "wmv", "vob", "ogv", "asf"
                )
                
                if (isAudio || isVideo) {
                    val fileSize = file.length()
                    
                    // Quick size shortcut optimization:
                    // A 30s track is typically >= 250KB. Skip smaller audio files instantly to avoid MediaMetadataRetriever overhead.
                    if (isAudio && fileSize < 250000L) {
                        continue
                    }
                    
                    val dur = estimateDuration(file) ?: 180000L // 3 minutes default fallback
                    if (isAudio && dur < 30000L) {
                        continue
                    }
                    
                    foundTracks.add(
                        TrackEntity(
                            title = file.nameWithoutExtension.replace("_", " ").replace("-", " "),
                            artist = if (isVideo) "Video Local" else "Artista Local",
                            album = if (isVideo) "Clips" else "Album Local",
                            filePath = path,
                            duration = dur,
                            folder = file.parentFile?.name ?: (if (isVideo) "Videos" else "All Beats"),
                            isLocal = true,
                            isVideo = isVideo
                        )
                    )
                }
            }
        }
    }

    private fun estimateDuration(file: java.io.File): Long? {
        var retriever: android.media.MediaMetadataRetriever? = null
        try {
            retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val timeStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            return timeStr?.toLong()
        } catch (e: Exception) {
            return null
        } finally {
            try {
                retriever?.release()
            } catch (ex: Exception) {
                // ignore
            }
        }
    }

    fun onTrackSelected(track: TrackEntity, queue: List<TrackEntity>) {
        currentQueue.value = queue
        val idx = queue.indexOfFirst { it.id == track.id }
        currentQueueIndex.value = if (idx != -1) idx else 0

        currentTrackCountedId = track.id

        // Update last played timestamp on IO thread without incrementing playCount yet
        viewModelScope.launch(Dispatchers.IO) {
            val updated = track.copy(
                lastPlayedAt = System.currentTimeMillis()
            )
            repository.updateTrack(updated)
        }

        AudioEngine.playTrack(getApplication(), track)
    }

    fun playVideoAsBackgroundAudio(track: TrackEntity, startPosMs: Int) {
        val videos = allTracks.value.filter { it.isVideo }
        currentQueue.value = videos
        val idx = videos.indexOfFirst { it.id == track.id }
        currentQueueIndex.value = if (idx != -1) idx else 0

        currentTrackCountedId = track.id

        viewModelScope.launch(Dispatchers.IO) {
            val updated = track.copy(
                lastPlayedAt = System.currentTimeMillis()
            )
            repository.updateTrack(updated)
        }

        AudioEngine.playTrack(getApplication(), track)
        if (startPosMs > 0) {
            AudioEngine.seekTo(startPosMs)
        }
    }

    fun playOrPause() {
        if (AudioEngine.isPlaying.value) {
            AudioEngine.pause()
        } else {
            AudioEngine.play()
        }
    }

    fun playNext() {
        val queue = currentQueue.value
        if (queue.isEmpty()) return
        val currentIdx = currentQueueIndex.value

        when (playbackMode.value) {
            PlaybackMode.REPEAT_ONE -> {
                if (currentIdx in queue.indices) {
                    onTrackSelected(queue[currentIdx], queue)
                }
            }
            PlaybackMode.SHUFFLE -> {
                val nextIdx = (Math.random() * queue.size).toInt()
                currentQueueIndex.value = nextIdx
                if (nextIdx in queue.indices) {
                    onTrackSelected(queue[nextIdx], queue)
                }
            }
            PlaybackMode.REPEAT_ALL, PlaybackMode.SMART_REPEAT -> {
                val nextIdx = (currentIdx + 1) % queue.size
                currentQueueIndex.value = nextIdx
                if (nextIdx in queue.indices) {
                    onTrackSelected(queue[nextIdx], queue)
                }
            }
        }
    }

    fun playPrevious() {
        val queue = currentQueue.value
        if (queue.isEmpty()) return
        val currentIdx = currentQueueIndex.value
        val prevIdx = if (currentIdx - 1 < 0) queue.size - 1 else currentIdx - 1

        currentQueueIndex.value = prevIdx
        if (prevIdx in queue.indices) {
            onTrackSelected(queue[prevIdx], queue)
        }
    }

    fun seekTo(ms: Int) {
        AudioEngine.seekTo(ms)
    }

    fun setPlaybackSpeed(speed: Float) {
        AudioEngine.setSpeed(speed)
    }

    fun toggleShuffleRepeat() {
        val current = playbackMode.value
        val index = (current.ordinal + 1) % PlaybackMode.entries.size
        val nextMode = PlaybackMode.entries[index]
        playbackMode.value = nextMode
        sharedPrefs.edit().putInt("playback_mode", nextMode.ordinal).apply()
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        playbackMode.value = mode
        sharedPrefs.edit().putInt("playback_mode", mode.ordinal).apply()
    }

    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrackById(track.id)
            // also remove physical file if desired, but let's just delete from Db for safety
        }
    }

    fun deleteTracks(tracks: List<TrackEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            tracks.forEach {
                repository.deleteTrackById(it.id)
            }
        }
    }

    fun editTrackMetadata(id: Long, title: String, artist: String, artUrl: String?, folder: String, isPermanent: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isPermanent) {
                allTracks.value.find { it.id == id }?.let { original ->
                    val updated = original.copy(
                        customTitle = title,
                        customArtist = artist,
                        customArtUri = artUrl,
                        folder = folder
                    )
                    repository.updateTrack(updated)
                }
            } else {
                val titles = tempTitles.value.toMutableMap()
                titles[id] = title
                tempTitles.value = titles

                val artists = tempArtists.value.toMutableMap()
                artists[id] = artist
                tempArtists.value = artists

                val uris = tempArtUris.value.toMutableMap()
                uris[id] = artUrl ?: ""
                tempArtUris.value = uris

                val foldersMap = tempFolders.value.toMutableMap()
                foldersMap[id] = folder
                tempFolders.value = foldersMap
            }
        }
    }

    fun moveTrackToFolder(id: Long, folder: String, isPermanent: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isPermanent) {
                allTracks.value.find { it.id == id }?.let { original ->
                    repository.updateTrack(original.copy(folder = folder))
                }
            } else {
                val foldersMap = tempFolders.value.toMutableMap()
                foldersMap[id] = folder
                tempFolders.value = foldersMap
            }
        }
    }

    fun moveTracksToFolder(ids: List<Long>, folder: String, isPermanent: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id ->
                if (isPermanent) {
                    allTracks.value.find { it.id == id }?.let { original ->
                        repository.updateTrack(original.copy(folder = folder))
                    }
                } else {
                    val foldersMap = tempFolders.value.toMutableMap()
                    foldersMap[id] = folder
                    tempFolders.value = foldersMap
                }
            }
        }
    }

    fun createPlaylist(name: String, desc: String, coverUrl: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPlaylist(PlaylistEntity(name = name, description = desc, coverUrl = coverUrl))
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePlaylist(playlist)
        }
    }

    fun removePlaylist(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylistById(id)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPlaylistTrack(playlistId, trackId)
            // also update orderedTrackIds
            repository.allPlaylists.first().find { it.id == playlistId }?.let { playlist ->
                val currentIds = playlist.orderedTrackIds.split(",").filter { it.isNotBlank() }.toMutableList()
                if (!currentIds.contains(trackId.toString())) {
                    currentIds.add(trackId.toString())
                    repository.updatePlaylist(playlist.copy(orderedTrackIds = currentIds.joinToString(",")))
                }
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylistTrack(playlistId, trackId)
            // also update orderedTrackIds
            repository.allPlaylists.first().find { it.id == playlistId }?.let { playlist ->
                val currentIds = playlist.orderedTrackIds.split(",").filter { it.isNotBlank() }.toMutableList()
                currentIds.remove(trackId.toString())
                repository.updatePlaylist(playlist.copy(orderedTrackIds = currentIds.joinToString(",")))
            }
        }
    }

    fun reorderTracksInPlaylist(playlistId: Long, trackIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.allPlaylists.first().find { it.id == playlistId }?.let { playlist ->
                val newOrder = trackIds.joinToString(",") { it.toString() }
                repository.updatePlaylist(playlist.copy(orderedTrackIds = newOrder))
            }
        }
    }

    fun toggleEnglishTrack(track: TrackEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTrack(track.copy(isEnglish = !track.isEnglish))
        }
    }

    fun addManualTrack(title: String, artist: String, path: String, isVideo: Boolean, folder: String, artUri: String? = null, isEnglish: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = TrackEntity(
                title = title,
                artist = artist,
                album = "Manual Import",
                filePath = path,
                duration = 180000,
                folder = folder,
                isLocal = !path.startsWith("http") && !path.startsWith("https"),
                isVideo = isVideo,
                customArtUri = artUri,
                isEnglish = isEnglish
            )
            repository.insertTrack(track)
        }
    }

    fun getTracksForPlaylistFlow(playlistId: Long): Flow<List<TrackEntity>> {
        return repository.getTracksForPlaylist(playlistId)
    }

    // Equalizer sliders
    fun modifyManualBand(index: Int, value: Int) {
        val clamped = value.coerceIn(-15, 15)
        when (index) {
            0 -> band32Hz.value = clamped
            1 -> band125Hz.value = clamped
            2 -> band500Hz.value = clamped
            3 -> band2kHz.value = clamped
            4 -> band8kHz.value = clamped
        }
        updateEqualizerTuning()
    }

    fun modifyBassBoost(value: Int) {
        bassBoost.value = value.coerceIn(0, 100)
        updateEqualizerTuning()
    }

    fun modifyVirtualizer(value: Int) {
        virtualizer.value = value.coerceIn(0, 100)
        updateEqualizerTuning()
    }

    fun applyPresetProfile(presetName: String) {
        val profile = when (presetName.lowercase()) {
            "rock" -> intArrayOf(5, 3, -1, 4, 6)
            "pop" -> intArrayOf(-2, 1, 5, 2, -1)
            "jazz" -> intArrayOf(3, 2, -2, 2, 4)
            "metal" -> intArrayOf(8, 5, -3, 6, 3)
            else -> intArrayOf(0, 0, 0, 0, 0) // Normal
        }
        band32Hz.value = profile[0]
        band125Hz.value = profile[1]
        band500Hz.value = profile[2]
        band2kHz.value = profile[3]
        band8kHz.value = profile[4]
        updateEqualizerTuning()
    }

    private fun updateEqualizerTuning() {
        val bands = intArrayOf(
            band32Hz.value,
            band125Hz.value,
            band500Hz.value,
            band2kHz.value,
            band8kHz.value
        )
        AudioEngine.applyHardTuning(bands, bassBoost.value, virtualizer.value)
    }

    private fun startTimeTracker() {
        viewModelScope.launch {
            var activeTrackPlaySeconds = 0L
            var lastTrackId: Long? = null

            while (true) {
                delay(1000) // Precise 1-second check for smooth progressive listening time updates
                if (AudioEngine.isPlaying.value) {
                    val track = currentTrack.value
                    if (track != null) {
                        if (lastTrackId != track.id) {
                            lastTrackId = track.id
                            activeTrackPlaySeconds = 0L
                        }

                        // Increment total listening seconds progressively
                        val currentSecs = sharedPrefs.getLong("listening_secs", 0L)
                        val newSecs = currentSecs + 1
                        sharedPrefs.edit().putLong("listening_secs", newSecs).apply()
                        totalListeningTimeMinutes.value = newSecs / 60

                        // Track individual song played duration to increment its database playCount
                        activeTrackPlaySeconds++

                        // Threshold to count as a genuine play listen (e.g. at least 30 seconds, or 50% if song is shorter)
                        val songDurationSec = (track.duration / 1000).coerceAtLeast(10L)
                        val thresholdSec = (songDurationSec * 0.5f).toLong().coerceIn(5, 30)

                        if (currentTrackCountedId == track.id && activeTrackPlaySeconds >= thresholdSec) {
                            // Increment play count inside database on standard IO
                            viewModelScope.launch(Dispatchers.IO) {
                                val updated = track.copy(
                                    playCount = track.playCount + 1
                                )
                                repository.updateTrack(updated)
                            }
                            // Clear currentTrackCountedId so we don't count it again in this play session
                            currentTrackCountedId = null
                        }
                    }
                }
            }
        }
    }

    fun setFloatingBubbleEnabled(enabled: Boolean, context: Context) {
        isFloatingBubbleEnabled.value = enabled
        sharedPrefs.edit().putBoolean("bubble_enabled", enabled).apply()
    }

    fun setBackgroundPlaybackPremiumEnabled(enabled: Boolean) {
        isBackgroundPlaybackPremiumEnabled.value = enabled
        sharedPrefs.edit().putBoolean("bg_playback_premium_enabled", enabled).apply()
    }

    fun stopPlayback() {
        AudioEngine.pause()
    }

    fun stopAndClear() {
        AudioEngine.stopAndClearNotification(getApplication())
        currentQueue.value = emptyList()
        currentQueueIndex.value = -1
    }

    fun getSdCardPath(): String? {
        val context = getApplication<Application>()
        val files = context.getExternalFilesDirs(null)
        if (files.size > 1) {
            // Index 1 corresponds to secondary storage (SD Card)
            val sdPath = files[1]?.absolutePath
            if (sdPath != null) {
                // Return directory root
                val idx = sdPath.indexOf("/Android")
                if (idx != -1) {
                    return sdPath.substring(0, idx)
                }
                return sdPath
            }
        }
        return null
    }

    fun hasSdCard(): Boolean {
        return getSdCardPath() != null
    }

    fun simulateLocalTrackImport(title: String, artist: String, path: String, isVideo: Boolean, folder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = TrackEntity(
                title = title,
                artist = artist,
                album = "Simulated Import",
                filePath = "/simulated/$path",
                duration = 180000,
                folder = folder,
                isLocal = true,
                isVideo = isVideo
            )
            repository.insertTrack(track)
        }
    }
}
