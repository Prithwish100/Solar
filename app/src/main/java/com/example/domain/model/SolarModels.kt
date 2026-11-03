package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderType(val displayName: String, val badgeLabel: String) {
    INNERTUBE("YouTube Music (InnerTube)", "YT MUSIC"),
    JIOSAAVN("JioSaavn", "SAAVN"),
    LOCAL("Local Device Storage", "LOCAL"),
    AUDIUS_CATALOG("Solar Open Studio", "STUDIO")
}

@Serializable
enum class RecordingType(val label: String, val isOfficialOriginal: Boolean) {
    ORIGINAL("Original Master", true),
    LIVE("Live Performance", false),
    ACOUSTIC("Acoustic Session", false),
    REMIX("Remix", false),
    COVER("Cover Recording", false),
    INSTRUMENTAL("Instrumental", false),
    UNOFFICIAL("Unofficial / Edit", false)
}

@Serializable
enum class AudioQuality(val label: String, val bitrateKbps: Int) {
    LOW("Data Saver (96 kbps)", 96),
    NORMAL("Standard (160 kbps)", 160),
    HIGH("High Extreme (320 kbps)", 320),
    LOSSLESS("Solar Hi-Res Lossless (FLAC)", 1411)
}

@Serializable
data class SolarTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String,
    val albumId: String = "",
    val artworkUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val provider: ProviderType = ProviderType.INNERTUBE,
    val recordingType: RecordingType = RecordingType.ORIGINAL,
    val isVerifiedArtist: Boolean = true,
    val isDownloadPermitted: Boolean = true,
    val genre: String = "Electronic",
    val mood: String = "Focus",
    val releaseYear: Int = 2026,
    val accuracyScore: Int = 98,
    val localFolderPath: String? = null,
    val audioFormat: String = "AAC 320kbps",
    val replayGainDb: Float = 0.0f
)

@Serializable
data class SolarAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val year: Int,
    val genre: String,
    val trackCount: Int,
    val provider: ProviderType = ProviderType.INNERTUBE,
    val tracks: List<SolarTrack> = emptyList()
)

@Serializable
data class SolarArtist(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val subscriberCountText: String,
    val bio: String,
    val isVerified: Boolean = true,
    val genres: List<String> = emptyList(),
    val topTracks: List<SolarTrack> = emptyList(),
    val albums: List<SolarAlbum> = emptyList(),
    val similarArtists: List<String> = emptyList()
)

@Serializable
data class SolarPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val artworkUrl: String,
    val isPublic: Boolean = false,
    val isCollaborative: Boolean = false,
    val sourceOrigin: String = "SOLAR",
    val tracks: List<SolarTrack> = emptyList(),
    val createdAtMs: Long = System.currentTimeMillis()
)

@Serializable
data class LyricsLine(
    val startTimeMs: Long,
    val text: String
)

@Serializable
data class LyricsData(
    val trackId: String,
    val isSynchronized: Boolean,
    val lines: List<LyricsLine>,
    val plainLyrics: String,
    val sourceAttribution: String
)

data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val levelMillibels: Short,
    val minMillibels: Short = -1500,
    val maxMillibels: Short = 1500
)

data class EqualizerState(
    val enabled: Boolean = false,
    val activePreset: String = "Solar Warmth",
    val presets: List<String> = listOf(
        "Flat Studio",
        "Solar Warmth",
        "Deep Bass",
        "Acoustic Clarity",
        "Vocal Presence",
        "Electronic Punch",
        "Late Night Lounge"
    ),
    val bands: List<EqualizerBand> = listOf(
        EqualizerBand(0, 60, 300),
        EqualizerBand(1, 230, 150),
        EqualizerBand(2, 910, 0),
        EqualizerBand(3, 3600, 200),
        EqualizerBand(4, 14000, 350)
    ),
    val bassBoostPercent: Int = 35,
    val virtualizerPercent: Int = 25,
    val loudnessGainDb: Float = 0f,
    val hardwareAvailable: Boolean = true
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class DownloadItem(
    val track: SolarTrack,
    val status: DownloadStatus,
    val progressPercent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val localFilePath: String,
    val quality: AudioQuality = AudioQuality.HIGH
)

data class PlaylistImportReport(
    val sourceName: String,
    val playlistName: String,
    val matchedTracks: List<SolarTrack>,
    val unmatchedTitles: List<String>
)

data class ListeningStats(
    val totalMinutesListened: Int,
    val totalTracksPlayed: Int,
    val currentStreakDays: Int,
    val topArtist: String,
    val topGenre: String,
    val mostPlayedTracks: List<Pair<SolarTrack, Int>>
)
