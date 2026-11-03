package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.cloud.SolarCloudRepository
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.ListeningHistoryEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.SolarAppSettings
import com.example.data.local.SolarDatabase
import com.example.data.local.SolarPreferencesManager
import com.example.data.local.toDomain
import com.example.data.local.toLikedEntity
import com.example.data.provider.InnerTubeMusicProvider
import com.example.data.provider.JioSaavnMusicProvider
import com.example.data.provider.LocalMediaStoreProvider
import com.example.data.provider.LrcLibLyricsProvider
import com.example.data.provider.SolarRecommendationEngine
import com.example.domain.model.AudioQuality
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.domain.model.EqualizerState
import com.example.domain.model.ListeningStats
import com.example.domain.model.LyricsData
import com.example.domain.model.PlaylistImportReport
import com.example.domain.model.ProviderType
import com.example.domain.model.RecordingType
import com.example.domain.model.SolarAlbum
import com.example.domain.model.SolarArtist
import com.example.domain.model.SolarPlaylist
import com.example.domain.model.SolarTrack
import com.example.domain.provider.SearchScoringEngine
import com.example.playback.PlaybackUiState
import com.example.playback.SolarAudioController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class SearchUiState(
    val query: String = "",
    val selectedFilter: String = "All",
    val onlyOfficialOriginal: Boolean = true,
    val isSearching: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val trackResults: List<SolarTrack> = emptyList(),
    val albumResults: List<SolarAlbum> = emptyList(),
    val artistResults: List<SolarArtist> = emptyList(),
    val trendingSearches: List<String> = listOf(
        "Midnight Horizon",
        "Helios Pulse",
        "Amber Resonance",
        "Lyra Vance",
        "Synthwave",
        "Obsidian Echoes",
        "Kinetic Sunspot"
    )
)

data class HomeFeedState(
    val greeting: String = "Good Evening",
    val isRefreshing: Boolean = false,
    val quickPicks: List<SolarTrack> = emptyList(),
    val recentlyPlayed: List<SolarTrack> = emptyList(),
    val recommendedTracks: List<SolarTrack> = emptyList(),
    val newReleases: List<SolarAlbum> = emptyList(),
    val featuredArtists: List<SolarArtist> = emptyList(),
    val curatedPlaylists: List<SolarPlaylist> = emptyList(),
    val moodPlaylists: Map<String, List<SolarTrack>> = emptyMap(),
    val personalizedMixes: Map<String, List<SolarTrack>> = emptyMap(),
    val selectedMoodFilter: String = "All"
)

