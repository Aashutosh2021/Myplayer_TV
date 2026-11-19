package com.myplayer.app.tv

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myplayer.app.cast.CastSpeakerDevice
import com.myplayer.app.cast.LocalCastManager
import com.myplayer.app.data.local.datastore.SettingsDataStore
import com.myplayer.app.data.preferences.AudioPlaybackState
import com.myplayer.app.data.preferences.AudioPlaybackStore
import com.myplayer.app.data.preferences.AudioQualityTier
import com.myplayer.app.data.preferences.ClarityOutputPreset
import com.myplayer.app.data.preferences.Eq15Preset
import com.myplayer.app.data.preferences.LoudnessMode
import com.myplayer.app.data.preferences.SpatialMode
import com.myplayer.app.data.recognize.ShazamRecognizer
import com.myplayer.app.data.sponsorblock.SponsorBlockManager
import com.myplayer.app.playback.BitPerfectUsbController
import com.myplayer.app.playback.MusicController
import com.myplayer.app.playback.dsp.AutoEqHeadphoneEntry
import com.myplayer.app.playback.dsp.AutoEqManager
import com.myplayer.app.ui.opentune.OpenTuneUiPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Dedicated TV ViewModel wiring all latest Android audiophile, DSP, OpenTune,
 * SponsorBlock, AutoEq, Shazam, USB Bit-Perfect DAC, and DLNA LAN Cast engines into Android TV.
 */
