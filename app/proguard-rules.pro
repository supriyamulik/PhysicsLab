# Add project specific ProGuard rules here.

# Room persistence
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.physicslab.app.data.** { *; }
-dontwarn androidx.room.paging.**

# Keep ViewBinding and Fragments
-keep class * extends androidx.fragment.app.Fragment {
    public <init>();
}
-keep class * implements androidx.viewbinding.ViewBinding {
    public static * inflate(...);
    public static * bind(...);
}

# Keep domain, sensor, and UI data classes / state classes
-keep class com.physicslab.app.domain.** { *; }
-keep class com.physicslab.app.sensor.** { *; }
-keep class com.physicslab.app.ui.** { *; }

# Preserve attributes for Kotlin reflection and coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
