-keepclassmembers class * extends android.webkit.WebViewClient { *; }
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}

-keep class com.fruitsplash.fruitsplashgame.nectar.** { *; }
-keep class com.fruitsplash.fruitsplashgame.nectar.box.NectarPushService { *; }
-keep class com.fruitsplash.fruitsplashgame.SplashActivity { *; }
-keep class com.fruitsplash.fruitsplashgame.MainActivity { *; }
-keep class com.fruitsplash.fruitsplashgame.WebPageActivity { *; }

-keep class com.fruitsplash.fruitsplashgame.nectar.kind.** { *; }
-keepclassmembers class com.fruitsplash.fruitsplashgame.nectar.kind.** {
    public static ** serializer(...);
}

-keep class com.appsflyer.** { *; }
-dontwarn com.appsflyer.**
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.**
