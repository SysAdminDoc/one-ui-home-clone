[CmdletBinding()]
param(
    [string]$DeviceSerial = $env:ANDROID_SERIAL,
    [string]$ApkPath = "",
    [string]$OutputDir = ""
)

$ErrorActionPreference = "Stop"
$packageName = "com.oneuihomeclone"
$activityName = ".MainActivity"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) { $DeviceSerial = "emulator-5590" }
if ($DeviceSerial -notmatch "^emulator-\d+$") {
    throw "Marketing capture only runs on an Android emulator. Received: $DeviceSerial"
}
if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $repoRoot "prototype-android\app\build\outputs\release-channel\one-ui-home-clone-v0.2.5-release.apk"
}
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $repoRoot "assets\screenshots"
}

function Resolve-Adb {
    foreach ($root in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA "Android\Sdk"))) {
        if ([string]::IsNullOrWhiteSpace($root)) { continue }
        $candidate = Join-Path $root "platform-tools\adb.exe"
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return (Resolve-Path -LiteralPath $candidate).Path }
    }
    $command = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($null -eq $command) { $command = Get-Command adb -ErrorAction SilentlyContinue }
    if ($null -eq $command) { throw "adb was not found." }
    $command.Source
}

$script:Adb = Resolve-Adb

function Invoke-AdbHost {
    param([string[]]$Arguments, [switch]$AllowFailure)
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $script:Adb @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "adb $($Arguments -join ' ') failed with exit $exitCode`n$($output -join "`n")"
    }
    [pscustomobject]@{ ExitCode = $exitCode; Output = @($output | ForEach-Object { $_.ToString() }) }
}

function Invoke-Adb {
    param([string[]]$Arguments, [switch]$AllowFailure)
    Invoke-AdbHost -Arguments (@("-s", $DeviceSerial) + $Arguments) -AllowFailure:$AllowFailure
}

function Get-AdbText {
    param([string[]]$Arguments, [switch]$AllowFailure)
    ((Invoke-Adb -Arguments $Arguments -AllowFailure:$AllowFailure).Output -join "`n").Trim()
}

function Get-ScreenSize {
    $text = Get-AdbText -Arguments @("shell", "wm", "size")
    if ($text -notmatch "(\d+)x(\d+)") { throw "Unable to read emulator screen size." }
    [pscustomobject]@{ Width = [int]$Matches[1]; Height = [int]$Matches[2] }
}

function Read-UiDump {
    $remotePath = "/sdcard/oneuihomeclone-marketing.xml"
    $localPath = Join-Path $script:TempRoot "window.xml"
    Invoke-Adb -Arguments @("shell", "uiautomator", "dump", "--compressed", $remotePath) | Out-Null
    Invoke-Adb -Arguments @("pull", $remotePath, $localPath) | Out-Null
    Invoke-Adb -Arguments @("shell", "rm", $remotePath) -AllowFailure | Out-Null
    [xml](Get-Content -Raw -LiteralPath $localPath)
}

function Find-UiNode {
    param([string]$Label, [switch]$AllowContains)
    $document = Read-UiDump
    foreach ($node in @($document.SelectNodes("//node"))) {
        $text = $node.GetAttribute("text")
        $description = $node.GetAttribute("content-desc")
        $matches = if ($AllowContains) {
            $text -like "*$Label*" -or $description -like "*$Label*"
        } else {
            $text -eq $Label -or $description -eq $Label
        }
        if ($matches) { return $node }
    }
    $null
}

function Wait-ForLabel {
    param([string]$Label, [switch]$AllowContains, [int]$TimeoutSeconds = 12)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $node = Find-UiNode -Label $Label -AllowContains:$AllowContains
        if ($null -ne $node) { return $node }
        Start-Sleep -Milliseconds 450
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for emulator UI label: $Label"
}

function Tap-Label {
    param([string]$Label, [switch]$AllowContains)
    $node = Wait-ForLabel -Label $Label -AllowContains:$AllowContains
    $bounds = $node.GetAttribute("bounds")
    if ($bounds -notmatch "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") {
        throw "Unable to parse bounds for '$Label': $bounds"
    }
    $x = [int](([int]$Matches[1] + [int]$Matches[3]) / 2)
    $y = [int](([int]$Matches[2] + [int]$Matches[4]) / 2)
    Invoke-Adb -Arguments @("shell", "input", "tap", "$x", "$y") | Out-Null
    Start-Sleep -Milliseconds 850
}

