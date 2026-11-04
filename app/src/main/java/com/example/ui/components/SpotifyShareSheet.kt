package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.LyricsData
import com.example.domain.model.SolarTrack

enum class SpotifyShareMode {
    SONG, LYRICS
}

@Composable
fun SpotifyShareSheet(
    track: SolarTrack,
    lyricsData: LyricsData? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(SpotifyShareMode.SONG) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    val bgPresets = listOf(
        // 0: Vibrant Red (Matching screenshot card 1)
        Color(0xFFE50914) to Brush.verticalGradient(listOf(Color(0xFFFF1E27), Color(0xFFD00010))),
        // 1: Deep Crimson Red (Matching screenshot card 2)
        Color(0xFF8B0000) to Brush.verticalGradient(listOf(Color(0xFF7A0909), Color(0xFF380202))),
        // 2: AMOLED Dark
        Color(0xFF181818) to Brush.verticalGradient(listOf(Color(0xFF242424), Color(0xFF101010))),
        // 3: Warm Solar Amber
        Color(0xFFD97706) to Brush.verticalGradient(listOf(Color(0xFFE68A00), Color(0xFF994D00)))
    )

    val currentBrush = bgPresets[selectedColorIndex].second

    val shareText = "Listening to \"${track.title}\" by ${track.artist} on SOLAR Music"
    val songUrl = if (track.id.startsWith("ytm_")) {
        "https://music.youtube.com/watch?v=${track.id.removePrefix("ytm_")}"
    } else {
        "https://open.spotify.com/track/${track.id}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF121212))
                .clickable(enabled = false) {} // Prevent click from dismissing
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle & Close
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF555555))
                        .align(Alignment.Center)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Preview Canvas Card (Matching Spotify share card layout)
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(370.dp)
                    .shadow(16.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(currentBrush)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedMode == SpotifyShareMode.SONG) {
                    // Song Mode: Inner dark rounded container with artwork and metadata
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.38f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Artwork
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Song Info
                        Column {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Spotify Branding Tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Spotify",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Lyrics Mode: Dynamic lyrics card with excerpt
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.38f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header with mini artwork
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Lyrics Lines Display
                        val linesToDisplay = remember(lyricsData, track) {
                            val activeLines = lyricsData?.lines?.map { it.text.trim() }?.filter { it.isNotBlank() }
                            if (!activeLines.isNullOrEmpty()) {
                                activeLines.take(4)
                            } else {
                                listOf(
                                    track.title,
                                    "By ${track.artist}",
                                    "Feel the rhythm, feel the rhyme",
                                    "Streaming on SOLAR"
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            linesToDisplay.forEach { lyricLine ->
                                Text(
                                    text = lyricLine,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        lineHeight = 24.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        // Spotify Branding Tag
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Spotify",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dual Pill Toggle: [ Song ]  [ Lyrics ]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF2C2C2E))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Song Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (selectedMode == SpotifyShareMode.SONG) Color.White else Color.Transparent)
                        .clickable { selectedMode = SpotifyShareMode.SONG }
                        .padding(horizontal = 28.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Song",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selectedMode == SpotifyShareMode.SONG) Color.Black else Color.White
                    )
                }

                // Lyrics Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (selectedMode == SpotifyShareMode.LYRICS) Color.White else Color.Transparent)
                        .clickable { selectedMode = SpotifyShareMode.LYRICS }
                        .padding(horizontal = 28.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selectedMode == SpotifyShareMode.LYRICS) Color.Black else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Swatches Row (Red, Gradient Red, AMOLED Black, Amber Palette)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bgPresets.forEachIndexed { index, (solidColor, brush) ->
                    val isSelected = selectedColorIndex == index
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, Color.White, CircleShape)
                                } else Modifier
                            )
                            .padding(if (isSelected) 3.dp else 0.dp)
                            .clip(CircleShape)
                            .background(brush)
                            .clickable { selectedColorIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == 3) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Custom palette",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share Action Targets: [Copy link] [Status] [WhatsApp] [Stories] [SMS]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Copy link
                ShareTargetItem(
                    icon = Icons.Default.Link,
                    label = "Copy link",
                    backgroundColor = Color(0xFF2C2C2E),
                    iconTint = Color.White,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Song Link", "$shareText\n$songUrl")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )

                // 2. Status
                ShareTargetItem(
                    icon = Icons.Default.DonutLarge,
                    label = "Status",
                    backgroundColor = Color(0xFF25D366),
                    iconTint = Color.White,
                    onClick = {
                        shareToSpecificOrGeneral(context, "com.whatsapp", "$shareText\n$songUrl")
                        onDismiss()
                    }
                )

                // 3. WhatsApp
                ShareTargetItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "WhatsApp",
                    backgroundColor = Color(0xFF25D366),
                    iconTint = Color.White,
                    onClick = {
                        shareToSpecificOrGeneral(context, "com.whatsapp", "$shareText\n$songUrl")
                        onDismiss()
                    }
                )

                // 4. Stories
                ShareTargetItem(
                    icon = Icons.Default.PhotoCamera,
                    label = "Stories",
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                    ),
                    iconTint = Color.White,
                    onClick = {
                        shareToSpecificOrGeneral(context, "com.instagram.android", "$shareText\n$songUrl")
                        onDismiss()
                    }
                )

                // 5. SMS
                ShareTargetItem(
                    icon = Icons.Default.Sms,
                    label = "SMS",
                    backgroundColor = Color(0xFF2C2C2E),
                    iconTint = Color.White,
                    onClick = {
                        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("sms:")
                            putExtra("sms_body", "$shareText\n$songUrl")
                        }
                        runCatching {
                            context.startActivity(smsIntent)
                        }.onFailure {
                            val general = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "$shareText\n$songUrl")
                            }
                            context.startActivity(Intent.createChooser(general, "Share Song"))
                        }
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ShareTargetItem(
    icon: ImageVector,
    label: String,
    backgroundColor: Color = Color.Transparent,
    brush: Brush? = null,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .then(
                    if (brush != null) {
                        Modifier.background(brush)
                    } else {
                        Modifier.background(backgroundColor)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

private fun shareToSpecificOrGeneral(context: Context, packageName: String, content: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = packageName
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val chooser = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(chooser, "Share Song"))
    }
}
