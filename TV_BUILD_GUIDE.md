# MyPlayer TV — Complete Standalone Android TV / Google TV / Fire TV Project

## Kya-Kya Features Included Hain (Full Android Feature Parity + D-Pad Optimized)
1. **10-Foot D-Pad UI (`TvComponents.kt`, `TvMainScreen.kt`)**:
   - Focus-zoom cards (1.05x–1.12x) + crisp white focus rings, zero touch-only sliders (D-Pad Left/Right steppers + hardware remote media keys).
   - **Global Hardware TV Remote Keys (`TvMainActivity.kt`)**: Play/Pause, Next, Previous, Fast-Forward (+10s), Rewind (-10s) har screen par direct kaam karte hain.
   - **TV Voice Search (`MEDIA_PLAY_FROM_SEARCH`)**: Google Assistant / TV mic se direct song search.
2. **Home & OpenTune Discovery**:
   - Time-of-day **Daylist Hero Banner**, **Mood & Genre Explorer** (14 moods/genres), **AI Recommended / Quick Picks**, **World Trending Top 50 Hotlist**, and **Favorites / Offline Library**.
3. **Search + Shazam Song Recognition**:
   - D-Pad search, Recent Searches, Played History, 1-click Save Offline & Export MP3, plus **Shazam Song Recognition** (`ShazamRecognizer`) via TV/remote mic.
4. **Library, Playlists & Cloud Sync (`TvLibraryAndGenres.kt`)**:
   - D-Pad **Playlist Detail** (Play All, Shuffle, Save Offline, Export MP3), **Favorites**, **Offline Downloads**, **Local Storage / USB Audio** with Rescan, **Recent History**, and **YouTube / Spotify Sync & Stats**.
5. **10-Foot Split-Screen Now Playing (`TvNowPlayingScreen.kt`)**:
   - Clickable **Audiophile Signal Path Inspector** badge, full D-Pad Transport Row, **Sleep Timer** cycle, **Save Offline**, **Export MP3 / M4A**, **Synced Karaoke Lyrics** with **Script Converter** (`Original`, `Hinglish`, `हिन्दी`, `اردو`), and **Up Next Queue**.
6. **Audio Lab & Settings (`TvAudioLabAndSettings.kt`)**:
   - Streaming & Download Quality tiers (Dolby Atmos -> Hi-Res 24-bit/192kHz -> CD Lossless -> 320k -> YouTube Music).
   - **15-Band Parametric EQ Presets** + **AutoEq 5,000+ Headphone/Speaker Database** search & apply.
   - **Studio Master Clarity Engine**, **Dolby Surround Atmos**, **3D Spatial Audio**, **ReplayGain Loudness Normalization**, **DJ Crossfade / Crossmix** stepper, and **Bit-Perfect USB DAC**.
   - **SponsorBlock Auto-Skip** toggle + category pills, **LAN DLNA / UPnP Speaker Cast**, **SyncPlay Multi-Room Wi-Fi Sync**, **Online Listen Together (Meld Room)**, and **In-App Update Check**.
7. **Phone-to-TV Wi-Fi Remote (`TvRemoteServer.kt`)**:
   - On-screen QR code + LAN URL for Play/Pause, Prev/Next, -10s/+10s, Shuffle, Repeat, Like, and Search-to-Play from any phone browser.

## Build Kaise Karein (Android Studio ya Terminal)
```bash
# Debug APK (sideload on Android TV / Google TV / Fire TV)
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## TV Par Install / Sideload Kaise Karein (ADB)
```bash
adb connect <TV_IP_ADDRESS>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
