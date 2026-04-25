# Example app specific R8/Proguard rules

# Keep all Compass MVI implementations in the example app to avoid over-shrinking of state models
-keep class com.usmonie.compass.example.** implements com.usmonie.compass.state.State { *; }
-keep class com.usmonie.compass.example.** implements com.usmonie.compass.state.Action { *; }
-keep class com.usmonie.compass.example.** implements com.usmonie.compass.state.Event { *; }
-keep class com.usmonie.compass.example.** implements com.usmonie.compass.state.Effect { *; }

# Keep Serializable classes if they are used for screen parameters or state persistence
-keep @kotlinx.serialization.Serializable class com.usmonie.compass.example.** { *; }

# General Compose rules (usually handled by Compose Gradle plugin, but added here for safety)
-keep class androidx.compose.ui.platform.** { *; }
-keep public class * extends androidx.activity.ComponentActivity { *; }

# Ignore missing optional window extensions
-dontwarn androidx.window.**
