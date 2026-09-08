[CmdletBinding()]
param()
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "../..")).Path
$verify = Join-Path $PSScriptRoot "verify-marketing.ps1"
$baseline = & $verify
$artifactDir = Join-Path $repoRoot "prototype-android/app/build/outputs/release-channel"
$screenshotDir = Join-Path $repoRoot "assets/screenshots"
$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$fixtureRoot = Join-Path $tempBase "OneUiHome-MarketingTests-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
function Write-FixtureJson($Value, [string]$Path) {
    [IO.File]::WriteAllText($Path, ($Value | ConvertTo-Json -Depth 10), [Text.UTF8Encoding]::new($false))
}
try {
    $cases = @(
        @{ Name = "release-metadata"; Expected = "Release metadata differs" },
        @{ Name = "apk-provenance"; Expected = "Screenshot source differs" },
        @{ Name = "image-provenance"; Expected = "Screenshot integrity differs" },
        @{ Name = "missing-capture"; Expected = "Screenshot missing" },
        @{ Name = "checksum"; Expected = "Checksum sidecar differs" }
    )
    foreach ($case in $cases) {
        $caseRoot = Join-Path $fixtureRoot $case.Name
        $artifacts = Join-Path $caseRoot "release"
        $captures = Join-Path $caseRoot "screenshots"
        New-Item -ItemType Directory -Path $artifacts, $captures -Force | Out-Null
        Get-ChildItem -LiteralPath $artifactDir -File | Copy-Item -Destination $artifacts
        Get-ChildItem -LiteralPath $screenshotDir -File | Where-Object { $case.Name -ne "missing-capture" -or $_.Name -ne "settings.png" } | Copy-Item -Destination $captures
        $reportPath = Join-Path $captures "capture-report.json"
        $report = Get-Content -Raw -LiteralPath $reportPath | ConvertFrom-Json
        switch ($case.Name) {
            "release-metadata" {
                $metadataPath = Join-Path $artifacts "one-ui-home-clone-v$($baseline.Version)-release.json"
                $metadata = Get-Content -Raw -LiteralPath $metadataPath | ConvertFrom-Json
                $metadata.artifact.sha256 = "0" * 64
                Write-FixtureJson $metadata $metadataPath
            }
            "apk-provenance" { $report.source.sha256 = "0" * 64; Write-FixtureJson $report $reportPath }
            "image-provenance" { $report.captureIntegrity.'home.png'.sha256 = "0" * 64; Write-FixtureJson $report $reportPath }
            "checksum" { [IO.File]::WriteAllText((Join-Path $artifacts "one-ui-home-clone-v$($baseline.Version)-release.apk.sha256"), "invalid checksum") }
        }
        $rejected = $false
        try { & $verify -ArtifactDir $artifacts -ScreenshotDir $captures | Out-Null }
        catch {
            if ($_.Exception.Message -notmatch [regex]::Escape($case.Expected)) { throw }
            $rejected = $true
        }
        if (-not $rejected) { throw "Verification accepted invalid fixture: $($case.Name)" }
    }
    [pscustomobject]@{ Version = $baseline.Version; ValidRelease = $true; RejectedInvalidFixtures = $cases.Count; Passed = $true }
} finally {
    $resolved = [IO.Path]::GetFullPath($fixtureRoot)
    if ($resolved.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -and [IO.Path]::GetFileName($resolved) -like "OneUiHome-MarketingTests-*") {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
