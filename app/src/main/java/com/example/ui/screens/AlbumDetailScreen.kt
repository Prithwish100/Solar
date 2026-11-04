package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.SolarAlbum
import com.example.domain.model.SolarTrack
import com.example.ui.theme.SolarBurntOrange
import com.example.ui.theme.SolarCardBackground
import com.example.ui.theme.SolarDeepBlack
import com.example.ui.theme.SolarElevatedSurface
import com.example.ui.theme.SolarMutedGrey
import com.example.ui.theme.SolarPrimaryOrange
import com.example.ui.theme.SolarWarmAmber
import com.example.ui.theme.SolarWhite

@Composable
fun SolarAlbumDetailScreen(
    album: SolarAlbum,
    currentPlayingTrack: SolarTrack?,
    likedTrackIds: Set<String>,
    onBack: () -> Unit,
    onPlayTrack: (SolarTrack, List<SolarTrack>) -> Unit,
    onShufflePlay: (List<SolarTrack>) -> Unit,
    onToggleLike: (SolarTrack) -> Unit,
    onPlayNext: (SolarTrack) -> Unit,
    onAddToQueue: (SolarTrack) -> Unit,
    onAddToPlaylist: (SolarTrack) -> Unit,
    onDownloadTrack: (SolarTrack) -> Unit
) {
    BackHandler {
        onBack()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarDeepBlack)
            .testTag("album_detail_screen"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SolarWhite
                    )
                }
                Text(
                    text = "Album",
                    style = MaterialTheme.typography.titleMedium,
                    color = SolarWhite,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Header Artwork & Album Info
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SolarPrimaryOrange.copy(alpha = 0.28f),
                                SolarDeepBlack
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = album.artworkUrl,
                        contentDescription = album.title,
                        modifier = Modifier
                            .size(200.dp)
                            .shadow(16.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = SolarWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.titleSmall,
                        color = SolarWarmAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Album • ${album.year} • ${album.tracks.size} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarMutedGrey
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons: Play All & Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val first = album.tracks.firstOrNull()
                                if (first != null) {
                                    onPlayTrack(first, album.tracks)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SolarWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", color = SolarWhite, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (album.tracks.isNotEmpty()) {
                                    onShufflePlay(album.tracks)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = SolarWarmAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", color = SolarWarmAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Text(
                text = "Tracks",
                style = MaterialTheme.typography.titleMedium,
                color = SolarWhite,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        // Track rows
        if (album.tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks available in this album",
                        color = SolarMutedGrey,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            itemsIndexed(album.tracks, key = { index, track -> "alb_${album.id}_${track.id}_$index" }) { index, track ->
                AlbumTrackRow(
                    index = index + 1,
                    track = track,
                    isPlaying = currentPlayingTrack?.id == track.id,
                    isLiked = likedTrackIds.contains(track.id),
                    onClick = { onPlayTrack(track, album.tracks) },
                    onToggleLike = { onToggleLike(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onDownload = { onDownloadTrack(track) }
                )
            }
        }
    }
}

@Composable
private fun AlbumTrackRow(
    index: Int,
    track: SolarTrack,
    isPlaying: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number or playing indicator
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Playing",
                    tint = SolarBurntOrange,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SolarMutedGrey
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isPlaying) SolarBurntOrange else SolarWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = SolarMutedGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Like button
        IconButton(
            onClick = onToggleLike,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) SolarBurntOrange else SolarMutedGrey,
                modifier = Modifier.size(18.dp)
            )
        }

        // More Menu
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = SolarMutedGrey,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(SolarElevatedSurface)
            ) {
                DropdownMenuItem(
                    text = { Text("Play Next", color = SolarWhite) },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = SolarMutedGrey) },
                    onClick = {
                        menuExpanded = false
                        onPlayNext()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = SolarWhite) },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, null, tint = SolarMutedGrey) },
                    onClick = {
                        menuExpanded = false
                        onAddToQueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = SolarWhite) },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, null, tint = SolarMutedGrey) },
                    onClick = {
                        menuExpanded = false
                        onAddToPlaylist()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Download Offline", color = SolarWhite) },
                    leadingIcon = { Icon(Icons.Default.Download, null, tint = SolarBurntOrange) },
                    onClick = {
                        menuExpanded = false
                        onDownload()
                    }
                )
            }
        }
    }
}
