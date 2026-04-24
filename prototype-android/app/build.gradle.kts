import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Signing config is opt-in: if keystore.properties exists at repo root or module, wire it up.
// Otherwise only debug builds succeed (useful for CI without release secrets).
val keystorePropsFile: File = rootProject.file("keystore.properties")
val keystoreProps: Properties = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use(::load)
    }
}
val hasReleaseKeystore: Boolean = keystoreProps.getProperty("storeFile")?.isNotBlank() == true

android {
    namespace = "com.oneuihomeclone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.oneuihomeclone"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/licenses/**",
                "META-INF/{AL2.0,LGPL2.1}",
            )
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        // Intentionally staged for Iteration 2 — tracked in ROADMAP v0.2.0 block
        disable += setOf("OldTargetApi", "GradleDependency")
        // Platform convention forces -v26 qualifier on adaptive-icon resources even when
        // minSdk>=26; lint's "obsolete" heuristic doesn't account for this, so ignore.
        disable += setOf("ObsoleteSdkInt")
        // Lint's own bundled custom checks fall out of sync with Compose compiler updates
        disable += setOf("ObsoleteLintCustomCheck")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
