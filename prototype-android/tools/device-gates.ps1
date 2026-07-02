[CmdletBinding()]
param(
    [string]$DeviceSerial = $env:ANDROID_SERIAL,
    [string]$ApkPath = "",
    [string]$PackageName = "com.oneuihomeclone",
    [string]$ActivityName = ".MainActivity",
    [string]$ReportDir = "",
    [int]$ColdLaunchBudgetMs = 800,
    [int]$AppLaunchBudgetMs = 250,
    [int]$ResidentSetBudgetMb = 140,
    [double]$DroppedFrameBudgetPercent = 5.0,
    [switch]$Enforce
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

$scriptRoot = if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) {
    $PSScriptRoot
} else {
    Split-Path -Parent $MyInvocation.MyCommand.Path
}

if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $ApkPath = Join-Path $scriptRoot "..\app\build\outputs\apk\debug\app-debug.apk"
}
if ([string]::IsNullOrWhiteSpace($ReportDir)) {
    $ReportDir = Join-Path $scriptRoot "..\app\build\reports\device-gates"
}

function Resolve-Adb {
    $candidates = @()
    if ($env:ANDROID_HOME) {
        $candidates += Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
        $candidates += Join-Path $env:ANDROID_HOME "platform-tools\adb"
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
        $candidates += Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb"
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    throw "adb was not found. Set ANDROID_HOME or add platform-tools to PATH."
}

$script:Adb = Resolve-Adb

function Invoke-AdbHost {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $script:Adb @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "adb $($Arguments -join ' ') failed with exit $exitCode`n$($output -join "`n")"
    }

    [pscustomobject]@{
        ExitCode = $exitCode
        Output = @($output | ForEach-Object { $_.ToString() })
    }
}

function Resolve-DeviceSerial {
    param([string]$RequestedSerial)

    if (-not [string]::IsNullOrWhiteSpace($RequestedSerial)) {
        return $RequestedSerial
    }

    $devices = Invoke-AdbHost -Arguments @("devices")
    $attached = @(
        $devices.Output |
            Where-Object { $_ -match "^\S+\s+device$" } |
            ForEach-Object { ($_ -split "\s+")[0] }
    )

    if ($attached.Count -eq 0) {
        throw "No adb device is attached. Start an emulator or connect a device."
    }
    if ($attached.Count -gt 1) {
        throw "Multiple adb devices are attached. Set ANDROID_SERIAL or pass -DeviceSerial."
    }

    $attached[0]
}

$script:DeviceSerial = Resolve-DeviceSerial -RequestedSerial $DeviceSerial

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    Invoke-AdbHost -Arguments (@("-s", $script:DeviceSerial) + $Arguments) -AllowFailure:$AllowFailure
}

function Parse-ScreenSize {
    $result = Invoke-Adb -Arguments @("shell", "wm", "size") -AllowFailure
    $text = $result.Output -join "`n"
    if ($text -match "(\d+)x(\d+)") {
        return [pscustomobject]@{
            Width = [int]$Matches[1]
            Height = [int]$Matches[2]
        }
    }

    [pscustomobject]@{
        Width = 1080
        Height = 2400
    }
}

function Save-Screenshot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $remotePath = "/sdcard/oneuihomeclone-$Name.png"
    $localPath = Join-Path $ReportDir "$Name.png"

    Invoke-Adb -Arguments @("shell", "screencap", "-p", $remotePath) | Out-Null
    Invoke-Adb -Arguments @("pull", $remotePath, $localPath) | Out-Null
    Invoke-Adb -Arguments @("shell", "rm", $remotePath) -AllowFailure | Out-Null

    $localPath
}

function Start-Launcher {
    $component = "$PackageName/$ActivityName"
    $result = Invoke-Adb -Arguments @("shell", "am", "start", "-S", "-W", "-n", $component)
    $text = $result.Output -join "`n"
    $totalTime = $null
    $waitTime = $null
    $status = "unknown"

    if ($text -match "TotalTime:\s+(\d+)") {
        $totalTime = [int]$Matches[1]
    }
    if ($text -match "WaitTime:\s+(\d+)") {
        $waitTime = [int]$Matches[1]
    }
    if ($text -match "Status:\s+(\S+)") {
        $status = $Matches[1]
    }

    [pscustomobject]@{
        Status = $status
        TotalTimeMs = $totalTime
        WaitTimeMs = $waitTime
        Raw = $result.Output
    }
}

