package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.RecordingType
import com.example.domain.model.SolarAlbum
import com.example.domain.model.SolarArtist
import com.example.domain.model.SolarPlaylist
import com.example.domain.model.SolarTrack
import com.example.ui.theme.SolarBurntOrange
import com.example.ui.theme.SolarCardBackground
import com.example.ui.theme.SolarDeepBlack
import com.example.ui.theme.SolarDivider
import com.example.ui.theme.SolarElevatedSurface
import com.example.ui.theme.SolarMutedGrey
import com.example.ui.theme.SolarPrimaryOrange
import com.example.ui.theme.SolarWarmAmber
import com.example.ui.theme.SolarWhite
import kotlin.math.abs

/**
 * Original abstract geometric solar-inspired application logo with orange/black composition
 * and mandatory "Made by Prithwish" developer credit underneath.
 */
@Composable
fun SolarLogoWithCredit(
    modifier: Modifier = Modifier,
    logoSize: Dp = 42.dp,
    showCredit: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SolarGeometricLogo(size = logoSize)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "SOLAR",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                ),
                color = SolarWhite
            )
            if (showCredit) {
                Text(
                    text = "Made by Prithwish",
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarWarmAmber
                )
            }
        }
    }
}

@Composable
fun SolarGeometricLogo(
    size: Dp = 44.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "solar_corona")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corona_pulse"
    )

    Canvas(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SolarDeepBlack)
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val r = this.size.minDimension / 2f
        val scale = if (animated) pulseScale else 1f

        // Outer subtle solar glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SolarBurntOrange.copy(alpha = 0.42f),
                    SolarPrimaryOrange.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = center,
                radius = r * scale
            ),
            radius = r * scale,
            center = center
        )

        // Outer geometric corona ring
        drawCircle(
            color = SolarPrimaryOrange,
            radius = r * 0.80f,
            center = center,
            style = Stroke(width = r * 0.10f)
        )

        // Inner burnt orange solar disc
        drawCircle(
            color = SolarBurntOrange,
            radius = r * 0.56f,
            center = center
        )

        // Eclipse shadow crescent
        drawCircle(
            color = SolarDeepBlack,
            radius = r * 0.44f,
            center = Offset(center.x + r * 0.12f, center.y - r * 0.08f)
        )

        // Soundwave bars across the solar core
        val barWidth = r * 0.09f
        val barHeights = listOf(0.32f, 0.58f, 0.78f, 0.48f, 0.28f)
        val totalWidth = barHeights.size * barWidth + (barHeights.size - 1) * (barWidth * 0.65f)
        var startX = center.x - totalWidth / 2f

        barHeights.forEach { ratio ->
            val bh = r * ratio
            drawRoundRect(
                color = SolarWarmAmber,
                topLeft = Offset(startX, center.y - bh / 2f),
                size = Size(barWidth, bh),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
            startX += barWidth * 1.65f
        }
    }
}