function Start-Launcher {
    Invoke-Adb -Arguments @("shell", "am", "force-stop", $packageName) | Out-Null
    Invoke-Adb -Arguments @("shell", "am", "start", "-S", "-W", "-n", "$packageName/$activityName") | Out-Null
    Start-Sleep -Milliseconds 1400
}

function Open-Drawer {
    param($ScreenSize)
    $x = [int]($ScreenSize.Width / 2)
    $startY = [int]($ScreenSize.Height * 0.84)
    $endY = [int]($ScreenSize.Height * 0.24)
    Invoke-Adb -Arguments @("shell", "input", "swipe", "$x", "$startY", "$x", "$endY", "320") | Out-Null
    Wait-ForLabel -Label "Apps" | Out-Null
    Start-Sleep -Milliseconds 700
}

function Save-MarketingScreenshot {
    param([string]$Name)
    $remotePath = "/sdcard/oneuihomeclone-$Name.png"
    $localPath = Join-Path $OutputDir "$Name.png"
    Invoke-Adb -Arguments @("shell", "screencap", "-p", $remotePath) | Out-Null
    Invoke-Adb -Arguments @("pull", $remotePath, $localPath) | Out-Null
    Invoke-Adb -Arguments @("shell", "rm", $remotePath) -AllowFailure | Out-Null
    & $script:Magick $localPath "-strip" "PNG24:$localPath"
    if ($LASTEXITCODE -ne 0) { throw "ImageMagick could not optimize $localPath." }
    $geometry = & $script:Magick identify -format "%wx%h" $localPath
    if ($LASTEXITCODE -ne 0 -or $geometry -ne "$($script:ScreenSize.Width)x$($script:ScreenSize.Height)") {
        throw "Unexpected screenshot geometry for $localPath`: $geometry"
    }
    (Get-Item -LiteralPath $localPath).Name
}

if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) { throw "APK not found: $ApkPath" }
$ApkPath = (Resolve-Path -LiteralPath $ApkPath).Path
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$OutputDir = (Resolve-Path -LiteralPath $OutputDir).Path
$magickCommand = Get-Command magick.exe -ErrorAction SilentlyContinue
if ($null -eq $magickCommand) { $magickCommand = Get-Command magick -ErrorAction SilentlyContinue }
if ($null -eq $magickCommand) { throw "ImageMagick is required for screenshot validation." }
$script:Magick = $magickCommand.Source

$deviceState = (Invoke-AdbHost -Arguments @("devices")).Output | Where-Object { $_ -match "^$([regex]::Escape($DeviceSerial))\s+device$" }
if (-not $deviceState) { throw "Emulator is not ready: $DeviceSerial" }
$qemu = Get-AdbText -Arguments @("shell", "getprop", "ro.kernel.qemu")
if ($qemu -ne "1") { throw "Capture target did not report an emulated Android kernel." }

$script:TempRoot = Join-Path ([System.IO.Path]::GetTempPath()) "OneUiHome-Capture-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $script:TempRoot -Force | Out-Null
$originalHomeHolders = @(
    (Get-AdbText -Arguments @("shell", "cmd", "role", "get-role-holders", "android.app.role.HOME", "--user", "0") -AllowFailure) -split "\r?\n" |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
)
$nightOutput = Get-AdbText -Arguments @("shell", "cmd", "uimode", "night") -AllowFailure
$originalNightMode = if ($nightOutput -match "Night mode:\s*(\S+)") { $Matches[1] } else { "auto" }
$captured = [ordered]@{}

