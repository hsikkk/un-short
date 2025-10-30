# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Accessibility Service
-keep class com.muuu.unshort.ShortsBlockService { *; }
-keep class * extends android.accessibilityservice.AccessibilityService

# Keep all Activities
-keep class * extends androidx.appcompat.app.AppCompatActivity
-keep class com.muuu.unshort.MainActivity { *; }
-keep class com.muuu.unshort.OnboardingActivity { *; }
-keep class com.muuu.unshort.SettingsActivity { *; }
-keep class com.muuu.unshort.PermissionSetupActivity { *; }
-keep class com.muuu.unshort.TimerActivity { *; }

# Keep BroadcastReceiver
-keep class com.muuu.unshort.AppRestartReceiver { *; }
-keep class * extends android.content.BroadcastReceiver

# Keep ViewBinding classes
-keep class com.muuu.unshort.databinding.** { *; }

# Keep ViewPager2 adapter
-keep class com.muuu.unshort.OnboardingAdapter { *; }

# Keep utility classes that may be called via reflection
-keep class com.muuu.unshort.SessionManager { *; }
-keep class com.muuu.unshort.AppUtils { *; }

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Material Design
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Remove logs in release build
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
