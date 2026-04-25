# Keep all Serializable classes for state management
-keep @kotlinx.serialization.Serializable class * { *; }

# Keep Compass MVI components as they define the core architecture
-keep interface com.usmonie.compass.state.State { *; }
-keep interface com.usmonie.compass.state.Action { *; }
-keep interface com.usmonie.compass.state.Event { *; }
-keep interface com.usmonie.compass.state.Effect { *; }

# Keep helper state classes
-keep public class com.usmonie.compass.state.ContentState { *; }
-keep public class com.usmonie.compass.state.ErrorState { *; }
-keep public class com.usmonie.compass.state.ThrowableErrorState { *; }
