# FocusTube Proguard Rules for Release Build

# Preserve annotations and type signatures
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Gson models and serialization fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keep class com.focustube.app.data.model.** { *; }
-keepclassmembers class com.focustube.app.data.model.** { *; }

# Keep WebKit JavaScriptInterface for embedded player bridge
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.focustube.app.ui.screens.player.** {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep OkHttp & Okio internals
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep AndroidX Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Kotlin Coroutines & Flow
-keep class kotlinx.coroutines.** { *; }
