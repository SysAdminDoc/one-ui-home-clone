import java.security.MessageDigest
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.baselineprofile")
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
val launcherApplicationId = "com.oneuihomeclone"
val launcherVersionCode = 5
val launcherVersionName = "0.2.3"

fun File.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun String.jsonEscaped(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.oneuihomeclone"
    compileSdk = 35

    defaultConfig {
        applicationId = launcherApplicationId
        minSdk = 28
        targetSdk = 35
        versionCode = launcherVersionCode
        versionName = launcherVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
        // Kotlin 1.9.24 pairs with Compose compiler 1.5.14+ per the official map.
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.5.14"
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
        // Platform convention forces -v26 qualifier on adaptive-icon resources even when
        // minSdk>=26; lint's "obsolete" heuristic doesn't account for this, so ignore.
        disable += setOf("ObsoleteSdkInt")
        // Lint's own bundled custom checks fall out of sync with Compose compiler updates
        disable += setOf("ObsoleteLintCustomCheck")
        // en-XA/ar resources are partial locale-stress fixtures, not shipping translations.
        disable += setOf("MissingTranslation")
    }
}

tasks.register("releaseChannelPackage") {
    group = "distribution"
    description = "Builds a signed release APK and writes release-channel metadata with SHA-256."
    dependsOn("assembleRelease")

    val outputDir = layout.buildDirectory.dir("outputs/release-channel")
    val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")
    val channelApkName = "one-ui-home-clone-v$launcherVersionName-release.apk"
    val channelApk = outputDir.map { it.file(channelApkName) }
    val metadataFile = outputDir.map { it.file("one-ui-home-clone-v$launcherVersionName-release.json") }
    outputs.files(channelApk, metadataFile)

    doLast {
        check(hasReleaseKeystore) {
            "releaseChannelPackage requires prototype-android/keystore.properties with a release keystore."
        }

        val sourceApk = releaseApk.get().asFile
        check(sourceApk.isFile) { "Release APK not found: ${sourceApk.absolutePath}" }

        val destinationDir = outputDir.get().asFile
        destinationDir.mkdirs()
        val packagedApk = channelApk.get().asFile
        sourceApk.copyTo(packagedApk, overwrite = true)

        val releaseStoreFile = rootProject.file(keystoreProps.getProperty("storeFile"))
        val metadata = """
            {
              "schemaVersion": 1,
              "applicationId": "$launcherApplicationId",
              "versionName": "$launcherVersionName",
              "versionCode": $launcherVersionCode,
              "minSdk": ${android.defaultConfig.minSdk ?: 0},
              "targetSdk": ${android.defaultConfig.targetSdk ?: 0},
              "compileSdk": ${android.compileSdk ?: 0},
              "artifact": {
                "type": "apk",
                "fileName": "${packagedApk.name.jsonEscaped()}",
                "sha256": "${packagedApk.sha256Hex()}",
                "sizeBytes": ${packagedApk.length()},
                "path": "${packagedApk.absolutePath.jsonEscaped()}"
              },
              "signing": {
                "scheme": "release",
                "keystoreConfigured": true,
                "storeFile": "${releaseStoreFile.name.jsonEscaped()}",
                "keyAlias": "${keystoreProps.getProperty("keyAlias", "").jsonEscaped()}"
              },
              "upgradeInstall": "adb install -r ${packagedApk.name.jsonEscaped()}"
            }
        """.trimIndent()
        metadataFile.get().asFile.writeText(metadata)
        logger.lifecycle("Release-channel APK: ${packagedApk.absolutePath}")
        logger.lifecycle("Release-channel metadata: ${metadataFile.get().asFile.absolutePath}")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // DataStore — typed async persistence for launcher toggles + widget IDs (v0.2.0)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    baselineProfile(project(":baselineprofile"))
}
