# One UI Home Clone — ProGuard / R8 rules
# Compose + AndroidX ship their own consumer rules; no blanket -keep needed.

# Application subclass — AndroidManifest references by class name
-keep public class com.oneuihomeclone.LauncherApp

# Activity — registered in AndroidManifest intent filters
-keep public class com.oneuihomeclone.MainActivity

# ActivityResultContract — framework instantiates via class reference
-keep class com.oneuihomeclone.widgets.WidgetBindContract { *; }
-keep class com.oneuihomeclone.widgets.WidgetBindRequest { *; }
-keep class com.oneuihomeclone.widgets.WidgetBindResult { *; }
-keep class com.oneuihomeclone.widgets.WidgetBindResult$* { *; }

# AppWidgetHost — framework callbacks arrive via IPC
-keep class * extends android.appwidget.AppWidgetHost { *; }

# BoundWidget fields are serialized to JSON via org.json — names must survive
-keepclassmembers class com.oneuihomeclone.data.BoundWidget {
    <fields>;
}

# Kotlin enum entries accessor + valueOf used by fromRaw() deserialization
-keepclassmembers enum com.oneuihomeclone.data.** {
    **[] values();
    public static ** valueOf(java.lang.String);
}

# Suppress warnings from coroutines debug agent and annotation retention
-dontwarn kotlinx.coroutines.debug.**
-dontwarn org.jetbrains.annotations.**
