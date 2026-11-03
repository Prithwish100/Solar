package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ListeningStats
import com.example.domain.model.SolarAlbum
import com.example.domain.model.SolarArtist
import com.example.domain.model.SolarPlaylist
import com.example.domain.model.SolarTrack
import com.example.ui.HomeFeedState
import com.example.ui.components.SolarAlbumCard
import com.example.ui.components.SolarArtistBubble
import com.example.ui.components.SolarArtworkBox
import com.example.ui.components.SolarLogoWithCredit
import com.example.ui.components.SolarPlaylistCard
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
fun SolarHomeScreen(
    homeState: HomeFeedState,
    currentPlayingTrack: SolarTrack?,
    likedTrackIds: Set<String>,
    localTracks: List<SolarTrack>,
    userName: String,
    isCloudSignedIn: Boolean,
    onSelectMood: (String) -> Unit,
    onRefresh: () -> Unit,
    onPlayTrack: (SolarTrack, List<SolarTrack>) -> Unit,
    onToggleLike: (SolarTrack) -> Unit,
    onPlayNext: (SolarTrack) -> Unit,
    onAddToQueue: (SolarTrack) -> Unit,
    onAddToPlaylistRequest: (SolarTrack) -> Unit,
    onDownloadTrack: (SolarTrack) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var selectedAlbumDetail by remember { mutableStateOf<SolarAlbum?>(null) }
    var selectedArtistDetail by remember { mutableStateOf<SolarArtist?>(null) }
    var selectedPlaylistDetail by remember { mutableStateOf<SolarPlaylist?>(null) }

    val moodKeys = listOf("All") + homeState.moodPlaylists.keys.toList()
    val displayedRecommended = if (homeState.selectedMoodFilter == "All") {
        homeState.recommendedTracks
    } else {
        homeState.moodPlaylists[homeState.selectedMoodFilter].orEmpty().ifEmpty { homeState.recommendedTracks }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // 1. Top Header with SOLAR Geometric Logo + "Made by Prithwish" + Profile / Settings / Search
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SolarLogoWithCredit(showCredit = true)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("home_search_shortcut")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Music",
                            tint = SolarWhite
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("home_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Discovery Feed",
                            tint = SolarWarmAmber
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_btn")
                    ) {
                        Icon(
                            imageVector = if (isCloudSignedIn) Icons.Default.AccountCircle else Icons.Default.Settings,
                            contentDescription = "Account & Settings",
                            tint = if (isCloudSignedIn) SolarBurntOrange else SolarWhite
                        )
                    }
                }
            }
        }

        // 2. Hero Greeting Banner with Subtle Burnt-Orange Solar Gradient
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    SolarPrimaryOrange.copy(alpha = 0.35f),
                                    SolarCardBackground,
                                    SolarDeepBlack
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "${homeState.greeting}, $userName",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = SolarWhite
                                )
                                Text(
                                    text = if (isCloudSignedIn) {
                                        "Cloud Sync Active • Lossless Audio Engine Ready"
                                    } else {
                                        "Local & InnerTube Discovery • Sign in for Cloud Sync"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SolarWarmAmber
                                )
                            }
                            Button(
                                onClick = {
                                    val first = displayedRecommended.firstOrNull()
                                    if (first != null) onPlayTrack(first, displayedRecommended)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SolarWhite)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Solar Mix", color = SolarWhite)
                            }
                        }
                    }
                }
            }
        }

        // 3. Mood & Activity Filter Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(moodKeys) { mood ->
                    val selected = homeState.selectedMoodFilter == mood
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectMood(mood) },
                        label = { Text(mood) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarBurntOrange,
                            selectedLabelColor = SolarWhite,
                            containerColor = SolarCardBackground,
                            labelColor = SolarMutedGrey
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = SolarDivider,
                            selectedBorderColor = SolarBurntOrange
                        )
                    )
                }
            }
        }

        // 4. Quick Picks (2-column compact grid)
        item {
            SectionTitle(title = "Quick Picks", subtitle = "Instant official studio masters")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                homeState.quickPicks.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SolarCardBackground)
                                    .clickable { onPlayTrack(track, homeState.quickPicks) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SolarArtworkBox(
                                    artworkUrl = track.artworkUrl,
                                    contentDescription = track.title,
                                    modifier = Modifier.size(44.dp),
                                    cornerRadius = 8.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SolarWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarMutedGrey,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 5. Personalized Daily Mixes & Discovery
        item {
            SectionTitle(title = "Your Solar Mixes", subtitle = "Personalized from your listening & likes")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(homeState.personalizedMixes.entries.toList()) { (mixTitle, mixTracks) ->
                    Card(
                        modifier = Modifier
                            .width(210.dp)
                            .clickable {
                                val first = mixTracks.firstOrNull()
                                if (first != null) onPlayTrack(first, mixTracks)
                            },
                        colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            SolarArtworkBox(
                                artworkUrl = mixTracks.firstOrNull()?.artworkUrl ?: "",
                                contentDescription = mixTitle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                cornerRadius = 12.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = mixTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = SolarWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = mixTracks.joinToString(", ") { it.artist },
                                style = MaterialTheme.typography.bodySmall,
                                color = SolarMutedGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 6. New Releases & Recommended Albums
        item {
            SectionTitle(title = "New Releases & Albums", subtitle = "Full-length studio albums")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(homeState.newReleases) { album ->
                    SolarAlbumCard(
                        album = album,
                        onClick = { selectedAlbumDetail = album }
                    )
                }
            }
        }

        // 7. Recommended Artists
        item {
            SectionTitle(title = "Recommended Artists", subtitle = "Official verified channels & creators")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(homeState.featuredArtists) { artist ->
                    SolarArtistBubble(
                        artist = artist,
                        onClick = { selectedArtistDetail = artist }
                    )
                }
            }
        }

        // 8. Popular Curated Playlists
        item {
            SectionTitle(title = "Popular Playlists", subtitle = "Hand-crafted for deep listening")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(homeState.curatedPlaylists) { playlist ->
                    SolarPlaylistCard(
                        playlist = playlist,
                        onClick = { selectedPlaylistDetail = playlist }
                    )
                }
            }
        }

        // 9. Recently Added Local Device Music (if scanned)
        if (localTracks.isNotEmpty()) {
            item {
                SectionTitle(title = "Recently Added Local Music", subtitle = "${localTracks.size} device tracks ready offline")
            }
            items(localTracks.take(3)) { localTrack ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SolarTrackRow(
                        track = localTrack,
                        isPlaying = currentPlayingTrack?.id == localTrack.id,
                        isLiked = likedTrackIds.contains(localTrack.id),
                        onPlayClick = { onPlayTrack(localTrack, localTracks) },
                        onLikeClick = { onToggleLike(localTrack) },
                        onPlayNext = { onPlayNext(localTrack) },
                        onAddToQueue = { onAddToQueue(localTrack) },
                        onAddToPlaylist = { onAddToPlaylistRequest(localTrack) },
                        onDownloadClick = {}
                    )
                }
            }
        }

        // 10. Recommended Songs Feed
        item {
            SectionTitle(
                title = "Recommended Songs (${homeState.selectedMoodFilter})",
                subtitle = "Verified official recordings • Zero unofficial edits"
            )
        }
        items(displayedRecommended, key = { it.id }) { track ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SolarTrackRow(
                    track = track,
                    isPlaying = currentPlayingTrack?.id == track.id,
                    isLiked = likedTrackIds.contains(track.id),
                    onPlayClick = { onPlayTrack(track, displayedRecommended) },
                    onLikeClick = { onToggleLike(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylistRequest(track) },
                    onDownloadClick = { onDownloadTrack(track) }
                )
            }
        }
    }

    // Album Detail Modal
    selectedAlbumDetail?.let { album ->
        AlertDialog(
            onDismissRequest = { selectedAlbumDetail = null },
            containerColor = SolarCardBackground,
            title = {
                Text("${album.title} (${album.year})", color = SolarWhite, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Artist: ${album.artist} • Genre: ${album.genre}", color = SolarWarmAmber)
                    album.tracks.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SolarElevatedSurface)
                                .clickable {
                                    onPlayTrack(t, album.tracks)
                                    selectedAlbumDetail = null
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t.title, color = SolarWhite, style = MaterialTheme.typography.titleSmall)
                                Text(t.audioFormat, color = SolarMutedGrey, style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = SolarBurntOrange)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val first = album.tracks.firstOrNull()
                        if (first != null) onPlayTrack(first, album.tracks)
                        selectedAlbumDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                ) {
                    Text("Play Album", color = SolarWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAlbumDetail = null }) {
                    Text("Close", color = SolarMutedGrey)
                }
            }
        )
    }

    // Artist Profile Modal
    selectedArtistDetail?.let { artist ->
        AlertDialog(
            onDismissRequest = { selectedArtistDetail = null },
            containerColor = SolarCardBackground,
            title = {
                Text(artist.name, color = SolarWhite, style = MaterialTheme.typography.headlineMedium)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(artist.subscriberCountText, color = SolarWarmAmber, style = MaterialTheme.typography.labelLarge)
                    Text(artist.bio, color = SolarMutedGrey, style = MaterialTheme.typography.bodySmall)
                    if (artist.similarArtists.isNotEmpty()) {
                        Text(
                            "Related Artists: ${artist.similarArtists.joinToString(", ")}",
                            color = SolarBurntOrange,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Top Official Tracks:", color = SolarWhite, style = MaterialTheme.typography.titleSmall)
                    artist.topTracks.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SolarElevatedSurface)
                                .clickable {
                                    onPlayTrack(t, artist.topTracks)
                                    selectedArtistDetail = null
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t.title, color = SolarWhite, style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.PlayArrow, null, tint = SolarBurntOrange)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val first = artist.topTracks.firstOrNull()
                        if (first != null) onPlayTrack(first, artist.topTracks)
                        selectedArtistDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                ) {
                    Text("Play Artist Radio", color = SolarWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedArtistDetail = null }) {
                    Text("Close", color = SolarMutedGrey)
                }
            }
        )
    }

    // Curated Playlist Detail Modal
    selectedPlaylistDetail?.let { pl ->
        AlertDialog(
            onDismissRequest = { selectedPlaylistDetail = null },
            containerColor = SolarCardBackground,
            title = { Text(pl.name, color = SolarWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(pl.description, color = SolarMutedGrey, style = MaterialTheme.typography.bodySmall)
                    pl.tracks.forEach { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SolarElevatedSurface)
                                .clickable {
                                    onPlayTrack(t, pl.tracks)
                                    selectedPlaylistDetail = null
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
                        selectedPlaylistDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                ) {
                    Text("Play Playlist", color = SolarWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPlaylistDetail = null }) {
                    Text("Close", color = SolarMutedGrey)
                }
            }
        )
    }
}