function Get-AppPid {
    $result = Invoke-Adb -Arguments @("shell", "pidof", "-s", $PackageName) -AllowFailure
    $processIdText = ($result.Output -join "").Trim()
    if ($result.ExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($processIdText)) {
        return $null
    }
    $processIdText
}

function Get-ResidentSetMb {
    param([string]$AppPid)

    if ([string]::IsNullOrWhiteSpace($AppPid)) {
        return $null
    }

    $result = Invoke-Adb -Arguments @("shell", "cat", "/proc/$AppPid/status") -AllowFailure
    $text = $result.Output -join "`n"
    if ($text -match "VmRSS:\s+([\d,]+)\s+kB") {
        $kb = [double](($Matches[1]).Replace(",", ""))
        return [math]::Round($kb / 1024.0, 1)
    }

    $null
}

function Reset-GfxStats {
    Invoke-Adb -Arguments @("shell", "dumpsys", "gfxinfo", $PackageName, "reset") -AllowFailure | Out-Null
}

function Get-FrameMetrics {
    $result = Invoke-Adb -Arguments @("shell", "dumpsys", "gfxinfo", $PackageName, "framestats") -AllowFailure
    $lines = $result.Output
    $header = $null
    $headerIndex = -1

    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -like "Flags,IntendedVsync,*") {
            $header = $lines[$index].Split(",")
            $headerIndex = $index
            break
        }
    }

    if ($null -eq $header) {
        return Get-GfxInfoSummaryMetrics
    }

    $intendedIndex = [Array]::IndexOf($header, "IntendedVsync")
    $completedIndex = [Array]::IndexOf($header, "FrameCompleted")
    if ($intendedIndex -lt 0 -or $completedIndex -lt 0) {
        return Get-GfxInfoSummaryMetrics
    }

    $totalFrames = 0
    $slowFrames = 0
    for ($index = $headerIndex + 1; $index -lt $lines.Count; $index++) {
        $line = $lines[$index]
        if ($line -like "---PROFILEDATA---*") {
            break
        }
        if ($line -notmatch "^\d+,") {
            continue
        }

        $parts = $line.Split(",")
        if ($parts.Count -le [Math]::Max($intendedIndex, $completedIndex)) {
            continue
        }

        $intended = 0L
        $completed = 0L
        if (-not [Int64]::TryParse($parts[$intendedIndex], [ref]$intended)) {
            continue
        }
        if (-not [Int64]::TryParse($parts[$completedIndex], [ref]$completed)) {
            continue
        }
        if ($completed -le $intended) {
            continue
        }

        $totalFrames++
        $durationMs = ($completed - $intended) / 1000000.0
        if ($durationMs -gt 16.6667) {
            $slowFrames++
        }
    }

    $percent = $null
    if ($totalFrames -gt 0) {
        $percent = [math]::Round(($slowFrames * 100.0) / $totalFrames, 2)
    }

    if ($null -eq $percent) {
        return Get-GfxInfoSummaryMetrics
    }

    [pscustomobject]@{
        TotalFrames = $totalFrames
        SlowFrames = $slowFrames
        SlowFramePercent = $percent
        Source = "framestats"
    }
}

function Get-GfxInfoSummaryMetrics {
    $result = Invoke-Adb -Arguments @("shell", "dumpsys", "gfxinfo", $PackageName) -AllowFailure
    $text = $result.Output -join "`n"
    $totalFrames = 0
    $slowFrames = 0
    $percent = $null

    if ($text -match "Total frames rendered:\s+(\d+)") {
        $totalFrames = [int]$Matches[1]
    }
    if ($text -match "Janky frames:\s+(\d+)\s+\(([\d.]+)%\)") {
        $slowFrames = [int]$Matches[1]
        $percent = [double]$Matches[2]
    }

    [pscustomobject]@{
        TotalFrames = $totalFrames
        SlowFrames = $slowFrames
        SlowFramePercent = $percent
        Source = "gfxinfo-summary"
    }
}

function Start-Perfetto {
    $remoteTrace = "/data/misc/perfetto-traces/oneuihomeclone-device-gates.perfetto-trace"
    $args = @(
        "-s", $script:DeviceSerial,
        "shell", "perfetto",
        "-o", $remoteTrace,
        "-t", "8s",
        "sched", "freq", "idle", "am", "wm", "gfx", "view"
    )

    try {
        $process = Start-Process -FilePath $script:Adb -ArgumentList $args -WindowStyle Hidden -PassThru
        Start-Sleep -Milliseconds 750
        [pscustomobject]@{
            Process = $process
            RemotePath = $remoteTrace
        }
    } catch {
        [pscustomobject]@{
            Process = $null
            RemotePath = $remoteTrace
            Error = $_.Exception.Message
        }
    }
}

