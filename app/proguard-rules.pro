# Game WebView (privacy / support).
-keepclassmembers class * extends android.webkit.WebViewClient { *; }

# Gray-flow entries referenced from the manifest / Firebase.
-keep class com.fruitsplash.fruitsplashgame.zest.ZestApp { *; }
-keep class com.fruitsplash.fruitsplashgame.zest.ZestGateActivity { *; }
-keep class com.fruitsplash.fruitsplashgame.zest.ZestShellActivity { *; }
-keep class com.fruitsplash.fruitsplashgame.zest.ZestStillActivity { *; }
-keep class com.fruitsplash.fruitsplashgame.zest.ZestPermitActivity { *; }
-keep class com.fruitsplash.fruitsplashgame.zest.vine.ZestDropService { *; }
-keep class com.fruitsplash.fruitsplashgame.WebPageActivity { *; }
-keep class com.fruitsplash.fruitsplashgame.MainActivity { *; }
-keep class com.fruitsplash.fruitsplashgame.SplashActivity { *; }

-keep class com.appsflyer.** { *; }
-dontwarn com.appsflyer.**
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.**
-dontwarn okhttp3.**
-dontwarn okio.**
