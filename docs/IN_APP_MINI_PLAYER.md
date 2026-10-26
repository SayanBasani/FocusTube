# FocusTube In-App Mini-Player Architecture & Verification

## 1. Overview & Problem Definition

In FocusTube V7 and prior versions, when a user opened a video watch screen and then navigated away (pressing back or switching tabs such as Discover, Search, Following, Library, or Focus), the watch screen was removed from the composition hierarchy. This resulted in:
1. Playback abruptly terminating during in-app navigation.
2. The user losing their place in long-form lectures and educational discussions.
3. While system PiP (floating window outside the app) worked on Android 15, there was no native in-app docking mechanism.

FocusTube V8 introduces a unified **In-App Mini-Player** architecture that seamlessly transitions active video playback between the **Full Watch Page** and a docked **Mini-Player Bar** directly above the bottom navigation bar.

---

## 2. Core Architectural Pillars

### A. The Single-Player Invariant via `movableContentOf`
A major hazard when creating mini-players in Jetpack Compose is instantiating a second player or WebView widget when transitioning to the mini-player layout. Doing so destroys the existing video state, reloads the video stream, introduces double-audio "ghost players", and spikes network bandwidth.

To guarantee that exactly **one** native `AndroidView` (WebView player) exists across the entire app lifecycle, FocusTube encapsulates the player inside Compose's `movableContentOf`:

```kotlin
// PlayerScreen.kt
val sharedPlayerView = remember(video.id) {
    movableContentOf { modifier: Modifier ->
        UnifiedPlayerHost(
            video = video,
            controller = playerController,
            state = playerState,
            modifier = modifier
        )
    }
}
```

When switching between `PlayerSheetState.EXPANDED` and `PlayerSheetState.COLLAPSED`:
- The composable tree node moves its location without triggering a disposal/re-instantiation lifecycle.
- The underlying `FocusPlayerWebView` instance remains attached to its active DOM / JavaScript session.
- Playback position, buffer state, and audio continue completely uninterrupted.

### B. Two-Level State Machine: Sheet State & Nav Host State
Playback state is held at the root level of `MainActivity.kt`:
- **`activeVideo: VideoItem?`**: Current video being played or docked. `null` when dismissed.
- **`playerSheetState: PlayerSheetState`**:
  - `EXPANDED`: Full Watch Page fills the screen above the status bar.
  - `COLLAPSED`: Video docks into a 64 dp `MiniPlayerBar` above the bottom navigation bar.
  - `HIDDEN`: Player is closed and removed from UI.

### C. System PiP vs. In-App Mini-Player Coexistence
- **In-App Mini-Player**: Active while navigating inside the app (`PlayerSheetState.COLLAPSED`).
- **System PiP**: Automatically triggered when the user presses Home or leaves FocusTube while video is actively playing (`isInPictureInPictureMode`).
- Both states share the identical `PlayerController` and `MediaSessionService` token.

---

## 3. UI & Ergonomics

### Mini-Player Specifications
- **Height**: 64 dp fixed bar with subtle elevation and border styling matching the app theme.
- **Preview**: 16:9 aspect ratio thumbnail/video surface occupying the left edge.
- **Metadata**: Video title (truncated with ellipsis, 1 line) and channel name (1 line).
- **Interactive Controls**:
  - **Re-expand**: Tap anywhere on the preview or metadata to return smoothly to the Full Watch Page.
  - **Play / Pause**: One-touch toggle reflecting real player state with appropriate accessible content descriptions.
  - **Dismiss / Close**: One-touch dismiss button (`x`) that stops playback, releases the WebView, and removes the mini-player bar.
  - **Progress Line**: 2 dp slim red played-progress track at the bottom edge.

### Layout Padding & Insets
To prevent the docked mini-player from obscuring list items, search inputs, or the bottom navigation bar:
```kotlin
val bottomContentPadding = if (activeVideo != null && playerSheetState == PlayerSheetState.COLLAPSED) {
    68.dp // Extra padding to offset MiniPlayerBar height + margin
} else {
    0.dp
}
```
All scrollable feeds (`HomeScreen`, `SearchScreen`, `FollowingScreen`, `LibraryScreen`, `SettingsScreen`) receive `contentPadding` accommodating both the system navigation bar and the active mini-player.

---

## 4. Empirical Emulator Verification

| Test Scenario | Action | Observed Result | Verdict |
|---|---|---|---|
| Open Video Watch Page | Tap Duolingo lecture from Discover | Full watch page opens, title and channel load, video plays at 0:00/12:54 | **PASS** |
| Collapse to Mini-Player | Press Back key | Watch page collapses into bottom `MiniPlayerBar` docked above tabs | **PASS** |
| Discover Tab Navigation | Scroll Discover feed while mini-player active | Discover scrolls smoothly; Mini-Player stays fixed above bottom bar | **PASS** |
| Tab Switching | Tap Search tab | Search screen opens; Mini-Player persists with "Pause" & "Close" | **PASS** |
| Tab Switching | Tap Library tab | Library screen opens; Mini-Player continues audio & control sync | **PASS** |
| Re-expansion | Tap Mini-Player preview/title | Watch page expands immediately back to full screen; no reload | **PASS** |
| Dismiss Player | Tap 'x' on Mini-Player | Player closes; Mini-Player disappears; UI padding resets cleanly | **PASS** |

### Verified Artifacts
- Full Watch Page: `focustube_v8_watch.png`
- Mini-Player on Discover: `focustube_v8_mini_player_discover.png`
- Mini-Player on Search: `focustube_v8_mini_player_search.png`
- Mini-Player on Library: `focustube_v8_mini_player_library.png`
- Re-expanded Watch Page: `focustube_v8_watch_expanded.png`
