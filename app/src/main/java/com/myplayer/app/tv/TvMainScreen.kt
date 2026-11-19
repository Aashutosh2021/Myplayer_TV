package com.myplayer.app.tv

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.myplayer.app.data.recognize.RecognizerUiState
import com.myplayer.app.sync.ui.SyncPlayViewModel
import com.myplayer.app.ui.common.AlbumArtImage
import com.myplayer.app.ui.screens.home.HomeViewModel
import com.myplayer.app.ui.screens.library.LibraryViewModel
import com.myplayer.app.ui.screens.library.PlaylistDetailViewModel
import com.myplayer.app.ui.screens.main.MainViewModel
import com.myplayer.app.ui.screens.nowplaying.LyricsViewModel
import com.myplayer.app.ui.screens.search.OnlineSearchViewModel
import com.myplayer.app.ui.screens.settings.SettingsViewModel
import java.util.Calendar

enum class TvDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    HOTLIST("Hotlist", Icons.Rounded.Whatshot),
    SEARCH("Search", Icons.Rounded.Search),
    LIBRARY("Library", Icons.Rounded.LibraryMusic),
    NOW_PLAYING("Player", Icons.Rounded.Album),
    AUDIO_LAB("Audio Lab", Icons.Rounded.Tune),
    PHONE_REMOTE("Remote", Icons.Rounded.QrCode2)
}