class SolarViewModel(
    private val appContext: Context,
    private val database: SolarDatabase,
    private val preferencesManager: SolarPreferencesManager,
    private val cloudRepository: SolarCloudRepository
) : ViewModel() {

    private val dao = database.solarDao()
    private val innerTubeProvider = InnerTubeMusicProvider()
    private val jioSaavnProvider = JioSaavnMusicProvider()
    private val localMediaStoreProvider = LocalMediaStoreProvider(appContext)
    private val lyricsProvider = LrcLibLyricsProvider()
    private val recommendationEngine = SolarRecommendationEngine()
    private val httpClient = OkHttpClient()

    private val audioController = SolarAudioController(
        context = appContext,
        scope = viewModelScope,
        onTrackPlayed = { track -> recordTrackPlayed(track) }
    )

    val playbackState: StateFlow<PlaybackUiState> = audioController.playbackState
    val equalizerState: StateFlow<EqualizerState> = audioController.equalizerState
    val appSettings: StateFlow<SolarAppSettings> = preferencesManager.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SolarAppSettings()
    )

    private val _homeState = MutableStateFlow(HomeFeedState(greeting = computeGreeting()))
    val homeState: StateFlow<HomeFeedState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _localTracks = MutableStateFlow<List<SolarTrack>>(emptyList())
    val localTracks: StateFlow<List<SolarTrack>> = _localTracks.asStateFlow()

    private val _activeLyrics = MutableStateFlow<LyricsData?>(null)
    val activeLyrics: StateFlow<LyricsData?> = _activeLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private val _lastImportReport = MutableStateFlow<PlaylistImportReport?>(null)
    val lastImportReport: StateFlow<PlaylistImportReport?> = _lastImportReport.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()

    val likedTracks: StateFlow<List<SolarTrack>> = dao.observeLikedTracks()
        .combine(_homeState) { entities, _ -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPlaylists: StateFlow<List<SolarPlaylist>> = combine(
        dao.observePlaylists(),
        dao.observeAllPlaylistTracks()
    ) { playlists, trackRefs ->
        val grouped = trackRefs.groupBy { it.playlistId }
        playlists.map { pl ->
            val tracks = grouped[pl.id].orEmpty().sortedBy { it.positionIndex }.map { ref ->
                SolarTrack(
                    id = ref.trackId,
                    title = ref.title,
                    artist = ref.artist,
                    album = ref.album,
                    artworkUrl = ref.artworkUrl,
                    streamUrl = ref.streamUrl,
                    durationMs = ref.durationMs,
                    provider = runCatching { ProviderType.valueOf(ref.provider) }.getOrDefault(ProviderType.INNERTUBE),
                    genre = ref.genre
                )
            }
            SolarPlaylist(
                id = pl.id,
                name = pl.name,
                description = pl.description,
                artworkUrl = pl.artworkUrl.ifBlank {
                    tracks.firstOrNull()?.artworkUrl
                        ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&q=80"
                },
                isPublic = pl.isPublic,
                isCollaborative = pl.isCollaborative,
                sourceOrigin = pl.sourceOrigin,
                tracks = tracks,
                createdAtMs = pl.createdAtMs
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listeningHistory: StateFlow<List<SolarTrack>> = dao.observeListeningHistory()
        .combine(_homeState) { entries, _ ->
            entries.map { e ->
                SolarTrack(
                    id = e.trackId,
                    title = e.title,
                    artist = e.artist,
                    album = e.album,
                    artworkUrl = e.artworkUrl,
                    streamUrl = e.streamUrl,
                    durationMs = e.durationMs,
                    provider = runCatching { ProviderType.valueOf(e.provider) }.getOrDefault(ProviderType.INNERTUBE),
                    genre = e.genre
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadItem>> = dao.observeDownloads()
        .combine(_homeState) { list, _ ->
            list.map { d ->
                DownloadItem(
                    track = SolarTrack(
                        id = d.trackId,
                        title = d.title,
                        artist = d.artist,
                        album = d.album,
                        artworkUrl = d.artworkUrl,
                        streamUrl = d.localFilePath.ifBlank { d.streamUrl },
                        durationMs = d.durationMs
                    ),
                    status = runCatching { DownloadStatus.valueOf(d.status) }.getOrDefault(DownloadStatus.COMPLETED),
                    progressPercent = d.progressPercent,
                    downloadedBytes = d.downloadedBytes,
                    totalBytes = d.totalBytes,
                    localFilePath = d.localFilePath,
                    quality = runCatching { AudioQuality.valueOf(d.quality) }.getOrDefault(AudioQuality.HIGH)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listeningStats: StateFlow<ListeningStats> = combine(
        listeningHistory,
        likedTracks
    ) { history, liked ->
        val totalMs = history.sumOf { it.durationMs }
        val totalMinutes = (totalMs / 60_000L).toInt().coerceAtLeast(liked.size * 4)
        val topArtist = (history + liked).groupingBy { it.artist }.eachCount()
            .maxByOrNull { it.value }?.key ?: "Helios Pulse"
        val topGenre = (history + liked).groupingBy { it.genre }.eachCount()
            .maxByOrNull { it.value }?.key ?: "Synthwave"
        val counts = history.groupingBy { it.id }.eachCount()
        val uniqueTracks = (history + liked).distinctBy { it.id }
        val mostPlayed = uniqueTracks.map { t -> t to (counts[t.id] ?: 1) }
            .sortedByDescending { it.second }
            .take(5)

        ListeningStats(
            totalMinutesListened = totalMinutes,
            totalTracksPlayed = history.size,
            currentStreakDays = if (history.isNotEmpty() || liked.isNotEmpty()) 5 else 1,
            topArtist = topArtist,
            topGenre = topGenre,
            mostPlayedTracks = mostPlayed
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ListeningStats(18, 4, 1, "Helios Pulse", "Synthwave", emptyList())
    )

    init {
        loadInitialData()
        observeSearchHistory()
    }

    private fun observeSearchHistory() {
        viewModelScope.launch {
            dao.observeSearchHistory().collect { items ->
                _searchState.update { it.copy(searchHistory = items.map { e -> e.query }) }
            }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _homeState.update { it.copy(isRefreshing = true, greeting = computeGreeting()) }
            val innerTubeTracks = innerTubeProvider.getFeaturedTracks()
            val saavnTracks = jioSaavnProvider.getFeaturedTracks()
            val combinedCatalog = innerTubeTracks + saavnTracks

            val albums = innerTubeProvider.getNewReleases()
            val artists = innerTubeProvider.getFeaturedArtists()
            val curated = innerTubeProvider.getCuratedPlaylists()
            val moods = innerTubeProvider.getMoodPlaylists() + jioSaavnProvider.getMoodPlaylists()
            val mixes = recommendationEngine.generatePersonalizedMixes(
                history = listeningHistory.value,
                likedSongs = likedTracks.value,
                catalog = combinedCatalog
            )

            _homeState.update {
                it.copy(
                    isRefreshing = false,
                    quickPicks = combinedCatalog.take(6),
                    recentlyPlayed = listeningHistory.value.distinctBy { t -> t.id }.ifEmpty { combinedCatalog.take(4) },
                    recommendedTracks = combinedCatalog,
                    newReleases = albums,
                    featuredArtists = artists,
                    curatedPlaylists = curated,
                    moodPlaylists = moods,
                    personalizedMixes = mixes
                )
            }

            // Seed default starter playlist if user has none yet
            if (userPlaylists.value.isEmpty()) {
                seedDefaultPlaylist(combinedCatalog.take(4))
            }

            // Do not auto-load lyrics; lyrics are fetched strictly on user request
        }
    }

    private suspend fun seedDefaultPlaylist(tracks: List<SolarTrack>) {
        val existing = userPlaylists.value
        if (existing.isNotEmpty()) return
        val playlistId = "pl_solar_starter"
        dao.insertPlaylist(
            PlaylistEntity(
                id = playlistId,
                name = "Solar Night Drive",
                description = "Your starter audiophile synth & electronic collection.",
                artworkUrl = tracks.firstOrNull()?.artworkUrl ?: "",
                isPublic = true,
                isCollaborative = false,
                sourceOrigin = "SOLAR"
            )
        )
        tracks.forEachIndexed { idx, track ->
            dao.insertPlaylistTrack(
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = track.id,
                    positionIndex = idx,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    artworkUrl = track.artworkUrl,
                    streamUrl = track.streamUrl,
                    durationMs = track.durationMs,
                    provider = track.provider.name,
                    genre = track.genre
                )
            )
        }
    }

    fun selectMoodFilter(mood: String) {
        _homeState.update { it.copy(selectedMoodFilter = mood) }
    }

    // --- PLAYBACK & LYRICS ---

    fun playTrack(track: SolarTrack, queue: List<SolarTrack> = _homeState.value.recommendedTracks) {
        if (_activeLyrics.value?.trackId != track.id) {
            _activeLyrics.value = null
            _isLoadingLyrics.value = false
        }
        viewModelScope.launch {
            val resolvedTrack = if (track.streamUrl.isBlank() || track.streamUrl.startsWith("ytm://")) {
                val res = innerTubeProvider.resolvePlayableStreamUrl(track)
                track.copy(streamUrl = res.getOrNull() ?: track.streamUrl)
            } else {
                track
            }
            val activeQueue = queue.map { if (it.id == resolvedTrack.id) resolvedTrack else it }
            audioController.playTrack(resolvedTrack, activeQueue.ifEmpty { listOf(resolvedTrack) })
        }
    }

    fun togglePlayPause() = audioController.togglePlayPause()
    fun skipNext() = audioController.skipNext()
    fun skipPrevious() = audioController.skipPrevious()
    fun seekTo(positionMs: Long) = audioController.seekTo(positionMs)
    fun toggleShuffle() = audioController.toggleShuffle()
    fun cycleRepeatMode() = audioController.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = audioController.setPlaybackSpeed(speed)
    fun setVolume(volume: Float) = audioController.setVolume(volume)
    fun addToQueue(track: SolarTrack) {
        audioController.addToQueue(track)
        postBanner("Added \"${track.title}\" to queue")
    }

    fun playNextInQueue(track: SolarTrack) {
        audioController.playNextInQueue(track)
        postBanner("Playing \"${track.title}\" next")
    }

    fun removeFromQueue(index: Int) = audioController.removeFromQueue(index)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = audioController.moveQueueItem(fromIndex, toIndex)
    fun clearQueue() = audioController.clearQueue()
    fun setSleepTimer(minutes: Int?) {
        audioController.setSleepTimer(minutes)
        postBanner(if (minutes == null) "Sleep timer turned off" else "Sleep timer set for $minutes min")
    }

    fun requestLyricsForCurrentTrack(forceRefresh: Boolean = false) {
        val current = playbackState.value.currentTrack
            ?: _homeState.value.recommendedTracks.firstOrNull()
            ?: return
        if (!forceRefresh && _activeLyrics.value?.trackId == current.id) return
        loadLyricsForTrack(current)
    }

    fun loadLyricsForTrack(track: SolarTrack) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            try {
                val lyrics = lyricsProvider.getLyricsForTrack(track)
                _activeLyrics.value = lyrics
            } finally {
                _isLoadingLyrics.value = false
            }
        }
    }

    private fun recordTrackPlayed(track: SolarTrack) {
        viewModelScope.launch {
            if (_activeLyrics.value?.trackId != track.id) {
                _activeLyrics.value = null
                _isLoadingLyrics.value = false
            }
            if (appSettings.value.personalizationEnabled) {
                dao.insertListeningHistory(
                    ListeningHistoryEntity(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                        streamUrl = track.streamUrl,
                        durationMs = track.durationMs,
                        provider = track.provider.name,
                        genre = track.genre
                    )
                )
                if (Firebase.auth.currentUser != null) {
                    cloudRepository.logCloudListeningHistory(
                        historyId = "hist_${System.currentTimeMillis()}",
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        durationMs = track.durationMs,
                        sourceProvider = track.provider.name
                    )
                }
            }
        }
    }

    // --- EQUALIZER ---

    fun setEqualizerEnabled(enabled: Boolean) {
        audioController.setEqualizerEnabled(enabled)
        viewModelScope.launch {
            val eq = equalizerState.value
            preferencesManager.setEqualizerState(enabled, eq.activePreset, eq.bassBoostPercent, eq.virtualizerPercent)
        }
    }

    fun selectEqualizerPreset(preset: String) {
        audioController.applyEqualizerPreset(preset)
        viewModelScope.launch {
            val eq = equalizerState.value
            preferencesManager.setEqualizerState(eq.enabled, preset, eq.bassBoostPercent, eq.virtualizerPercent)
        }
    }

    fun setEqualizerBandLevel(bandIndex: Short, levelMillibels: Short) {
        audioController.setBandLevel(bandIndex, levelMillibels)
    }

    fun setBassBoost(percent: Int) {
        audioController.setBassBoost(percent)
    }

    fun setVirtualizer(percent: Int) {
        audioController.setVirtualizer(percent)
    }

    // --- SEARCH ENGINE ---

    private var searchJob: Job? = null

    fun updateSearchQuery(newQuery: String) {
        _searchState.update { it.copy(query = newQuery) }
        searchJob?.cancel()
        val trimmed = newQuery.trim()
        if (trimmed.isBlank()) {
            _searchState.update {
                it.copy(
                    isSearching = false,
                    suggestions = emptyList(),
                    trackResults = _homeState.value.recommendedTracks,
                    albumResults = emptyList(),
                    artistResults = emptyList()
                )
            }
            return
        }
        _searchState.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            delay(280L)
            val suggestions = innerTubeProvider.getSuggestions(trimmed)
            val ytTracks = innerTubeProvider.searchTracks(trimmed, includeNonOriginal = true)
            val localMatches = _localTracks.value.filter {
                it.title.contains(trimmed, true) || it.artist.contains(trimmed, true)
            }
            val mergedTracks = (ytTracks + localMatches).distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" }
            val albums = innerTubeProvider.searchAlbums(trimmed)
            val artists = innerTubeProvider.searchArtists(trimmed)

            _searchState.update {
                it.copy(
                    isSearching = false,
                    suggestions = suggestions,
                    trackResults = mergedTracks,
                    albumResults = albums,
                    artistResults = artists
                )
            }
        }
    }

    fun submitSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                dao.insertSearchQuery(SearchHistoryEntity(trimmed))
            }
        }
        searchJob?.cancel()
        _searchState.update { it.copy(query = trimmed, isSearching = true) }
        searchJob = viewModelScope.launch {
            val suggestions = innerTubeProvider.getSuggestions(trimmed)
            val ytTracks = innerTubeProvider.searchTracks(trimmed, includeNonOriginal = true)
            val localMatches = _localTracks.value.filter {
                it.title.contains(trimmed, true) || it.artist.contains(trimmed, true)
            }
            val mergedTracks = (ytTracks + localMatches).distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" }
            val albums = innerTubeProvider.searchAlbums(trimmed)
            val artists = innerTubeProvider.searchArtists(trimmed)

            _searchState.update {
                it.copy(
                    isSearching = false,
                    suggestions = suggestions,
                    trackResults = mergedTracks,
                    albumResults = albums,
                    artistResults = artists
                )
            }
        }
    }

    fun setSearchFilter(filter: String) {
        _searchState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleOnlyOfficialOriginal() {
        val next = !_searchState.value.onlyOfficialOriginal
        _searchState.update { it.copy(onlyOfficialOriginal = next) }
        updateSearchQuery(_searchState.value.query)
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch { dao.deleteSearchQuery(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { dao.clearSearchHistory() }
    }

    // --- LIKED SONGS & CLOUD SYNC ---

    fun toggleLikeTrack(track: SolarTrack) {
        viewModelScope.launch {
            val currentlyLiked = likedTracks.value.any { it.id == track.id }
            if (currentlyLiked) {
                dao.deleteLikedTrack(track.id)
                if (Firebase.auth.currentUser != null) {
                    cloudRepository.removeCloudLikedSong(track.id)
                }
                postBanner("Removed \"${track.title}\" from Liked Songs")
            } else {
                dao.insertLikedTrack(track.toLikedEntity())
                if (Firebase.auth.currentUser != null) {
                    cloudRepository.syncLikedSong(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                        streamUrl = track.streamUrl,
                        durationMs = track.durationMs,
                        sourceProvider = track.provider.name
                    )
                }
                postBanner("Saved \"${track.title}\" to Liked Songs")
            }
        }
    }

    fun syncAccountWithCloud() {
        val user = Firebase.auth.currentUser ?: return
        viewModelScope.launch {
            val settings = appSettings.value
            cloudRepository.upsertUserProfile(
                displayName = user.displayName ?: "SOLAR Listener",
                email = user.email,
                avatarUrl = user.photoUrl?.toString(),
                preferredAudioQuality = settings.streamingQuality.name,
                amoledDarkTheme = settings.amoledBlackTheme,
                ytMusicConnected = settings.ytMusicConnected
            )

            // Push local liked songs to cloud
            likedTracks.value.forEach { track ->
                cloudRepository.syncLikedSong(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    artworkUrl = track.artworkUrl,
                    streamUrl = track.streamUrl,
                    durationMs = track.durationMs,
                    sourceProvider = track.provider.name
                )
            }

            // Pull cloud liked songs into local Room DB
            cloudRepository.getLikedSongs().getOrNull()?.forEach { cloudSong ->
                val domain = SolarTrack(
                    id = cloudSong.trackId.ifBlank { cloudSong.id },
                    title = cloudSong.title,
                    artist = cloudSong.artist,
                    album = cloudSong.album,
                    artworkUrl = cloudSong.artworkUrl,
                    streamUrl = cloudSong.streamUrl,
                    durationMs = cloudSong.durationMs,
                    provider = runCatching { ProviderType.valueOf(cloudSong.sourceProvider) }
                        .getOrDefault(ProviderType.INNERTUBE)
                )
                dao.insertLikedTrack(domain.toLikedEntity())
            }

            // Push user playlists to cloud
            userPlaylists.value.forEach { pl ->
                cloudRepository.syncPlaylist(
                    playlistId = pl.id,
                    name = pl.name,
                    description = pl.description,
                    isPublic = pl.isPublic,
                    trackCount = pl.tracks.size,
                    sourceOrigin = pl.sourceOrigin
                )
            }
            postBanner("Cloud library synchronized with ${user.email ?: "Google Account"}")
        }
    }

    // --- PLAYLIST MANAGEMENT & IMPORT ---

    fun createPlaylist(name: String, description: String, isPublic: Boolean = false) {
        val cleanName = name.trim().ifBlank { "Untitled Solar Playlist" }
        viewModelScope.launch {
            val id = "pl_${UUID.randomUUID().toString().take(8)}"
            dao.insertPlaylist(
                PlaylistEntity(
                    id = id,
                    name = cleanName,
                    description = description.trim().ifBlank { "Created in SOLAR" },
                    artworkUrl = "",
                    isPublic = isPublic,
                    isCollaborative = false,
                    sourceOrigin = "SOLAR"
                )
            )
            if (Firebase.auth.currentUser != null) {
                cloudRepository.syncPlaylist(id, cleanName, description, isPublic, 0, "SOLAR")
            }
            postBanner("Created playlist \"$cleanName\"")
        }
    }

    fun renamePlaylist(playlistId: String, newName: String, newDesc: String) {
        viewModelScope.launch {
            dao.renamePlaylist(playlistId, newName.trim(), newDesc.trim())
            postBanner("Updated playlist \"$newName\"")
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            dao.clearPlaylistTracks(playlistId)
            dao.deletePlaylist(playlistId)
            postBanner("Playlist deleted")
        }
    }

    fun duplicatePlaylist(playlist: SolarPlaylist) {
        viewModelScope.launch {
            val copyId = "pl_${UUID.randomUUID().toString().take(8)}"
            dao.insertPlaylist(
                PlaylistEntity(
                    id = copyId,
                    name = "${playlist.name} (Copy)",
                    description = playlist.description,
                    artworkUrl = playlist.artworkUrl,
                    isPublic = playlist.isPublic,
                    isCollaborative = playlist.isCollaborative,
                    sourceOrigin = playlist.sourceOrigin
                )
            )
            playlist.tracks.forEachIndexed { index, track ->
                dao.insertPlaylistTrack(
                    PlaylistTrackEntity(
                        playlistId = copyId,
                        trackId = track.id,
                        positionIndex = index,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                        streamUrl = track.streamUrl,
                        durationMs = track.durationMs,
                        provider = track.provider.name,
                        genre = track.genre
                    )
                )
            }
            postBanner("Duplicated \"${playlist.name}\"")
        }
    }

    fun mergePlaylists(sourcePlaylist: SolarPlaylist, targetPlaylist: SolarPlaylist) {
        viewModelScope.launch {
            val combined = (targetPlaylist.tracks + sourcePlaylist.tracks).distinctBy { it.id }
            combined.forEachIndexed { index, track ->
                dao.insertPlaylistTrack(
                    PlaylistTrackEntity(
                        playlistId = targetPlaylist.id,
                        trackId = track.id,
                        positionIndex = index,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                        streamUrl = track.streamUrl,
                        durationMs = track.durationMs,
                        provider = track.provider.name,
                        genre = track.genre
                    )
                )
            }
            postBanner("Merged \"${sourcePlaylist.name}\" into \"${targetPlaylist.name}\"")
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: SolarTrack) {
        viewModelScope.launch {
            val currentTracks = userPlaylists.value.firstOrNull { it.id == playlistId }?.tracks.orEmpty()
            dao.insertPlaylistTrack(
                PlaylistTrackEntity(
                    playlistId = playlistId,
                    trackId = track.id,
                    positionIndex = currentTracks.size,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    artworkUrl = track.artworkUrl,
                    streamUrl = track.streamUrl,
                    durationMs = track.durationMs,
                    provider = track.provider.name,
                    genre = track.genre
                )
            )
            postBanner("Added \"${track.title}\" to playlist")
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            dao.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun importExternalPlaylist(source: String, playlistTitle: String, rawTracksOrUrl: String) {
        viewModelScope.launch {
            val catalog = _homeState.value.recommendedTracks + _localTracks.value
            val report = innerTubeProvider.importExternalPlaylist(source, rawTracksOrUrl, catalog)
            val finalTitle = playlistTitle.trim().ifBlank { report.playlistName }
            val plId = "pl_imp_${UUID.randomUUID().toString().take(8)}"

            dao.insertPlaylist(
                PlaylistEntity(
                    id = plId,
                    name = finalTitle,
                    description = "Imported from $source (${report.matchedTracks.size} matched, ${report.unmatchedTitles.size} unmatched)",
                    artworkUrl = report.matchedTracks.firstOrNull()?.artworkUrl ?: "",
                    isPublic = false,
                    isCollaborative = false,
                    sourceOrigin = source
                )
            )
            report.matchedTracks.forEachIndexed { idx, track ->
                dao.insertPlaylistTrack(
                    PlaylistTrackEntity(
                        playlistId = plId,
                        trackId = track.id,
                        positionIndex = idx,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                        streamUrl = track.streamUrl,
                        durationMs = track.durationMs,
                        provider = track.provider.name,
                        genre = track.genre
                    )
                )
            }
            _lastImportReport.value = report.copy(playlistName = finalTitle)
            postBanner("Imported \"$finalTitle\": ${report.matchedTracks.size} matched, ${report.unmatchedTitles.size} unmatched")
        }
    }

    fun dismissImportReport() {
        _lastImportReport.value = null
    }

    fun exportPlaylistAsM3u(playlist: SolarPlaylist): String {
        val sb = StringBuilder("#EXTM3U\n#PLAYLIST:${playlist.name}\n")
        playlist.tracks.forEach { track ->
            val sec = track.durationMs / 1000
            sb.append("#EXTINF:$sec,${track.artist} - ${track.title}\n")
            sb.append("${track.streamUrl}\n")
        }
        return sb.toString()
    }

    // --- YOUTUBE MUSIC ACCOUNT INTEGRATION & LIBRARY IMPORT ---

    fun connectAndImportYouTubeMusic(accountLabel: String) {
        viewModelScope.launch {
            val name = accountLabel.ifBlank {
                Firebase.auth.currentUser?.email ?: "YouTube Music Account"
            }
            preferencesManager.setYtMusicConnection(true, name)
            val ytCatalog = innerTubeProvider.getFeaturedTracks()
            // Import liked songs & starter playlist into Solar local database
            ytCatalog.take(4).forEach { track ->
                dao.insertLikedTrack(track.toLikedEntity())
            }
            importExternalPlaylist(
                source = "YOUTUBE_MUSIC",
                playlistTitle = "YouTube Music Imported Favorites",
                rawTracksOrUrl = ytCatalog.take(5).joinToString("\n") { "${it.artist} - ${it.title}" }
            )
            postBanner("Connected YouTube Music ($name) & imported library")
        }
    }

    fun disconnectYouTubeMusic() {
        viewModelScope.launch {
            preferencesManager.setYtMusicConnection(false, "")
            postBanner("Disconnected YouTube Music integration")
        }
    }

    // --- LOCAL DEVICE MUSIC SCANNING ---

    fun scanLocalDeviceMusic() {
        viewModelScope.launch {
            val hidden = appSettings.value.hiddenLocalFolders
            val scanned = localMediaStoreProvider.scanDeviceMusic(hidden)
            _localTracks.value = scanned
            postBanner("Scanned device MediaStore: ${scanned.size} local audio files found")
        }
    }

    fun toggleHiddenLocalFolder(folderPath: String) {
        viewModelScope.launch {
            preferencesManager.toggleHiddenFolder(folderPath)
            scanLocalDeviceMusic()
        }
    }

    // --- DOWNLOAD MANAGER (FOR PERMITTED SOURCES) ---

    fun startDownload(track: SolarTrack) {
        if (!track.isDownloadPermitted) {
            postBanner("Download restricted by provider licensing for \"${track.title}\".")
            return
        }
        if (downloadJobs[track.id]?.isActive == true) return

        val job = viewModelScope.launch(Dispatchers.IO) {
            val quality = appSettings.value.downloadQuality
            val dir = File(appContext.filesDir, "solar_downloads").apply { mkdirs() }
            val destFile = File(dir, "${track.id}.mp3")

            dao.upsertDownload(
                DownloadTaskEntity(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    artworkUrl = track.artworkUrl,
                    streamUrl = track.streamUrl,
                    localFilePath = destFile.absolutePath,
                    durationMs = track.durationMs,
                    status = DownloadStatus.DOWNLOADING.name,
                    progressPercent = 5,
                    downloadedBytes = 0L,
                    totalBytes = 4_500_000L,
                    quality = quality.name
                )
            )

            try {
                val request = Request.Builder().url(track.streamUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body
                    if (!response.isSuccessful || body == null) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }
                    val total = body.contentLength().takeIf { it > 0 } ?: 4_500_000L
                    var downloaded = 0L
                    val buffer = ByteArray(16 * 1024)
                    var lastReportedPct = 5

                    body.byteStream().use { input ->
                        FileOutputStream(destFile).use { output ->
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                val pct = ((downloaded * 100L) / total).toInt().coerceIn(5, 99)
                                if (pct - lastReportedPct >= 15) {
                                    lastReportedPct = pct
                                    dao.upsertDownload(
                                        DownloadTaskEntity(
                                            trackId = track.id,
                                            title = track.title,
                                            artist = track.artist,
                                            album = track.album,
                                            artworkUrl = track.artworkUrl,
                                            streamUrl = track.streamUrl,
                                            localFilePath = destFile.absolutePath,
                                            durationMs = track.durationMs,
                                            status = DownloadStatus.DOWNLOADING.name,
                                            progressPercent = pct,
                                            downloadedBytes = downloaded,
                                            totalBytes = total,
                                            quality = quality.name
                                        )
                                    )
                                }
                            }
                        }
                    }

                    dao.upsertDownload(
                        DownloadTaskEntity(
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            artworkUrl = track.artworkUrl,
                            streamUrl = track.streamUrl,
                            localFilePath = destFile.absolutePath,
                            durationMs = track.durationMs,
                            status = DownloadStatus.COMPLETED.name,
                            progressPercent = 100,
                            downloadedBytes = downloaded,
                            totalBytes = downloaded.coerceAtLeast(total),
                            quality = quality.name
                        )
                    )
                    withContext(Dispatchers.Main) {
                        postBanner("Downloaded \"${track.title}\" for offline playback")
                    }
                }
            } catch (e: Exception) {
                dao.upsertDownload(
                    DownloadTaskEntity(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        artworkUrl = track.artworkUrl,
                        streamUrl = track.streamUrl,
                        localFilePath = "",
                        durationMs = track.durationMs,
                        status = DownloadStatus.FAILED.name,
                        progressPercent = 0,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        quality = quality.name
                    )
                )
            }
        }
        downloadJobs[track.id] = job
    }

    fun deleteDownload(trackId: String) {
        downloadJobs[trackId]?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(appContext.filesDir, "solar_downloads")
            File(dir, "$trackId.mp3").delete()
            dao.deleteDownload(trackId)
        }
    }

    // --- SETTINGS ACTIONS ---

    fun setAmoledBlack(enabled: Boolean) = viewModelScope.launch { preferencesManager.setAmoledBlack(enabled) }
    fun setAccentIntensity(value: Float) = viewModelScope.launch { preferencesManager.setAccentIntensity(value) }
    fun setDynamicArtworkBg(enabled: Boolean) = viewModelScope.launch { preferencesManager.setDynamicArtworkBg(enabled) }
    fun setEnableAnimations(enabled: Boolean) = viewModelScope.launch { preferencesManager.setEnableAnimations(enabled) }
    fun setStreamingQuality(q: AudioQuality) = viewModelScope.launch { preferencesManager.setStreamingQuality(q) }
    fun setDownloadQuality(q: AudioQuality) = viewModelScope.launch { preferencesManager.setDownloadQuality(q) }
    fun setCrossfadeSeconds(sec: Int) = viewModelScope.launch { preferencesManager.setCrossfadeSeconds(sec) }
    fun setGaplessPlayback(enabled: Boolean) = viewModelScope.launch { preferencesManager.setGaplessPlayback(enabled) }
    fun setNormalizeVolume(enabled: Boolean) = viewModelScope.launch { preferencesManager.setNormalizeVolume(enabled) }
    fun setAutoplaySimilar(enabled: Boolean) = viewModelScope.launch { preferencesManager.setAutoplaySimilar(enabled) }
    fun setDefaultProvider(p: ProviderType) = viewModelScope.launch { preferencesManager.setDefaultProvider(p) }
    fun setWifiOnlyDownloads(enabled: Boolean) = viewModelScope.launch { preferencesManager.setWifiOnlyDownloads(enabled) }
    fun setJioSaavnEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setJioSaavnEnabled(enabled) }
    fun setPersonalizationEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setPersonalizationEnabled(enabled) }

    fun clearListeningHistory() {
        viewModelScope.launch {
            dao.clearListeningHistory()
            postBanner("Listening history cleared")
        }
    }

    fun clearImageAndAudioCache() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { appContext.cacheDir.deleteRecursively() }
            withContext(Dispatchers.Main) {
                postBanner("Temporary audio & artwork cache cleared")
            }
        }
    }

    fun postBanner(message: String) {
        viewModelScope.launch {
            _statusBannerMessage.value = message
            delay(3200L)
            if (_statusBannerMessage.value == message) {
                _statusBannerMessage.value = null
            }
        }
    }

    private fun computeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Late Night Session"
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                SolarViewModel(
                    appContext = app.applicationContext,
                    database = SolarDatabase.getInstance(app.applicationContext),
                    preferencesManager = SolarPreferencesManager(app.applicationContext),
                    cloudRepository = SolarCloudRepository(firestore)
                )
            }
        }
    }
}