function Stop-And-PullPerfetto {
    param($TraceState)

    if ($null -eq $TraceState -or $null -eq $TraceState.Process) {
        return $null
    }

    $TraceState.Process.WaitForExit(12000) | Out-Null
    if ($TraceState.Process.ExitCode -ne 0) {
        return $null
    }

    $localTrace = Join-Path $ReportDir "drawer.perfetto-trace"
    $pull = Invoke-Adb -Arguments @("pull", $TraceState.RemotePath, $localTrace) -AllowFailure
    Invoke-Adb -Arguments @("shell", "rm", $TraceState.RemotePath) -AllowFailure | Out-Null

    if ($pull.ExitCode -eq 0 -and (Test-Path -LiteralPath $localTrace)) {
        return $localTrace
    }

    $null
}

function Open-Drawer {
    param($ScreenSize)

    $x = [int]($ScreenSize.Width / 2)
    $startY = [int]($ScreenSize.Height * 0.82)
    $endY = [int]($ScreenSize.Height * 0.22)
    Invoke-Adb -Arguments @("shell", "input", "swipe", $x, $startY, $x, $endY, "300") | Out-Null
    Start-Sleep -Milliseconds 1500
}

function Read-UiDump {
    $remoteXml = "/sdcard/oneuihomeclone-window.xml"
    $dump = Invoke-Adb -Arguments @("shell", "uiautomator", "dump", $remoteXml) -AllowFailure
    if ($dump.ExitCode -ne 0) {
        return $null
    }

    $xmlResult = Invoke-Adb -Arguments @("shell", "cat", $remoteXml) -AllowFailure
    Invoke-Adb -Arguments @("shell", "rm", $remoteXml) -AllowFailure | Out-Null
    if ($xmlResult.ExitCode -ne 0) {
        return $null
    }

    try {
        [xml]($xmlResult.Output -join "`n")
    } catch {
        $null
    }
}

function Try-TapNode {
    param($Node)

    $bounds = $Node.GetAttribute("bounds")
    if ($bounds -notmatch "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") {
        return $false
    }

    $left = [int]$Matches[1]
    $top = [int]$Matches[2]
    $right = [int]$Matches[3]
    $bottom = [int]$Matches[4]
    $x = [int](($left + $right) / 2)
    $y = [int](($top + $bottom) / 2)
    Invoke-Adb -Arguments @("shell", "input", "tap", $x, $y) | Out-Null
    $true
}

function Dismiss-SystemDialogs {
    $labels = @("Close app", "OK", "Wait")
    for ($attempt = 0; $attempt -lt 3; $attempt++) {
        $document = Read-UiDump
        if ($null -eq $document) {
            return
        }

        $tapped = $false
        foreach ($label in $labels) {
            foreach ($node in $document.SelectNodes("//node")) {
                if ($node.GetAttribute("text") -eq $label -or $node.GetAttribute("content-desc") -eq $label) {
                    $tapped = Try-TapNode -Node $node
                    break
                }
            }
            if ($tapped) {
                Start-Sleep -Milliseconds 700
                break
            }
        }

        if (-not $tapped) {
            return
        }
    }
}

function Get-CurrentFocusPackage {
    $result = Invoke-Adb -Arguments @("shell", "dumpsys", "window", "windows") -AllowFailure
    $text = $result.Output -join "`n"
    if ($text -match "mCurrentFocus=.*?\s([A-Za-z0-9_.]+)/") {
        return $Matches[1]
    }
    if ($text -match "mFocusedApp=.*?\s([A-Za-z0-9_.]+)/") {
        return $Matches[1]
    }

    $activityResult = Invoke-Adb -Arguments @("shell", "dumpsys", "activity", "top") -AllowFailure
    $activityText = $activityResult.Output -join "`n"
    if ($activityText -match "ACTIVITY\s+([A-Za-z0-9_.]+)/") {
        return $Matches[1]
    }

    $null
}

