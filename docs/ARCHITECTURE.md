# FocusTube Architecture Decision Record & System Design

## 1. Executive Overview

FocusTube is a high-performance, native Android application engineered in **Kotlin 2.0** with **Jetpack Compose**, designed to provide an intentional, distraction-free video viewing experience. It takes architectural inspiration from empirical reverse engineering of YouTube APK `v21.42.244`, while deliberately eliminating algorithmic attention traps, autoplay loops, and invasive tracking.

---

## 2. Layered Architecture

FocusTube follows the official Android Architecture guidelines with clear separation of concerns across presentation, domain/business logic, playback, and data layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  (Jetpack Compose UI, Material 3, Navigation, ViewModels)   │
│  - DiscoverScreen  - SearchScreen    - FollowingScreen      │
│  - LibraryScreen   - FocusSettings   - ReelsScreen (Gated)  │
│  - PlayerScreen (Gesture Fullscreen, Speeds up to 3×)       │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow / Coroutines
┌──────────────────────────────▼──────────────────────────────┐
│                    Domain & Business Logic                  │
│  - FilterManager: Strict Shorts detection & sanitization    │
│  - Reels Gating & Feature Controller                        │
│  - Gesture & Fullscreen State Coordinator                   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Playback Subsystem                       │
│  - Hybrid Video Player: Embedded Privacy IFrame + JS API    │
│  - AndroidX Media3 FocusTubeMediaService (MediaSession)     │
│  - Audio Focus, Background Service & Lockscreen Controls    │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Data & Persistence Layer                 │
│  - VideoRepository: Aggregates & filters video metadata     │
│  - YouTubeApiClient: Resilient multi-instance Invidious API │
│  - LocalStorageRepository: Local Subscriptions, Watch Later,│
│    Playback Resume Positions, Watch History, Preferences    │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Key Architectural Decisions

### ADR-01: Navigation Pipeline & Reels Gating
* **Context:** The product requires 5 main navigation destinations (**Discover**, **Search**, **Following**, **Library**, **Focus**) and an optional, isolated **Reels** destination.
* **Decision:**
  - The default configuration sets `reelsEnabled = false`.
  - When disabled, the Reels destination is omitted from `FocusBottomBar`, no Reels feeds or shelves appear in Discover or Search, and navigation attempts to `/reels` are rejected with a focus explanation and redirect.
  - When explicitly enabled by the user in Focus Settings, the Reels tab appears dynamically, providing a manual, non-autoplay short-form browsing sandbox.

### ADR-02: Playback Engine & Background Audio
* **Context:** Video playback requires standard controls, speed up to 3×, gesture-controlled fullscreen, and background audio playback across device lock/sleep.
* **Decision:**
  - **Video Surface:** Implemented using YouTube's privacy-enhanced embed (`youtube-nocookie.com/embed/{id}`) via an accelerated Android `WebView`. Configured with `rel=0` and `modestbranding=1` to suppress third-party viral recommendation grids.
  - **Background Audio:** Handled by a dedicated AndroidX Media3 `MediaSessionService` (`FocusTubeMediaService`). When background playback is enabled, a foreground service maintains audio playback, lock-screen controls, and system media session metadata when the device is locked or minimized.
  - **Speed Control:** Supports 12 granular playback speeds: `0.25×, 0.5×, 0.75×, 1.0×, 1.25×, 1.5×, 1.75×, 2.0×, 2.25×, 2.5×, 2.75×, 3.0×`. Speeds are dynamically dispatched to the player via bidirectional JavaScript/API bridges.

### ADR-03: Fullscreen Gestures
* **Context:** Users need intuitive transitions between portrait and landscape fullscreen without accidental seeks.
* **Decision:**
  - Gestures are captured via Compose pointer drag detection on the player surface:
    - **Swipe Up in Portrait:** Detects upward vertical drag (`dragAmount.y < -80f` with vertical vector dominance `abs(y) > abs(x) * 1.3`). Requests landscape orientation.
    - **Swipe Down in Fullscreen:** Detects downward vertical drag (`dragAmount.y > 80f`). Restores portrait orientation.
  - Horizontal gestures are reserved for scrubbing and double-tap seeking (+10s / -10s), ensuring zero collision.

### ADR-04: Shorts Detection & Distraction Stripping
* **Context:** Algorithmic Shorts must be eliminated from discovery and search.
* **Decision:**
  - Implemented in `FilterManager`:
    1. Analyzes URL patterns matching `/(?:youtube\.com|youtu\.be)/shorts/{id}`.
    2. Scans video titles and metadata for `#shorts` and `#reel` hashtags.
    3. Flags vertical aspect ratios and durations <= 60 seconds with short-form indicators.
    4. Strips view counts and publication dates if the user enables "Hide Metrics" to eliminate clickbait triggers.

### ADR-05: Local Storage & Data Privacy
* **Context:** Features like Following (subscriptions), Watch History, and Watch Later must work without requiring a Google Account login or transmitting telemetry.
* **Decision:**
  - Subscriptions, saved videos, playback positions, and history are persisted locally on-device using encrypted/private `SharedPreferences` and JSON serialization.
  - Users have explicit control to Pause History or Clear All History at any time.

### ADR-06: Dynamic Theming
* **Context:** Support Light, Dark, and Follow System modes.
* **Decision:**
  - `ThemeMode` (`SYSTEM`, `DARK`, `LIGHT`) is persisted in preferences.
  - `FocusTubeTheme` reacts to the active mode in realtime, configuring Material 3 color schemes, status bars, and navigation bars with zero restart required.

---

## 4. Security & Permissions Footprint

| Category | Target YouTube APK (`v21.42.244`) | FocusTube |
| :--- | :---: | :---: |
| Advertising SDKs | Google AdServices, AdMob, DoubleClick | **None** |
| Tracking Permissions | `AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `READ_PHONE_STATE` | **None** |
| Hardware Access | Camera, Microphone, Fine/Coarse Location | **None** |
| Permissions Declared | 51 permissions | **6 permissions** (`INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`, `POST_NOTIFICATIONS`) |
