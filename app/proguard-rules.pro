# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK proguard-android.txt file.

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.abhiram.music.** {
    *** Companion;
}
-keepclasseswithmembers class com.abhiram.music.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedesobfuscationclassmembers class com.abhiram.music.**$$serializer { *; }

# Keep the Innertube / InnerTubeX response models (reflection-free but parsed reflectively by R8)
-keep class com.abhiram.innertube.** { *; }
-keep class com.metrolist.innertubex.** { *; }

# Media3
-dontwarn androidx.media3.**
