# Keep Compose metadata so R8 doesn't strip composable function markers
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep data models used by the prototype without reflection guards
-keep class com.oneuihomeclone.ui.** { *; }

# Standard AndroidX exclusions — avoid warnings from material/compose bundled metadata
-dontwarn kotlinx.coroutines.debug.**
-dontwarn kotlinx.coroutines.flow.**
-dontwarn org.jetbrains.annotations.**

# Keep Activity/Application class names — Android wires them by class-string in intent filters
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
