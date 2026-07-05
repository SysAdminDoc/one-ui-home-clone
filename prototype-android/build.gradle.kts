plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.android.test") version "9.2.1" apply false
    id("androidx.baselineprofile") version "1.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
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

tasks.register<org.gradle.api.tasks.Exec>("deviceGates") {
    group = "verification"
    description = "Builds the debug APK and records local adb screenshots/performance gate data."
    dependsOn(":app:assembleDebug")
    commandLine(deviceGatesCommand(enforce = false))
}

tasks.register<org.gradle.api.tasks.Exec>("deviceGatesEnforced") {
    group = "verification"
    description = "Runs deviceGates and exits non-zero when ROADMAP.md thresholds are missed."
    dependsOn(":app:assembleDebug")
    commandLine(deviceGatesCommand(enforce = true))
}