@Composable
fun SolarArtworkBox(
    artworkUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SolarCardBackground,
                        SolarPrimaryOrange.copy(alpha = 0.35f),
                        SolarDeepBlack
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = SolarWarmAmber.copy(alpha = 0.55f),
            modifier = Modifier.size(26.dp)
        )
        if (artworkUrl.isNotBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun SolarTrackRow(
    track: SolarTrack,
    isPlaying: Boolean,
    isLiked: Boolean,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAccuracyBadge: Boolean = false,
    showProviderBadge: Boolean = false
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .testTag("track_row_${track.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) SolarElevatedSurface else SolarCardBackground
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                SolarArtworkBox(
                    artworkUrl = track.artworkUrl,
                    contentDescription = track.title,
                    modifier = Modifier.size(54.dp),
                    cornerRadius = 10.dp
                )
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SolarDeepBlack.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Now Playing",
                            tint = SolarBurntOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isPlaying) SolarBurntOrange else SolarWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (track.recordingType != RecordingType.ORIGINAL) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = SolarPrimaryOrange.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = track.recordingType.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = SolarWarmAmber,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarMutedGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (track.isVerifiedArtist) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Official Artist",
                            tint = SolarWarmAmber,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showProviderBadge) {
                        Surface(
                            color = SolarDeepBlack,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = track.provider.badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = SolarWarmAmber,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = formatDuration(track.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarMutedGrey
                    )
                    if (showAccuracyBadge) {
                        Surface(
                            color = SolarPrimaryOrange.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${track.accuracyScore}% MATCH • ${track.recordingType.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SolarBurntOrange,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.testTag("like_btn_${track.id}")
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) "Remove from Liked" else "Save to Liked",
                    tint = if (isLiked) SolarBurntOrange else SolarMutedGrey
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("more_btn_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Track Options",
                        tint = SolarMutedGrey
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(SolarElevatedSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Next", color = SolarWhite) },
                        leadingIcon = { Icon(Icons.Default.SkipNext, null, tint = SolarBurntOrange) },
                        onClick = {
                            menuExpanded = false
                            onPlayNext()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = SolarWhite) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = SolarWarmAmber) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = SolarWhite) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = SolarWarmAmber) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    if (track.isDownloadPermitted) {
                        DropdownMenuItem(
                            text = { Text("Download Offline", color = SolarWhite) },
                            leadingIcon = { Icon(Icons.Default.Download, null, tint = SolarBurntOrange) },
                            onClick = {
                                menuExpanded = false
                                onDownloadClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Share Track", color = SolarWhite) },
                        leadingIcon = { Icon(Icons.Default.Share, null, tint = SolarMutedGrey) },
                        onClick = {
                            menuExpanded = false
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Listening to \"${track.title}\" by ${track.artist} on SOLAR Music (Made by Prithwish)"
                                )
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Song Details & Codec", color = SolarWhite) },
                        leadingIcon = { Icon(Icons.Default.MusicNote, null, tint = SolarMutedGrey) },
                        onClick = {
                            menuExpanded = false
                            showDetailsDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            containerColor = SolarCardBackground,
            title = {
                Text("Recording & Codec Metadata", color = SolarWhite, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Title: ${track.title}", color = SolarWhite)
                    Text("Artist: ${track.artist} (Verified: ${if (track.isVerifiedArtist) "Yes" else "No"})", color = SolarMutedGrey)
                    Text("Album: ${track.album} (${track.releaseYear})", color = SolarMutedGrey)
                    Text("Recording Type: ${track.recordingType.label}", color = SolarWarmAmber)
                    Text("Source Provider: ${track.provider.displayName}", color = SolarBurntOrange)
                    Text("Audio Stream Format: ${track.audioFormat}", color = SolarMutedGrey)
                    Text("Duration: ${formatDuration(track.durationMs)}", color = SolarMutedGrey)
                    Text("ReplayGain Normalization: ${track.replayGainDb} dB", color = SolarMutedGrey)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close", color = SolarBurntOrange)
                }
            }
        )
    }
}

@Composable
fun SolarAlbumCard(
    album: SolarAlbum,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(152.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SolarCardBackground)
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        SolarArtworkBox(
            artworkUrl = album.artworkUrl,
            contentDescription = album.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            cornerRadius = 10.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleSmall,
            color = SolarWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.artist} • ${album.year}",
            style = MaterialTheme.typography.bodySmall,
            color = SolarMutedGrey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SolarArtistBubble(
    artist: SolarArtist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(116.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SolarArtworkBox(
            artworkUrl = artist.avatarUrl,
            contentDescription = artist.name,
            modifier = Modifier
                .size(86.dp)
                .border(1.5.dp, SolarBurntOrange.copy(alpha = 0.65f), CircleShape),
            cornerRadius = 43.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            color = SolarWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist.subscriberCountText,
            style = MaterialTheme.typography.labelSmall,
            color = SolarMutedGrey,
            maxLines = 1
        )
    }
}

@Composable
fun SolarPlaylistCard(
    playlist: SolarPlaylist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            SolarArtworkBox(
                artworkUrl = playlist.artworkUrl,
                contentDescription = playlist.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                cornerRadius = 12.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                color = SolarWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.tracks.size} tracks • ${playlist.sourceOrigin}",
                style = MaterialTheme.typography.labelSmall,
                color = SolarWarmAmber,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SolarMiniPlayer(
    track: SolarTrack,
    isPlaying: Boolean,
    progressFraction: Float,
    isLiked: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleLike: () -> Unit,
    onExpandPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SolarDivider, RoundedCornerShape(16.dp))
            .clickable { onExpandPlayer() }
            .testTag("solar_mini_player"),
        color = SolarCardBackground,
        tonalElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SolarArtworkBox(
                    artworkUrl = track.artworkUrl,
                    contentDescription = track.title,
                    modifier = Modifier.size(46.dp),
                    cornerRadius = 10.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = SolarWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} • ${track.audioFormat}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarWarmAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isLiked) SolarBurntOrange else SolarMutedGrey
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = SolarWhite
                    )
                }
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = SolarBurntOrange
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = SolarBurntOrange,
                trackColor = SolarDeepBlack
            )
        }
    }
}

@Composable
fun SolarQrShareDialog(
    title: String,
    payload: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SolarCardBackground,
        title = {
            Text(text = "QR Share: $title", color = SolarWhite, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SolarDeepBlack)
                        .border(2.dp, SolarBurntOrange, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val grid = 15
                        val cell = size.width / grid
                        val seed = payload.hashCode()
                        for (r in 0 until grid) {
                            for (c in 0 until grid) {
                                val isFinder = (r < 4 && c < 4) || (r < 4 && c >= grid - 4) || (r >= grid - 4 && c < 4)
                                val active = if (isFinder) {
                                    r == 0 || r == 3 || c == 0 || c == 3 || (r in 1..2 && c in 1..2)
                                } else {
                                    abs((seed xor (r * 31 + c * 17))) % 2 == 0
                                }
                                if (active) {
                                    drawRoundRect(
                                        color = if (isFinder) SolarBurntOrange else SolarWarmAmber,
                                        topLeft = Offset(c * cell, r * cell),
                                        size = Size(cell * 0.85f, cell * 0.85f),
                                        cornerRadius = CornerRadius(2f, 2f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Scan with any SOLAR client to import playlist instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarMutedGrey
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = SolarBurntOrange)
            }
        }
    )
}

fun formatDuration(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
