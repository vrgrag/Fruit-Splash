import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Load signing credentials from keystore.properties (sits next to root build.gradle.kts)
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.fruitsplash.fruitsplashgame"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fruitsplash.fruitsplashgame"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile     = file(keystoreProps.getProperty("storeFile", "fruitsplash.jks"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias      = keystoreProps.getProperty("keyAlias")
            keyPassword   = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled    = true
            isShrinkResources  = true
            signingConfig      = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions { unitTests.isReturnDefaultValues = true }

    // Output APK named fruitsplash-1.0.0.apk
    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                val impl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
                impl.outputFileName = "fruitsplash-${versionName}.apk"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
