package com.myplayer.app.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.media3.common.Player
import com.myplayer.app.data.lyrics.LyricsScript
import com.myplayer.app.ui.common.AlbumArtImage
import com.myplayer.app.ui.opentune.OpenTuneUiPrefs
import com.myplayer.app.ui.screens.main.MainViewModel
import com.myplayer.app.ui.screens.nowplaying.LyricsMetadata
import com.myplayer.app.ui.screens.nowplaying.LyricsUiState
import com.myplayer.app.ui.screens.nowplaying.LyricsViewModel
import com.myplayer.app.ui.screens.nowplaying.SignalPathInspectorDialog

@Composable
fun TvNowPlayingScreen(
    mainViewModel: MainViewModel,
    lyricsViewModel: LyricsViewModel,
    tvExtrasViewModel: TvExtrasViewModel
) {
    val currentSong by mainViewModel.currentSong.collectAsStateWithLifecycle()
    val currentOnlineSong by mainViewModel.currentOnlineSong.collectAsStateWithLifecycle()
    val isPlaying by mainViewModel.isPlaying.collectAsStateWithLifecycle()
    val positionMs by mainViewModel.currentPosition.collectAsStateWithLifecycle()
    val durationMs by mainViewModel.currentDuration.collectAsStateWithLifecycle()
    val audioQuality by mainViewModel.currentAudioQuality.collectAsStateWithLifecycle()
    val isFavorite by mainViewModel.isFavorite.collectAsStateWithLifecycle()
    val isShuffleOn by mainViewModel.isShuffleOn.collectAsStateWithLifecycle()
    val repeatMode by mainViewModel.repeatMode.collectAsStateWithLifecycle()
    val sleepTimerSec by mainViewModel.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    val playbackQueue by mainViewModel.playbackQueue.collectAsStateWithLifecycle()
    val isCurrentDownloaded by mainViewModel.isCurrentSongDownloaded.collectAsStateWithLifecycle()
    val isCurrentDownloading by mainViewModel.isCurrentSongDownloading.collectAsStateWithLifecycle()

    val lyricsState by lyricsViewModel.lyricsState.collectAsStateWithLifecycle()
    val scriptMode by lyricsViewModel.scriptMode.collectAsStateWithLifecycle()
    val audioState by tvExtrasViewModel.audioState.collectAsStateWithLifecycle()
    val usbStatus by tvExtrasViewModel.usbDacStatus.collectAsStateWithLifecycle()
    val lastSkippedSponsor by tvExtrasViewModel.lastSkippedSponsorCategory.collectAsStateWithLifecycle()

    var rightPaneTab by remember { mutableStateOf(0) } // 0 = Karaoke Lyrics, 1 = Up Next Queue
    var showSignalPath by remember { mutableStateOf(false) }

    val displayTitle = currentSong?.title ?: currentOnlineSong?.title ?: "No Track Playing"
    val displayArtist = currentSong?.artist ?: currentOnlineSong?.artist ?: "Pick any song from Home, Hotlist, or Search"
    val displayArt = currentSong?.albumArt ?: currentOnlineSong?.thumbnailUrl
    val currentTrackId = currentSong?.id ?: currentOnlineSong?.videoId ?: ""

    // Automatically load lyrics whenever track changes
    LaunchedEffect(currentTrackId, displayTitle, displayArtist) {
        if (currentTrackId.isNotBlank() && displayTitle != "No Track Playing") {
            lyricsViewModel.fetchLyrics(
                LyricsMetadata(
                    songId = currentTrackId,
                    title = displayTitle,
                    artist = displayArtist,
                    album = currentSong?.album,
                    durationMs = durationMs,
                    localPath = currentSong?.path
                )
            )
        }
    }

    if (showSignalPath) {
        SignalPathInspectorDialog(
            qualityInfo = audioQuality,
            eqEnabled = audioState.eqEnabled,
            spatialEnabled = audioState.spatialAudioEnabled || audioState.dolbySurroundAtmos,
            autoEqModel = OpenTuneUiPrefs.activeAutoEqModel.takeIf { it != "None" && it.isNotBlank() },
            usbStatus = usbStatus,
            onDismiss = { showSignalPath = false }
        )
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // ── LEFT PANE: ALBUM ART + TRANSPORT + QUICK ACTIONS ──
        Column(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Artwork + Title + Signal Path Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtImage(
                    uri = displayArt,
                    contentDescription = displayTitle,
                    modifier = Modifier
                        .size(164.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E30))
                )
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    TvFocusableSurface(
                        onClick = { showSignalPath = true },
                        shape = RoundedCornerShape(50),
                        focusedScale = 1.05f,
                        containerColor = if (audioQuality.isLossless) TvAccentGreen.copy(alpha = 0.25f) else TvAccentPurple.copy(alpha = 0.28f),
                        focusedContainerColor = TvAccentPurple
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${audioQuality.displayBadge} • Signal Path",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = displayArtist,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!lastSkippedSponsor.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "SponsorBlock skipped: $lastSkippedSponsor",
                            color = TvAccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Progress Bar (D-Pad friendly visual bar; seek via -10s / +10s buttons or hardware remote)
            Column(modifier = Modifier.fillMaxWidth()) {
                val safeDur = durationMs.coerceAtLeast(1L)
                val fraction = (positionMs.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvAccentPurple)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTvTime(positionMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(formatTvTime(durationMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            // Primary Transport Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvTransportButton(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) TvAccentPink else Color.White,
                    onClick = { mainViewModel.toggleFavorite() }
                )
                TvTransportButton(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    onClick = { mainViewModel.skipToPrevious() }
                )
                TvTransportButton(
                    icon = Icons.Rounded.Replay10,
                    contentDescription = "Rewind 10s",
                    onClick = { mainViewModel.seekTo((positionMs - 10_000L).coerceAtLeast(0L)) }
                )
                TvFocusableSurface(
                    onClick = { mainViewModel.playPause() },
                    shape = CircleShape,
                    focusedScale = 1.1f,
                    containerColor = TvAccentPurple,
                    focusedContainerColor = Color(0xFF9333EA)
                ) {
                    Box(modifier = Modifier.size(66.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                TvTransportButton(
                    icon = Icons.Rounded.Forward10,
                    contentDescription = "Forward 10s",
                    onClick = { mainViewModel.seekTo((positionMs + 10_000L).coerceAtMost(durationMs.coerceAtLeast(10_000L))) }
                )
                TvTransportButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    onClick = { mainViewModel.skipToNext() }
                )
                TvTransportButton(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleOn) TvAccentGreen else Color.White.copy(alpha = 0.75f),
                    onClick = { mainViewModel.toggleShuffle() }
                )
                TvTransportButton(
                    icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) TvAccentGreen else Color.White.copy(alpha = 0.75f),
                    onClick = { mainViewModel.cycleRepeatMode() }
                )
            }

            // Secondary Action Pills (Sleep Timer, Save Offline, Export MP3/M4A)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val timerLabel = if (sleepTimerSec > 0L) {
                        "Sleep: ${(sleepTimerSec / 60L) + 1}m"
                    } else {
                        "Sleep Timer"
                    }
                    TvActionChip(
                        label = timerLabel,
                        icon = Icons.Rounded.Bedtime,
                        selected = sleepTimerSec > 0L,
                        accentColor = TvAccentBlue,
                        onClick = {
                            when {
                                sleepTimerSec <= 0L -> mainViewModel.startSleepTimer(15)
                                sleepTimerSec <= 15 * 60L -> mainViewModel.startSleepTimer(30)
                                sleepTimerSec <= 30 * 60L -> mainViewModel.startSleepTimer(60)
                                else -> mainViewModel.cancelSleepTimer()
                            }
                        }
                    )
                }
                item {
                    val dlLabel = when {
                        isCurrentDownloading -> "Downloading..."
                        isCurrentDownloaded -> "Saved Offline"
                        else -> "Save Offline"
                    }
                    TvActionChip(
                        label = dlLabel,
                        icon = if (isCurrentDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                        selected = isCurrentDownloaded,
                        accentColor = TvAccentGreen,
                        onClick = { mainViewModel.downloadCurrentSong() }
                    )
                }
                item {
                    TvActionChip(
                        label = "Export MP3",
                        icon = Icons.Rounded.AudioFile,
                        onClick = { mainViewModel.exportCurrentSongAsFile("mp3") }
                    )
                }
                item {
                    TvActionChip(
                        label = "Export M4A",
                        icon = Icons.Rounded.SaveAlt,
                        onClick = { mainViewModel.exportCurrentSongAsFile("m4a") }
                    )
                }
            }
        }

        // ── RIGHT PANE: TOGGLEABLE KARAOKE LYRICS (WITH SCRIPT CONVERTER) OR UP NEXT QUEUE ──
        Column(
            modifier = Modifier
                .weight(1.05f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvActionChip(
                        label = "Karaoke Lyrics",
                        icon = Icons.Rounded.Lyrics,
                        selected = rightPaneTab == 0,
                        onClick = { rightPaneTab = 0 }
                    )
                    TvActionChip(
                        label = "Up Next (${playbackQueue.size})",
                        icon = Icons.Rounded.QueueMusic,
                        selected = rightPaneTab == 1,
                        onClick = { rightPaneTab = 1 }
                    )
                }
                if (rightPaneTab == 0) {
                    TvActionChip(
                        label = "Retry",
                        icon = Icons.Rounded.Refresh,
                        onClick = { lyricsViewModel.retryLyrics() }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (rightPaneTab == 0) {
                // Script Converter Selector (Original / Hinglish / Hindi / Urdu)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(LyricsScript.entries) { script ->
                        TvActionChip(
                            label = script.label,
                            selected = scriptMode == script,
                            accentColor = TvAccentPink,
                            onClick = { lyricsViewModel.setScriptMode(script) }
                        )
                    }
                }

                // Lyrics Content
                when (val st = lyricsState) {
                    is LyricsUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Fetching synced lyrics...", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
                        }
                    }
                    is LyricsUiState.Found -> {
                        val listState = rememberLazyListState()
                        val synced = st.syncedLines
                        if (synced.isNotEmpty()) {
                            val activeIdx = remember(positionMs, synced) {
                                synced.indexOfLast { it.first <= positionMs }.coerceAtLeast(0)
                            }
                            LaunchedEffect(activeIdx) {
                                listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
                            }
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(synced) { idx, (timeMs, line) ->
                                    val isCurrent = idx == activeIdx
                                    TvFocusableSurface(
                                        onClick = { mainViewModel.seekTo(timeMs) },
                                        shape = RoundedCornerShape(12.dp),
                                        focusedScale = 1.02f,
                                        containerColor = if (isCurrent) TvAccentPurple.copy(alpha = 0.22f) else Color.Transparent,
                                        focusedContainerColor = TvCardFocusedBg
                                    ) {
                                        Text(
                                            text = line.ifBlank { "♪" },
                                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.45f),
                                            fontSize = if (isCurrent) 24.sp else 18.sp,
                                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                            lineHeight = if (isCurrent) 30.sp else 24.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            val plainLines = (st.result.plainLyrics ?: "").lines().filter { it.isNotBlank() }
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(plainLines) { line ->
                                    Text(
                                        text = line,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 19.sp,
                                        lineHeight = 26.sp
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Lyrics not available for this track yet.\nPress Retry or switch to Up Next Queue.",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                // Up Next Queue
                if (playbackQueue.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Queue is empty — pick a song to start playing.", color = Color.White.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(playbackQueue) { idx, song ->
                            val isCurrent = song.id == currentTrackId || song.title == displayTitle
                            TvCompactTrackRow(
                                title = song.title,
                                subtitle = song.artist,
                                artworkUrl = song.albumArt,
                                indexBadge = "${idx + 1}",
                                isPlaying = isCurrent,
                                onClick = { mainViewModel.playQueueIndex(idx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvTransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    TvFocusableSurface(
        onClick = onClick,
        shape = CircleShape,
        focusedScale = 1.12f,
        containerColor = Color.White.copy(alpha = 0.10f),
        focusedContainerColor = TvAccentPurple
    ) {
        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}
