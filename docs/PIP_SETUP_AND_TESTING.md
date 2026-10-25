# FocusTube: Picture-in-Picture (PiP) Architecture & Testing Guide

## 1. Executive Summary

FocusTube provides native Picture-in-Picture (PiP) support on Android 12 through Android 15 (API 31–35). This document details the Android manifest configuration, Activity-level PiP parameters, auto-enter behavior, aspect ratio adaptation, and physical device/emulator testing results.

---

## 2. Manifest & System Configuration

To allow an Activity to transition into a floating PiP window without being recreated by configuration changes, the following entries are specified in `app/src/main/AndroidManifest.xml`:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:supportsPictureInPicture="true"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
    android:resizeableActivity="true"
    android:theme="@style/Theme.FocusTube">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### Key Flags Explained:
- `android:supportsPictureInPicture="true"`: Informs Android OS that `MainActivity` is eligible for PiP mode.
- `android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"`: Prevents Activity recreation when transitioning into or out of PiP mode, ensuring video playback does not stutter or reload.
- `android:resizeableActivity="true"`: Ensures multi-window and PiP mode compatibility across tablet, foldables, and desktop/DeX modes.

---

## 3. Implementation Details

### A. Dynamic PiP Parameters in `MainActivity.kt`
On Android 8.0+ (API 26), PiP parameters are passed via `PictureInPictureParams`. On Android 12+ (API 31+), `setAutoEnterEnabled(true)` allows the system to enter PiP automatically when the user swipes home.

```kotlin
fun updatePipParams(enabled: Boolean, isReel: Boolean = false) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val aspectRatio = if (isReel) Rational(9, 16) else Rational(16, 9)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(enabled)
            builder.setSeamlessResizeEnabled(true)
        }
        setPictureInPictureParams(builder.build())
    }
}
```

### B. The `autoEnterEnabled` Lifecycle Conflict (API 31+)
During testing on Android 15 (API 35), invoking `enterPictureInPictureMode()` inside `onUserLeaveHint()` while `setAutoEnterEnabled(true)` was active triggered the following system warning:
```
ActivityTaskManager: Skip client enterPictureInPictureMode request while pausing, auto-enter-pip is enabled
```
**Resolution**:
- When `autoEnterEnabled(true)` is registered, the OS automatically handles the transition when the user navigates Home. Calling `enterPictureInPictureMode()` manually inside `onUserLeaveHint` is redundant and blocked by the OS.
- For manual user interaction within the app, FocusTube provides a dedicated PiP button in the top toolbar of `PlayerScreen.kt` that directly invokes `enterPipMode()`.

### C. Dedicated Toolbar PiP Button
In `PlayerScreen.kt`, the top toolbar features a one-touch PiP button:
```kotlin
IconButton(
    onClick = { onEnterPip() },
    modifier = Modifier.size(40.dp)
) {
    Icon(
        imageVector = Icons.Default.PictureInPictureAlt,
        contentDescription = "Enter Picture in Picture",
        tint = Color.White
    )
}
```

---

## 4. Testing & Verification

### A. Manual Toolbar PiP Trigger
1. Launch FocusTube on Android 15 emulator (`emulator-5554`).
2. Open a video from the Discover feed (e.g., "Jerry The Dog [FULL MOVIE]").
3. Autoplay begins in the 16:9 player.
4. Tap the **PiP button** in the top-right toolbar.
5. **Observed Result**:
   - The Activity instantly transitions into a floating 16:9 PiP window pinned to the corner of the screen.
   - The user can browse the Discover feed or Settings screen while video and audio continue uninterrupted.
   - Verified via screenshot [`focustube_v5_pip_result.png`](file:///C:/dev/Zeztube/artifacts/focustube_v5_pip_result.png) and [`focustube_v5_settings_screen.png`](file:///C:/dev/Zeztube/artifacts/focustube_v5_settings_screen.png).

### B. Swipe Home Auto-Enter PiP
1. While video is playing on the Watch screen, swipe up from the navigation bar (or press HOME).
2. **Observed Result**:
   - Smooth animation shrinks the video player directly into the floating PiP window on the Android home screen.
   - No audio clipping or reloading.

### C. Restoring from PiP
1. Tap the floating PiP window.
2. Tap the expand / full-window icon in the center of the PiP overlay.
3. **Observed Result**:
   - App smoothly expands back to the full-screen Watch page with player state, playback position, and UI controls intact.

---

## 5. UI Settings & Capability Reporting

In the **Settings > Playback & Background** screen:
- **Picture-in-Picture Toggle**: Enables or disables PiP capability.
- **Capability Chip**: Displays real-time status: `"Capability: Supported (Android 15)"`.
- **PiP Settings Button**: Deep links directly to Android system's PiP permission settings (`android.settings.PICTURE_IN_PICTURE_SETTINGS`), allowing the user to grant or verify "Allow picture-in-picture" permission.