@Composable
fun SolarExploreScreen(
    homeState: HomeFeedState,
    listeningStats: ListeningStats,
    currentPlayingTrack: SolarTrack?,
    likedTrackIds: Set<String>,
    onPlayTrack: (SolarTrack, List<SolarTrack>) -> Unit,
    onToggleLike: (SolarTrack) -> Unit,
    onPlayNext: (SolarTrack) -> Unit,
    onAddToQueue: (SolarTrack) -> Unit,
    onAddToPlaylistRequest: (SolarTrack) -> Unit,
    onDownloadTrack: (SolarTrack) -> Unit,
    onSelectMoodAndPlay: (String, List<SolarTrack>) -> Unit
) {
    val genres = listOf(
        "Synthwave" to "Analog horizons & neon arpeggios",
        "Electronic" to "Precision modular soundscapes",
        "Ambient Techno" to "Deep-focus minimal rhythms",
        "Progressive House" to "Euphoric melodic builds",
        "Indie Pop" to "Intimate vocals & dream textures",
        "Indie Fusion" to "South Asian & global acoustic-electronic",
        "Drum & Bass" to "174 BPM kinetic breakbeats",
        "Acoustic" to "Unplugged studio & live sessions"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_screen_list"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "Explore & Analytics",
                    style = MaterialTheme.typography.displayMedium,
                    color = SolarWhite
                )
                Text(
                    text = "Global Trending Charts, Mood Discovery & Your Personal Listening Report",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarMutedGrey
                )
            }
        }

        // Personal Listening Statistics & Streak Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Equalizer, contentDescription = null, tint = SolarBurntOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Monthly Listening Report",
                            style = MaterialTheme.typography.titleLarge,
                            color = SolarWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatMetricBox("Minutes", "${listeningStats.totalMinutesListened}m")
                        StatMetricBox("Streak", "${listeningStats.currentStreakDays} Days")
                        StatMetricBox("Top Artist", listeningStats.topArtist)
                        StatMetricBox("Top Genre", listeningStats.topGenre)
                    }
                }
            }
        }

        // Genre & Mood Exploration Grid
        item {
            SectionTitle(title = "Genre & Mood Matrix", subtitle = "Tap any frequency to launch an instant mix")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                genres.chunked(2).forEach { rowGenres ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowGenres.forEach { (genreName, desc) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                SolarCardBackground,
                                                SolarPrimaryOrange.copy(alpha = 0.28f)
                                            )
                                        )
                                    )
                                    .border(1.dp, SolarDivider, RoundedCornerShape(14.dp))
                                    .clickable {
                                        val matching = homeState.recommendedTracks.filter {
                                            it.genre.equals(genreName, ignoreCase = true)
                                        }.ifEmpty { homeState.recommendedTracks }
                                        onSelectMoodAndPlay(genreName, matching)
                                    }
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = genreName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SolarWarmAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SolarMutedGrey,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trending Top 10 Official Songs
        item {
            SectionTitle(
                title = "Trending on SOLAR",
                subtitle = "Ranked by official master quality & listener velocity"
            )
        }

        items(homeState.recommendedTracks, key = { "trend_${it.id}" }) { track ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SolarTrackRow(
                    track = track,
                    isPlaying = currentPlayingTrack?.id == track.id,
                    isLiked = likedTrackIds.contains(track.id),
                    onPlayClick = { onPlayTrack(track, homeState.recommendedTracks) },
                    onLikeClick = { onToggleLike(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylistRequest(track) },
                    onDownloadClick = { onDownloadTrack(track) },
                    showAccuracyBadge = true
                )
            }
        }
    }
}

@Composable
private fun StatMetricBox(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SolarDeepBlack)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = SolarWarmAmber,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SolarMutedGrey
        )
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = SolarWhite
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SolarMutedGrey
            )
        }
    }
}
