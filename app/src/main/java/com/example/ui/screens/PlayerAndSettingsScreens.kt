package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.local.SolarAppSettings
import com.example.domain.model.AudioQuality
import com.example.domain.model.EqualizerState
import com.example.domain.model.LyricsData
import com.example.domain.model.ProviderType
import com.example.domain.model.SolarTrack
import com.example.playback.PlaybackUiState
import com.example.playback.RepeatModeState
import com.example.ui.components.SolarAnimatedIconButton
import com.example.ui.components.SolarArtworkBox
import com.example.ui.components.SolarLogoWithCredit
import com.example.ui.components.formatDuration
import com.example.ui.theme.SolarBurntOrange
import com.example.ui.theme.SolarCardBackground
import com.example.ui.theme.SolarDeepBlack
import com.example.ui.theme.SolarDivider
import com.example.ui.theme.SolarElevatedSurface
import com.example.ui.theme.SolarMutedGrey
import com.example.ui.theme.SolarPrimaryOrange
import com.example.ui.theme.SolarWarmAmber
import com.example.ui.theme.SolarWhite
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SolarFullScreenPlayer(
    playbackState: PlaybackUiState,
    lyricsData: LyricsData?,
    isLoadingLyrics: Boolean,
    onRequestLyrics: (Boolean) -> Unit,
    equalizerState: EqualizerState,
    isLiked: Boolean,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onDownloadTrack: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onPlayTrackFromQueue: (SolarTrack) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSelectEqPreset: (String) -> Unit,
    onSetBandLevel: (Short, Short) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit
) {
    val track = playbackState.currentTrack ?: return
    var activeSubPanel by remember { mutableStateOf("PLAYER") } // PLAYER, LYRICS, QUEUE, EQUALIZER

    BackHandler {
        onCollapse()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarDeepBlack)
            .statusBarsPadding()
            .testTag("full_screen_player")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar (Share option removed, clean centered header)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SolarAnimatedIconButton(
                    onClick = onCollapse,
                    modifier = Modifier.testTag("collapse_player_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Minimize Player",
                        tint = SolarWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (playbackState.isBuffering) "LOADING STREAM..." else "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarWarmAmber
                    )
                    Text(
                        text = track.album,
                        style = MaterialTheme.typography.titleSmall,
                        color = SolarWhite,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            // Mode Switcher Pills (Artwork / Lyrics / Queue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "PLAYER" to "Artwork",
                    "LYRICS" to "Lyrics",
                    "QUEUE" to "Queue (${playbackState.queue.size})"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = activeSubPanel == key,
                        onClick = {
                            activeSubPanel = key
                            if (key == "LYRICS" && (lyricsData == null || lyricsData.trackId != track.id)) {
                                onRequestLyrics(false)
                            }
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarBurntOrange,
                            selectedLabelColor = SolarWhite,
                            containerColor = SolarCardBackground,
                            labelColor = SolarMutedGrey
                        )
                    )
                }
            }

            // Center Dynamic Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (activeSubPanel) {
                    "PLAYER" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            SolarArtworkBox(
                                artworkUrl = track.artworkUrl,
                                contentDescription = track.title,
                                modifier = Modifier
                                    .size(280.dp)
                                    .border(
                                        width = 1.dp,
                                        color = SolarDivider,
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                cornerRadius = 24.dp
                            )
                            if (playbackState.sleepTimerRemainingMinutes != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = SolarPrimaryOrange.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Sleep: ${playbackState.sleepTimerRemainingMinutes}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarWarmAmber,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    "LYRICS" -> {
                        OnDemandPlainLyricsPanel(
                            track = track,
                            lyricsData = lyricsData?.takeIf { it.trackId == track.id },
                            isLoading = isLoadingLyrics,
                            onRequestLyrics = { force -> onRequestLyrics(force) }
                        )
                    }

                    "QUEUE" -> {
                        QueueManagementPanel(
                            queue = playbackState.queue,
                            currentTrackId = track.id,
                            onSelectTrack = onPlayTrackFromQueue,
                            onRemoveIndex = onRemoveFromQueue,
                            onMoveItem = onMoveQueueItem,
                            onClearQueue = onClearQueue
                        )
                    }
                }
            }

            // Track Title, Artist & Favorite / Download Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = SolarWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SolarMutedGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var downloadInitiated by remember(track.id) { mutableStateOf(false) }
                    if (track.isDownloadPermitted) {
                        SolarAnimatedIconButton(
                            onClick = {
                                downloadInitiated = true
                                onDownloadTrack()
                            }
                        ) {
                            Icon(
                                imageVector = if (downloadInitiated) Icons.Default.DownloadDone else Icons.Default.Download,
                                contentDescription = if (downloadInitiated) "Downloaded" else "Download Track",
                                tint = if (downloadInitiated) SolarBurntOrange else SolarWarmAmber
                            )
                        }
                    }
                    SolarAnimatedIconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite Track",
                            tint = if (isLiked) SolarBurntOrange else SolarWhite
                        )
                    }
                }
            }

            // Seek Bar with Faded Buffered/Loaded Color & Elapsed / Total Duration
            val duration = playbackState.durationMs.coerceAtLeast(1L)
            val currentPos = playbackState.currentPositionMs.coerceIn(0L, duration)
            val playedFraction = (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            val bufferedPos = playbackState.bufferedPositionMs.coerceIn(currentPos, duration)
            val bufferedFraction = maxOf(
                (bufferedPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                (playbackState.bufferedPercentage / 100f).coerceIn(0f, 1f),
                playedFraction
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Faded buffered progress bar rendered inside the songbar track
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(6.dp)
                ) {
                    val r = CornerRadius(size.height / 2f, size.height / 2f)
                    // 1. Unloaded dark track
                    drawRoundRect(
                        color = SolarCardBackground,
                        size = size,
                        cornerRadius = r
                    )
                    // 2. Faded color showing how much of the song is loaded/buffered
                    if (bufferedFraction > 0f) {
                        drawRoundRect(
                            color = SolarWarmAmber.copy(alpha = 0.42f),
                            size = Size(size.width * bufferedFraction, size.height),
                            cornerRadius = r
                        )
                    }
                    // 3. Active played portion
                    if (playedFraction > 0f) {
                        drawRoundRect(
                            color = SolarBurntOrange,
                            size = Size(size.width * playedFraction, size.height),
                            cornerRadius = r
                        )
                    }
                }

                Slider(
                    value = currentPos.toFloat(),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = SolarBurntOrange,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(currentPos),
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarMutedGrey
                )
                if (playbackState.isBuffering) {
                    Text(
                        text = "Loading ${(bufferedFraction * 100).toInt()}%...",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarWarmAmber
                    )
                }
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarMutedGrey
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Transport Controls (Shuffle, Previous, Play/Pause, Next, Repeat) with spring animations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SolarAnimatedIconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) SolarBurntOrange else SolarMutedGrey
                    )
                }

                SolarAnimatedIconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = SolarWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                val playInteraction = remember { MutableInteractionSource() }
                val isPlayPressed by playInteraction.collectIsPressedAsState()
                val playScale by animateFloatAsState(
                    targetValue = if (isPlayPressed) 0.86f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "play_pause_scale"
                )

                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = playScale
                            scaleY = playScale
                        }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = playInteraction,
                            indication = null
                        ) { onTogglePlayPause() }
                        .testTag("full_player_play_pause"),
                    color = SolarBurntOrange,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (playbackState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = SolarWarmAmber,
                                strokeWidth = 2.5.dp
                            )
                        }
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = SolarWhite,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                SolarAnimatedIconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = SolarWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                SolarAnimatedIconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatModeState.ONE) {
                            Icons.Default.RepeatOne
                        } else {
                            Icons.Default.Repeat
                        },
                        contentDescription = "Repeat Mode",
                        tint = if (playbackState.repeatMode != RepeatModeState.OFF) SolarBurntOrange else SolarMutedGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Utility Bar (Sleep Timer, Lyrics, Queue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SolarCardBackground)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val nextTimer = when (playbackState.sleepTimerRemainingMinutes) {
                            null -> 15
                            15 -> 30
                            30 -> 60
                            else -> null
                        }
                        onSetSleepTimer(nextTimer)
                    }
                ) {
                    Icon(Icons.Default.Bedtime, null, tint = SolarWarmAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = playbackState.sleepTimerRemainingMinutes?.let { "Sleep ${it}m" } ?: "Sleep Timer",
                        color = SolarWhite,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SolarAnimatedIconButton(
                        onClick = {
                            if (activeSubPanel == "LYRICS") {
                                activeSubPanel = "PLAYER"
                            } else {
                                activeSubPanel = "LYRICS"
                                if (lyricsData == null || lyricsData.trackId != track.id) {
                                    onRequestLyrics(false)
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (activeSubPanel == "LYRICS") SolarBurntOrange else SolarMutedGrey
                        )
                    }

                    SolarAnimatedIconButton(onClick = { activeSubPanel = if (activeSubPanel == "QUEUE") "PLAYER" else "QUEUE" }) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (activeSubPanel == "QUEUE") SolarBurntOrange else SolarMutedGrey
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnDemandPlainLyricsPanel(
    track: SolarTrack,
    lyricsData: LyricsData?,
    isLoading: Boolean,
    onRequestLyrics: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = SolarBurntOrange,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Loading lyrics for \"${track.title}\"...",
                        style = MaterialTheme.typography.titleSmall,
                        color = SolarWhite
                    )
                }
            }

            lyricsData == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = null,
                        tint = SolarWarmAmber,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.titleLarge,
                        color = SolarWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "View lyrics for \"${track.title}\" by ${track.artist}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarMutedGrey
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onRequestLyrics(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange),
                        modifier = Modifier.testTag("load_lyrics_now_btn")
                    ) {
                        Icon(Icons.Default.Lyrics, contentDescription = null, tint = SolarWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load Lyrics", color = SolarWhite)
                    }
                }
            }

            else -> {
                val plainLines = remember(lyricsData.plainLyrics, lyricsData.lines) {
                    if (lyricsData.plainLyrics.isNotBlank()) {
                        lyricsData.plainLyrics.lines()
                    } else {
                        lyricsData.lines.map { it.text }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lyrics",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarWhite,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { onRequestLyrics(true) }) {
                            Text("Reload", color = SolarBurntOrange, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(plainLines) { _, lineText ->
                            if (lineText.isBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                val isSectionHeader = lineText.startsWith("[") && lineText.endsWith("]")
                                Text(
                                    text = lineText,
                                    style = if (isSectionHeader) {
                                        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    } else {
                                        MaterialTheme.typography.titleMedium
                                    },
                                    color = if (isSectionHeader) SolarWarmAmber else SolarWhite.copy(alpha = 0.92f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueManagementPanel(
    queue: List<SolarTrack>,
    currentTrackId: String,
    onSelectTrack: (SolarTrack) -> Unit,
    onRemoveIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onClearQueue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Playback Queue (${queue.size})", style = MaterialTheme.typography.titleMedium, color = SolarWhite)
                TextButton(onClick = onClearQueue) {
                    Text("Clear Queue", color = SolarBurntOrange)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(queue) { index, item ->
                    val isCurrent = item.id == currentTrackId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) SolarElevatedSurface else SolarDeepBlack)
                            .clickable { onSelectTrack(item) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${item.title}",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isCurrent) SolarBurntOrange else SolarWhite,
                                maxLines = 1
                            )
                            Text(
                                text = item.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = SolarMutedGrey,
                                maxLines = 1
                            )
                        }
                        Row {
                            if (index > 0) {
                                IconButton(onClick = { onMoveItem(index, index - 1) }) {
                                    Icon(Icons.Default.ArrowUpward, "Move Up", tint = SolarMutedGrey, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < queue.lastIndex) {
                                IconButton(onClick = { onMoveItem(index, index + 1) }) {
                                    Icon(Icons.Default.ArrowDownward, "Move Down", tint = SolarMutedGrey, modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = { onRemoveIndex(index) }) {
                                Icon(Icons.Default.Delete, "Remove", tint = SolarMutedGrey, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerAudioEffectsPanel(
    state: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onSetBandLevel: (Short, Short) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Equalizer", style = MaterialTheme.typography.titleMedium, color = SolarWhite)
                        Text("Preset: ${state.activePreset}", style = MaterialTheme.typography.labelSmall, color = SolarWarmAmber)
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = SolarWhite, checkedTrackColor = SolarBurntOrange)
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.presets) { preset ->
                        FilterChip(
                            selected = state.activePreset == preset,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset) }
                        )
                    }
                }
            }

            items(state.bands) { band ->
                val freqLabel = if (band.centerFreqHz >= 1000) "${band.centerFreqHz / 1000}kHz" else "${band.centerFreqHz}Hz"
                val dbValue = band.levelMillibels / 100f
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(freqLabel, style = MaterialTheme.typography.labelMedium, color = SolarWhite)
                        Text("%+.1f dB".format(dbValue), style = MaterialTheme.typography.labelSmall, color = SolarWarmAmber)
                    }
                    Slider(
                        value = band.levelMillibels.toFloat(),
                        onValueChange = { onSetBandLevel(band.index, it.toInt().toShort()) },
                        valueRange = band.minMillibels.toFloat()..band.maxMillibels.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = SolarBurntOrange,
                            activeTrackColor = SolarBurntOrange
                        )
                    )
                }
            }

            item {
                Text("Bass Boost (${state.bassBoostPercent}%)", style = MaterialTheme.typography.labelMedium, color = SolarWhite)
                Slider(
                    value = state.bassBoostPercent.toFloat(),
                    onValueChange = { onSetBassBoost(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = SolarWarmAmber, activeTrackColor = SolarWarmAmber)
                )
                Text("3D Spatial Virtualizer (${state.virtualizerPercent}%)", style = MaterialTheme.typography.labelMedium, color = SolarWhite)
                Slider(
                    value = state.virtualizerPercent.toFloat(),
                    onValueChange = { onSetVirtualizer(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = SolarWarmAmber, activeTrackColor = SolarWarmAmber)
                )
            }
        }
    }
}

// --- GOOGLE SIGN-IN & CREDENTIAL MANAGER HELPERS (MANDATORY ONE-OPTION-PER-REQUEST) ---

fun attemptSolarAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onSolarGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutSolarAccount(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun SolarSettingsAndAccountScreen(
    currentUser: FirebaseUser?,
    settings: SolarAppSettings,
    onAuthStatusChanged: () -> Unit,
    onSyncCloudNow: () -> Unit,
    onConnectYtMusic: (String) -> Unit,
    onDisconnectYtMusic: () -> Unit,
    onSetAmoledBlack: (Boolean) -> Unit,
    onSetAccentIntensity: (Float) -> Unit,
    onSetDynamicArtworkBg: (Boolean) -> Unit,
    onSetEnableAnimations: (Boolean) -> Unit,
    onSetStreamingQuality: (AudioQuality) -> Unit,
    onSetDownloadQuality: (AudioQuality) -> Unit,
    onSetCrossfadeSeconds: (Int) -> Unit,
    onSetGaplessPlayback: (Boolean) -> Unit,
    onSetNormalizeVolume: (Boolean) -> Unit,
    onSetAutoplaySimilar: (Boolean) -> Unit,
    onSetDefaultProvider: (ProviderType) -> Unit,
    onSetWifiOnlyDownloads: (Boolean) -> Unit,
    onSetJioSaavnEnabled: (Boolean) -> Unit,
    onSetPersonalizationEnabled: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenWebAdminPanel: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authErrorText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log in",
                    style = MaterialTheme.typography.displayMedium,
                    color = SolarWhite
                )
                Surface(
                    color = SolarElevatedSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("sign_in_version_badge")
                ) {
                    Text(
                        text = "Version 1.7",
                        style = MaterialTheme.typography.labelLarge,
                        color = SolarWarmAmber,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 1. Google Sign-In Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Log in to sign in with Google account with one tap",
                        style = MaterialTheme.typography.titleLarge,
                        color = SolarWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SOLAR App • Version 1.7",
                        style = MaterialTheme.typography.labelMedium,
                        color = SolarWarmAmber
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (currentUser != null) {
                        Text(
                            text = "Signed in as ${currentUser.displayName ?: "SOLAR Member"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SolarWarmAmber
                        )
                        Text(
                            text = currentUser.email ?: currentUser.uid,
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarMutedGrey
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onSyncCloudNow,
                                colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = SolarWhite)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Library", color = SolarWhite)
                            }
                            OutlinedButton(
                                onClick = {
                                    signOutSolarAccount(
                                        credentialManager = credentialManager,
                                        onSignOutComplete = onAuthStatusChanged,
                                        scope = scope
                                    )
                                }
                            ) {
                                Text("Sign Out", color = SolarMutedGrey)
                            }
                        }
                    } else {
                        Text(
                            text = "Sign in with your Google Account to sync your Liked Songs, Playlists & History across devices.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SolarMutedGrey
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isSigningIn = true
                                authErrorText = null
                                onSolarGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        onAuthStatusChanged()
                                        onSyncCloudNow()
                                    },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        authErrorText = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isSigningIn = false }
                                )
                            },
                            enabled = !isSigningIn,
                            colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_sign_in_button")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = SolarWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Sign in with Google", color = SolarWhite)
                            }
                        }
                        authErrorText?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = err, style = MaterialTheme.typography.labelSmall, color = SolarWarmAmber)
                        }
                    }
                }
            }
        }

        // 2. Web Admin Panel Card (Send Notifications & Direct App Updates to Users)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SolarBurntOrange.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = SolarBurntOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Web Admin Panel",
                                style = MaterialTheme.typography.titleLarge,
                                color = SolarWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Send live notifications & OTA updates directly to users",
                                style = MaterialTheme.typography.labelSmall,
                                color = SolarWarmAmber
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onOpenWebAdminPanel,
                        colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_web_admin_panel_btn")
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = SolarWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Web Admin Panel", color = SolarWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Developer Credit Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SolarLogoWithCredit(showCredit = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "App Version 1.7",
                        style = MaterialTheme.typography.labelLarge,
                        color = SolarWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This is developer's first app bugs are forgiven",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SolarWarmAmber,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SolarWhite)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = SolarMutedGrey)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SolarWhite,
                checkedTrackColor = SolarBurntOrange
            )
        )
    }
}
