package com.example.domain.provider

import com.example.domain.model.LyricsData
import com.example.domain.model.PlaylistImportReport
import com.example.domain.model.ProviderType
import com.example.domain.model.RecordingType
import com.example.domain.model.SolarAlbum
import com.example.domain.model.SolarArtist
import com.example.domain.model.SolarPlaylist
import com.example.domain.model.SolarTrack
import kotlin.math.abs

interface MusicProvider {
    val providerType: ProviderType
    suspend fun isAvailable(): Boolean
    suspend fun getFeaturedTracks(): List<SolarTrack>
    suspend fun getNewReleases(): List<SolarAlbum>
    suspend fun getFeaturedArtists(): List<SolarArtist>
    suspend fun getMoodPlaylists(): Map<String, List<SolarTrack>>
}

interface SearchProvider {
    val providerType: ProviderType
    suspend fun searchTracks(query: String, includeNonOriginal: Boolean = false): List<SolarTrack>
    suspend fun searchAlbums(query: String): List<SolarAlbum>
    suspend fun searchArtists(query: String): List<SolarArtist>
    suspend fun getSuggestions(prefix: String): List<String>
}

interface StreamingProvider {
    val providerType: ProviderType
    suspend fun resolvePlayableStreamUrl(track: SolarTrack): Result<String>
}

interface PlaylistProvider {
    suspend fun getCuratedPlaylists(): List<SolarPlaylist>
    suspend fun importExternalPlaylist(
        source: String,
        rawInputOrUrl: String,
        availableCatalog: List<SolarTrack>
    ): PlaylistImportReport
}

interface AuthenticationProvider {
    val isSignedIn: Boolean
    val currentUserId: String?
    val currentUserDisplayName: String?
    val currentUserEmail: String?
}

interface LyricsProvider {
    suspend fun getLyricsForTrack(track: SolarTrack): LyricsData
}

interface RecommendationProvider {
    fun generatePersonalizedMixes(
        history: List<SolarTrack>,
        likedSongs: List<SolarTrack>,
        catalog: List<SolarTrack>
    ): Map<String, List<SolarTrack>>
}

/**
 * Search Accuracy & Recording Verification Engine.
 * Prioritizes official original recordings and demotes/filters fan edits, shorts, karaoke,
 * nightcore, slowed+reverb, covers, and remixes unless explicitly requested in the query.
 */
object SearchScoringEngine {

    private val remixKeywords = listOf("remix", "mix", "bootleg", "vip", "flip", "club edit")
    private val liveKeywords = listOf("live", "concert", "tour", "festival", "unplugged live")
    private val acousticKeywords = listOf("acoustic", "stripped", "piano version", "guitar version")
    private val coverKeywords = listOf("cover", "tribute", "originally performed by", "karaoke")
    private val instrumentalKeywords = listOf("instrumental", "backing track", "karaoke version")
    private val unofficialNoiseKeywords = listOf(
        "nightcore", "slowed", "reverb", "8d audio", "bass boosted",
        "fan edit", "shorts", "tiktok", "lyric video", "lyrics video"
    )

    fun classifyRecording(title: String, artist: String, album: String): RecordingType {
        val combined = "$title $album $artist".lowercase()
        return when {
            unofficialNoiseKeywords.any { combined.contains(it) } -> RecordingType.UNOFFICIAL
            instrumentalKeywords.any { combined.contains(it) } -> RecordingType.INSTRUMENTAL
            coverKeywords.any { combined.contains(it) } -> RecordingType.COVER
            remixKeywords.any { combined.contains(it) } -> RecordingType.REMIX
            liveKeywords.any { combined.contains(it) } -> RecordingType.LIVE
            acousticKeywords.any { combined.contains(it) } -> RecordingType.ACOUSTIC
            else -> RecordingType.ORIGINAL
        }
    }

    fun queryExplicitlyRequestsNonOriginal(query: String): Boolean {
        val q = query.lowercase()
        val allModifiers = remixKeywords + liveKeywords + acousticKeywords +
            coverKeywords + instrumentalKeywords + unofficialNoiseKeywords
        return allModifiers.any { q.contains(it) }
    }

    fun scoreTrack(
        track: SolarTrack,
        rawQuery: String,
        expectedDurationMs: Long? = null
    ): Int {
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty()) return track.accuracyScore

        val titleLower = track.title.lowercase()
        val artistLower = track.artist.lowercase()
        val albumLower = track.album.lowercase()

        var score = 0

        // 1. Exact or prefix title match (up to 40 pts)
        when {
            titleLower == q -> score += 40
            titleLower.startsWith(q) -> score += 34
            titleLower.contains(q) -> score += 26
            q.split(" ").all { token -> token.isNotBlank() && (titleLower.contains(token) || artistLower.contains(token)) } -> score += 22
            else -> score += 8
        }

        // 2. Artist match (up to 25 pts)
        when {
            artistLower == q -> score += 25
            artistLower.contains(q) || q.contains(artistLower) -> score += 20
            q.split(" ").any { token -> token.length > 2 && artistLower.contains(token) } -> score += 12
        }

        // 3. Album match (up to 10 pts)
        if (albumLower.contains(q)) {
            score += 10
        }

        // 4. Official artist/channel verification (12 pts)
        if (track.isVerifiedArtist) {
            score += 12
        }

        // 5. Duration similarity & standard song length check (up to 8 pts)
        if (expectedDurationMs != null) {
            val diffSec = abs(track.durationMs - expectedDurationMs) / 1000
            if (diffSec <= 5) score += 8 else if (diffSec <= 15) score += 4
        } else if (track.durationMs in 90_000L..480_000L) {
            // Standard studio song duration (not a 15-second Short)
            score += 6
        } else if (track.durationMs < 60_000L) {
            // Penalize short clips
            score -= 25
        }

        // 6. Recording / version classification penalty or boost
        val explicitNonOriginal = queryExplicitlyRequestsNonOriginal(q)
        val detectedType = classifyRecording(track.title, track.artist, track.album)
        if (!explicitNonOriginal) {
            when (detectedType) {
                RecordingType.ORIGINAL -> score += 15
                RecordingType.ACOUSTIC, RecordingType.LIVE -> score -= 18
                RecordingType.REMIX, RecordingType.COVER, RecordingType.INSTRUMENTAL -> score -= 30
                RecordingType.UNOFFICIAL -> score -= 50
            }
        } else {
            score += 10
        }

        // 7. Provider confidence
        when (track.provider) {
            ProviderType.INNERTUBE -> score += 5
            ProviderType.JIOSAAVN -> score += 4
            ProviderType.LOCAL -> score += 4
            ProviderType.AUDIUS_CATALOG -> score += 3
        }

        return score.coerceIn(1, 100)
    }

    fun rankAndFilterResults(
        tracks: List<SolarTrack>,
        query: String,
        onlyOriginal: Boolean = false
    ): List<SolarTrack> {
        return tracks
            .map { track ->
                val recordingType = classifyRecording(track.title, track.artist, track.album)
                val computedScore = scoreTrack(track.copy(recordingType = recordingType), query)
                track.copy(
                    recordingType = recordingType,
                    accuracyScore = computedScore
                )
            }
            .sortedByDescending { it.accuracyScore }
    }
}
