# Proguard rules for Tsuki Reader
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn com.github.junrar.**
-keep class com.github.junrar.** { *; }
