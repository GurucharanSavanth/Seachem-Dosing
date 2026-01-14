# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Keep data classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Material Design
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Navigation
-keepnames class androidx.navigation.fragment.NavHostFragment

# App specific - keep calculation logic
-keep class com.example.seachem_dosing.logic.** { *; }
-keep class com.example.seachem_dosing.ui.MainViewModel { *; }
-keep class com.example.seachem_dosing.ui.MainViewModel$** { *; }