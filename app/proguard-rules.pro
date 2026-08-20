# R8 — Fruit Splash. Keep only manifest entry points; helpers go to package `jx`.

-allowaccessmodification
-repackageclasses 'jx'
-renamesourcefileattribute SourceFile

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Manifest / launcher — class names only. Members obfuscate except SDK overrides.
-keep public class com.fruitsplash.fruitsplashgame.grove.GroveApp
-keep public class com.fruitsplash.fruitsplashgame.grove.HarvestGate
-keep public class com.fruitsplash.fruitsplashgame.grove.CanopyShell
-keep public class com.fruitsplash.fruitsplashgame.grove.TrellisAsk
-keep public class com.fruitsplash.fruitsplashgame.grove.QuietCanopy
-keep public class com.fruitsplash.fruitsplashgame.grove.crate.GroveNoteService
-keep public class com.fruitsplash.fruitsplashgame.SplashActivity
-keep public class com.fruitsplash.fruitsplashgame.MainActivity
-keep public class com.fruitsplash.fruitsplashgame.WebPageActivity

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# kotlinx.serialization — GroveRawReply only, not the whole lane package.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class com.appsflyer.** { *; }
-dontwarn com.appsflyer.**
-keep class com.android.installreferrer.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-keep class androidx.security.crypto.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn kotlinx.**

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
