package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.model.ProviderType
import com.example.domain.model.RecordingType
import com.example.domain.model.SolarTrack
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "liked_tracks")
data class LikedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val provider: String,
    val recordingType: String,
    val genre: String,
    val mood: String,
    val releaseYear: Int,
    val likedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val artworkUrl: String,
    val isPublic: Boolean,
    val isCollaborative: Boolean,
    val sourceOrigin: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val positionIndex: Int,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val provider: String,
    val genre: String,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "listening_history")
data class ListeningHistoryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val provider: String,
    val genre: String,
    val playedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadTaskEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val streamUrl: String,
    val localFilePath: String,
    val durationMs: Long,
    val status: String,
    val progressPercent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val quality: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAtMs: Long = System.currentTimeMillis()
)

@Dao
interface SolarDao {
    @Query("SELECT * FROM liked_tracks ORDER BY likedAtMs DESC")
    fun observeLikedTracks(): Flow<List<LikedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikedTrack(track: LikedTrackEntity)

    @Query("DELETE FROM liked_tracks WHERE id = :trackId")
    suspend fun deleteLikedTrack(trackId: String)

    @Query("SELECT * FROM playlists ORDER BY createdAtMs DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :newName, description = :newDesc WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: String, newName: String, newDesc: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: String)

    @Query("SELECT * FROM playlist_tracks ORDER BY positionIndex ASC")
    fun observeAllPlaylistTracks(): Flow<List<PlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("SELECT * FROM listening_history ORDER BY playedAtMs DESC LIMIT 100")
    fun observeListeningHistory(): Flow<List<ListeningHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListeningHistory(entry: ListeningHistoryEntity)

    @Query("DELETE FROM listening_history")
    suspend fun clearListeningHistory()

    @Query("SELECT * FROM downloads ORDER BY updatedAtMs DESC")
    fun observeDownloads(): Flow<List<DownloadTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(task: DownloadTaskEntity)

    @Query("DELETE FROM downloads WHERE trackId = :trackId")
    suspend fun deleteDownload(trackId: String)

    @Query("SELECT * FROM search_history ORDER BY searchedAtMs DESC LIMIT 15")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    @Query("UPDATE listening_history SET streamUrl = :streamUrl, artworkUrl = :artworkUrl WHERE title LIKE '%' || :titleQuery || '%'")
    suspend fun repairHistoryTrack(titleQuery: String, streamUrl: String, artworkUrl: String)

    @Query("UPDATE liked_tracks SET streamUrl = :streamUrl, artworkUrl = :artworkUrl WHERE title LIKE '%' || :titleQuery || '%'")
    suspend fun repairLikedTrack(titleQuery: String, streamUrl: String, artworkUrl: String)

    @Query("UPDATE listening_history SET streamUrl = '' WHERE streamUrl LIKE '%061102711a11009c4726b66264c93a52%' OR streamUrl LIKE '%c72a75582084c9e31a0e8f60339da41e%' OR streamUrl LIKE '%dzcdn.net%' OR streamUrl LIKE '%itunes.apple.com%' OR streamUrl LIKE '%mzaf_%'")
    suspend fun clearCorruptedHistoryStreams()

    @Query("UPDATE liked_tracks SET streamUrl = '' WHERE streamUrl LIKE '%061102711a11009c4726b66264c93a52%' OR streamUrl LIKE '%c72a75582084c9e31a0e8f60339da41e%' OR streamUrl LIKE '%dzcdn.net%' OR streamUrl LIKE '%itunes.apple.com%' OR streamUrl LIKE '%mzaf_%'")
    suspend fun clearCorruptedLikedStreams()
}

@Database(
    entities = [
        LikedTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        ListeningHistoryEntity::class,
        DownloadTaskEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SolarDatabase : RoomDatabase() {
    abstract fun solarDao(): SolarDao

    companion object {
        @Volatile
        private var INSTANCE: SolarDatabase? = null

        fun getInstance(context: Context): SolarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SolarDatabase::class.java,
                    "solar_music.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}

fun LikedTrackEntity.toDomain(): SolarTrack = SolarTrack(
    id = id,
    title = title,
    artist = artist,
    album = album,
    artworkUrl = artworkUrl,
    streamUrl = streamUrl,
    durationMs = durationMs,
    provider = runCatching { ProviderType.valueOf(provider) }.getOrDefault(ProviderType.INNERTUBE),
    recordingType = runCatching { RecordingType.valueOf(recordingType) }.getOrDefault(RecordingType.ORIGINAL),
    genre = genre,
    mood = mood,
    releaseYear = releaseYear
)

fun SolarTrack.toLikedEntity(): LikedTrackEntity = LikedTrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    artworkUrl = artworkUrl,
    streamUrl = streamUrl,
    durationMs = durationMs,
    provider = provider.name,
    recordingType = recordingType.name,
    genre = genre,
    mood = mood,
    releaseYear = releaseYear
)
