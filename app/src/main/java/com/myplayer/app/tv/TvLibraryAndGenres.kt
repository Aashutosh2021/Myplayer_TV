package com.myplayer.app.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myplayer.app.data.repository.PlayableSong
import com.myplayer.app.ui.screens.home.HomeViewModel
import com.myplayer.app.ui.screens.library.LibraryViewModel
import com.myplayer.app.ui.screens.library.PlaylistDetailViewModel
import com.myplayer.app.ui.screens.main.MainViewModel
import com.myplayer.app.ui.screens.settings.SettingsViewModel

private val TV_MOOD_GENRES = listOf(
    "All", "Bollywood", "Punjabi", "Lo-Fi", "Chill", "Workout",
    "Party", "Romantic", "Focus", "Pop", "Hip-Hop", "Rock", "Devotional", "Retro"
)

@Composable
fun TvHotlistAndGenresTab(
    homeViewModel: HomeViewModel,
    onOpenNowPlaying: () -> Unit
) {
    val hotlistSongs by homeViewModel.hotlistSongs.collectAsStateWithLifecycle()
    val recommendations by homeViewModel.recommendations.collectAsStateWithLifecycle()
    val selectedCategory by homeViewModel.selectedCategory.collectAsStateWithLifecycle()
    val currentSong by homeViewModel.currentSong.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            TvSectionHeader(
                title = "Moods & Genres Explorer",
                subtitle = "Select any vibe with D-Pad — press Play Mix to start instant radio",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvActionChip(
                            label = "Play $selectedCategory Mix",
                            icon = Icons.Rounded.PlayArrow,
                            selected = true,
                            accentColor = TvAccentPurple,
                            onClick = {
                                homeViewModel.playCategory(selectedCategory) { onOpenNowPlaying() }
                            }
                        )
                        TvActionChip(
                            label = "Refresh Hotlist",
                            icon = Icons.Rounded.Refresh,
                            onClick = { homeViewModel.refreshTrendingMusic() }
                        )
                    }
                }
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(TV_MOOD_GENRES) { genre ->
                    TvActionChip(
                        label = genre,
                        selected = selectedCategory.equals(genre, ignoreCase = true),
                        accentColor = TvAccentPink,
                        onClick = { homeViewModel.selectCategory(genre) }
                    )
                }
            }
        }

        if (selectedCategory != "All" && recommendations.isNotEmpty()) {
            item {
                TvSectionHeader(
                    title = "$selectedCategory Picks",
                    subtitle = "${recommendations.size} curated tracks"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(recommendations.take(24)) { idx, song ->
                        TvPosterCard(
                            title = song.title,
                            subtitle = song.artist,
                            artworkUrl = song.thumbnailUrl,
                            badge = selectedCategory,
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

        item {
            TvSectionHeader(
                title = "World Trending Top 50",
                subtitle = "Live global & regional chart toppers"
            )
        }

        if (hotlistSongs.isEmpty()) {
            item {
                Text(
                    "Loading World Trending Hotlist...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
            }
        } else {
            itemsIndexed(hotlistSongs.take(50)) { idx, song ->
                TvCompactTrackRow(
                    title = song.title,
                    subtitle = song.artist,
                    artworkUrl = song.thumbnailUrl,
                    indexBadge = "#${idx + 1}",
                    isPlaying = currentSong?.id == song.videoId,
                    onClick = {
                        homeViewModel.playRecommendedSongs(hotlistSongs, idx)
                        onOpenNowPlaying()
                    }
                )
            }
        }
    }
}

@Composable
fun TvLibraryTab(
    mainViewModel: MainViewModel,
    libraryViewModel: LibraryViewModel,
    playlistDetailViewModel: PlaylistDetailViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenNowPlaying: () -> Unit
) {
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val playlistStats by libraryViewModel.playlistStats.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favoriteSongs.collectAsStateWithLifecycle()
    val downloads by libraryViewModel.downloadedSongs.collectAsStateWithLifecycle()
    val localSongs by libraryViewModel.localSongs.collectAsStateWithLifecycle()
    val recentHistory by libraryViewModel.recentHistory.collectAsStateWithLifecycle()
    val isScanningLocal by libraryViewModel.isScanningLocal.collectAsStateWithLifecycle()
    val importStatus by libraryViewModel.importStatusMessage.collectAsStateWithLifecycle()
    val currentSong by mainViewModel.currentSong.collectAsStateWithLifecycle()

    val isSpotifyLoggedIn by settingsViewModel.isSpotifyLoggedIn.collectAsStateWithLifecycle()
    val spotifyUsername by settingsViewModel.spotifyUsername.collectAsStateWithLifecycle()
    val isSpotifySyncing by settingsViewModel.isSpotifySyncing.collectAsStateWithLifecycle()
    val isYouTubeLoggedIn by settingsViewModel.isYouTubeLoggedIn.collectAsStateWithLifecycle()
    val youtubeUsername by settingsViewModel.youtubeUsername.collectAsStateWithLifecycle()
    val isYouTubeSyncing by settingsViewModel.isYouTubeSyncing.collectAsStateWithLifecycle()

    var selectedSubTab by remember { mutableStateOf(0) } // 0=Playlists, 1=Favorites, 2=Offline, 3=Local, 4=History, 5=Sync & Replay
    var openedPlaylistId by remember { mutableStateOf<Long?>(null) }

    val activePlaylist by playlistDetailViewModel.playlist.collectAsStateWithLifecycle()
    val activePlaylistSongs by playlistDetailViewModel.songs.collectAsStateWithLifecycle()

    BackHandler(enabled = openedPlaylistId != null) {
        openedPlaylistId = null
    }

    // ── PLAYLIST DETAIL VIEW (D-Pad friendly) ──
    if (openedPlaylistId != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TvActionChip(
                        label = "Back to Library",
                        icon = Icons.Rounded.ArrowBack,
                        onClick = { openedPlaylistId = null }
                    )
                    Column {
                        Text(
                            text = activePlaylist?.name ?: "Playlist",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${activePlaylistSongs.size} tracks",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvActionChip(
                        label = "Play All",
                        icon = Icons.Rounded.PlayArrow,
                        selected = true,
                        accentColor = TvAccentPurple,
                        onClick = {
                            if (activePlaylistSongs.isNotEmpty()) {
                                mainViewModel.playPlaylist(activePlaylistSongs, 0)
                                onOpenNowPlaying()
                            }
                        }
                    )
                    TvActionChip(
                        label = "Shuffle",
                        icon = Icons.Rounded.Shuffle,
                        accentColor = TvAccentPink,
                        onClick = {
                            if (activePlaylistSongs.isNotEmpty()) {
                                mainViewModel.playPlaylist(activePlaylistSongs.shuffled(), 0)
                                onOpenNowPlaying()
                            }
                        }
                    )
                    TvActionChip(
                        label = "Save Offline",
                        icon = Icons.Rounded.Download,
                        accentColor = TvAccentGreen,
                        onClick = { playlistDetailViewModel.toggleDownloadRemainingSongs() }
                    )
                }
            }

            if (activePlaylistSongs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs in this playlist yet.", color = Color.White.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(activePlaylistSongs) { idx, song ->
                        TvCompactTrackRow(
                            title = song.title,
                            subtitle = song.artist,
                            artworkUrl = song.thumbnailUrl,
                            indexBadge = "${idx + 1}",
                            isPlaying = currentSong?.id == song.id,
                            onClick = {
                                mainViewModel.playPlaylist(activePlaylistSongs, idx)
                                onOpenNowPlaying()
                            },
                            trailingAction = {
                                TvActionChip(
                                    label = "MP3",
                                    icon = Icons.Rounded.SaveAlt,
                                    onClick = { playlistDetailViewModel.exportSongAsFile(song, "mp3") }
                                )
                            }
                        )
                    }
                }
            }
        }
        return
    }

    // ── MAIN LIBRARY VIEW ──
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 18.dp)
        ) {
            val tabs = listOf(
                "Playlists (${playlists.size})" to Icons.Rounded.QueueMusic,
                "Favorites (${favorites.size})" to Icons.Rounded.Favorite,
                "Offline (${downloads.size})" to Icons.Rounded.DownloadDone,
                "Local Storage (${localSongs.size})" to Icons.Rounded.Folder,
                "Recent (${recentHistory.size})" to Icons.Rounded.History,
                "Sync & Stats" to Icons.Rounded.CloudSync
            )
            itemsIndexed(tabs) { idx, (label, icon) ->
                TvActionChip(
                    label = label,
                    icon = icon,
                    selected = selectedSubTab == idx,
                    accentColor = TvAccentPurple,
                    onClick = { selectedSubTab = idx }
                )
            }
        }

        if (!importStatus.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvAccentGreen.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(importStatus.orEmpty(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        when (selectedSubTab) {
            0 -> {
                if (playlists.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No playlists yet — import from Spotify/YouTube or sync your library.",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(playlists) { pl ->
                            val stats = playlistStats[pl.id]
                            val sub = if (stats != null) {
                                "${stats.totalCount} tracks • ${stats.downloadedCount} offline"
                            } else {
                                "Open playlist"
                            }
                            TvFocusableSurface(
                                onClick = {
                                    playlistDetailViewModel.setPlaylistId(pl.id)
                                    openedPlaylistId = pl.id
                                },
                                modifier = Modifier.fillMaxWidth(),
                                focusedScale = 1.02f
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(TvAccentPurple.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = Color.White)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            pl.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(sub, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
                                    }
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
            1 -> PlayableSongTvList(
                songs = favorites,
                emptyMessage = "No favorite songs yet — press ♥ in Now Playing.",
                currentSongId = currentSong?.id,
                onPlay = { list, idx ->
                    mainViewModel.playPlaylist(list, idx)
                    onOpenNowPlaying()
                },
                onExportMp3 = { libraryViewModel.exportSongAsFile(it, "mp3") }
            )
            2 -> PlayableSongTvList(
                songs = downloads,
                emptyMessage = "No offline downloads yet — save any song from Search or Now Playing.",
                currentSongId = currentSong?.id,
                onPlay = { list, idx ->
                    mainViewModel.playPlaylist(list, idx)
                    onOpenNowPlaying()
                },
                onExportMp3 = { libraryViewModel.exportSongAsFile(it, "mp3") }
            )
            3 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Local Storage & USB Drive Audio",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        TvActionChip(
                            label = if (isScanningLocal) "Scanning..." else "Rescan Local Storage",
                            icon = Icons.Rounded.Refresh,
                            accentColor = TvAccentBlue,
                            onClick = { libraryViewModel.rescanLocalSongs() }
                        )
                    }
                    PlayableSongTvList(
                        songs = localSongs,
                        emptyMessage = "No local audio files found. Plug in a USB drive or add music to TV storage and press Rescan.",
                        currentSongId = currentSong?.id,
                        onPlay = { list, idx ->
                            mainViewModel.playPlaylist(list, idx)
                            onOpenNowPlaying()
                        },
                        onExportMp3 = null
                    )
                }
            }
            4 -> {
                if (recentHistory.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No recently played history yet.", color = Color.White.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        itemsIndexed(recentHistory.take(40)) { idx, item ->
                            TvCompactTrackRow(
                                title = item.title,
                                subtitle = item.artist,
                                artworkUrl = item.thumbnailUrl,
                                indexBadge = "${idx + 1}",
                                isPlaying = currentSong?.id == item.id,
                                onClick = {
                                    mainViewModel.playSong(
                                        PlayableSong.Online(
                                            id = item.id,
                                            title = item.title,
                                            artist = item.artist,
                                            durationMs = item.durationMs,
                                            thumbnailUrl = item.thumbnailUrl
                                        )
                                    )
                                    onOpenNowPlaying()
                                }
                            )
                        }
                    }
                }
            }
            5 -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        TvSectionHeader(
                            title = "Cloud Sync & Listening Stats",
                            subtitle = "Sync playlists from YouTube & Spotify directly to your TV"
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            TvStatBox("Playlists", "${playlists.size}", TvAccentPurple, Modifier.weight(1f))
                            TvStatBox("Favorites", "${favorites.size}", TvAccentPink, Modifier.weight(1f))
                            TvStatBox("Offline Tracks", "${downloads.size}", TvAccentGreen, Modifier.weight(1f))
                            TvStatBox("Local Files", "${localSongs.size}", TvAccentBlue, Modifier.weight(1f))
                        }
                    }
                    item {
                        TvFocusableSurface(
                            onClick = { settingsViewModel.syncYouTube() },
                            modifier = Modifier.fillMaxWidth(),
                            focusedScale = 1.02f
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = Color(0xFFFF4444), modifier = Modifier.size(30.dp))
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("YouTube Music Sync", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = if (isYouTubeLoggedIn) {
                                            "Connected as ${youtubeUsername ?: "YouTube User"} • Click to Sync Playlists"
                                        } else {
                                            "Not logged in on TV yet — press to refresh or import via Phone Backup"
                                        },
                                        color = Color.White.copy(alpha = 0.68f),
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    if (isYouTubeSyncing) "Syncing..." else "Sync Now",
                                    color = TvAccentGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    item {
                        TvFocusableSurface(
                            onClick = { settingsViewModel.syncSpotify() },
                            modifier = Modifier.fillMaxWidth(),
                            focusedScale = 1.02f
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(30.dp))
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Spotify Sync", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = if (isSpotifyLoggedIn) {
                                            "Connected as ${spotifyUsername ?: "Spotify User"} • Click to Sync Library"
                                        } else {
                                            "Not logged in on TV yet — press to refresh or restore Phone Backup"
                                        },
                                        color = Color.White.copy(alpha = 0.68f),
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    if (isSpotifySyncing) "Syncing..." else "Sync Now",
                                    color = TvAccentGreen,
                                    fontWeight = FontWeight.Bold
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
private fun TvStatBox(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.18f))
            .padding(16.dp)
    ) {
        Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlayableSongTvList(
    songs: List<PlayableSong>,
    emptyMessage: String,
    currentSongId: String?,
    onPlay: (List<PlayableSong>, Int) -> Unit,
    onExportMp3: ((PlayableSong) -> Unit)?
) {
    if (songs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
        }
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(songs) { idx, song ->
            TvCompactTrackRow(
                title = song.title,
                subtitle = song.artist,
                artworkUrl = song.thumbnailUrl,
                indexBadge = "${idx + 1}",
                isPlaying = currentSongId == song.id,
                onClick = { onPlay(songs, idx) },
                trailingAction = onExportMp3?.let { exporter ->
                    {
                        TvActionChip(
                            label = "MP3",
                            icon = Icons.Rounded.SaveAlt,
                            onClick = { exporter(song) }
                        )
                    }
                }
            )
        }
    }
}
