# BOOST MASTER ProGuard / R8 rules

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep class com.boost.your.srt.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.boost.your.srt.**$$serializer { *; }
-keepclassmembers class com.boost.your.srt.** { *** Companion; }
-keepclasseswithmembers class com.boost.your.srt.** { kotlinx.serialization.KSerializer serializer(...); }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Tink (used by security-crypto)
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-keep class com.google.crypto.tink.** { *; }

# Overlay views are instantiated by code, keep for stability
-keep class com.boost.your.srt.overlay.** { *; }
-keep class com.boost.your.srt.service.** { *; }
