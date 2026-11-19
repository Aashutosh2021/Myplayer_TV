package com.myplayer.app.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myplayer.app.data.preferences.AudioQualityTier
import com.myplayer.app.data.preferences.ClarityOutputPreset
import com.myplayer.app.data.preferences.Eq15Preset
import com.myplayer.app.data.preferences.LoudnessMode
import com.myplayer.app.data.preferences.SpatialMode
import com.myplayer.app.data.sponsorblock.SponsorBlockManager
import com.myplayer.app.sync.LtConnectionState
import com.myplayer.app.sync.SyncConnectionState
import com.myplayer.app.sync.ui.SyncPlayViewModel
import com.myplayer.app.ui.opentune.OpenTuneUiPrefs
import com.myplayer.app.ui.screens.settings.ManualUpdateState
import com.myplayer.app.ui.screens.settings.SettingsViewModel

@Composable
fun TvAudioLabAndSettingsTab(
    tvExtrasViewModel: TvExtrasViewModel,
    settingsViewModel: SettingsViewModel,
    syncPlayViewModel: SyncPlayViewModel
) {
    val context = LocalContext.current
    val audioState by tvExtrasViewModel.audioState.collectAsStateWithLifecycle()
    val autoplayEnabled by tvExtrasViewModel.autoplayEnabled.collectAsStateWithLifecycle()
    val usbStatus by tvExtrasViewModel.usbDacStatus.collectAsStateWithLifecycle()
    val castDevices by tvExtrasViewModel.castDevices.collectAsStateWithLifecycle()
    val activeCast by tvExtrasViewModel.activeCastDevice.collectAsStateWithLifecycle()
    val isScanningCast by tvExtrasViewModel.isScanningCast.collectAsStateWithLifecycle()
    val castStatusMsg by tvExtrasViewModel.castStatusMessage.collectAsStateWithLifecycle()
    val autoEqEntries by tvExtrasViewModel.autoEqEntries.collectAsStateWithLifecycle()
    val autoEqLoading by tvExtrasViewModel.autoEqLoading.collectAsStateWithLifecycle()
    val autoEqStatus by tvExtrasViewModel.autoEqStatus.collectAsStateWithLifecycle()
    val updateState by settingsViewModel.updateState.collectAsStateWithLifecycle()
    val syncLanState by syncPlayViewModel.uiState.collectAsStateWithLifecycle()
    val listenTogetherState by syncPlayViewModel.ltState.collectAsStateWithLifecycle()

    var section by remember { mutableStateOf(0) } // 0=Audio & DSP, 1=EQ & AutoEq, 2=SponsorBlock & Cast, 3=SyncPlay & Updates
    var autoEqQuery by remember { mutableStateOf("") }

    val qualityList = AudioQualityTier.entries
    val eqPresetList = Eq15Preset.entries
    val clarityList = ClarityOutputPreset.entries
    val loudnessList = LoudnessMode.entries
    val spatialList = SpatialMode.entries

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 18.dp)
        ) {
            item {
                TvActionChip(
                    label = "Audio & DSP",
                    icon = Icons.Rounded.Tune,
                    selected = section == 0,
                    onClick = { section = 0 }
                )
            }
            item {
                TvActionChip(
                    label = "15-Band EQ & AutoEq",
                    icon = Icons.Rounded.Equalizer,
                    selected = section == 1,
                    onClick = {
                        section = 1
                        tvExtrasViewModel.loadAutoEqIndexIfNeeded()
                    }
                )
            }
            item {
                TvActionChip(
                    label = "SponsorBlock & Cast",
                    icon = Icons.Rounded.Cast,
                    selected = section == 2,
                    onClick = { section = 2 }
                )
            }
            item {
                TvActionChip(
                    label = "SyncPlay & Updates",
                    icon = Icons.Rounded.Groups,
                    selected = section == 3,
                    onClick = { section = 3 }
                )
            }
        }

        when (section) {
            // ── SECTION 0: AUDIO QUALITY, SPATIAL, CLARITY, CROSSFADE, BIT-PERFECT USB ──
            0 -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        TvSectionHeader(
                            title = "Audio Quality & Playback Engine",
                            subtitle = "Use D-Pad Left / Right or OK on any stepper to switch values"
                        )
                    }
                    item {
                        val idx = qualityList.indexOf(audioState.streamingQuality).coerceAtLeast(0)
                        TvStepperCard(
                            title = "Streaming Quality",
                            subtitle = audioState.streamingQuality.subtitle,
                            valueText = audioState.streamingQuality.title,
                            icon = Icons.Rounded.HighQuality,
                            onPrevious = {
                                val prev = qualityList[(idx - 1 + qualityList.size) % qualityList.size]
                                tvExtrasViewModel.setStreamingQuality(prev)
                            },
                            onNext = {
                                val next = qualityList[(idx + 1) % qualityList.size]
                                tvExtrasViewModel.setStreamingQuality(next)
                            }
                        )
                    }
                    item {
                        val idx = qualityList.indexOf(audioState.downloadQuality).coerceAtLeast(0)
                        TvStepperCard(
                            title = "Offline Download Quality",
                            subtitle = audioState.downloadQuality.subtitle,
                            valueText = audioState.downloadQuality.title,
                            icon = Icons.Rounded.Download,
                            onPrevious = {
                                val prev = qualityList[(idx - 1 + qualityList.size) % qualityList.size]
                                tvExtrasViewModel.setDownloadQuality(prev)
                            },
                            onNext = {
                                val next = qualityList[(idx + 1) % qualityList.size]
                                tvExtrasViewModel.setDownloadQuality(next)
                            }
                        )
                    }
                    item {
                        TvToggleCard(
                            title = "Studio Master Clarity Engine",
                            subtitle = "64-bit native C++ DSP mastering chain for TV speakers & soundbars",
                            checked = audioState.studioMasterClarity,
                            icon = Icons.Rounded.AutoAwesome,
                            onToggle = { tvExtrasViewModel.setStudioMasterClarity(!audioState.studioMasterClarity) }
                        )
                    }
                    item {
                        val idx = clarityList.indexOf(audioState.clarityOutputPreset).coerceAtLeast(0)
                        TvStepperCard(
                            title = "Clarity Output Profile",
                            subtitle = audioState.clarityOutputPreset.description,
                            valueText = audioState.clarityOutputPreset.title,
                            icon = Icons.Rounded.Speaker,
                            onPrevious = {
                                val prev = clarityList[(idx - 1 + clarityList.size) % clarityList.size]
                                tvExtrasViewModel.setClarityOutputPreset(prev)
                            },
                            onNext = {
                                val next = clarityList[(idx + 1) % clarityList.size]
                                tvExtrasViewModel.setClarityOutputPreset(next)
                            }
                        )
                    }
                    item {
                        TvToggleCard(
                            title = "Dolby Surround Atmos",
                            subtitle = "Wide living-room surround soundstage for TV & Home Theater",
                            checked = audioState.dolbySurroundAtmos,
                            icon = Icons.Rounded.SurroundSound,
                            onToggle = { tvExtrasViewModel.setDolbySurroundAtmos(!audioState.dolbySurroundAtmos) }
                        )
                    }
                    item {
                        TvToggleCard(
                            title = "3D Spatial Audio",
                            subtitle = audioState.spatialMode.description,
                            checked = audioState.spatialAudioEnabled,
                            icon = Icons.Rounded.SpatialAudio,
                            onToggle = { tvExtrasViewModel.setSpatialAudioEnabled(!audioState.spatialAudioEnabled) }
                        )
                    }
                    item {
                        val idx = spatialList.indexOf(audioState.spatialMode).coerceAtLeast(0)
                        TvStepperCard(
                            title = "Spatial Audio Mode",
                            subtitle = audioState.spatialMode.description,
                            valueText = audioState.spatialMode.title,
                            icon = Icons.Rounded.Headphones,
                            onPrevious = {
                                val prev = spatialList[(idx - 1 + spatialList.size) % spatialList.size]
                                tvExtrasViewModel.setSpatialMode(prev)
                            },
                            onNext = {
                                val next = spatialList[(idx + 1) % spatialList.size]
                                tvExtrasViewModel.setSpatialMode(next)
                            }
                        )
                    }
                    item {
                        val idx = loudnessList.indexOf(audioState.loudnessMode).coerceAtLeast(0)
                        TvStepperCard(
                            title = "Loudness Normalization (ReplayGain)",
                            subtitle = audioState.loudnessMode.description,
                            valueText = audioState.loudnessMode.title,
                            icon = Icons.Rounded.GraphicEq,
                            onPrevious = {
                                val prev = loudnessList[(idx - 1 + loudnessList.size) % loudnessList.size]
                                tvExtrasViewModel.setLoudnessMode(prev)
                            },
                            onNext = {
                                val next = loudnessList[(idx + 1) % loudnessList.size]
                                tvExtrasViewModel.setLoudnessMode(next)
                            }
                        )
                    }
                    item {
                        TvToggleCard(
                            title = "DJ Crossfade / Crossmix",
                            subtitle = "Smooth dual-deck crossfade between consecutive tracks",
                            checked = audioState.crossfadeEnabled,
                            icon = Icons.Rounded.SwapHoriz,
                            onToggle = { tvExtrasViewModel.setCrossfade(!audioState.crossfadeEnabled) }
                        )
                    }
                    if (audioState.crossfadeEnabled) {
                        item {
                            TvStepperCard(
                                title = "Crossfade Duration",
                                subtitle = "Use D-Pad Left / Right to adjust fade seconds",
                                valueText = "${audioState.crossfadeSeconds}s",
                                icon = Icons.Rounded.Timer,
                                onPrevious = {
                                    tvExtrasViewModel.setCrossfade(true, (audioState.crossfadeSeconds - 1).coerceAtLeast(1))
                                },
                                onNext = {
                                    tvExtrasViewModel.setCrossfade(true, (audioState.crossfadeSeconds + 1).coerceAtMost(12))
                                }
                            )
                        }
                    }
                    item {
                        TvToggleCard(
                            title = "Bit-Perfect USB DAC Output",
                            subtitle = if (usbStatus.connected) {
                                "USB DAC: ${usbStatus.deviceName ?: "Connected"} (${usbStatus.activeSampleRateHz} Hz)"
                            } else {
                                "Connect a USB-C / USB-A DAC to your Android TV box for bit-perfect bypass"
                            },
                            checked = audioState.bitPerfectMode,
                            icon = Icons.Rounded.Usb,
                            onToggle = { tvExtrasViewModel.setBitPerfect(!audioState.bitPerfectMode) }
                        )
                    }
                    item {
                        TvToggleCard(
                            title = "Autoplay Similar Radio",
                            subtitle = "Automatically queue matching tracks when playlist ends",
                            checked = autoplayEnabled,
                            icon = Icons.Rounded.Radio,
                            onToggle = { tvExtrasViewModel.setAutoplayEnabled(!autoplayEnabled) }
                        )
                    }
                }
            }

            // ── SECTION 1: 15-BAND EQ PRESETS + AUTOEQ DATABASE ──
            1 -> {
                val filteredAutoEq = remember(autoEqEntries, autoEqQuery) {
                    if (autoEqQuery.isBlank()) autoEqEntries.take(20)
                    else tvExtrasViewModel.autoEqManager.searchHeadphones(autoEqEntries, autoEqQuery, limit = 24)
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        TvToggleCard(
                            title = "15-Band Parametric Equalizer",
                            subtitle = "Active Preset: ${audioState.eqPreset.label} • Active AutoEq: ${OpenTuneUiPrefs.activeAutoEqModel}",
                            checked = audioState.eqEnabled,
                            icon = Icons.Rounded.Equalizer,
                            onToggle = { tvExtrasViewModel.setEqEnabled(!audioState.eqEnabled) }
                        )
                    }
                    item {
                        val idx = eqPresetList.indexOf(audioState.eqPreset).coerceAtLeast(0)
                        TvStepperCard(
                            title = "15-Band EQ Preset",
                            subtitle = "D-Pad Left / Right to switch presets instantly",
                            valueText = audioState.eqPreset.label,
                            icon = Icons.Rounded.Tune,
                            onPrevious = {
                                val prev = eqPresetList[(idx - 1 + eqPresetList.size) % eqPresetList.size]
                                tvExtrasViewModel.setEqPreset(prev)
                            },
                            onNext = {
                                val next = eqPresetList[(idx + 1) % eqPresetList.size]
                                tvExtrasViewModel.setEqPreset(next)
                            }
                        )
                    }
                    item {
                        TvSectionHeader(
                            title = "AutoEq Headphone & Speaker Profiles (5,000+ Models)",
                            subtitle = autoEqStatus ?: "Search your headphone or soundbar model below"
                        )
                        OutlinedTextField(
                            value = autoEqQuery,
                            onValueChange = { autoEqQuery = it },
                            placeholder = { Text("Search e.g. Sony WH-1000XM5, Sennheiser, AirPods...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {}),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (autoEqLoading && filteredAutoEq.isEmpty()) {
                        item {
                            Text("Loading AutoEq profiles...", color = Color.White.copy(alpha = 0.65f))
                        }
                    } else {
                        items(filteredAutoEq) { entry ->
                            val isSelected = OpenTuneUiPrefs.activeAutoEqModel == entry.name
                            TvFocusableSurface(
                                onClick = { tvExtrasViewModel.applyAutoEqProfile(context, entry) },
                                modifier = Modifier.fillMaxWidth(),
                                focusedScale = 1.02f,
                                containerColor = if (isSelected) TvAccentGreen.copy(alpha = 0.22f) else TvCardBg
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Headphones,
                                        contentDescription = null,
                                        tint = if (isSelected) TvAccentGreen else Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Measured by ${entry.source}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                    Text(
                                        if (isSelected) "APPLIED" else "Apply",
                                        color = if (isSelected) TvAccentGreen else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── SECTION 2: SPONSORBLOCK & LAN DLNA CAST ──
            2 -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        TvSectionHeader(
                            title = "SponsorBlock Auto-Skip",
                            subtitle = "Automatically skip non-music intros, outros, and sponsor segments"
                        )
                    }
                    item {
                        TvToggleCard(
                            title = "Enable SponsorBlock",
                            subtitle = "Skips selected categories on YouTube music videos automatically",
                            checked = OpenTuneUiPrefs.sponsorBlockEnabled,
                            icon = Icons.Rounded.SkipNext,
                            onToggle = {
                                tvExtrasViewModel.toggleSponsorBlock(context, !OpenTuneUiPrefs.sponsorBlockEnabled)
                            }
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(SponsorBlockManager.DEFAULT_CATEGORIES.toList()) { cat ->
                                val enabled = cat in OpenTuneUiPrefs.sponsorBlockCategories
                                TvActionChip(
                                    label = cat.replace("_", " ").uppercase(),
                                    icon = if (enabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                    selected = enabled,
                                    accentColor = TvAccentGreen,
                                    onClick = { tvExtrasViewModel.toggleSponsorBlockCategory(context, cat) }
                                )
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(10.dp))
                        TvSectionHeader(
                            title = "LAN Speaker Cast (DLNA / UPnP)",
                            subtitle = castStatusMsg ?: "Stream current TV audio to Wi-Fi speakers or AV receivers",
                            trailing = {
                                TvActionChip(
                                    label = if (isScanningCast) "Scanning..." else "Scan Speakers",
                                    icon = Icons.Rounded.Radar,
                                    selected = true,
                                    accentColor = TvAccentBlue,
                                    onClick = { tvExtrasViewModel.discoverCastSpeakers() }
                                )
                            }
                        )
                    }
                    if (castDevices.isEmpty()) {
                        item {
                            Text(
                                "Press Scan Speakers to discover DLNA/UPnP receivers on your Wi-Fi network.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        items(castDevices) { dev ->
                            val isActive = activeCast?.id == dev.id
                            TvFocusableSurface(
                                onClick = { tvExtrasViewModel.castCurrentTrack(dev) },
                                modifier = Modifier.fillMaxWidth(),
                                focusedScale = 1.02f,
                                containerColor = if (isActive) TvAccentBlue.copy(alpha = 0.25f) else TvCardBg
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.SpeakerGroup, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dev.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${dev.protocol} • ${dev.host}:${dev.port}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                    Text(
                                        if (isActive) "CASTING" else "Cast Track",
                                        color = if (isActive) TvAccentGreen else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── SECTION 3: SYNCPLAY / LISTEN TOGETHER & UPDATES ──
            3 -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        TvSectionHeader(
                            title = "SyncPlay (Multi-Room Wi-Fi Sync)",
                            subtitle = "Host or join synchronized playback across your TV and phones on the same Wi-Fi"
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TvActionChip(
                                label = "Host LAN Room",
                                icon = Icons.Rounded.Hub,
                                selected = syncLanState.connectionState == SyncConnectionState.CONNECTED,
                                accentColor = TvAccentPurple,
                                onClick = { syncPlayViewModel.createRoom("MyPlayer TV") }
                            )
                            TvActionChip(
                                label = "Join Nearby Room",
                                icon = Icons.Rounded.WifiFind,
                                accentColor = TvAccentBlue,
                                onClick = { syncPlayViewModel.joinNearbyRoom() }
                            )
                            if (syncLanState.connectionState != SyncConnectionState.IDLE) {
                                TvActionChip(
                                    label = "Leave LAN Room",
                                    icon = Icons.Rounded.ExitToApp,
                                    accentColor = TvAccentPink,
                                    onClick = { syncPlayViewModel.leaveRoom() }
                                )
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TvCardBg)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "LAN Sync Status: ${syncLanState.connectionState} • IP: ${syncLanState.localIpAddress ?: "Detecting..."} • Connected Devices: ${syncLanState.devices.size}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(6.dp))
                        TvSectionHeader(
                            title = "Online Listen Together (Meld Room)",
                            subtitle = "Create a shareable room code so friends can listen live with your TV"
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TvActionChip(
                                label = "Create Online Room",
                                icon = Icons.Rounded.Public,
                                selected = listenTogetherState.connectionState == LtConnectionState.CONNECTED,
                                accentColor = TvAccentGreen,
                                onClick = { syncPlayViewModel.createOnlineRoom("MyPlayer TV") }
                            )
                            if (listenTogetherState.connectionState != LtConnectionState.IDLE) {
                                TvActionChip(
                                    label = "Leave Online Room",
                                    icon = Icons.Rounded.ExitToApp,
                                    accentColor = TvAccentPink,
                                    onClick = { syncPlayViewModel.leaveOnlineRoom() }
                                )
                            }
                        }
                    }
                    if (listenTogetherState.roomCode.isNotBlank() || listenTogetherState.connectionState != LtConnectionState.IDLE) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TvAccentGreen.copy(alpha = 0.16f))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "Room Code: ${listenTogetherState.roomCode.ifBlank { "Connecting..." }} • Status: ${listenTogetherState.connectionState} • Listeners: ${listenTogetherState.users.size}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        TvSectionHeader(
                            title = "App Updates",
                            subtitle = "Check GitHub releases for the latest MyPlayer TV build"
                        )
                    }
                    item {
                        val statusText = when (val u = updateState) {
                            is ManualUpdateState.Idle -> "Press to check for new MyPlayer updates"
                            is ManualUpdateState.Checking -> "Checking for updates..."
                            is ManualUpdateState.UpToDate -> "You are running the latest version!"
                            is ManualUpdateState.Available -> "Update Available: v${u.updateInfo.latestVersion}"
                            is ManualUpdateState.NoInternet -> u.message
                            is ManualUpdateState.Error -> u.message
                        }
                        TvFocusableSurface(
                            onClick = { settingsViewModel.checkForUpdates() },
                            modifier = Modifier.fillMaxWidth(),
                            focusedScale = 1.02f
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = TvAccentAmber)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Check for Updates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(statusText, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                                Text("Check", color = TvAccentAmber, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
