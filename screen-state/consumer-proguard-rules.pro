# Keep ScreenId and its subclasses as they are used for navigation and often serialized
-keep public class com.usmonie.compass.screen.state.navigation.ScreenId { *; }
-keep public class * extends com.usmonie.compass.screen.state.navigation.ScreenId { *; }

# Keep all Serializable classes for navigation parameters
-keep @kotlinx.serialization.Serializable class * { *; }
