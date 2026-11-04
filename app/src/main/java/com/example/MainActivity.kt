package com.example

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.SolarAlbum
import com.example.domain.model.SolarTrack
import com.example.ui.SolarViewModel
import com.example.ui.components.SolarGeometricLogo
import com.example.ui.components.SolarLogoWithCredit
import com.example.ui.components.SolarMiniPlayer
import com.example.ui.screens.SolarAlbumDetailScreen
import com.example.ui.screens.SolarAppUpdateDialog
import com.example.ui.screens.SolarExploreScreen
import com.example.ui.screens.SolarFullScreenPlayer
import com.example.ui.screens.SolarHomeScreen
import com.example.ui.screens.SolarLibraryScreen
import com.example.ui.screens.SolarLiveNotificationBanner
import com.example.ui.screens.SolarSearchScreen
import com.example.ui.screens.SolarSettingsAndAccountScreen
import com.example.ui.screens.SolarWebAdminScreen
import com.example.ui.screens.attemptSolarAutoSignIn
import com.example.ui.screens.onSolarGoogleSignInClicked
import com.example.ui.theme.SolarBurntOrange
import com.example.ui.theme.SolarCardBackground
import com.example.ui.theme.SolarDeepBlack
import com.example.ui.theme.SolarDivider
import com.example.ui.theme.SolarElevatedSurface
import com.example.ui.theme.SolarMutedGrey
import com.example.ui.theme.SolarPrimaryOrange
import com.example.ui.theme.SolarTheme
import com.example.ui.theme.SolarWarmAmber
import com.example.ui.theme.SolarWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class SolarNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    LIBRARY("library", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolarRootApp()
        }
    }
}

@Composable
fun SolarRootApp() {
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var guestModeUnlocked by rememberSaveable { mutableStateOf(true) }
    var showDedicatedAuthGate by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Request Android 13+ notification permission for Media3 foreground playback controls
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }
        LaunchedEffect(Unit) {
            runCatching { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    // Attempt silent Google Auto-Sign-In on cold launch
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            val credentialManager = CredentialManager.create(context)
            attemptSolarAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    if (currentUser == null && showDedicatedAuthGate && !guestModeUnlocked) {
        SolarTheme {
            SolarAuthGateScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    showDedicatedAuthGate = false
                },
                onContinueAsGuest = {
                    guestModeUnlocked = true
                    showDedicatedAuthGate = false
                }
            )
        }
    } else {
        val viewModel: SolarViewModel = viewModel(
            key = currentUser?.uid ?: "guest_session",
            factory = SolarViewModel.Factory
        )
        val settings by viewModel.appSettings.collectAsStateWithLifecycle()

        SolarTheme(
            amoledBlack = settings.amoledBlackTheme,
            accentIntensity = settings.accentIntensity
        ) {
            SolarMainScaffold(
                viewModel = viewModel,
                currentUser = currentUser,
                onAuthChanged = { currentUser = Firebase.auth.currentUser }
            )
        }
    }
}

