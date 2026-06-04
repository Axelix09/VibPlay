package com.example

import kotlinx.coroutines.flow.Flow

class MusicRepository(private val musicDao: MusicDao) {
    val allTracks: Flow<List<TrackEntity>> = musicDao.getAllTracks()
    val allPlaylists: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists()

    suspend fun insertTrack(track: TrackEntity): Long = musicDao.insertTrack(track)
    suspend fun insertTracks(tracks: List<TrackEntity>) = musicDao.insertTracks(tracks)
    suspend fun updateTrack(track: TrackEntity) = musicDao.updateTrack(track)
    suspend fun deleteTrack(track: TrackEntity) = musicDao.deleteTrack(track)
    suspend fun deleteTrackById(id: Long) = musicDao.deleteTrackById(id)

    suspend fun insertPlaylist(playlist: PlaylistEntity): Long = musicDao.insertPlaylist(playlist)
    suspend fun updatePlaylist(playlist: PlaylistEntity) = musicDao.updatePlaylist(playlist)
    suspend fun deletePlaylistById(id: Long) = musicDao.deletePlaylistById(id)

    suspend fun insertPlaylistTrack(playlistId: Long, trackId: Long) {
        musicDao.insertPlaylistTrack(PlaylistTrackCrossRef(playlistId, trackId))
    }

    suspend fun deletePlaylistTrack(playlistId: Long, trackId: Long) {
        musicDao.deletePlaylistTrack(playlistId, trackId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>> = musicDao.getTracksForPlaylist(playlistId)

    suspend fun insertEqualizerProfile(profile: EqualizerProfileEntity) = musicDao.insertEqualizerProfile(profile)
    suspend fun getEqualizerProfile(name: String): EqualizerProfileEntity? = musicDao.getEqualizerProfile(name)
}
