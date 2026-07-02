package com.oneuihomeclone.ui

internal enum class SeslInteropCost {
    LOW,
    MEDIUM,
    HIGH,
}

internal enum class SeslInteropDecision {
    KEEP_COMPOSE,
    ADOPT_SESL,
}

internal data class SeslInteropCandidate(
    val controlName: String,
    val composeImplementation: String,
    val seslComponent: String,
    val parityGaps: List<String>,
    val binaryCost: SeslInteropCost,
    val dependencyCost: SeslInteropCost,
    val lifecycleRisk: SeslInteropCost,
    val fidelityGain: SeslInteropCost,
    val requiresAuthenticatedPackageRegistry: Boolean,
    val needsAndroidViewBridge: Boolean,
    val decision: SeslInteropDecision,
    val evidence: List<String>,
)

internal fun selectedSeslInteropAssessment(): SeslInteropCandidate =
    SeslInteropCandidate(
        controlName = "Home screen settings toggle rows",
        composeImplementation = "SettingsToggleCard",
        seslComponent = "dev.oneuiproject.oneui.preference.SwitchBarPreference",
        parityGaps = listOf(
            "Compose row already supports title, summary, row-level switch semantics, and resource-backed copy.",
            "SESL would mainly improve exact Samsung preference chrome, not launcher behavior or recovery safety.",
        ),
        binaryCost = SeslInteropCost.HIGH,
        dependencyCost = SeslInteropCost.HIGH,
        lifecycleRisk = SeslInteropCost.HIGH,
        fidelityGain = SeslInteropCost.MEDIUM,
        requiresAuthenticatedPackageRegistry = true,
        needsAndroidViewBridge = true,
        decision = SeslInteropDecision.KEEP_COMPOSE,
        evidence = listOf(
            "prototype-android/app/src/main/java/com/oneuihomeclone/ui/SharedComponents.kt#SettingsToggleCard",
            "ROADMAP.md Known Pitfalls: Compose AndroidView wrapping SeslSwitchBar leaks Lifecycle if bound to Activity lifecycle.",
            "https://github.com/tribalfs/oneui-design/blob/main/lib/src/main/java/dev/oneuiproject/oneui/preference/SwitchBarPreference.kt",
            "https://github.com/tribalfs/oneui-design#usage",
            "https://github.com/tribalfs/sesl-material-components-android/packages/2110054",
        ),
    )

internal fun SeslInteropCandidate.shouldAdoptSesl(): Boolean =
    decision == SeslInteropDecision.ADOPT_SESL &&
        fidelityGain == SeslInteropCost.HIGH &&
        binaryCost != SeslInteropCost.HIGH &&
        dependencyCost != SeslInteropCost.HIGH &&
        lifecycleRisk != SeslInteropCost.HIGH &&
        !requiresAuthenticatedPackageRegistry
