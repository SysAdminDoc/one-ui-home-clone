plugins {
    id("com.android.application") version "8.2.2" apply false
    // Kotlin 1.9.24 pairs with Compose compiler 1.5.14 (Compose BOM 2024.10.01 line).
    // See https://developer.android.com/jetpack/androidx/releases/compose-kotlin.
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
