# ProGuard / R8 Configuration for AdDash Release Builds

# --- General Optimization & Crash Reporting ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions

# Strip debug and verbose log calls in production
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# --- App Data Models & Serialization ---
# Keep only our app models and serializers to allow full R8 optimization everywhere else
-keep class com.solarad.app.model.** { *; }
-keepclassmembers class com.solarad.app.model.** { *; }

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    java.lang.Object deserialize(...);
}
-keepclassmembers class * implements kotlinx.serialization.SerializationStrategy {
    void serialize(...);
}

# Allow R8 to freely shrink and obfuscate library dependencies
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn io.ktor.**
-dontwarn com.russhwolf.settings.**
