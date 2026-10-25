# FocusTube 🛡️
> Distraction-Free, Intentional YouTube Client for Android

FocusTube is a modern native Android application built with **Kotlin 2.0**, **Jetpack Compose**, and **AndroidX Media3**. It preserves the deliberate, high-value educational and long-form video experience of YouTube while systematically eliminating cognitive traps, viral dopamine loops, and intrusive clutter.

FocusTube was designed and built following a deep reverse engineering investigation of the official YouTube Android APK (`com.google.android.youtube` v21.42.244) using **REA (Reverse Engineer Anything)**.

---

## 🚀 Key Features

### 1. Intuitive Five-Destination Navigation
1. **Discover** — ordinary long-form video discovery organized by topic (Science, Technology, Philosophy, Education, Documentaries).
2. **Search** — real search with instant query execution and Shorts elimination.
3. **Following** — subscribe to channels locally without requiring a Google Account.
4. **Library** — watch history (with pause & clear) and saved Watch Later bookmarks.
5. **Focus** — intentional viewing rules, distraction controls, and theme settings.

### 2. Optional, Isolated Reels Section
* **Disabled by default**: On a fresh installation, Reels is absent from the navigation bar, search, and feeds.
* **Isolated Sandbox**: When explicitly enabled in Settings, the Reels tab appears with **manual next/previous controls** and **no automatic advancement**.

### 3. Advanced Player & Gesture Controls
* **Fullscreen Gestures**:
  * **Swipe UP** on portrait player -> Smoothly transitions into fullscreen landscape.
  * **Swipe DOWN** on fullscreen player -> Restores portrait layout without accidental seeks.
  * Includes a dedicated fullscreen toggle button for accessibility.
* **Playback Speed up to 3×**: Choose from 12 granular speed options: `0.25×, 0.5×, 0.75×, 1.0×, 1.25×, 1.5×, 1.75×, 2.0×, 2.25×, 2.5×, 2.75×, 3.0×`.
* **Double-Tap Seeking**: Tap left half to seek -10s; tap right half to seek +10s.
* **Study & Sleep Timer**: Set automatic pause timers (15m, 30m, 45m, 60m) with live countdown display.
* **Keep Screen Awake**: Optional display wake-lock to prevent screen timeout during study sessions.
* **Resume Playback**: Remembers playback timestamps across sessions.

### 4. Background Audio & Screen-Off Playback
* Built on **AndroidX Media3** `MediaSessionService` (`FocusTubeMediaService`).
* Keeps audio playing when the app is minimized, when switching to another application, or when the screen is turned off and the device is locked.
* Full lockscreen media notification with play/pause and seek controls.

### 5. Multi-Theme Support
* Instant toggle between **Light Mode**, **Dark Mode**, and **Follow System Appearance**.
* Persists seamlessly across application restarts.

### 6. Distraction-Free Philosophy
* **No Autoplay or AutoNav**: Playback stays stopped when a video ends—no countdown timers or algorithmic auto-transitions.
* **Ad-Free Interface**: Zero third-party ad SDKs, zero AdMob trackers, zero Google AdServices attribution libraries, and no sponsored video cards.
* **Optional Grayscale Thumbnails & Hide Metrics**: De-saturate thumbnails and hide view counts to eliminate clickbait triggers.

---

## 🛠️ Architecture & Technologies

- **Language**: Kotlin 2.0 (100% native)
- **UI Toolkit**: Jetpack Compose + Material 3
- **Architecture Pattern**: MVVM with unidirectional data flow (StateFlow / Coroutines)
- **Playback & Media Session**: AndroidX Media3 `ExoPlayer` + `MediaSessionService` + YouTube NoCookie embed player
- **Networking**: Resilient Invidious / Piped API clients with automatic instance rotation and fallback
- **Persistence**: Android `SharedPreferences` + `Gson` serialization
- **Image Loading**: Coil for Jetpack Compose (with custom Grayscale ColorMatrix transformation)
- **Min SDK**: Android 8.0 (API 26) | **Target SDK**: Android 15 (API 35)

---

## 📁 Project Structure

```
FocusTube/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/focustube/app/
│   │   │   │   ├── FocusTubeApp.kt             # Application entry point
│   │   │   │   ├── MainActivity.kt             # NavHost and deep link routing
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/                  # VideoItem, ChannelInfo, FocusPreferences, ThemeMode
│   │   │   │   │   ├── network/                # Resilient YouTubeApiClient
│   │   │   │   │   └── repository/             # VideoRepository, FilterManager, LocalStorage
│   │   │   │   ├── playback/
│   │   │   │   │   └── FocusTubeMediaService.kt# AndroidX Media3 Background Service
│   │   │   │   └── ui/
│   │   │   │       ├── components/             # VideoCard, CommonViews
│   │   │   │       ├── navigation/             # FocusBottomBar, NavRoutes
│   │   │   │       ├── screens/                # Discover, Search, Following, Library, Focus, Reels, Player, Channel
│   │   │   │       └── theme/                  # Theme (System/Dark/Light), Color, Type
│   │   │   └── res/                            # Adaptive icons, styles, strings
│   │   └── test/java/com/focustube/app/        # Unit tests (FilterManagerTest, ReelsGatingTest, ThemePersistenceTest)
│   └── build.gradle.kts
├── docs/
│   ├── ARCHITECTURE.md                         # Architecture decision record & design
│   ├── REVERSE_ENGINEERING_REPORT.md           # REA analysis of official YouTube APK
│   └── IMPLEMENTATION_STATUS.md                # Checklist of features & validation
├── artifacts/                                  # Built installable APK deliverables
└── README.md
```

---

## 📦 How to Build the APK

### Prerequisites
- JDK 17 (e.g. Temurin or Android Studio JBR)
- Android SDK with Platform 35 and Build-Tools 34.0.0+

### Build Commands
From the project root:

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Release APK
./gradlew assembleRelease

# Output APK locations:
# artifacts/FocusTube-debug.apk
# artifacts/FocusTube-release.apk
```

---

## 🔒 Privacy & Permissions Comparison

| Permission / Tracker | Official YouTube APK (`v21.42.244`) | FocusTube |
| :--- | :---: | :---: |
| `android.permission.INTERNET` | Yes | **Yes** |
| `android.permission.ACCESS_NETWORK_STATE` | Yes | **Yes** |
| `android.permission.FOREGROUND_SERVICE` | Yes | **Yes** (Media playback only) |
| `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Yes | **Yes** |
| `android.permission.WAKE_LOCK` | Yes | **Yes** (Audio & screen awake) |
| `android.permission.POST_NOTIFICATIONS` | Yes | **Yes** (Lockscreen media controls) |
| `com.google.android.gms.permission.AD_ID` | Yes | **Removed** |
| `android.permission.ACCESS_ADSERVICES_ATTRIBUTION` | Yes | **Removed** |
| `android.permission.READ_PHONE_STATE` | Yes | **Removed** |
| `android.permission.ACCESS_FINE_LOCATION` | Yes | **Removed** |
| `android.permission.RECORD_AUDIO` | Yes | **Removed** |
| `android.permission.CAMERA` | Yes | **Removed** |
| Total Permissions | **51** | **6** |

---

## 📜 License & Ethics
FocusTube is an independent educational client built to encourage digital wellbeing. It does not bypass DRM, steal credentials, or compromise access controls. All video playback uses YouTube's standard embedded iframe architecture.
