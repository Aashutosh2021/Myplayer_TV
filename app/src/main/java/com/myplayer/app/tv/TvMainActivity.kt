package com.myplayer.app.tv

import android.app.SearchManager
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.myplayer.app.playback.MusicController
import com.myplayer.app.ui.opentune.OpenTuneUiPrefs
import com.myplayer.app.ui.theme.MyPlayerTheme
import com.myplayer.app.ui.theme.ThemeState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Utility to detect if the current device is an Android TV / Google TV / Fire TV / Leanback Box.
 */
fun isTelevisionDevice(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
    val pm = context.packageManager
    return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
}

/**
 * Dedicated Android TV / Google TV / Fire TV Activity for MyPlayer (`LEANBACK_LAUNCHER`).
 * - Handles TV Voice Search ("Play Arijit Singh on MyPlayer" via `MEDIA_PLAY_FROM_SEARCH`).
 * - Intercepts hardware TV remote media keys (Play/Pause, Next, Previous, Fast-Forward, Rewind)
 *   globally across every tab without needing focus on transport buttons.
 */
@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {

    @Inject
    lateinit var musicController: MusicController

    private var voiceQueryState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeState.isDarkMode = true
        OpenTuneUiPrefs.load(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        extractVoiceSearchQuery(intent)?.let { voiceQueryState = it }

        setContent {
            MyPlayerTheme(darkTheme = true) {
                TvMainScreen(
                    initialVoiceQuery = voiceQueryState,
                    onConsumeVoiceQuery = { voiceQueryState = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractVoiceSearchQuery(intent)?.let { voiceQueryState = it }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    musicController.playPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    if (!musicController.isPlaying.value) musicController.playPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    if (musicController.isPlaying.value) musicController.playPause()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    musicController.skipToNext()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    musicController.skipToPrevious()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    val dur = musicController.currentDuration.value.coerceAtLeast(10_000L)
                    val target = (musicController.currentPosition.value + 10_000L).coerceAtMost(dur)
                    musicController.seekTo(target)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    val target = (musicController.currentPosition.value - 10_000L).coerceAtLeast(0L)
                    musicController.seekTo(target)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun extractVoiceSearchQuery(intent: Intent?): String? {
        if (intent?.action != MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) return null
        return intent.getStringExtra(SearchManager.QUERY)?.trim()?.takeIf { it.isNotBlank() }
    }
}