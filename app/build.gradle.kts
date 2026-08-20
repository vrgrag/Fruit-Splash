import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

if (rootProject.file("app/google-services.json").exists()) {
    pluginManager.apply("com.google.gms.google-services")
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}

val pulpProps = Properties().apply {
    val f = rootProject.file("pulp.properties")
    if (f.exists()) load(f.inputStream())
}

fun pulpString(name: String): String = pulpProps.getProperty(name).orEmpty()

fun quoted(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

fun pulpBoolean(name: String, defaultValue: Boolean): Boolean =
    pulpProps.getProperty(name)?.toBooleanStrictOrNull() ?: defaultValue

android {
    namespace = "com.fruitsplash.fruitsplashgame"
    compileSdk = 36
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.fruitsplash.fruitsplashgame"
        minSdk = 30
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PROBE_LINK", quoted(pulpString("probeLink")))
        buildConfigField("String", "FORCE_AF_STATUS", quoted(pulpString("forceAfStatus")))
        buildConfigField("String", "FORCE_PARAMS", quoted(pulpString("forceParams")))
        buildConfigField("Boolean", "STICKY_VERDICT", pulpBoolean("stickyVerdict", true).toString())
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps.getProperty("storeFile", "fruitsplash.jks"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    testOptions { unitTests.isReturnDefaultValues = true }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

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
    coreLibraryDesugaring(libs.desugar.jdk)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.webkit)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.appsflyer)
    implementation(libs.installreferrer)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.okhttp)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation("junit:junit:4.13.2")
}
