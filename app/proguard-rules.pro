# Keep JsBridge methods accessible from JavaScript
-keepclassmembers class com.nova.ai.bridge.JsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep data classes for JSON serialization
-keep class com.nova.ai.ai.** { *; }
-keep class com.nova.ai.utils.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
