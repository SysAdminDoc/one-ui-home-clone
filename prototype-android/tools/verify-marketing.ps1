[CmdletBinding()]
param([string]$ArtifactDir = "", [string]$ScreenshotDir = "")

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path
if (-not $ArtifactDir) { $ArtifactDir = Join-Path $repoRoot "prototype-android/app/build/outputs/release-channel" }
if (-not $ScreenshotDir) { $ScreenshotDir = Join-Path $repoRoot "assets/screenshots" }

function Assert-True($Condition, [string]$Message) { if (-not $Condition) { throw $Message } }
function Read-Json([string]$Path) { Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json }
function Get-Digest([string]$Path) { (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant() }

$build = Get-Content -Raw -LiteralPath (Join-Path $repoRoot "prototype-android/app/build.gradle.kts")
Assert-True ($build -match 'val launcherVersionName = "([\d.]+)"') "Launcher version missing."
$version = $Matches[1]
Assert-True ($build -match 'val launcherVersionCode = (\d+)') "Launcher version code missing."
$versionCode = [int]$Matches[1]
$readme = Get-Content -Raw -LiteralPath (Join-Path $repoRoot "README.md")
Assert-True ($readme.Contains("badge/version-$version-")) "README version differs from the build."

$originals = [ordered]@{
    "direction-01-selected-modular-home.png" = "a2f2db3e0eb69a36c0ac3867abad5baf492570247a124830f8f4dff968c512f0"
    "direction-02-modular-home-grid.png" = "6d95ad7b3893424691c0f68d4fe2c5604aad456d08186613fdb844da949d8af3"
    "direction-03-home-grid-container.png" = "aea792d4f1545f8f5083816ea5b3ba0accb2e56455bc9960a6dd30e4924a4918"
    "direction-04-modular-home-light.png" = "2623586dca0231dbea93b15d7d4d0ab4a80d5599a63c9810f7f27a0b20c1102f"
    "direction-05-modular-home-flat.png" = "6653a001eed037c7ba0da3195974b1aacf80b44dc91cfcdf9597603aaa5bd4ac"
}
$conceptDir = Join-Path $repoRoot "assets/brand/concepts"
foreach ($file in $originals.Keys) {
    Assert-True ((Get-Digest (Join-Path $conceptDir $file)) -eq $originals[$file]) "Original concept differs: $file"
}
$selection = Read-Json (Join-Path $conceptDir "selection.json")
Assert-True (($selection.selectedConcepts -join ",") -eq "direction-01-selected-modular-home.png") "Selected direction differs."
Assert-True (($selection.selectedMasters -join ",") -eq "../one-ui-home-selected-master.png") "Selected master path differs."
$masterHash = Get-Digest (Join-Path $repoRoot "assets/brand/one-ui-home-selected-master.png")
Assert-True ($masterHash -eq $originals["direction-01-selected-modular-home.png"]) "Selected master differs from its original."

$linkCount = 0
foreach ($relative in @("README.md", "prototype-android/README.md", "assets/brand/concepts/README.md")) {
    $path = Join-Path $repoRoot $relative
    $content = Get-Content -Raw -LiteralPath $path
    foreach ($link in [regex]::Matches($content, '\]\(([^)]+)\)|(?:src|href)="([^"]+)"')) {
        $target = if ($link.Groups[1].Success) { $link.Groups[1].Value } else { $link.Groups[2].Value }
        if ($target -match '^(?:[a-z]+:|#|//)') { continue }
        $target = [uri]::UnescapeDataString(($target -split '[#?]')[0])
        if (-not $target) { continue }
        Assert-True (Test-Path -LiteralPath (Join-Path (Split-Path $path) $target)) "Broken local link in ${relative}: $target"
        $linkCount++
    }
}

$apkName = "one-ui-home-clone-v$version-release.apk"
$apk = Get-Item -LiteralPath (Join-Path $ArtifactDir $apkName)
$digest = Get-Digest $apk.FullName
$metadata = Read-Json (Join-Path $ArtifactDir "one-ui-home-clone-v$version-release.json")
Assert-True ($metadata.versionName -eq $version -and $metadata.versionCode -eq $versionCode) "Release version differs."
Assert-True ($metadata.applicationId -eq "com.oneuihomeclone") "Release application ID differs."
Assert-True ($metadata.artifact.fileName -eq $apkName -and $metadata.artifact.sha256 -eq $digest -and $metadata.artifact.sizeBytes -eq $apk.Length) "Release metadata differs from the final APK."
$checksum = (Get-Content -Raw -LiteralPath (Join-Path $ArtifactDir "$apkName.sha256")).Trim()
Assert-True ($checksum -ceq "$digest  $apkName") "Checksum sidecar differs from the final APK."

$report = Read-Json (Join-Path $ScreenshotDir "capture-report.json")
Assert-True ($report.schemaVersion -eq 2) "Capture report must include screenshot integrity records."
Assert-True ($report.source.artifact -eq $apkName -and $report.source.sha256 -eq $digest -and $report.source.sizeBytes -eq $apk.Length) "Screenshot source differs from the final APK."
Assert-True ($report.environment.type -eq "isolated Android emulator") "Screenshot environment must be isolated."
Assert-True ($report.environment.resolution -match '^(\d+)x(\d+)$') "Screenshot resolution missing."
$width = [int]$Matches[1]
$height = [int]$Matches[2]
$names = @("apps.png", "edit-mode.png", "finder.png", "home.png", "settings.png", "widgets.png")
Assert-True ((@($report.captures.PSObject.Properties.Value | Sort-Object) -join ",") -eq ($names -join ",")) "Capture list differs from the six expected surfaces."
Assert-True ((@($report.captureIntegrity.PSObject.Properties.Name | Sort-Object) -join ",") -eq ($names -join ",")) "Screenshot integrity list differs."
foreach ($name in $names) {
    $path = Join-Path $ScreenshotDir $name
    Assert-True (Test-Path -LiteralPath $path -PathType Leaf) "Screenshot missing: $name"
    $record = $report.captureIntegrity.$name
    Assert-True ($record.sha256 -eq (Get-Digest $path) -and $record.sizeBytes -eq (Get-Item -LiteralPath $path).Length) "Screenshot integrity differs: $name"
    $bytes = [IO.File]::ReadAllBytes($path)
    Assert-True ($bytes.Length -ge 24 -and [BitConverter]::ToString($bytes, 0, 8) -eq "89-50-4E-47-0D-0A-1A-0A") "Screenshot is not a PNG: $name"
    $imageWidth = [uint32]$bytes[16] * 16777216 + [uint32]$bytes[17] * 65536 + [uint32]$bytes[18] * 256 + $bytes[19]
    $imageHeight = [uint32]$bytes[20] * 16777216 + [uint32]$bytes[21] * 65536 + [uint32]$bytes[22] * 256 + $bytes[23]
    Assert-True ($imageWidth -eq $width -and $imageHeight -eq $height) "Screenshot dimensions differ: $name"
}
[pscustomobject]@{ Version = $version; OriginalConcepts = $originals.Count; LocalLinks = $linkCount; Screenshots = $names.Count; ApkSha256 = $digest; Passed = $true }