@HiltViewModel
class TvExtrasViewModel @Inject constructor(
    val audioPlaybackStore: AudioPlaybackStore,
    val sponsorBlockManager: SponsorBlockManager,
    val autoEqManager: AutoEqManager,
    val bitPerfectUsbController: BitPerfectUsbController,
    val localCastManager: LocalCastManager,
    val shazamRecognizer: ShazamRecognizer,
    val settingsDataStore: SettingsDataStore,
    val musicController: MusicController
) : ViewModel() {

    val audioState: StateFlow<AudioPlaybackState> = audioPlaybackStore.stateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioPlaybackState())

    val autoplayEnabled: StateFlow<Boolean> = settingsDataStore.isAutoplayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val usbDacStatus = bitPerfectUsbController.status
    val castDevices = localCastManager.devices
    val activeCastDevice = localCastManager.activeDevice
    val shazamState = shazamRecognizer.state
    val shazamHistory = shazamRecognizer.history
    val lastSkippedSponsorCategory = sponsorBlockManager.lastSkippedCategory

    private val _isScanningCast = MutableStateFlow(false)
    val isScanningCast: StateFlow<Boolean> = _isScanningCast.asStateFlow()

    private val _castStatusMessage = MutableStateFlow<String?>(null)
    val castStatusMessage: StateFlow<String?> = _castStatusMessage.asStateFlow()

    private val _autoEqEntries = MutableStateFlow<List<AutoEqHeadphoneEntry>>(emptyList())
    val autoEqEntries: StateFlow<List<AutoEqHeadphoneEntry>> = _autoEqEntries.asStateFlow()

    private val _autoEqLoading = MutableStateFlow(false)
    val autoEqLoading: StateFlow<Boolean> = _autoEqLoading.asStateFlow()

    private val _autoEqStatus = MutableStateFlow<String?>(null)
    val autoEqStatus: StateFlow<String?> = _autoEqStatus.asStateFlow()

    fun setStreamingQuality(tier: AudioQualityTier) = viewModelScope.launch {
        audioPlaybackStore.setStreamingQuality(tier)
    }

    fun setDownloadQuality(tier: AudioQualityTier) = viewModelScope.launch {
        audioPlaybackStore.setDownloadQuality(tier)
    }

    fun setEqEnabled(enabled: Boolean) = viewModelScope.launch {
        audioPlaybackStore.setEqEnabled(enabled)
    }

    fun setEqPreset(preset: Eq15Preset) = viewModelScope.launch {
        audioPlaybackStore.setEqPreset(preset)
    }

    fun setEqBandGain(index: Int, gainDb: Float) = viewModelScope.launch {
        audioPlaybackStore.setEqBandGain(index, gainDb, audioState.value.eqBandGains)
    }

    fun setStudioMasterClarity(enabled: Boolean) = viewModelScope.launch {
        audioPlaybackStore.setStudioMasterClarity(enabled)
    }

    fun setClarityOutputPreset(preset: ClarityOutputPreset) = viewModelScope.launch {
        audioPlaybackStore.setClarityOutputPreset(preset)
    }

    fun setDolbySurroundAtmos(enabled: Boolean) = viewModelScope.launch {
        audioPlaybackStore.setDolbySurroundAtmos(enabled)
    }

    fun setSpatialAudioEnabled(enabled: Boolean) = viewModelScope.launch {
        audioPlaybackStore.setSpatialAudioEnabled(enabled)
    }

    fun setSpatialMode(mode: SpatialMode) = viewModelScope.launch {
        audioPlaybackStore.setSpatialMode(mode)
    }

    fun setLoudnessMode(mode: LoudnessMode) = viewModelScope.launch {
        audioPlaybackStore.setLoudnessMode(mode)
    }

    fun setCrossfade(enabled: Boolean, seconds: Int = audioState.value.crossfadeSeconds) = viewModelScope.launch {
        audioPlaybackStore.setCrossfade(enabled, seconds)
    }

    fun setBitPerfect(enabled: Boolean) = viewModelScope.launch {
        audioPlaybackStore.setBitPerfect(enabled)
        bitPerfectUsbController.setBitPerfectEnabled(enabled)
    }

    fun setUsbExclusive(enabled: Boolean) = viewModelScope.launch {
        audioPlaybackStore.setUsbExclusive(enabled)
        bitPerfectUsbController.setBitPerfectEnabled(enabled)
    }

    fun setKeepAwakeLyrics(enabled: Boolean) = viewModelScope.launch {
        audioPlaybackStore.setKeepAwakeLyrics(enabled)
    }

    fun setAutoplayEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsDataStore.setAutoplayEnabled(enabled)
    }

    fun toggleSponsorBlock(context: Context, enabled: Boolean) {
        OpenTuneUiPrefs.sponsorBlockEnabled = enabled
        sponsorBlockManager.isEnabled = enabled
        OpenTuneUiPrefs.save(context)
    }

    fun toggleSponsorBlockCategory(context: Context, category: String) {
        val current = OpenTuneUiPrefs.sponsorBlockCategories
        OpenTuneUiPrefs.sponsorBlockCategories = if (category in current) {
            current - category
        } else {
            current + category
        }
        OpenTuneUiPrefs.save(context)
    }

    fun discoverCastSpeakers() = viewModelScope.launch {
        _isScanningCast.value = true
        _castStatusMessage.value = "Scanning Wi-Fi for DLNA / UPnP speakers..."
        runCatching { localCastManager.discoverLanSpeakers() }
        _isScanningCast.value = false
        val count = castDevices.value.size
        _castStatusMessage.value = if (count > 0) {
            "Found $count speaker(s) on your LAN"
        } else {
            "No DLNA speakers found on this Wi-Fi network"
        }
    }

    fun castCurrentTrack(device: CastSpeakerDevice) = viewModelScope.launch {
        val local = musicController.currentSong.value
        val online = musicController.currentOnlineSong.value
        val title = local?.title ?: online?.title ?: "MyPlayer TV Track"
        val artist = local?.artist ?: online?.artist ?: "MyPlayer"
        val localFile = local?.path?.takeIf { !it.startsWith("http") && !it.startsWith("online://") }
            ?.let { File(it) }?.takeIf { it.exists() }
        val remoteUrl = online?.streamUrl ?: local?.path?.takeIf { it.startsWith("http") }

        val ok = runCatching {
            localCastManager.castTrackToDevice(
                scope = viewModelScope,
                device = device,
                title = title,
                artist = artist,
                localFile = localFile,
                remoteStreamUrl = remoteUrl
            )
        }.getOrDefault(false)

        _castStatusMessage.value = if (ok) {
            "Casting \"$title\" to ${device.name}"
        } else {
            "Could not cast to ${device.name} (start playing a track first)"
        }
    }

    fun loadAutoEqIndexIfNeeded() {
        if (_autoEqEntries.value.isNotEmpty() || _autoEqLoading.value) return
        viewModelScope.launch {
            _autoEqLoading.value = true
            _autoEqStatus.value = "Loading AutoEq database..."
            val entries = runCatching { autoEqManager.loadIndex() }.getOrDefault(emptyList())
            _autoEqEntries.value = entries
            _autoEqLoading.value = false
            _autoEqStatus.value = if (entries.isNotEmpty()) {
                "${entries.size} headphone & speaker profiles ready"
            } else {
                "AutoEq database offline — check internet"
            }
        }
    }

    fun applyAutoEqProfile(context: Context, entry: AutoEqHeadphoneEntry) = viewModelScope.launch {
        _autoEqLoading.value = true
        _autoEqStatus.value = "Applying ${entry.name}..."
        val profile = runCatching { autoEqManager.fetchAndApplyProfile(entry) }.getOrNull()
        _autoEqLoading.value = false
        if (profile != null) {
            OpenTuneUiPrefs.activeAutoEqModel = entry.name
            OpenTuneUiPrefs.save(context)
            audioPlaybackStore.setEqEnabled(true)
            _autoEqStatus.value = "Applied AutoEq: ${entry.name}"
        } else {
            _autoEqStatus.value = "Failed to fetch AutoEq profile for ${entry.name}"
        }
    }

    fun startShazam() = shazamRecognizer.startListening()
    fun cancelShazam() = shazamRecognizer.cancelListening()
    fun resetShazam() = shazamRecognizer.resetState()
}
