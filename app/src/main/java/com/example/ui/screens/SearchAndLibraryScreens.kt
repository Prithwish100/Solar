package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.domain.model.PlaylistImportReport
import com.example.domain.model.SolarPlaylist
import com.example.domain.model.SolarTrack
import com.example.ui.SearchUiState
import com.example.ui.components.SolarAlbumCard
import com.example.ui.components.SolarArtistBubble
import com.example.ui.components.SolarQrShareDialog
import com.example.ui.components.SolarTrackRow
import com.example.ui.theme.SolarBurntOrange
import com.example.ui.theme.SolarCardBackground
import com.example.ui.theme.SolarDeepBlack
import com.example.ui.theme.SolarDivider
import com.example.ui.theme.SolarElevatedSurface
import com.example.ui.theme.SolarMutedGrey
import com.example.ui.theme.SolarPrimaryOrange
import com.example.ui.theme.SolarWarmAmber
import com.example.ui.theme.SolarWhite

@Composable
fun SolarSearchScreen(
    searchState: SearchUiState,
    currentPlayingTrack: SolarTrack?,
    likedTrackIds: Set<String>,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onSelectFilter: (String) -> Unit,
    onToggleOfficialOriginal: () -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    onPlayTrack: (SolarTrack, List<SolarTrack>) -> Unit,
    onToggleLike: (SolarTrack) -> Unit,
    onPlayNext: (SolarTrack) -> Unit,
    onAddToQueue: (SolarTrack) -> Unit,
    onAddToPlaylistRequest: (SolarTrack) -> Unit,
    onDownloadTrack: (SolarTrack) -> Unit
) {
    val filters = listOf("All", "Songs", "Albums", "Artists", "Local")

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onSubmitSearch(spoken)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen_list"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.displayMedium,
                    color = SolarWhite
                )
                Text(
                    text = "Search and stream any song, artist, or album",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarMutedGrey
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchState.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field"),
                    placeholder = {
                        Text("Search songs, artists, albums...", color = SolarMutedGrey)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SolarBurntOrange)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchState.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = SolarMutedGrey)
                                }
                            }
                            IconButton(
                                onClick = {
                                    runCatching {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a song or artist name")
                                        }
                                        voiceLauncher.launch(intent)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = SolarWarmAmber)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SolarBurntOrange,
                        unfocusedBorderColor = SolarDivider,
                        focusedContainerColor = SolarCardBackground,
                        unfocusedContainerColor = SolarCardBackground,
                        focusedTextColor = SolarWhite,
                        unfocusedTextColor = SolarWhite
                    )
                )
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { f ->
                    val selected = searchState.selectedFilter == f
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilter(f) },
                        label = { Text(f) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarBurntOrange,
                            selectedLabelColor = SolarWhite,
                            containerColor = SolarCardBackground,
                            labelColor = SolarMutedGrey
                        )
                    )
                }
            }
        }

        // Suggestions & Recent Searches when query is empty
        if (searchState.query.isBlank()) {
            if (searchState.searchHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", style = MaterialTheme.typography.titleMedium, color = SolarWhite)
                        TextButton(onClick = onClearHistory) {
                            Text("Clear All", color = SolarBurntOrange)
                        }
                    }
                }
                items(searchState.searchHistory) { histQuery ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubmitSearch(histQuery) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = SolarMutedGrey)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(histQuery, color = SolarWhite, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onRemoveHistoryItem(histQuery) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Remove", tint = SolarMutedGrey)
                        }
                    }
                }
            }

            item {
                SectionTitle("Trending Searches", "Tap to search official masters")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchState.trendingSearches) { trending ->
                        Surface(
                            modifier = Modifier.clickable { onSubmitSearch(trending) },
                            color = SolarCardBackground,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = trending,
                                color = SolarWarmAmber,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Instant Autocomplete Suggestions
            if (searchState.suggestions.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(searchState.suggestions) { suggestion ->
                            Surface(
                                modifier = Modifier.clickable { onSubmitSearch(suggestion) },
                                color = SolarElevatedSurface,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    color = SolarWarmAmber,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (searchState.isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SolarBurntOrange)
                }
            }
        }

        // Artist Results
        if ((searchState.selectedFilter == "All" || searchState.selectedFilter == "Artists") &&
            searchState.artistResults.isNotEmpty()
        ) {
            item {
                SectionTitle("Verified Artists", null)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(searchState.artistResults) { artist ->
                        SolarArtistBubble(
                            artist = artist,
                            onClick = {
                                val first = artist.topTracks.firstOrNull()
                                if (first != null) onPlayTrack(first, artist.topTracks)
                            }
                        )
                    }
                }
            }
        }

        // Album Results
        if ((searchState.selectedFilter == "All" || searchState.selectedFilter == "Albums") &&
            searchState.albumResults.isNotEmpty()
        ) {
            item {
                SectionTitle("Matching Albums", null)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchState.albumResults) { album ->
                        SolarAlbumCard(
                            album = album,
                            onClick = {
                                val first = album.tracks.firstOrNull()
                                if (first != null) onPlayTrack(first, album.tracks)
                            }
                        )
                    }
                }
            }
        }

        // Track Results
        if (searchState.selectedFilter in listOf("All", "Songs", "Local")) {
            val filteredTracks = if (searchState.selectedFilter == "Local") {
                searchState.trackResults.filter { it.provider.name == "LOCAL" }
            } else {
                searchState.trackResults
            }

            item {
                SectionTitle(
                    title = if (searchState.query.isBlank()) "Featured Songs" else "Tracks (${filteredTracks.size})",
                    subtitle = null
                )
            }

            items(filteredTracks, key = { "search_${it.id}" }) { track ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SolarTrackRow(
                        track = track,
                        isPlaying = currentPlayingTrack?.id == track.id,
                        isLiked = likedTrackIds.contains(track.id),
                        onPlayClick = {
                            onSubmitSearch(searchState.query)
                            onPlayTrack(track, filteredTracks)
                        },
                        onLikeClick = { onToggleLike(track) },
                        onPlayNext = { onPlayNext(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onAddToPlaylist = { onAddToPlaylistRequest(track) },
                        onDownloadClick = { onDownloadTrack(track) },
                        showAccuracyBadge = false,
                        showProviderBadge = false
                    )
                }
            }
        }
    }
}

