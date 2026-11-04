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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QueueMusic
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
import com.example.domain.model.SolarAlbum
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
    onDownloadTrack: (SolarTrack) -> Unit,
    onOpenAlbumDetail: (SolarAlbum) -> Unit = {}
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

        // Recent Searches when query is empty
        if (searchState.query.isBlank()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Searches", style = MaterialTheme.typography.titleMedium, color = SolarWhite)
                    if (searchState.searchHistory.isNotEmpty()) {
                        TextButton(onClick = onClearHistory) {
                            Text("Clear All", color = SolarBurntOrange)
                        }
                    }
                }
            }
            if (searchState.searchHistory.isNotEmpty()) {
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
            } else {
                item {
                    EmptyLibraryBox("Your recent search history will appear here.")
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

        if (searchState.query.isNotBlank()) {
            // 1. Track Results first
            if (searchState.selectedFilter in listOf("All", "Songs", "Local")) {
                val filteredTracks = if (searchState.selectedFilter == "Local") {
                    searchState.trackResults.filter { it.provider.name == "LOCAL" }
                } else {
                    searchState.trackResults
                }

                if (filteredTracks.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "Tracks (${filteredTracks.size})",
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

            // 2. Album Results second
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
                                onClick = { onOpenAlbumDetail(album) }
                            )
                        }
                    }
                }
            }

            // 3. Artist Results (only when explicitly filtering by Artists)
            if (searchState.selectedFilter == "Artists" && searchState.artistResults.isNotEmpty()) {
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
    var activeDetailView by remember { mutableStateOf<String?>(null) } // null = main list, "Playlists", "Liked Songs", "Downloads"

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showImportPlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylistForDetail by remember { mutableStateOf<SolarPlaylist?>(null) }
    var qrSharePlaylist by remember { mutableStateOf<SolarPlaylist?>(null) }
    var exportedM3uPreview by remember { mutableStateOf<Pair<String, String>?>(null) }

    val likedIds = remember(likedTracks) { likedTracks.map { it.id }.toSet() }

    BackHandler(enabled = activeDetailView != null) {
        activeDetailView = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen_list"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        if (activeDetailView == null) {
            // Main Library View: Clean vertical layout (stacked above to below)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "Your Library",
                        style = MaterialTheme.typography.displayMedium,
                        color = SolarWhite
                    )
                    Text(
                        text = "Playlists, Liked Songs & Offline Downloads",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarMutedGrey
                    )
                }
            }

            // 1. Playlists Card (Above)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { activeDetailView = "Playlists" },
                    colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SolarElevatedSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QueueMusic, contentDescription = "Playlists", tint = SolarBurntOrange)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Playlists", style = MaterialTheme.typography.titleMedium, color = SolarWhite)
                                Text("${playlists.size} playlists", style = MaterialTheme.typography.bodySmall, color = SolarMutedGrey)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Open Playlists", tint = SolarMutedGrey)
                    }
                }
            }

            // 2. Liked Songs Card (Middle)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { activeDetailView = "Liked Songs" },
                    colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SolarElevatedSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = "Liked Songs", tint = SolarWarmAmber)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Liked Songs", style = MaterialTheme.typography.titleMedium, color = SolarWhite)
                                Text("${likedTracks.size} saved songs", style = MaterialTheme.typography.bodySmall, color = SolarMutedGrey)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Open Liked Songs", tint = SolarMutedGrey)
                    }
                }
            }

            // 3. Downloads Card (Below)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { activeDetailView = "Downloads" },
                    colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SolarElevatedSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DownloadDone, contentDescription = "Downloads", tint = SolarBurntOrange)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Downloads", style = MaterialTheme.typography.titleMedium, color = SolarWhite)
                                Text("${downloads.size} offline tracks", style = MaterialTheme.typography.bodySmall, color = SolarMutedGrey)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Open Downloads", tint = SolarMutedGrey)
                    }
                }
            }
        } else {
            // Dedicated Detail Page with Navigation Back Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeDetailView = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library", tint = SolarWhite)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeDetailView ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = SolarWhite
                    )
                }
            }

            when (activeDetailView) {
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

                    if (playlists.isEmpty()) {
                        item {
                            EmptyLibraryBox("No playlists created yet. Tap 'Create Playlist' above to create your first playlist.")
                        }
                    } else {
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
                                                "${pl.tracks.size} tracks • ${if (pl.isPublic) "Public" else "Private"}",
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
                }

                "Liked Songs" -> {
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

                "Downloads" -> {
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
                                                text = if (item.status == DownloadStatus.COMPLETED) {
                                                    "${item.track.artist} • Offline"
                                                } else {
                                                    "${item.track.artist} • Downloading"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SolarMutedGrey
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Import from Spotify, YouTube Music, or M3U link",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarWarmAmber
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Spotify URL", "YouTube URL", "Playlist URL", "M3U / Text List")) { src ->
                            FilterChip(
                                selected = selectedSource == src,
                                onClick = {
                                    selectedSource = src
                                    if (src == "Spotify URL") {
                                        rawLines = "https://open.spotify.com/playlist/"
                                    } else if (src == "YouTube URL") {
                                        rawLines = "https://music.youtube.com/playlist?list="
                                    }
                                },
                                label = { Text(src) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SolarBurntOrange,
                                    selectedLabelColor = SolarWhite,
                                    containerColor = SolarElevatedSurface,
                                    labelColor = SolarMutedGrey
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = playlistTitle,
                        onValueChange = { playlistTitle = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rawLines,
                        onValueChange = { rawLines = it },
                        label = { Text("URL or Track List") },
                        placeholder = {
                            Text(
                                when (selectedSource) {
                                    "Spotify URL" -> "https://open.spotify.com/playlist/..."
                                    "YouTube URL" -> "https://music.youtube.com/playlist?list=..."
                                    "Playlist URL" -> "https://example.com/playlist.m3u"
                                    else -> "Paste track names or M3U lines"
                                },
                                color = SolarMutedGrey
                            )
                        },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
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
