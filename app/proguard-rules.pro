# youtubedl-android uses reflection/JNI and must survive R8 minification.
# Proven by the releaseTest build on a physical device (arm64-v8a).
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# Suppress R8 warnings for classes needed by yt-dlp dependencies
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn org.tukaani.xz.**
-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry

# Keep Jackson and Apache Commons Compression classes needed by yt-dlp
-keep class java.beans.** { *; }
-keep class org.tukaani.xz.** { *; }
-keep class org.apache.commons.compress.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class com.fasterxml.jackson.databind.** { *; }
-keep class com.fasterxml.jackson.databind.ext.** { *; }
-keep class com.fasterxml.jackson.core.** { *; }
