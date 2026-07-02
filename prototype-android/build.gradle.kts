plugins {
    id("com.android.application") version "8.6.0" apply false
    id("com.android.test") version "8.6.0" apply false
    id("androidx.baselineprofile") version "1.4.1" apply false
    // Kotlin 1.9.24 pairs with Compose compiler 1.5.14 (Compose BOM 2024.10.01 line).
    // See https://developer.android.com/jetpack/androidx/releases/compose-kotlin.
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

fun deviceGatesCommand(enforce: Boolean): List<String> {
    val powershell = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        "powershell"
    } else {
        "pwsh"
    }
    val command = mutableListOf(
        powershell,
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        rootProject.layout.projectDirectory.file("tools/device-gates.ps1").asFile.absolutePath,
    )
    if (enforce) {
        command += "-Enforce"
    }
    return command
}

tasks.register("deviceGates") {
    group = "verification"
    description = "Builds the debug APK and records local adb screenshots/performance gate data."
    dependsOn(":app:assembleDebug")

    doLast {
        exec {
            commandLine(deviceGatesCommand(enforce = false))
        }
    }
}

tasks.register("deviceGatesEnforced") {
    group = "verification"
    description = "Runs deviceGates and exits non-zero when ROADMAP.md thresholds are missed."
    dependsOn(":app:assembleDebug")

    doLast {
        exec {
            commandLine(deviceGatesCommand(enforce = true))
        }
    }
}
