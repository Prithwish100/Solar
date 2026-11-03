package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    BackHandler { onCollapse() }

    val context = LocalContext.current
    val track = playbackState.currentTrack ?: return
    var activeSubPanel by remember { mutableStateOf("PLAYER") } // PLAYER, LYRICS, QUEUE, EQUALIZER

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SolarPrimaryOrange.copy(alpha = 0.30f),
                        SolarDeepBlack,
                        SolarDeepBlack
                    )
                )
            )
            .statusBarsPadding()
            .testTag("full_screen_player")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
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
                        text = "NOW PLAYING",
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

                IconButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Listening to \"${track.title}\" by ${track.artist} on SOLAR Music"
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Song", tint = SolarWhite)
                }
            }

            // Mode Switcher Pills (Artwork / Lyrics / Queue / Equalizer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "PLAYER" to "Artwork",
                    "LYRICS" to "Lyrics",
                    "QUEUE" to "Queue (${playbackState.queue.size})",
                    "EQUALIZER" to "Equalizer"
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
                                    .size(265.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = SolarBurntOrange.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(24.dp)
                                    ),
                                cornerRadius = 24.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SolarCardBackground,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = track.audioFormat,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarWarmAmber,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = SolarCardBackground,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = track.recordingType.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SolarBurntOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (playbackState.sleepTimerRemainingMinutes != null) {
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
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    activeSubPanel = "LYRICS"
                                    onRequestLyrics(false)
                                },
                                modifier = Modifier.testTag("request_lyrics_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lyrics,
                                    contentDescription = null,
                                    tint = SolarWarmAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lyricsData?.trackId == track.id) "View Plain Lyrics" else "Load Lyrics on Request",
                                    color = SolarWarmAmber,
                                    style = MaterialTheme.typography.labelMedium
                                )
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

                    "EQUALIZER" -> {
                        EqualizerAudioEffectsPanel(
                            state = equalizerState,
                            onToggleEnabled = onSetEqualizerEnabled,
                            onSelectPreset = onSelectEqPreset,
                            onSetBandLevel = onSetBandLevel,
                            onSetBassBoost = onSetBassBoost,
                            onSetVirtualizer = onSetVirtualizer
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
                        text = "${track.artist} • ${track.genre}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SolarMutedGrey,
                        maxLines = 1
                    )
                }

                Row {
                    if (track.isDownloadPermitted) {
                        IconButton(onClick = onDownloadTrack) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Track",
                                tint = SolarWarmAmber
                            )
                        }
                    }
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite Track",
                            tint = if (isLiked) SolarBurntOrange else SolarWhite
                        )
                    }
                }
            }

            // Seek Bar & Elapsed / Remaining Time
            val duration = playbackState.durationMs.coerceAtLeast(1L)
            val currentPos = playbackState.currentPositionMs.coerceIn(0L, duration)
            val remainingMs = (duration - currentPos).coerceAtLeast(0L)

            Slider(
                value = currentPos.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = SolarBurntOrange,
                    activeTrackColor = SolarBurntOrange,
                    inactiveTrackColor = SolarCardBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPos),
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarMutedGrey
                )
                Text(
                    text = "-${formatDuration(remainingMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarMutedGrey
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Transport Controls (Shuffle, Previous, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) SolarBurntOrange else SolarMutedGrey
                    )
                }

                IconButton(
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

                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable { onTogglePlayPause() }
                        .testTag("full_player_play_pause"),
                    color = SolarBurntOrange,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = SolarWhite,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                IconButton(
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

                IconButton(onClick = onCycleRepeat) {
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

            // Bottom Utility Bar (Speed, Sleep Timer, Lyrics, Equalizer, Output Route)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SolarCardBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val nextSpeed = when (playbackState.playbackSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            2.0f -> 0.75f
                            else -> 1.0f
                        }
                        onSetPlaybackSpeed(nextSpeed)
                    }
                ) {
                    Icon(Icons.Default.Speed, null, tint = SolarWarmAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${playbackState.playbackSpeed}x", color = SolarWhite, style = MaterialTheme.typography.labelMedium)
                }

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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = playbackState.sleepTimerRemainingMinutes?.let { "${it}m" } ?: "Sleep",
                        color = SolarWhite,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                IconButton(
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

                IconButton(onClick = { activeSubPanel = if (activeSubPanel == "QUEUE") "PLAYER" else "QUEUE" }) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (activeSubPanel == "QUEUE") SolarBurntOrange else SolarMutedGrey
                    )
                }

                IconButton(onClick = { activeSubPanel = if (activeSubPanel == "EQUALIZER") "PLAYER" else "EQUALIZER" }) {
                    Icon(
                        Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = if (activeSubPanel == "EQUALIZER") SolarBurntOrange else SolarMutedGrey
                    )
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
                        text = "Fetching plain lyrics for \"${track.title}\"...",
                        style = MaterialTheme.typography.titleSmall,
                        color = SolarWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "On-demand lyrics • No time-sync overhead",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarMutedGrey
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
                        text = "Lyrics on Request",
                        style = MaterialTheme.typography.titleLarge,
                        color = SolarWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Load full plain-text lyrics for \"${track.title}\" by ${track.artist} whenever you want to read along.",
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PLAIN LYRICS (ON REQUEST)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SolarWarmAmber
                            )
                            Text(
                                text = lyricsData.sourceAttribution,
                                style = MaterialTheme.typography.labelSmall,
                                color = SolarMutedGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                        Text("5-Band Studio Equalizer & DSP", style = MaterialTheme.typography.titleMedium, color = SolarWhite)
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
    onClearHistory: () -> Unit
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
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SolarLogoWithCredit(showCredit = true)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Account, Audio Engine, Providers & System Preferences",
                style = MaterialTheme.typography.bodySmall,
                color = SolarMutedGrey
            )
        }

        // 1. Account & Cloud Synchronization Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Account & Cloud Synchronization", style = MaterialTheme.typography.titleLarge, color = SolarWhite)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (currentUser != null) {
                        Text(
                            text = "Signed in as ${currentUser.displayName ?: "SOLAR Member"} (${currentUser.email ?: currentUser.uid})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SolarWarmAmber
                        )
                        Text(
                            text = "Real-time Firestore sync active for Liked Songs, Playlists & Listening History.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarMutedGrey
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onSyncCloudNow,
                                colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = SolarWhite)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Library Now", color = SolarWhite)
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
                            text = "Guest Mode Active • Local playback, playlists & offline downloads work without an account. Sign in with Google to synchronize across devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarMutedGrey
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = err, style = MaterialTheme.typography.labelSmall, color = SolarWarmAmber)
                        }
                    }
                }
            }
        }

        // 2. YouTube Music Account & Library Import Integration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("YouTube Music Account & Library Import", style = MaterialTheme.typography.titleLarge, color = SolarWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (settings.ytMusicConnected) {
                            "Connected: ${settings.ytMusicAccountName} • Liked songs & playlists imported into SOLAR database."
                        } else {
                            "Authorized import of YouTube Music playlists, liked songs & subscriptions into SOLAR's local database."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settings.ytMusicConnected) SolarWarmAmber else SolarMutedGrey
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onConnectYtMusic(currentUser?.email ?: "Google Authorized Session")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
                        ) {
                            Text(
                                text = if (settings.ytMusicConnected) "Refresh Imported Library" else "Connect & Import YT Music",
                                color = SolarWhite
                            )
                        }
                        if (settings.ytMusicConnected) {
                            OutlinedButton(onClick = onDisconnectYtMusic) {
                                Text("Disconnect", color = SolarMutedGrey)
                            }
                        }
                    }
                }
            }
        }

        // 3. Appearance Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance & Visual Identity", style = MaterialTheme.typography.titleLarge, color = SolarWhite)
                    SettingsToggleRow(
                        title = "Pure Black AMOLED Theme (#080808)",
                        subtitle = "Deep obsidian background for OLED battery efficiency",
                        checked = settings.amoledBlackTheme,
                        onCheckedChange = onSetAmoledBlack
                    )
                    SettingsToggleRow(
                        title = "Dynamic Artwork Gradients",
                        subtitle = "Subtle burnt-orange & album aura on player screens",
                        checked = settings.dynamicArtworkBackground,
                        onCheckedChange = onSetDynamicArtworkBg
                    )
                    SettingsToggleRow(
                        title = "Fluid Micro-Animations",
                        subtitle = "Solar corona pulse and smooth transitions",
                        checked = settings.enableAnimations,
                        onCheckedChange = onSetEnableAnimations
                    )
                    Column {
                        Text(
                            text = "Orange Accent Intensity (${(settings.accentIntensity * 100).toInt()}%)",
                            style = MaterialTheme.typography.titleSmall,
                            color = SolarWhite
                        )
                        Slider(
                            value = settings.accentIntensity,
                            onValueChange = onSetAccentIntensity,
                            valueRange = 0.5f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = SolarBurntOrange,
                                activeTrackColor = SolarBurntOrange
                            )
                        )
                    }
                }
            }
        }

        // 4. Playback & Audio Engine Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Playback & Audio Quality", style = MaterialTheme.typography.titleLarge, color = SolarWhite)
                    Text("Streaming Quality: ${settings.streamingQuality.label}", style = MaterialTheme.typography.titleSmall, color = SolarWarmAmber)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AudioQuality.entries.toList()) { q ->
                            FilterChip(
                                selected = settings.streamingQuality == q,
                                onClick = { onSetStreamingQuality(q) },
                                label = { Text("${q.name} (${q.bitrateKbps}k)") }
                            )
                        }
                    }

                    Text("Default Discovery Provider: ${settings.defaultProvider.displayName}", style = MaterialTheme.typography.titleSmall, color = SolarWhite)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(ProviderType.INNERTUBE, ProviderType.JIOSAAVN, ProviderType.LOCAL)) { p ->
                            FilterChip(
                                selected = settings.defaultProvider == p,
                                onClick = { onSetDefaultProvider(p) },
                                label = { Text(p.badgeLabel) }
                            )
                        }
                    }

                    SettingsToggleRow(
                        title = "Enable JioSaavn Secondary Provider",
                        subtitle = "Include JioSaavn regional & independent catalog in search",
                        checked = settings.jioSaavnEnabled,
                        onCheckedChange = onSetJioSaavnEnabled
                    )
                    SettingsToggleRow(
                        title = "Gapless Playback",
                        subtitle = "Zero silence between consecutive album tracks",
                        checked = settings.gaplessPlayback,
                        onCheckedChange = onSetGaplessPlayback
                    )
                    SettingsToggleRow(
                        title = "ReplayGain Loudness Normalization",
                        subtitle = "Balance volume levels across different providers",
                        checked = settings.normalizeVolume,
                        onCheckedChange = onSetNormalizeVolume
                    )
                    SettingsToggleRow(
                        title = "Autoplay Similar Official Songs",
                        subtitle = "Continue playing related tracks when queue ends",
                        checked = settings.autoplaySimilar,
                        onCheckedChange = onSetAutoplaySimilar
                    )
                    Column {
                        Text("Crossfade Transition: ${settings.crossfadeSeconds} seconds", style = MaterialTheme.typography.titleSmall, color = SolarWhite)
                        Slider(
                            value = settings.crossfadeSeconds.toFloat(),
                            onValueChange = { onSetCrossfadeSeconds(it.toInt()) },
                            valueRange = 0f..12f,
                            colors = SliderDefaults.colors(thumbColor = SolarBurntOrange, activeTrackColor = SolarBurntOrange)
                        )
                    }
                }
            }
        }

        // 5. Network, Privacy & Cache Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Network, Storage & Privacy", style = MaterialTheme.typography.titleLarge, color = SolarWhite)
                    SettingsToggleRow(
                        title = "Wi-Fi Only Offline Downloads",
                        subtitle = "Pause large track downloads on metered mobile data",
                        checked = settings.wifiOnlyDownloads,
                        onCheckedChange = onSetWifiOnlyDownloads
                    )
                    SettingsToggleRow(
                        title = "Listening History & Personalization",
                        subtitle = "Record played tracks for Daily Mixes & Monthly Reports",
                        checked = settings.personalizationEnabled,
                        onCheckedChange = onSetPersonalizationEnabled
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onClearCache) {
                            Text("Clear Audio/Image Cache", color = SolarWarmAmber)
                        }
                        OutlinedButton(onClick = onClearHistory) {
                            Text("Clear History", color = SolarBurntOrange)
                        }
                    }
                }
            }
        }

        // 6. About SOLAR Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SolarLogoWithCredit(showCredit = true)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version 1.0.0 (Production Native Build)", style = MaterialTheme.typography.titleSmall, color = SolarWhite)
                    Text("Developer Credit: Made by Prithwish", style = MaterialTheme.typography.labelLarge, color = SolarWarmAmber)
                    Text(
                        "Architecture: 100% Native Kotlin • Jetpack Compose Material 3 • AndroidX Media3 ExoPlayer & MediaSessionService • Room Database • DataStore • Cloud Firestore & Credential Manager • OkHttp/Retrofit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarMutedGrey
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
