package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "",
    val filePath: String,
    val duration: Long = 0,
    val folder: String = "All Beats",
    val isLocal: Boolean = true,
    val isVideo: Boolean = false,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0,
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customArtUri: String? = null,
    val isEnglish: Boolean = false
) {
    val displayTitle: String
        get() = customTitle ?: title

    val displayArtist: String
        get() = customArtist ?: artist
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverUrl: String? = null,
    val orderedTrackIds: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long
)

@Entity(tableName = "equalizer_profiles")
data class EqualizerProfileEntity(
    @PrimaryKey val profileName: String,
    val band32: Int,
    val band125: Int,
    val band500: Int,
    val band2k: Int,
    val band8k: Int
)