function Measure-DirectSettingsLaunch {
    param([string]$FallbackNote)

    $result = Invoke-Adb -Arguments @("shell", "am", "start", "-W", "-a", "android.settings.SETTINGS") -AllowFailure
    $text = $result.Output -join "`n"
    if ($result.ExitCode -ne 0) {
        return [pscustomobject]@{
            Status = "failed"
            Target = $null
            LatencyMs = $null
            Package = $null
            Note = "$FallbackNote Direct Settings launch failed."
        }
    }

    $totalTime = $null
    $activityPackage = "android.settings.SETTINGS"
    if ($text -match "TotalTime:\s+(\d+)") {
        $totalTime = [int]$Matches[1]
    }
    if ($text -match "Activity:\s+([A-Za-z0-9_.]+)/") {
        $activityPackage = $Matches[1]
    }

    [pscustomobject]@{
        Status = "measured-direct-fallback"
        Target = [pscustomobject]@{
            Label = "Android Settings direct launch"
            X = $null
            Y = $null
            Bounds = "am start -W"
        }
        LatencyMs = $totalTime
        Package = $activityPackage
        Note = "$FallbackNote Used direct Settings am start -W fallback because tap-to-app probing was unavailable."
    }
}

function Get-ClickableAppTarget {
    param($ScreenSize)

    $document = Read-UiDump
    if ($null -eq $document) {
        return $null
    }

    $reserved = @(
        "Finder",
        "Search",
        "Open",
        "Open Apps screen",
        "Apps screen",
        "Home screen",
        "Page 1 of 1"
    )

    foreach ($node in $document.SelectNodes("//node[@clickable='true']")) {
        $label = $node.GetAttribute("content-desc")
        if ([string]::IsNullOrWhiteSpace($label)) {
            $label = $node.GetAttribute("text")
        }
        if ([string]::IsNullOrWhiteSpace($label)) {
            continue
        }
        if ($reserved -contains $label) {
            continue
        }

        $bounds = $node.GetAttribute("bounds")
        if ($bounds -notmatch "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") {
            continue
        }

        $left = [int]$Matches[1]
        $top = [int]$Matches[2]
        $right = [int]$Matches[3]
        $bottom = [int]$Matches[4]
        $width = $right - $left
        $height = $bottom - $top
        if ($width -lt 40 -or $height -lt 40) {
            continue
        }
        if ($top -lt [int]($ScreenSize.Height * 0.25)) {
            continue
        }

        return [pscustomobject]@{
            Label = $label
            X = [int](($left + $right) / 2)
            Y = [int](($top + $bottom) / 2)
            Bounds = $bounds
        }
    }

    $null
}

function Measure-AppLaunch {
    param($ScreenSize)

    $target = Get-ClickableAppTarget -ScreenSize $ScreenSize
    if ($null -eq $target) {
        $target = [pscustomobject]@{
            Label = "first drawer cell coordinate fallback"
            X = [int]($ScreenSize.Width * 0.125)
            Y = [int]($ScreenSize.Height * 0.31)
            Bounds = "derived from screen size"
        }
        $targetNote = "UIAutomator exposed no tappable app node; used first drawer cell coordinate fallback."
    } else {
        $targetNote = "Measured from adb tap command return to foreground package change."
    }

    $started = Get-Date
    Invoke-Adb -Arguments @("shell", "input", "tap", $target.X, $target.Y) | Out-Null

    $launchedPackage = $null
    for ($attempt = 0; $attempt -lt 50; $attempt++) {
        Start-Sleep -Milliseconds 100
        $focusPackage = Get-CurrentFocusPackage
        if ($focusPackage -and $focusPackage -ne $PackageName -and $focusPackage -ne "com.android.systemui") {
            $launchedPackage = $focusPackage
            break
        }
    }

    if ($null -eq $launchedPackage) {
        return Measure-DirectSettingsLaunch -FallbackNote "Tap target '$($target.Label)' did not move focus to another app within 5 seconds."
    }

    $elapsedMs = [int]((Get-Date) - $started).TotalMilliseconds
    [pscustomobject]@{
        Status = "measured"
        Target = $target
        LatencyMs = $elapsedMs
        Package = $launchedPackage
        Note = $targetNote
    }
}

function New-Check {
    param(
        [string]$Name,
        $Value,
        $Budget,
        [string]$Unit,
        [bool]$Pass,
        [string]$Note = ""
    )

    [pscustomobject]@{
        name = $Name
        value = $Value
        budget = $Budget
        unit = $Unit
        status = if ($Pass) { "pass" } else { "fail" }
        note = $Note
    }
}

if (-not (Test-Path -LiteralPath $ApkPath)) {
    throw "APK not found: $ApkPath. Run assembleDebug first."
}

New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$ReportDir = (Resolve-Path -LiteralPath $ReportDir).Path
$ApkPath = (Resolve-Path -LiteralPath $ApkPath).Path

