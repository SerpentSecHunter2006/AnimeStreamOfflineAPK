# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep JavascriptInterface for WebView bridge
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keep public class com.example.serpentanimestream.AndroidBridge { *; }

# Keep Compose and basic Android stuff working
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Obfuscate code to prevent simple decompilation, but keep entry points
-keepclasseswithmembers class * {
    public <init>(...);
}
