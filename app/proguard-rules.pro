-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Chaquopy Python runtime
-keep class com.chaquo.python.** { *; }

# Room database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Security crypto
-keep class androidx.security.crypto.** { *; }

# Serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **.*$serializer { *; }

# Timber
-dontwarn org.jetbrains.annotations.**
