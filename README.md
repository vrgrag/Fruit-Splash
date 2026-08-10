# Fruit Splash

Native Android/Kotlin portrait action-survival game (`com.fruitsplash.fruitsplashgame`).

## Build

Requirements: JDK 17 and Android SDK 36.

```powershell
$env:JAVA_HOME="$env:USERPROFILE\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat bundleRelease
.\gradlew.bat test
```

Outputs:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

No custom keystore is required for local debug builds. Sign the AAB with your Play upload key before publishing.

## Offline behavior

Gameplay, progression, upgrades, missions, collection and settings are fully offline. Network is used only for Privacy Policy and Support WebView pages. Those screens check connectivity first and show a native Retry/Close fallback when offline.

## Controls

Drag on the playfield to move. Collect fruit and crystals, avoid pests and obstacles, use Shield, fill Energy, then fire Fruit Splash. Pause is available during a run.

## Legal URLs

- Privacy Policy: https://fruitsplassh.com/privacy-policy.html
- Support: https://fruitsplassh.com/support.html
