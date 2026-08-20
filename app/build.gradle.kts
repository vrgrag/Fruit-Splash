import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

if (rootProject.file("app/google-services.json").exists()) {
    pluginManager.apply("com.google.gms.google-services")
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.fruitsplash.fruitsplashgame"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fruitsplash.fruitsplashgame"
        minSdk = 30
        targetSdk = 35
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

    buildFeatures {
        buildConfig = true
    }

    testOptions { unitTests.isReturnDefaultValues = true }

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
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("com.appsflyer:af-android-sdk:6.16.2")
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    testImplementation("junit:junit:4.13.2")
}