@Composable
fun SolarAuthGateScreen(
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SolarPrimaryOrange.copy(alpha = 0.28f),
                        SolarDeepBlack,
                        SolarDeepBlack
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SolarGeometricLogo(size = 76.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SOLAR",
                    style = MaterialTheme.typography.displayMedium,
                    color = SolarWhite
                )
                Text(
                    text = stringResource(R.string.developer_credit),
                    style = MaterialTheme.typography.labelLarge,
                    color = SolarWarmAmber
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Audiophile Music Streaming, Synchronized Lyrics, 5-Band Equalizer & Cross-Device Cloud Sync.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SolarMutedGrey,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        isLoading = true
                        errorMsg = null
                        onSolarGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMsg = msg
                            },
                            scope = scope,
                            onAuthCancelled = { isLoading = false }
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("auth_gate_google_sign_in")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = SolarWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.sign_in_with_google), color = SolarWhite)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_gate_guest_btn")
                ) {
                    Text(stringResource(R.string.continue_as_guest), color = SolarWarmAmber)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Version 1.7",
                    color = SolarMutedGrey,
                    style = MaterialTheme.typography.labelMedium
                )

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = SolarWarmAmber, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SolarMainScaffold(
    viewModel: SolarViewModel,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    onAuthChanged: () -> Unit
) {
    val context = LocalContext.current
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val likedTracks by viewModel.likedTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.userPlaylists.collectAsStateWithLifecycle()
    val localTracks by viewModel.localTracks.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val history by viewModel.listeningHistory.collectAsStateWithLifecycle()
    val activeLyrics by viewModel.activeLyrics.collectAsStateWithLifecycle()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsStateWithLifecycle()
    val lastImportReport by viewModel.lastImportReport.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val selectedAlbumForDetail by viewModel.selectedAlbumForDetail.collectAsStateWithLifecycle()
    val adminBroadcasts by viewModel.adminBroadcasts.collectAsStateWithLifecycle()
    val activeNotificationBroadcast by viewModel.activeNotificationBroadcast.collectAsStateWithLifecycle()
    val activeAppUpdateBroadcast by viewModel.activeAppUpdateBroadcast.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(SolarNavDestination.HOME) }
    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showWebAdminPanel by rememberSaveable { mutableStateOf(false) }
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var trackPendingPlaylistAdd by remember { mutableStateOf<SolarTrack?>(null) }
    var lastHomeBackPressMs by remember { mutableLongStateOf(0L) }

    val likedIds = remember(likedTracks) { likedTracks.map { it.id }.toSet() }
    val effectiveUserName = remember(settings.userName, currentUser) {
        when {
            settings.userName.isNotBlank() -> settings.userName
            !currentUser?.displayName.isNullOrBlank() -> currentUser?.displayName?.split(" ")?.firstOrNull() ?: ""
            else -> ""
        }
    }

    // Back Navigation & Double-Back-to-Exit without interrupting music playback
    BackHandler {
        when {
            isPlayerExpanded -> {
                isPlayerExpanded = false
            }
            showWebAdminPanel -> {
                showWebAdminPanel = false
            }
            selectedAlbumForDetail != null -> {
                viewModel.closeAlbumDetail()
            }
            showSettingsScreen -> {
                showSettingsScreen = false
            }
            currentDestination != SolarNavDestination.HOME -> {
                currentDestination = SolarNavDestination.HOME
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastHomeBackPressMs < 2500L) {
                    (context as? Activity)?.finish()
                } else {
                    lastHomeBackPressMs = now
                    val msg = context.getString(R.string.press_back_again_to_exit)
                    viewModel.postBanner(msg)
                    runCatching {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SolarDeepBlack.copy(alpha = 0.96f))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Status Toast Banner
                    AnimatedVisibility(visible = statusBanner != null) {
                        statusBanner?.let { msg ->
                            Surface(
                                color = SolarElevatedSurface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = SolarWarmAmber,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Persistent Mini-Player above Bottom Navigation Bar
                    val activeTrack = playbackState.currentTrack ?: homeState.recommendedTracks.firstOrNull()
                    if (activeTrack != null) {
                        val dur = playbackState.durationMs.coerceAtLeast(1L)
                        val progress = if (playbackState.currentTrack != null) {
                            (playbackState.currentPositionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        val bufferedFrac = if (playbackState.currentTrack != null) {
                            maxOf(
                                (playbackState.bufferedPositionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f),
                                (playbackState.bufferedPercentage / 100f).coerceIn(0f, 1f),
                                progress
                            )
                        } else 0f

                        SolarMiniPlayer(
                            track = activeTrack,
                            isPlaying = playbackState.isPlaying,
                            isBuffering = playbackState.isBuffering && playbackState.currentTrack != null,
                            progressFraction = progress,
                            bufferedFraction = bufferedFrac,
                            isLiked = likedIds.contains(activeTrack.id),
                            onTogglePlayPause = {
                                if (playbackState.currentTrack == null) {
                                    viewModel.playTrack(activeTrack, homeState.recommendedTracks)
                                } else {
                                    viewModel.togglePlayPause()
                                }
                            },
                            onSkipNext = { viewModel.skipNext() },
                            onToggleLike = { viewModel.toggleLikeTrack(activeTrack) },
                            onExpandPlayer = {
                                if (playbackState.currentTrack == null) {
                                    viewModel.playTrack(activeTrack, homeState.recommendedTracks)
                                }
                                isPlayerExpanded = true
                            }
                        )
                    }

                    NavigationBar(
                        containerColor = SolarDeepBlack,
                        tonalElevation = 0.dp
                    ) {
                        SolarNavDestination.entries.forEach { dest ->
                            val selected = !showSettingsScreen && !showWebAdminPanel && selectedAlbumForDetail == null && currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    showSettingsScreen = false
                                    showWebAdminPanel = false
                                    viewModel.closeAlbumDetail()
                                    currentDestination = dest
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SolarWhite,
                                    selectedTextColor = SolarBurntOrange,
                                    indicatorColor = SolarPrimaryOrange.copy(alpha = 0.32f),
                                    unselectedIconColor = SolarMutedGrey,
                                    unselectedTextColor = SolarMutedGrey
                                ),
                                modifier = Modifier.testTag("nav_tab_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                val activeAlbum = selectedAlbumForDetail
                if (showWebAdminPanel) {
                    SolarWebAdminScreen(
                        broadcasts = adminBroadcasts,
                        onBack = { showWebAdminPanel = false },
                        onSendNotification = { title, message ->
                            viewModel.sendAdminNotification(title, message)
                        },
                        onSendAppUpdate = { version, title, notes, downloadUrl, mandatory ->
                            viewModel.sendAdminAppUpdate(version, title, notes, downloadUrl, mandatory)
                        },
                        onDeleteBroadcast = { viewModel.deleteAdminBroadcast(it) }
                    )
                } else if (activeAlbum != null) {
                    SolarAlbumDetailScreen(
                        album = activeAlbum,
                        currentPlayingTrack = playbackState.currentTrack,
                        likedTrackIds = likedIds,
                        onBack = { viewModel.closeAlbumDetail() },
                        onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                        onShufflePlay = { tracks ->
                            val shuffled = tracks.shuffled()
                            val first = shuffled.firstOrNull()
                            if (first != null) viewModel.playTrack(first, shuffled)
                        },
                        onToggleLike = { viewModel.toggleLikeTrack(it) },
                        onPlayNext = { viewModel.playNextInQueue(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onAddToPlaylist = { trackPendingPlaylistAdd = it },
                        onDownloadTrack = { viewModel.startDownload(it) }
                    )
                } else if (showSettingsScreen) {
                    SolarSettingsAndAccountScreen(
                        currentUser = currentUser,
                        settings = settings,
                        onAuthStatusChanged = onAuthChanged,
                        onSyncCloudNow = { viewModel.syncAccountWithCloud() },
                        onConnectYtMusic = { acc -> viewModel.connectAndImportYouTubeMusic(acc) },
                        onDisconnectYtMusic = { viewModel.disconnectYouTubeMusic() },
                        onSetAmoledBlack = { viewModel.setAmoledBlack(it) },
                        onSetAccentIntensity = { viewModel.setAccentIntensity(it) },
                        onSetDynamicArtworkBg = { viewModel.setDynamicArtworkBg(it) },
                        onSetEnableAnimations = { viewModel.setEnableAnimations(it) },
                        onSetStreamingQuality = { viewModel.setStreamingQuality(it) },
                        onSetDownloadQuality = { viewModel.setDownloadQuality(it) },
                        onSetCrossfadeSeconds = { viewModel.setCrossfadeSeconds(it) },
                        onSetGaplessPlayback = { viewModel.setGaplessPlayback(it) },
                        onSetNormalizeVolume = { viewModel.setNormalizeVolume(it) },
                        onSetAutoplaySimilar = { viewModel.setAutoplaySimilar(it) },
                        onSetDefaultProvider = { viewModel.setDefaultProvider(it) },
                        onSetWifiOnlyDownloads = { viewModel.setWifiOnlyDownloads(it) },
                        onSetJioSaavnEnabled = { viewModel.setJioSaavnEnabled(it) },
                        onSetPersonalizationEnabled = { viewModel.setPersonalizationEnabled(it) },
                        onClearCache = { viewModel.clearImageAndAudioCache() },
                        onClearHistory = { viewModel.clearListeningHistory() },
                        onOpenWebAdminPanel = { showWebAdminPanel = true }
                    )
                } else {
                    when (currentDestination) {
                        SolarNavDestination.HOME -> {
                            SolarHomeScreen(
                                homeState = homeState,
                                currentPlayingTrack = playbackState.currentTrack,
                                likedTrackIds = likedIds,
                                localTracks = localTracks,
                                userName = effectiveUserName,
                                isCloudSignedIn = currentUser != null,
                                onSelectMood = { viewModel.selectMoodFilter(it) },
                                onRefresh = { viewModel.loadInitialData() },
                                onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                onToggleLike = { viewModel.toggleLikeTrack(it) },
                                onPlayNext = { viewModel.playNextInQueue(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylistRequest = { trackPendingPlaylistAdd = it },
                                onDownloadTrack = { viewModel.startDownload(it) },
                                onNavigateToSearch = { currentDestination = SolarNavDestination.SEARCH },
                                onNavigateToSettings = { showSettingsScreen = true },
                                onOpenAlbumDetail = { viewModel.openAlbumDetail(it) }
                            )
                        }

                        SolarNavDestination.SEARCH -> {
                            SolarSearchScreen(
                                searchState = searchState,
                                currentPlayingTrack = playbackState.currentTrack,
                                likedTrackIds = likedIds,
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                onSubmitSearch = { viewModel.submitSearchQuery(it) },
                                onSelectFilter = { viewModel.setSearchFilter(it) },
                                onToggleOfficialOriginal = { viewModel.toggleOnlyOfficialOriginal() },
                                onRemoveHistoryItem = { viewModel.removeSearchHistoryItem(it) },
                                onClearHistory = { viewModel.clearSearchHistory() },
                                onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                onToggleLike = { viewModel.toggleLikeTrack(it) },
                                onPlayNext = { viewModel.playNextInQueue(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylistRequest = { trackPendingPlaylistAdd = it },
                                onDownloadTrack = { viewModel.startDownload(it) },
                                onOpenAlbumDetail = { viewModel.openAlbumDetail(it) }
                            )
                        }

                        SolarNavDestination.LIBRARY -> {
                            SolarLibraryScreen(
                                likedTracks = likedTracks,
                                playlists = playlists,
                                localTracks = localTracks,
                                downloads = downloads,
                                history = history,
                                hiddenFolders = settings.hiddenLocalFolders,
                                lastImportReport = lastImportReport,
                                currentPlayingTrack = playbackState.currentTrack,
                                onPlayTrack = { track, q -> viewModel.playTrack(track, q) },
                                onToggleLike = { viewModel.toggleLikeTrack(it) },
                                onPlayNext = { viewModel.playNextInQueue(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onAddToPlaylistRequest = { trackPendingPlaylistAdd = it },
                                onCreatePlaylist = { n, d, pub -> viewModel.createPlaylist(n, d, pub) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onDuplicatePlaylist = { viewModel.duplicatePlaylist(it) },
                                onMergePlaylists = { src, dst -> viewModel.mergePlaylists(src, dst) },
                                onImportExternalPlaylist = { src, title, raw ->
                                    viewModel.importExternalPlaylist(src, title, raw)
                                },
                                onDismissImportReport = { viewModel.dismissImportReport() },
                                onExportPlaylistM3u = { viewModel.exportPlaylistAsM3u(it) },
                                onScanLocalMusic = { viewModel.scanLocalDeviceMusic() },
                                onToggleHiddenFolder = { viewModel.toggleHiddenLocalFolder(it) },
                                onStartDownload = { viewModel.startDownload(it) },
                                onDeleteDownload = { viewModel.deleteDownload(it) },
                                onClearHistory = { viewModel.clearListeningHistory() }
                            )
                        }
                    }
                }
            }
        }

        // Full-Screen Immersive Player Overlay
        AnimatedVisibility(
            visible = isPlayerExpanded && playbackState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val activeTrack = playbackState.currentTrack
            if (activeTrack != null) {
                SolarFullScreenPlayer(
                    playbackState = playbackState,
                    lyricsData = activeLyrics,
                    isLoadingLyrics = isLoadingLyrics,
                    onRequestLyrics = { force -> viewModel.requestLyricsForCurrentTrack(force) },
                    equalizerState = equalizerState,
                    isLiked = likedIds.contains(activeTrack.id),
                    onCollapse = { isPlayerExpanded = false },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.skipNext() },
                    onSkipPrevious = { viewModel.skipPrevious() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    onToggleLike = { viewModel.toggleLikeTrack(activeTrack) },
                    onDownloadTrack = { viewModel.startDownload(activeTrack) },
                    onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
                    onSetSleepTimer = { viewModel.setSleepTimer(it) },
                    onPlayTrackFromQueue = { viewModel.playTrack(it, playbackState.queue) },
                    onRemoveFromQueue = { viewModel.removeFromQueue(it) },
                    onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                    onClearQueue = { viewModel.clearQueue() },
                    onSetEqualizerEnabled = { viewModel.setEqualizerEnabled(it) },
                    onSelectEqPreset = { viewModel.selectEqualizerPreset(it) },
                    onSetBandLevel = { idx, lvl -> viewModel.setEqualizerBandLevel(idx, lvl) },
                    onSetBassBoost = { viewModel.setBassBoost(it) },
                    onSetVirtualizer = { viewModel.setVirtualizer(it) }
                )
            }
        }

        // Live Web Admin Broadcast Notification Banner at Top of Screen
        activeNotificationBroadcast?.let { notifBroadcast ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .align(Alignment.TopCenter)
            ) {
                SolarLiveNotificationBanner(
                    broadcast = notifBroadcast,
                    onDismiss = { viewModel.dismissActiveNotificationBroadcast() }
                )
            }
        }

        // Live Web Admin OTA App Update Dialog
        activeAppUpdateBroadcast?.let { updateBroadcast ->
            SolarAppUpdateDialog(
                updateBroadcast = updateBroadcast,
                onDismiss = { viewModel.dismissActiveAppUpdateBroadcast() }
            )
        }
    }

    // Add Track to Playlist Picker Modal
    trackPendingPlaylistAdd?.let { track ->
        AlertDialog(
            onDismissRequest = { trackPendingPlaylistAdd = null },
            containerColor = SolarCardBackground,
            title = { Text("Add \"${track.title}\" to Playlist", color = SolarWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    playlists.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SolarElevatedSurface)
                                .clickable {
                                    viewModel.addTrackToPlaylist(pl.id, track)
                                    trackPendingPlaylistAdd = null
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pl.name, color = SolarWhite, style = MaterialTheme.typography.titleSmall)
                            Text("${pl.tracks.size} tracks", color = SolarWarmAmber, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylist("Solar Favorites", "Quick created playlist", false)
                    }
                ) {
                    Text("+ New Playlist", color = SolarBurntOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { trackPendingPlaylistAdd = null }) {
                    Text("Cancel", color = SolarMutedGrey)
                }
            }
        )
    }

    // First-Launch Aesthetic Minimal User Name Prompt Modal
    if (!settings.hasPromptedForName) {
        var inputName by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = { viewModel.skipNamePrompt() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SolarDeepBlack.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name_prompt_modal"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SolarDivider)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SolarBurntOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = SolarWarmAmber,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Welcome to SOLAR",
                            color = SolarWhite,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "What should we call you?",
                            color = SolarMutedGrey,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            placeholder = { Text("Your name (optional)", color = SolarMutedGrey) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SolarWhite,
                                unfocusedTextColor = SolarWhite,
                                focusedBorderColor = SolarBurntOrange,
                                unfocusedBorderColor = SolarDivider,
                                focusedContainerColor = SolarElevatedSurface,
                                unfocusedContainerColor = SolarElevatedSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("name_prompt_input")
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = { viewModel.saveUserName(inputName.trim()) },
                            colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("name_prompt_save_btn")
                        ) {
                            Text(
                                text = "Get Started",
                                color = SolarWhite,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { viewModel.skipNamePrompt() },
                            modifier = Modifier.testTag("name_prompt_cancel_btn")
                        ) {
                            Text(
                                text = "Skip for now",
                                color = SolarMutedGrey,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