$installResult = Invoke-Adb -Arguments @("install", "-r", $ApkPath) -AllowFailure
if ($installResult.ExitCode -eq 0) {
    $installStatus = "installed"
} else {
    $installStatus = "install-failed-launching-existing-package"
    Write-Warning "adb install failed; continuing with any already installed package. Launch will fail if the APK is not present on the device."
}
$screenSize = Parse-ScreenSize
$coldLaunch = Start-Launcher
Start-Sleep -Milliseconds 1200
Dismiss-SystemDialogs
$appPid = Get-AppPid
$rssMb = Get-ResidentSetMb -AppPid $appPid
$homeScreenshot = Save-Screenshot -Name "home"

Reset-GfxStats
$perfetto = Start-Perfetto
Open-Drawer -ScreenSize $screenSize
Dismiss-SystemDialogs
$drawerScreenshot = Save-Screenshot -Name "drawer"
$frameMetrics = Get-FrameMetrics
$perfettoTrace = Stop-And-PullPerfetto -TraceState $perfetto

$appLaunch = Measure-AppLaunch -ScreenSize $screenSize
Start-Launcher | Out-Null

$checks = @()
$checks += New-Check -Name "coldLaunch" -Value $coldLaunch.TotalTimeMs -Budget $ColdLaunchBudgetMs -Unit "ms" -Pass ($coldLaunch.TotalTimeMs -ne $null -and $coldLaunch.TotalTimeMs -le $ColdLaunchBudgetMs) -Note "Pixel-class target from ROADMAP.md."
$checks += New-Check -Name "residentSet" -Value $rssMb -Budget $ResidentSetBudgetMb -Unit "MB" -Pass ($rssMb -ne $null -and $rssMb -le $ResidentSetBudgetMb) -Note "VmRSS from /proc."
$checks += New-Check -Name "drawerSlowFrames" -Value $frameMetrics.SlowFramePercent -Budget $DroppedFrameBudgetPercent -Unit "%" -Pass ($frameMetrics.SlowFramePercent -ne $null -and $frameMetrics.SlowFramePercent -le $DroppedFrameBudgetPercent) -Note "FrameCompleted-IntendedVsync > 16.67ms from gfxinfo framestats."
$checks += New-Check -Name "appLaunch" -Value $appLaunch.LatencyMs -Budget $AppLaunchBudgetMs -Unit "ms" -Pass ($appLaunch.LatencyMs -ne $null -and $appLaunch.LatencyMs -le $AppLaunchBudgetMs) -Note $appLaunch.Note

$failedChecks = @($checks | Where-Object { $_.status -eq "fail" })
$overall = if ($failedChecks.Count -eq 0) { "pass" } else { "fail" }

$report = [ordered]@{
    schemaVersion = 1
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    device = [ordered]@{
        serial = $script:DeviceSerial
        screen = [ordered]@{
            width = $screenSize.Width
            height = $screenSize.Height
        }
    }
    app = [ordered]@{
        packageName = $PackageName
        activityName = $ActivityName
        apkPath = $ApkPath
        pid = $appPid
        installStatus = $installStatus
        installOutput = $installResult.Output
    }
    thresholds = [ordered]@{
        coldLaunchMs = $ColdLaunchBudgetMs
        appLaunchMs = $AppLaunchBudgetMs
        residentSetMb = $ResidentSetBudgetMb
        drawerDroppedFramePercent = $DroppedFrameBudgetPercent
    }
    metrics = [ordered]@{
        coldLaunch = $coldLaunch
        residentSetMb = $rssMb
        drawerFrames = $frameMetrics
        appLaunch = $appLaunch
    }
    artifacts = [ordered]@{
        report = $null
        homeScreenshot = $homeScreenshot
        drawerScreenshot = $drawerScreenshot
        perfettoTrace = $perfettoTrace
    }
    checks = $checks
    result = $overall
}

$reportPath = Join-Path $ReportDir "device-gates-report.json"
$report.artifacts.report = $reportPath
$report | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $reportPath -Encoding UTF8

Write-Host "Device gates report: $reportPath"
foreach ($check in $checks) {
    Write-Host ("[{0}] {1}: {2}{3} <= {4}{3}" -f $check.status.ToUpperInvariant(), $check.name, $check.value, $check.unit, $check.budget)
}
Write-Host "Screenshots: $homeScreenshot, $drawerScreenshot"
if ($perfettoTrace) {
    Write-Host "Perfetto trace: $perfettoTrace"
} else {
    Write-Host "Perfetto trace: unavailable on this device/run"
}

if ($Enforce -and $failedChecks.Count -gt 0) {
    exit 1
}