try {
    $installResult = Invoke-Adb -Arguments @("install", "-r", $ApkPath) -AllowFailure
    if ($installResult.ExitCode -ne 0 -and ($installResult.Output -join "`n") -match "INSTALL_FAILED_UPDATE_INCOMPATIBLE") {
        Invoke-Adb -Arguments @("uninstall", $packageName) -AllowFailure | Out-Null
        $installResult = Invoke-Adb -Arguments @("install", $ApkPath) -AllowFailure
    }
    if ($installResult.ExitCode -ne 0) {
        throw "Unable to install the supplied APK on $DeviceSerial`: $($installResult.Output -join ' ')"
    }
    Invoke-Adb -Arguments @("shell", "pm", "clear", $packageName) | Out-Null
    Invoke-Adb -Arguments @("shell", "cmd", "uimode", "night", "no") | Out-Null
    Invoke-Adb -Arguments @("shell", "cmd", "role", "add-role-holder", "android.app.role.HOME", $packageName, "0") | Out-Null
    $script:ScreenSize = Get-ScreenSize

    Start-Launcher
    Wait-ForLabel -Label "Home and Apps screens" | Out-Null
    $captured.home = Save-MarketingScreenshot -Name "home"

    Open-Drawer -ScreenSize $script:ScreenSize
    $captured.apps = Save-MarketingScreenshot -Name "apps"

    Tap-Label -Label "Search from the bottom" -AllowContains
    Invoke-Adb -Arguments @("shell", "input", "text", "settings") | Out-Null
    Start-Sleep -Milliseconds 900
    Invoke-Adb -Arguments @("shell", "input", "keyevent", "KEYCODE_BACK") | Out-Null
    Wait-ForLabel -Label "Home screen settings" | Out-Null
    Start-Sleep -Milliseconds 500
    $captured.finder = Save-MarketingScreenshot -Name "finder"

    Start-Launcher
    Open-Drawer -ScreenSize $script:ScreenSize
    Tap-Label -Label "Settings"
    Wait-ForLabel -Label "Home screen settings" | Out-Null
    $captured.settings = Save-MarketingScreenshot -Name "settings"

    Start-Launcher
    $longPressX = [int]($script:ScreenSize.Width / 2)
    $longPressY = [int]($script:ScreenSize.Height * 0.105)
    Invoke-Adb -Arguments @("shell", "input", "swipe", "$longPressX", "$longPressY", "$longPressX", "$longPressY", "900") | Out-Null
    Wait-ForLabel -Label "Wallpapers and style" | Out-Null
    $captured.editMode = Save-MarketingScreenshot -Name "edit-mode"

    Tap-Label -Label "Widgets"
    Wait-ForLabel -Label "Search widgets" | Out-Null
    Tap-Label -Label "All"
    Tap-Label -Label "Search widgets"
    Invoke-Adb -Arguments @("shell", "input", "text", "Calendar") | Out-Null
    Start-Sleep -Milliseconds 900
    Invoke-Adb -Arguments @("shell", "input", "keyevent", "KEYCODE_BACK") | Out-Null
    Wait-ForLabel -Label "Calendar month view" | Out-Null
    $captured.widgets = Save-MarketingScreenshot -Name "widgets"

    $report = [ordered]@{
        schemaVersion = 1
        generator = "prototype-android/tools/capture-marketing.ps1"
        generatedAt = (Get-Date).ToUniversalTime().ToString("o")
        source = [ordered]@{
            artifact = (Get-Item -LiteralPath $ApkPath).Name
            sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $ApkPath).Hash.ToLowerInvariant()
            sizeBytes = (Get-Item -LiteralPath $ApkPath).Length
        }
        environment = [ordered]@{
            type = "isolated Android emulator"
            androidApi = (Get-AdbText -Arguments @("shell", "getprop", "ro.build.version.sdk"))
            resolution = "$($script:ScreenSize.Width)x$($script:ScreenSize.Height)"
        }
        captures = $captured
    }
    $reportPath = Join-Path $OutputDir "capture-report.json"
    $reportJson = (($report | ConvertTo-Json -Depth 6) -replace "`r`n", "`n") + "`n"
    [System.IO.File]::WriteAllText($reportPath, $reportJson, [System.Text.UTF8Encoding]::new($false))
    $report
} finally {
    Invoke-Adb -Arguments @("shell", "cmd", "uimode", "night", $originalNightMode) -AllowFailure | Out-Null
    if ($originalHomeHolders.Count -gt 0) {
        foreach ($holder in $originalHomeHolders) {
            Invoke-Adb -Arguments @("shell", "cmd", "role", "add-role-holder", "android.app.role.HOME", $holder, "0") -AllowFailure | Out-Null
        }
    } else {
        Invoke-Adb -Arguments @("shell", "cmd", "role", "remove-role-holder", "android.app.role.HOME", $packageName, "0") -AllowFailure | Out-Null
    }
    $fullTempRoot = [System.IO.Path]::GetFullPath($script:TempRoot)
    $tempBase = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    if ($fullTempRoot.StartsWith($tempBase, [System.StringComparison]::OrdinalIgnoreCase) -and
        [System.IO.Path]::GetFileName($fullTempRoot) -like "OneUiHome-Capture-*") {
        Remove-Item -LiteralPath $fullTempRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