@Composable
fun SolarLibraryScreen(
    likedTracks: List<SolarTrack>,
    playlists: List<SolarPlaylist>,
    localTracks: List<SolarTrack>,
    downloads: List<DownloadItem>,
    history: List<SolarTrack>,
    hiddenFolders: Set<String>,
    lastImportReport: PlaylistImportReport?,
    currentPlayingTrack: SolarTrack?,
    onPlayTrack: (SolarTrack, List<SolarTrack>) -> Unit,
    onToggleLike: (SolarTrack) -> Unit,
    onPlayNext: (SolarTrack) -> Unit,
    onAddToQueue: (SolarTrack) -> Unit,
    onAddToPlaylistRequest: (SolarTrack) -> Unit,
    onCreatePlaylist: (String, String, Boolean) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDuplicatePlaylist: (SolarPlaylist) -> Unit,
    onMergePlaylists: (SolarPlaylist, SolarPlaylist) -> Unit,
    onImportExternalPlaylist: (String, String, String) -> Unit,
    onDismissImportReport: () -> Unit,
    onExportPlaylistM3u: (SolarPlaylist) -> String,
    onScanLocalMusic: () -> Unit,
    onToggleHiddenFolder: (String) -> Unit,
    onStartDownload: (SolarTrack) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Playlists") }
    val tabs = listOf("Playlists", "Liked Songs", "Local Files", "Downloads", "History")

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showImportPlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylistForDetail by remember { mutableStateOf<SolarPlaylist?>(null) }
    var qrSharePlaylist by remember { mutableStateOf<SolarPlaylist?>(null) }
    var exportedM3uPreview by remember { mutableStateOf<Pair<String, String>?>(null) }
    var localSortBy by remember { mutableStateOf("Title") }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onScanLocalMusic()
    }

    val likedIds = remember(likedTracks) { likedTracks.map { it.id }.toSet() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen_list"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.displayMedium,
                    color = SolarWhite
                )
                Text(
                    text = "Playlists, Cloud Favorites, Offline Downloads & Local MediaStore Audio",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarMutedGrey
                )
            }
        }

        // Library Category Tabs
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs) { tab ->
                    val active = selectedTab == tab
                    FilterChip(
                        selected = active,
                        onClick = { selectedTab = tab },
                        label = { Text(tab) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarBurntOrange,
                            selectedLabelColor = SolarWhite,
                            containerColor = SolarCardBackground,
                            labelColor = SolarMutedGrey
                        )
                    )
                }
            }
        }

        when (selectedTab) {
            "Playlists" -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCreatePlaylistDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_playlist_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = SolarWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Playlist", color = SolarWhite)
                        }
                        OutlinedButton(
                            onClick = { showImportPlaylistDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_playlist_btn")
                        ) {
                            Icon(Icons.Default.ImportExport, contentDescription = null, tint = SolarWarmAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import YT / M3U", color = SolarWarmAmber)
                        }
                    }
                }

                items(playlists, key = { it.id }) { pl ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { selectedPlaylistForDetail = pl },
                        colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pl.name, style = MaterialTheme.typography.titleMedium, color = SolarWhite)
                                    Text(
                                        "${pl.tracks.size} tracks • Origin: ${pl.sourceOrigin} • ${if (pl.isPublic) "Public" else "Private"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarWarmAmber
                                    )
                                    if (pl.description.isNotBlank()) {
                                        Text(
                                            pl.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SolarMutedGrey,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val first = pl.tracks.firstOrNull()
                                        if (first != null) onPlayTrack(first, pl.tracks)
                                    }
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Playlist", tint = SolarBurntOrange)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { onDuplicatePlaylist(pl) }) {
                                    Icon(Icons.Default.ContentCopy, null, tint = SolarMutedGrey, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Duplicate", color = SolarMutedGrey)
                                }
                                if (playlists.size > 1) {
                                    TextButton(
                                        onClick = {
                                            val other = playlists.firstOrNull { it.id != pl.id }
                                            if (other != null) onMergePlaylists(pl, other)
                                        }
                                    ) {
                                        Icon(Icons.Default.MergeType, null, tint = SolarMutedGrey, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Merge", color = SolarMutedGrey)
                                    }
                                }
                                TextButton(onClick = { qrSharePlaylist = pl }) {
                                    Icon(Icons.Default.QrCode2, null, tint = SolarWarmAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("QR", color = SolarWarmAmber)
                                }
                                TextButton(onClick = { exportedM3uPreview = pl.name to onExportPlaylistM3u(pl) }) {
                                    Text("M3U", color = SolarWarmAmber)
                                }
                                TextButton(onClick = { onDeletePlaylist(pl.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = SolarMutedGrey, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            "Liked Songs" -> {
                item {
                    SectionTitle(
                        title = "Liked Songs (${likedTracks.size})",
                        subtitle = "Synchronized with SOLAR Cloud & available across devices"
                    )
                }
                if (likedTracks.isEmpty()) {
                    item {
                        EmptyLibraryBox("Tap the heart icon on any track to add it to your Liked Songs.")
                    }
                } else {
                    items(likedTracks, key = { "liked_${it.id}" }) { track ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SolarTrackRow(
                                track = track,
                                isPlaying = currentPlayingTrack?.id == track.id,
                                isLiked = true,
                                onPlayClick = { onPlayTrack(track, likedTracks) },
                                onLikeClick = { onToggleLike(track) },
                                onPlayNext = { onPlayNext(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onAddToPlaylist = { onAddToPlaylistRequest(track) },
                                onDownloadClick = { onStartDownload(track) }
                            )
                        }
                    }
                }
            }

            "Local Files" -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Local MediaStore Scanner", style = MaterialTheme.typography.titleMedium, color = SolarWhite)
                                    Text(
                                        "Supports MP3, FLAC, WAV, AAC, M4A, OGG & OPUS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarWarmAmber
                                    )
                                }
                                Button(
                                    onClick = { permissionLauncher.launch(audioPermission) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = SolarWhite)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan Device", color = SolarWhite)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Sort by:", style = MaterialTheme.typography.labelSmall, color = SolarMutedGrey)
                                listOf("Title", "Artist", "Duration").forEach { sortKey ->
                                    FilterChip(
                                        selected = localSortBy == sortKey,
                                        onClick = { localSortBy = sortKey },
                                        label = { Text(sortKey) }
                                    )
                                }
                            }
                        }
                    }
                }

                val sortedLocal = when (localSortBy) {
                    "Artist" -> localTracks.sortedBy { it.artist }
                    "Duration" -> localTracks.sortedByDescending { it.durationMs }
                    else -> localTracks.sortedBy { it.title }
                }

                val folders = localTracks.mapNotNull { it.localFolderPath }.distinct()
                if (folders.isNotEmpty()) {
                    item {
                        SectionTitle("Device Audio Folders", "Tap to hide/exclude folder")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(folders) { folder ->
                                FilterChip(
                                    selected = !hiddenFolders.contains(folder),
                                    onClick = { onToggleHiddenFolder(folder) },
                                    leadingIcon = { Icon(Icons.Default.Folder, null, tint = SolarWarmAmber) },
                                    label = { Text(folder) }
                                )
                            }
                        }
                    }
                }

                if (sortedLocal.isEmpty()) {
                    item {
                        EmptyLibraryBox("No local audio files scanned yet. Tap 'Scan Device' above to index device MP3/FLAC/WAV files.")
                    }
                } else {
                    items(sortedLocal, key = { it.id }) { track ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SolarTrackRow(
                                track = track,
                                isPlaying = currentPlayingTrack?.id == track.id,
                                isLiked = likedIds.contains(track.id),
                                onPlayClick = { onPlayTrack(track, sortedLocal) },
                                onLikeClick = { onToggleLike(track) },
                                onPlayNext = { onPlayNext(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onAddToPlaylist = { onAddToPlaylistRequest(track) },
                                onDownloadClick = {}
                            )
                        }
                    }
                }
            }

            "Downloads" -> {
                val totalBytes = downloads.sumOf { it.downloadedBytes }
                val totalMb = "%.1f MB".format(totalBytes / (1024f * 1024f))
                item {
                    SectionTitle(
                        title = "Offline Downloads (${downloads.size})",
                        subtitle = "Storage used: $totalMb • Permitted offline tracks"
                    )
                }
                if (downloads.isEmpty()) {
                    item {
                        EmptyLibraryBox("No offline downloads yet. Open any track menu and select 'Download Offline'.")
                    }
                } else {
                    items(downloads, key = { "dl_${it.track.id}" }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .clickable { onPlayTrack(item.track, downloads.map { it.track }) },
                            colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.track.title, style = MaterialTheme.typography.titleSmall, color = SolarWhite)
                                        Text(
                                            "${item.track.artist} • ${item.quality.label} • ${item.status.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SolarWarmAmber
                                        )
                                    }
                                    Row {
                                        if (item.status == DownloadStatus.COMPLETED) {
                                            Icon(
                                                Icons.Default.DownloadDone,
                                                contentDescription = "Downloaded",
                                                tint = SolarBurntOrange,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                        IconButton(onClick = { onDeleteDownload(item.track.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Download", tint = SolarMutedGrey)
                                        }
                                    }
                                }
                                if (item.status == DownloadStatus.DOWNLOADING) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { item.progressPercent / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = SolarBurntOrange
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "History" -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Listening History (${history.size})", style = MaterialTheme.typography.titleLarge, color = SolarWhite)
                        if (history.isNotEmpty()) {
                            TextButton(onClick = onClearHistory) {
                                Text("Clear History", color = SolarBurntOrange)
                            }
                        }
                    }
                }
                if (history.isEmpty()) {
                    item {
                        EmptyLibraryBox("Your recently played songs will appear here automatically.")
                    }
                } else {
                    items(history, key = { "hist_${it.id}_${it.hashCode()}" }) { track ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SolarTrackRow(
                                track = track,
                                isPlaying = currentPlayingTrack?.id == track.id,
                                isLiked = likedIds.contains(track.id),
                                onPlayClick = { onPlayTrack(track, history) },
                                onLikeClick = { onToggleLike(track) },
                                onPlayNext = { onPlayNext(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onAddToPlaylist = { onAddToPlaylistRequest(track) },
                                onDownloadClick = { onStartDownload(track) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var isPublic by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = SolarCardBackground,
            title = { Text("Create SOLAR Playlist", color = SolarWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Playlist Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Public in SOLAR Cloud", color = SolarWhite)
                        Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePlaylist(name, desc, isPublic)
                        showCreatePlaylistDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                ) {
                    Text("Create", color = SolarWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = SolarMutedGrey)
                }
            }
        )
    }

    // Import External Playlist Dialog (YouTube Music / JioSaavn / M3U)
    if (showImportPlaylistDialog) {
        var selectedSource by remember { mutableStateOf("YOUTUBE_MUSIC") }
        var playlistTitle by remember { mutableStateOf("Imported Favorites") }
        var rawLines by remember {
            mutableStateOf("Helios Pulse - Midnight Horizon\nAethelgard - Amber Resonance\nNova Drift - Obsidian Echoes")
        }

        AlertDialog(
            onDismissRequest = { showImportPlaylistDialog = false },
            containerColor = SolarCardBackground,
            title = { Text("Import External Playlist", color = SolarWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("YOUTUBE_MUSIC", "JIOSAAVN", "M3U").forEach { src ->
                            FilterChip(
                                selected = selectedSource == src,
                                onClick = { selectedSource = src },
                                label = { Text(src) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = playlistTitle,
                        onValueChange = { playlistTitle = it },
                        label = { Text("Target Playlist Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rawLines,
                        onValueChange = { rawLines = it },
                        label = { Text("Paste M3U lines, Track list, or Playlist URL") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportExternalPlaylist(selectedSource, playlistTitle, rawLines)
                        showImportPlaylistDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                ) {
                    Text("Match & Import", color = SolarWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportPlaylistDialog = false }) {
                    Text("Cancel", color = SolarMutedGrey)
                }
            }
        )
    }

    // Import Report Modal (Shows matched vs unmatched tracks without silent substitution)
    lastImportReport?.let { report ->
        AlertDialog(
            onDismissRequest = onDismissImportReport,
            containerColor = SolarCardBackground,
            title = { Text("Import Verification Report", color = SolarWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Source: ${report.sourceName}", color = SolarWarmAmber)
                    Text("Matched Official Recordings: ${report.matchedTracks.size}", color = SolarWhite)
                    if (report.unmatchedTitles.isNotEmpty()) {
                        Text(
                            "Unmatched Tracks (Not silently substituted):",
                            color = SolarBurntOrange,
                            style = MaterialTheme.typography.titleSmall
                        )
                        report.unmatchedTitles.forEach { u ->
                            Text("• $u", color = SolarMutedGrey, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("All tracks matched with verified metadata!", color = SolarMutedGrey)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissImportReport) {
                    Text("Done", color = SolarBurntOrange)
                }
            }
        )
    }

    // Playlist Detail Modal
    selectedPlaylistForDetail?.let { pl ->
        AlertDialog(
            onDismissRequest = { selectedPlaylistForDetail = null },
            containerColor = SolarCardBackground,
            title = { Text(pl.name, color = SolarWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${pl.tracks.size} tracks • ${pl.description}", color = SolarMutedGrey)
                    pl.tracks.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SolarElevatedSurface)
                                .clickable {
                                    onPlayTrack(t, pl.tracks)
                                    selectedPlaylistForDetail = null
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${t.title} — ${t.artist}", color = SolarWhite, maxLines = 1)
                            Icon(Icons.Default.PlayArrow, null, tint = SolarBurntOrange)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val first = pl.tracks.firstOrNull()
                        if (first != null) onPlayTrack(first, pl.tracks)
                        selectedPlaylistForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                ) {
                    Text("Play All", color = SolarWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlaylistForDetail = null }) {
                    Text("Close", color = SolarMutedGrey)
                }
            }
        )
    }

    qrSharePlaylist?.let { pl ->
        SolarQrShareDialog(
            title = pl.name,
            payload = "solar://playlist/${pl.id}?name=${pl.name}",
            onDismiss = { qrSharePlaylist = null }
        )
    }

    exportedM3uPreview?.let { (name, m3uContent) ->
        AlertDialog(
            onDismissRequest = { exportedM3uPreview = null },
            containerColor = SolarCardBackground,
            title = { Text("Exported M3U: $name", color = SolarWhite) },
            text = {
                Surface(
                    color = SolarDeepBlack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = m3uContent,
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarWarmAmber,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { exportedM3uPreview = null }) {
                    Text("Done", color = SolarBurntOrange)
                }
            }
        )
    }
}

@Composable
private fun EmptyLibraryBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SolarCardBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = SolarMutedGrey
        )
    }
}