@Composable
fun TvMainScreen(
    initialVoiceQuery: String? = null,
    onConsumeVoiceQuery: () -> Unit = {},
    mainViewModel: MainViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: OnlineSearchViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playlistDetailViewModel: PlaylistDetailViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    syncPlayViewModel: SyncPlayViewModel = hiltViewModel(),
    tvExtrasViewModel: TvExtrasViewModel = hiltViewModel()
) {
    var activeTab by remember { mutableStateOf(TvDestination.HOME) }
    val scope = rememberCoroutineScope()

    val currentSong by mainViewModel.currentSong.collectAsStateWithLifecycle()
    val currentOnlineSong by mainViewModel.currentOnlineSong.collectAsStateWithLifecycle()
    val isPlaying by mainViewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by mainViewModel.currentPosition.collectAsStateWithLifecycle()
    val durationMs by mainViewModel.currentDuration.collectAsStateWithLifecycle()

    val displayTitle = currentSong?.title ?: currentOnlineSong?.title ?: ""
    val displayArtist = currentSong?.artist ?: currentOnlineSong?.artist ?: ""
    val displayArt = currentSong?.albumArt ?: currentOnlineSong?.thumbnailUrl

    // Start Embedded Phone-to-TV LAN Remote Server automatically
    val remoteServer = remember {
        TvRemoteServer(
            onCommand = { action, value ->
                when (action) {
                    "playpause" -> mainViewModel.playPause()
                    "next" -> mainViewModel.skipToNext()
                    "prev" -> mainViewModel.skipToPrevious()
                    "fav" -> mainViewModel.toggleFavorite()
                    "shuffle" -> mainViewModel.toggleShuffle()
                    "repeat" -> mainViewModel.cycleRepeatMode()
                    "seek_fwd" -> mainViewModel.seekTo((positionMs + 10_000L).coerceAtMost(durationMs.coerceAtLeast(10_000L)))
                    "seek_back" -> mainViewModel.seekTo((positionMs - 10_000L).coerceAtLeast(0L))
                    "search_play" -> {
                        if (!value.isNullOrBlank()) {
                            searchViewModel.onSearch(value)
                            activeTab = TvDestination.SEARCH
                        }
                    }
                }
            },
            currentTrackProvider = { displayTitle to displayArtist }
        )
    }
    var remoteLanUrl by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        remoteLanUrl = remoteServer.start(scope)
        onDispose { remoteServer.stop() }
    }

    // Handle Android TV Voice Search ("Play ... on MyPlayer")
    LaunchedEffect(initialVoiceQuery) {
        if (!initialVoiceQuery.isNullOrBlank()) {
            searchViewModel.onSearch(initialVoiceQuery)
            activeTab = TvDestination.SEARCH
            onConsumeVoiceQuery()
        }
    }

    BackHandler(enabled = activeTab != TvDestination.HOME) {
        activeTab = TvDestination.HOME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1F1135), Color(0xFF0A0A12)),
                    radius = 1650f
                )
            )
            .padding(horizontal = 32.dp, vertical = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── TOP 10-FOOT TV NAVIGATION BAR + MINI NOW-PLAYING PILL ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF6D28D9), Color(0xFFBE185D))))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("MYPLAYER TV", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                    items(TvDestination.entries) { tab ->
                        val selected = activeTab == tab
                        TvFocusableSurface(
                            onClick = { activeTab = tab },
                            shape = RoundedCornerShape(50),
                            containerColor = if (selected) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
                            focusedContainerColor = TvAccentPurple
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = tab.label,
                                    color = Color.White,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Quick Jump Mini-Player Pill
                if (displayTitle.isNotBlank() && activeTab != TvDestination.NOW_PLAYING) {
                    Spacer(Modifier.width(14.dp))
                    TvFocusableSurface(
                        onClick = { activeTab = TvDestination.NOW_PLAYING },
                        shape = RoundedCornerShape(50),
                        containerColor = TvAccentPurple.copy(alpha = 0.28f),
                        focusedContainerColor = TvAccentPurple
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtImage(
                                data = displayArt,
                                contentDescription = displayTitle,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.widthIn(max = 180.dp)) {
                                Text(
                                    text = displayTitle,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isPlaying) "Playing • $displayArtist" else "Paused • $displayArtist",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── ACTIVE TV TAB CONTENT ──
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    TvDestination.HOME -> TvHomeTab(
                        homeViewModel = homeViewModel,
                        onOpenNowPlaying = { activeTab = TvDestination.NOW_PLAYING },
                        onOpenHotlist = { activeTab = TvDestination.HOTLIST }
                    )
                    TvDestination.HOTLIST -> TvHotlistAndGenresTab(
                        homeViewModel = homeViewModel,
                        onOpenNowPlaying = { activeTab = TvDestination.NOW_PLAYING }
                    )
                    TvDestination.SEARCH -> TvSearchAndShazamTab(
                        searchViewModel = searchViewModel,
                        homeViewModel = homeViewModel,
                        tvExtrasViewModel = tvExtrasViewModel,
                        onOpenNowPlaying = { activeTab = TvDestination.NOW_PLAYING }
                    )
                    TvDestination.LIBRARY -> TvLibraryTab(
                        mainViewModel = mainViewModel,
                        libraryViewModel = libraryViewModel,
                        playlistDetailViewModel = playlistDetailViewModel,
                        settingsViewModel = settingsViewModel,
                        onOpenNowPlaying = { activeTab = TvDestination.NOW_PLAYING }
                    )
                    TvDestination.NOW_PLAYING -> TvNowPlayingScreen(
                        mainViewModel = mainViewModel,
                        lyricsViewModel = lyricsViewModel,
                        tvExtrasViewModel = tvExtrasViewModel
                    )
                    TvDestination.AUDIO_LAB -> TvAudioLabAndSettingsTab(
                        tvExtrasViewModel = tvExtrasViewModel,
                        settingsViewModel = settingsViewModel,
                        syncPlayViewModel = syncPlayViewModel
                    )
                    TvDestination.PHONE_REMOTE -> TvPhoneRemoteTab(
                        remoteUrl = remoteLanUrl,
                        displayTitle = displayTitle,
                        displayArtist = displayArtist
                    )
                }
            }
        }
    }
}

@Composable
private fun TvHomeTab(
    homeViewModel: HomeViewModel,
    onOpenNowPlaying: () -> Unit,
    onOpenHotlist: () -> Unit
) {
    val recommendations by homeViewModel.recommendations.collectAsStateWithLifecycle()
    val hotlistSongs by homeViewModel.hotlistSongs.collectAsStateWithLifecycle()
    val recentSongs by homeViewModel.recentSongs.collectAsStateWithLifecycle()
    val favoriteSongs by homeViewModel.favoriteSongs.collectAsStateWithLifecycle()
    val selectedCategory by homeViewModel.selectedCategory.collectAsStateWithLifecycle()
    val currentSong by homeViewModel.currentSong.collectAsStateWithLifecycle()
    val userName by homeViewModel.userName.collectAsStateWithLifecycle()

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val (daylistTitle, daylistSub) = remember(hour, userName) {
        val who = userName.ifBlank { "MyPlayer" }
        when (hour) {
            in 5..11 -> "Morning Daylist for $who" to "Fresh acoustic & energetic picks to start your day"
            in 12..16 -> "Afternoon Focus Mix" to "Smooth flow & trending hits for your afternoon"
            in 17..20 -> "Evening Chill Daylist" to "Unwind on the big screen with curated living-room vibes"
            else -> "Late Night Lo-Fi & Soul" to "Deep, relaxing late-night soundscapes"
        }
    }

    val quickMoods = listOf("All", "Bollywood", "Punjabi", "Lo-Fi", "Chill", "Workout", "Party", "Romantic")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. OpenTune Daylist Hero Banner
        item {
            TvFocusableSurface(
                onClick = {
                    val pool = recommendations.ifEmpty { hotlistSongs }
                    if (pool.isNotEmpty()) {
                        homeViewModel.playRecommendedSongs(pool, 0)
                        onOpenNowPlaying()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                focusedScale = 1.02f,
                containerColor = Color(0xFF22163D),
                focusedContainerColor = Color(0xFF331F5C)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF4C1D95).copy(alpha = 0.65f), Color(0xFF9D174D).copy(alpha = 0.45f))
                            )
                        )
                        .padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DAYLIST • TV BIG SCREEN", color = Color(0xFFE9D5FF), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(6.dp))
                        Text(daylistTitle, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text(daylistSub, color = Color.White.copy(alpha = 0.78f), fontSize = 14.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text("Play Daylist", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }
        }

        // 2. Quick Mood Filter Pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(quickMoods) { mood ->
                    TvActionChip(
                        label = mood,
                        selected = selectedCategory.equals(mood, ignoreCase = true),
                        accentColor = TvAccentPurple,
                        onClick = { homeViewModel.selectCategory(mood) }
                    )
                }
                item {
                    TvActionChip(
                        label = "All Genres →",
                        icon = Icons.Rounded.Explore,
                        accentColor = TvAccentPink,
                        onClick = onOpenHotlist
                    )
                }
            }
        }

        // 3. AI Recommended / Quick Picks
        if (recommendations.isNotEmpty()) {
            item {
                TvSectionHeader(
                    title = if (selectedCategory == "All") "Quick Picks & AI Recommended" else "$selectedCategory Radio",
                    subtitle = "Tailored to your listening taste & connected accounts"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(recommendations.take(25)) { idx, song ->
                        TvPosterCard(
                            title = song.title,
                            subtitle = song.artist,
                            artworkUrl = song.thumbnailUrl,
                            isActive = currentSong?.id == song.videoId,
                            onClick = {
                                homeViewModel.playRecommendedSongs(recommendations, idx)
                                onOpenNowPlaying()
                            }
                        )
                    }
                }
            }
        }

        // 4. World Trending Hotlist
        if (hotlistSongs.isNotEmpty()) {
            item {
                TvSectionHeader(
                    title = "Trending Now",
                    subtitle = "Top charts across YouTube Music & global feeds",
                    trailing = {
                        TvActionChip(label = "See Top 50", onClick = onOpenHotlist)
                    }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(hotlistSongs.take(20)) { idx, song ->
                        TvPosterCard(
                            title = song.title,
                            subtitle = song.artist,
                            artworkUrl = song.thumbnailUrl,
                            badge = "#${idx + 1}",
                            isActive = currentSong?.id == song.videoId,
                            onClick = {
                                homeViewModel.playRecommendedSongs(hotlistSongs, idx)
                                onOpenNowPlaying()
                            }
                        )
                    }
                }
            }
        }

        // 5. Favorites & Offline Downloads
        val localPool = (favoriteSongs + recentSongs).distinctBy { it.id }
        if (localPool.isNotEmpty()) {
            item {
                TvSectionHeader(
                    title = "Your Favorites & Offline Library",
                    subtitle = "Instant zero-buffering playback"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(localPool.take(20)) { idx, song ->
                        TvPosterCard(
                            title = song.title,
                            subtitle = song.artist,
                            artworkUrl = song.albumArt,
                            badge = "OFFLINE",
                            isActive = currentSong?.id == song.id,
                            onClick = {
                                homeViewModel.playSong(localPool, idx)
                                onOpenNowPlaying()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSearchAndShazamTab(
    searchViewModel: OnlineSearchViewModel,
    homeViewModel: HomeViewModel,
    tvExtrasViewModel: TvExtrasViewModel,
    onOpenNowPlaying: () -> Unit
) {
    val context = LocalContext.current
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    val recentSearches by searchViewModel.recentSearches.collectAsStateWithLifecycle(initialValue = emptyList())
    val playedHistory by searchViewModel.playedHistory.collectAsStateWithLifecycle()
    val searchResults = searchViewModel.searchResults.collectAsLazyPagingItems()
    val shazamState by tvExtrasViewModel.shazamState.collectAsStateWithLifecycle()

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) tvExtrasViewModel.startShazam()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Input + Shazam Song Recognition Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { searchViewModel.onQueryChange(it) },
                placeholder = { Text("Search songs, artists, albums (or use TV Voice Remote)...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchViewModel.onSearch(query) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.weight(1f)
            )
            TvActionChip(
                label = "Search",
                icon = Icons.Rounded.Search,
                selected = true,
                accentColor = TvAccentPurple,
                onClick = { searchViewModel.onSearch(query) }
            )
            val isListening = shazamState is RecognizerUiState.Listening
            TvActionChip(
                label = if (isListening) "Listening..." else "Recognize Song",
                icon = if (isListening) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                selected = isListening,
                accentColor = TvAccentPink,
                onClick = {
                    if (isListening) {
                        tvExtrasViewModel.cancelShazam()
                    } else {
                        val hasMic = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) tvExtrasViewModel.startShazam()
                        else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
        }

        // Shazam Live Status / Match Banner
        when (val st = shazamState) {
            is RecognizerUiState.Listening -> {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TvAccentPink.copy(alpha = 0.22f))
                        .padding(14.dp)
                ) {
                    Text(
                        "Listening via TV/Remote mic (${"%.1f".format(st.seconds)}s) — Hold music near mic...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            is RecognizerUiState.Found -> {
                Spacer(Modifier.height(10.dp))
                TvFocusableSurface(
                    onClick = {
                        homeViewModel.playRecognizedSong(st.song.title, st.song.artist, st.song.coverUrl) {
                            tvExtrasViewModel.resetShazam()
                            onOpenNowPlaying()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = TvAccentGreen.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = TvAccentGreen)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Matched: ${st.song.title}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(st.song.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                        }
                        Text("Press OK to Play", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is RecognizerUiState.Failed -> {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Recognition: ${st.reason}",
                    color = TvAccentAmber,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            else -> Unit
        }

        Spacer(Modifier.height(14.dp))

        // Recent Searches Row
        if (recentSearches.isNotEmpty() && query.isBlank()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                items(recentSearches.take(10)) { q ->
                    TvActionChip(
                        label = q,
                        icon = Icons.Rounded.History,
                        onClick = {
                            searchViewModel.onQueryChange(q)
                            searchViewModel.onSearch(q)
                        }
                    )
                }
            }
        }

        // Results or Played History
        if (query.isNotBlank() && searchResults.itemCount > 0) {
            val snapshot = (0 until searchResults.itemCount.coerceAtMost(25)).mapNotNull { searchResults[it] }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults.itemCount) { index ->
                    val song = searchResults[index] ?: return@items
                    TvCompactTrackRow(
                        title = song.title,
                        subtitle = song.artist,
                        artworkUrl = song.thumbnailUrl,
                        onClick = {
                            searchViewModel.playSongFromSearch(song, snapshot)
                            onOpenNowPlaying()
                        },
                        trailingAction = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TvActionChip(
                                    label = "Save",
                                    icon = Icons.Rounded.Download,
                                    accentColor = TvAccentGreen,
                                    onClick = { searchViewModel.downloadSong(song) }
                                )
                                TvActionChip(
                                    label = "MP3",
                                    icon = Icons.Rounded.SaveAlt,
                                    onClick = { searchViewModel.exportSongAsFile(song, "mp3") }
                                )
                            }
                        }
                    )
                }
            }
        } else {
            TvSectionHeader(
                title = "Recently Played on TV",
                subtitle = "Press OK on any track to resume"
            )
            if (playedHistory.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Type a song name, use your TV Voice Remote, or press Recognize Song.",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(playedHistory.take(30)) { idx, item ->
                        TvCompactTrackRow(
                            title = item.title,
                            subtitle = item.artist,
                            artworkUrl = item.thumbnailUrl,
                            indexBadge = "${idx + 1}",
                            onClick = {
                                searchViewModel.playHistorySong(item)
                                onOpenNowPlaying()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvPhoneRemoteTab(
    remoteUrl: String?,
    displayTitle: String,
    displayArtist: String
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("PHONE-TO-TV WI-FI REMOTE", color = Color(0xFFC4B5FD), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Control MyPlayer TV from any Phone Browser",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Scan the QR code or open the link below on any phone connected to the same Wi-Fi network. Control Play/Pause, Skip, -10s/+10s Seek, Shuffle, Repeat, Like, or type any song to play directly on TV!",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvAccentPurple.copy(alpha = 0.25f))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(
                    text = remoteUrl ?: "Connect TV to Wi-Fi to start LAN Remote Server",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (displayTitle.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Now Playing: $displayTitle — $displayArtist",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
        }

        if (!remoteUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                TvPairingMatrixBadge(url = remoteUrl, modifier = Modifier.fillMaxSize())
            }
        }
    }
}