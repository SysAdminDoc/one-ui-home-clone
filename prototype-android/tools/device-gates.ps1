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
    Invoke-Adb -Arguments @("shell", "input", "swipe", $x, $startY, $x, $endY, "300") -AllowFailure | Out-Null
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
    $tap = Invoke-Adb -Arguments @("shell", "input", "tap", $x, $y) -AllowFailure
    $tap.ExitCode -eq 0
}

function Get-UiTextSnapshot {
    $document = Read-UiDump
    if ($null -eq $document) {
        return [pscustomobject]@{
            Available = $false
            Labels = @()
            Text = ""
            Note = "UIAutomator dump unavailable."
        }
    }

    $labels = @()
    foreach ($node in @($document.SelectNodes("//node"))) {
        $text = $node.GetAttribute("text")
        $description = $node.GetAttribute("content-desc")
        if (-not [string]::IsNullOrWhiteSpace($text)) {
            $labels += $text
        }
        if (-not [string]::IsNullOrWhiteSpace($description)) {
            $labels += $description
        }
    }

    [pscustomobject]@{
        Available = $true
        Labels = @($labels)
        Text = ($labels -join "`n")
        Note = "UIAutomator dump available."
    }
}

function Test-UiContainsAny {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Labels
    )

    $snapshot = Get-UiTextSnapshot
    if (-not $snapshot.Available) {
        return [pscustomobject]@{
            Found = $false
            Label = $null
            Snapshot = $snapshot
            Note = $snapshot.Note
        }
    }

    foreach ($label in $Labels) {
        if ($snapshot.Text -like "*$label*") {
            return [pscustomobject]@{
                Found = $true
                Label = $label
                Snapshot = $snapshot
                Note = "Found '$label'."
            }
        }
    }

    [pscustomobject]@{
        Found = $false
        Label = $null
        Snapshot = $snapshot
        Note = "None of these labels were visible: $($Labels -join ', ')."
    }
}

function Find-UiNodeByLabel {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,
        [switch]$AllowContains
    )

    $document = Read-UiDump
    if ($null -eq $document) {
        return $null
    }

    foreach ($node in @($document.SelectNodes("//node"))) {
        $text = $node.GetAttribute("text")
        $description = $node.GetAttribute("content-desc")
        if ($text -eq $Label -or $description -eq $Label) {
            return $node
        }
    }

    if ($AllowContains) {
        foreach ($node in @($document.SelectNodes("//node"))) {
            $text = $node.GetAttribute("text")
            $description = $node.GetAttribute("content-desc")
            if ((!([string]::IsNullOrWhiteSpace($text)) -and $text -like "*$Label*") -or
                (!([string]::IsNullOrWhiteSpace($description)) -and $description -like "*$Label*")) {
                return $node
            }
        }
    }

    $null
}

function Tap-UiLabel {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Label,
        [switch]$AllowContains
    )

    $node = Find-UiNodeByLabel -Label $Label -AllowContains:$AllowContains
    if ($null -eq $node) {
        return [pscustomobject]@{
            Tapped = $false
            Label = $Label
            Note = "No UI node matched '$Label'."
        }
    }

    [pscustomobject]@{
        Tapped = (Try-TapNode -Node $node)
        Label = $Label
        Note = "Tapped '$Label'."
    }
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

function Press-BackKey {
    Invoke-Adb -Arguments @("shell", "input", "keyevent", "KEYCODE_BACK") -AllowFailure | Out-Null
    Start-Sleep -Milliseconds 700
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
    Invoke-Adb -Arguments @("shell", "input", "tap", $target.X, $target.Y) -AllowFailure | Out-Null

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

function New-SmokeCheck {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [bool]$Pass,
        [Parameter(Mandatory = $true)]
        [string]$Note,
        $Evidence = $null
    )

    [pscustomobject]@{
        name = $Name
        pass = $Pass
        value = if ($Pass) { 1 } else { 0 }
        note = $Note
        evidence = $Evidence
    }
}

function Invoke-HomeBackAbsorptionSmoke {
    $launch = Start-Launcher
    Start-Sleep -Milliseconds 1200
    Dismiss-SystemDialogs
    $beforeFocus = Get-CurrentFocusPackage
    Press-BackKey
    $afterFocus = Get-CurrentFocusPackage
    $pass = $beforeFocus -eq $PackageName -and $afterFocus -eq $PackageName

    New-SmokeCheck `
        -Name "homeBackAbsorption" `
        -Pass $pass `
        -Note "BACK on clean Home should be absorbed by the launcher instead of moving focus away." `
        -Evidence ([ordered]@{
            launchStatus = $launch.Status
            focusBeforeBack = $beforeFocus
            focusAfterBack = $afterFocus
        })
}

function Invoke-DrawerSwipeDownSmoke {
    param($ScreenSize)

    Start-Launcher | Out-Null
    Start-Sleep -Milliseconds 1000
    Open-Drawer -ScreenSize $ScreenSize
    $drawerOpen = Test-UiContainsAny -Labels @("Apps screen", "Search from the bottom", "Finder")

    $x = [int]($ScreenSize.Width / 2)
    $startY = [int]($ScreenSize.Height * 0.42)
    $endY = [int]($ScreenSize.Height * 0.88)
    Invoke-Adb -Arguments @("shell", "input", "swipe", $x, $startY, $x, $endY, "350") -AllowFailure | Out-Null
    Start-Sleep -Milliseconds 1000

    $homeVisible = Test-UiContainsAny -Labels @("Open Apps screen", "Search apps")
    $pass = $drawerOpen.Found -and $homeVisible.Found
    New-SmokeCheck `
        -Name "drawerSwipeDownClose" `
        -Pass $pass `
        -Note "Apps screen swipe-down should close the drawer and return to Home." `
        -Evidence ([ordered]@{
            drawerOpen = $drawerOpen.Note
            homeVisible = $homeVisible.Note
            swipe = [ordered]@{
                x = $x
                startY = $startY
                endY = $endY
            }
        })
}

function Invoke-FinderImeSmoke {
    param($ScreenSize)

    Start-Launcher | Out-Null
    Start-Sleep -Milliseconds 1000
    Open-Drawer -ScreenSize $ScreenSize

    $x = [int]($ScreenSize.Width / 2)
    $searchY = [int]($ScreenSize.Height * 0.88)
    Invoke-Adb -Arguments @("shell", "input", "tap", $x, $searchY) -AllowFailure | Out-Null
    Start-Sleep -Milliseconds 500
    Invoke-Adb -Arguments @("shell", "input", "text", "settings") -AllowFailure | Out-Null
    Start-Sleep -Milliseconds 1000

    $finderBeforeBack = Test-UiContainsAny -Labels @("Finder")
    $settingsBeforeBack = Test-UiContainsAny -Labels @("Settings", "Home screen settings")
    Press-BackKey
    $finderAfterBack = Test-UiContainsAny -Labels @("Finder")
    $settingsAfterBack = Test-UiContainsAny -Labels @("Settings", "Home screen settings")
    $pass = $finderBeforeBack.Found -and $settingsBeforeBack.Found -and $finderAfterBack.Found -and $settingsAfterBack.Found

    New-SmokeCheck `
        -Name "finderImeBackKeepsResults" `
        -Pass $pass `
        -Note "BACK while the Finder IME is open should dismiss the keyboard without clearing query results." `
        -Evidence ([ordered]@{
            searchTap = [ordered]@{ x = $x; y = $searchY }
            finderBeforeBack = $finderBeforeBack.Note
            settingsBeforeBack = $settingsBeforeBack.Note
            finderAfterBack = $finderAfterBack.Note
            settingsAfterBack = $settingsAfterBack.Note
        })
}

function Invoke-OverlayCollapseSmoke {
    param($ScreenSize)

    Start-Launcher | Out-Null
    Start-Sleep -Milliseconds 1000
    Open-Drawer -ScreenSize $ScreenSize

    $tapResult = Tap-UiLabel -Label "Settings"
    if (-not $tapResult.Tapped) {
        Invoke-Adb -Arguments @("shell", "input", "tap", ([int]($ScreenSize.Width * 0.86)), ([int]($ScreenSize.Height * 0.07))) -AllowFailure | Out-Null
        $tapResult = [pscustomobject]@{
            Tapped = $true
            Label = "Settings coordinate fallback"
            Note = "Tapped top-right settings coordinate fallback."
        }
    }
    Start-Sleep -Milliseconds 1000

    $settingsVisible = Test-UiContainsAny -Labels @("Home screen settings")
    Press-BackKey
    $homeVisible = Test-UiContainsAny -Labels @("Open Apps screen", "Search apps")
    $settingsStillVisible = Test-UiContainsAny -Labels @("Home screen settings")
    $pass = $settingsVisible.Found -and $homeVisible.Found -and (-not $settingsStillVisible.Found)

    New-SmokeCheck `
        -Name "overlayBackCollapseOrder" `
        -Pass $pass `
        -Note "BACK from a launcher overlay should close the overlay before leaving Home." `
        -Evidence ([ordered]@{
            settingsTap = $tapResult.Note
            settingsVisible = $settingsVisible.Note
            homeVisibleAfterBack = $homeVisible.Note
            settingsStillVisibleAfterBack = $settingsStillVisible.Note
        })
}

function Get-UiRootBounds {
    $document = Read-UiDump
    if ($null -eq $document) {
        return $null
    }

    $rootNode = $document.SelectSingleNode("//node")
    if ($null -eq $rootNode) {
        return $null
    }

    $bounds = $rootNode.GetAttribute("bounds")
    if ($bounds -notmatch "\[(\d+),(\d+)\]\[(\d+),(\d+)\]") {
        return $null
    }

    [pscustomobject]@{
        Left = [int]$Matches[1]
        Top = [int]$Matches[2]
        Right = [int]$Matches[3]
        Bottom = [int]$Matches[4]
        Width = [int]$Matches[3] - [int]$Matches[1]
        Height = [int]$Matches[4] - [int]$Matches[2]
        Bounds = $bounds
    }
}

function Invoke-LandscapeFinderSmoke {
    param($ScreenSize)

    $originalAutoRotate = ((Invoke-Adb -Arguments @("shell", "settings", "get", "system", "accelerometer_rotation") -AllowFailure).Output -join "").Trim()
    $originalUserRotation = ((Invoke-Adb -Arguments @("shell", "settings", "get", "system", "user_rotation") -AllowFailure).Output -join "").Trim()

    try {
        Invoke-Adb -Arguments @("shell", "settings", "put", "system", "accelerometer_rotation", "0") -AllowFailure | Out-Null
        Invoke-Adb -Arguments @("shell", "settings", "put", "system", "user_rotation", "1") -AllowFailure | Out-Null
        Start-Sleep -Milliseconds 1800

        Start-Launcher | Out-Null
        Start-Sleep -Milliseconds 1000
        Open-Drawer -ScreenSize $ScreenSize

        $rootBounds = Get-UiRootBounds
        $searchVisible = Test-UiContainsAny -Labels @("Search from the bottom", "Search apps and settings")
        $isLandscape = $rootBounds -ne $null -and $rootBounds.Width -gt $rootBounds.Height
        $pass = $isLandscape -and $searchVisible.Found

        New-SmokeCheck `
            -Name "landscapeFinderSearchVisible" `
            -Pass $pass `
            -Note "Landscape drawer should keep the Finder search field visible and inside the rotated viewport." `
            -Evidence ([ordered]@{
                originalAutoRotate = $originalAutoRotate
                originalUserRotation = $originalUserRotation
                rootBounds = $rootBounds
                searchVisible = $searchVisible.Note
            })
    } finally {
        if (-not [string]::IsNullOrWhiteSpace($originalAutoRotate) -and $originalAutoRotate -ne "null") {
            Invoke-Adb -Arguments @("shell", "settings", "put", "system", "accelerometer_rotation", $originalAutoRotate) -AllowFailure | Out-Null
        }
        if (-not [string]::IsNullOrWhiteSpace($originalUserRotation) -and $originalUserRotation -ne "null") {
            Invoke-Adb -Arguments @("shell", "settings", "put", "system", "user_rotation", $originalUserRotation) -AllowFailure | Out-Null
        }
        Start-Sleep -Milliseconds 1200
    }
}

function Get-SystemBarAppearance {
    $result = Invoke-Adb -Arguments @("shell", "dumpsys", "window") -AllowFailure
    $text = $result.Output -join "`n"

    [pscustomobject]@{
        statusLight = ($text -match "mLastAppearance=.*LIGHT_STATUS_BARS")
        navigationLight = ($text -match "mLastAppearance=.*LIGHT_NAVIGATION_BARS")
        focusPackage = Get-CurrentFocusPackage
        rawMatched = if ($text -match "mLastAppearance=([^\r\n]+)") { $Matches[1].Trim() } else { "" }
    }
}

function Get-AverageLuminance {
    param(
        [Parameter(Mandatory = $true)]
        $Bitmap,
        [Parameter(Mandatory = $true)]
        [int]$Y
    )

    $left = [int]($Bitmap.Width * 0.15)
    $right = [int]($Bitmap.Width * 0.85)
    $step = [math]::Max(1, [int](($right - $left) / 20))
    $sum = 0.0
    $count = 0
    for ($x = $left; $x -le $right; $x += $step) {
        $pixel = $Bitmap.GetPixel($x, $Y)
        $linear = (0.2126 * $pixel.R + 0.7152 * $pixel.G + 0.0722 * $pixel.B) / 255.0
        $sum += $linear
        $count++
    }
    if ($count -eq 0) {
        return $null
    }

    [math]::Round($sum / $count, 3)
}

function Test-BarContrast {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ImagePath,
        [Parameter(Mandatory = $true)]
        $Appearance
    )

    try {
        Add-Type -AssemblyName System.Drawing -ErrorAction Stop
        $bitmap = [System.Drawing.Bitmap]::new($ImagePath)
        try {
            $statusY = [math]::Min(10, $bitmap.Height - 1)
            $navY = [math]::Max(0, $bitmap.Height - 10)
            $statusLuminance = Get-AverageLuminance -Bitmap $bitmap -Y $statusY
            $navigationLuminance = Get-AverageLuminance -Bitmap $bitmap -Y $navY
        } finally {
            $bitmap.Dispose()
        }

        $statusPass = if ($Appearance.statusLight) { $statusLuminance -ge 0.45 } else { $statusLuminance -le 0.65 }
        $navigationPass = if ($Appearance.navigationLight) { $navigationLuminance -ge 0.45 } else { $navigationLuminance -le 0.65 }
        [pscustomobject]@{
            available = $true
            statusLuminance = $statusLuminance
            navigationLuminance = $navigationLuminance
            statusPass = $statusPass
            navigationPass = $navigationPass
            note = "Sampled screenshot strips against system bar light/dark appearance flags."
        }
    } catch {
        [pscustomobject]@{
            available = $false
            statusLuminance = $null
            navigationLuminance = $null
            statusPass = $false
            navigationPass = $false
            note = "Unable to sample screenshot luminance: $($_.Exception.Message)"
        }
    }
}

function Invoke-EdgeToEdgeSmoke {
    Start-Launcher | Out-Null
    Start-Sleep -Milliseconds 1200
    $screenshot = Save-Screenshot -Name "edge-to-edge"
    $appearance = Get-SystemBarAppearance
    $contrast = Test-BarContrast -ImagePath $screenshot -Appearance $appearance
    $pass = $appearance.focusPackage -eq $PackageName -and $contrast.available -and $contrast.statusPass -and $contrast.navigationPass

    New-SmokeCheck `
        -Name "edgeToEdgeSystemBarContrast" `
        -Pass $pass `
        -Note "Status/navigation bar appearance flags should match sampled edge-to-edge screenshot luminance." `
        -Evidence ([ordered]@{
            screenshot = $screenshot
            appearance = $appearance
            contrast = $contrast
        })
}

function Invoke-InteractionSmoke {
    param($ScreenSize)

    $checks = @()
    $checks += Invoke-HomeBackAbsorptionSmoke
    $checks += Invoke-DrawerSwipeDownSmoke -ScreenSize $ScreenSize
    $checks += Invoke-FinderImeSmoke -ScreenSize $ScreenSize
    $checks += Invoke-OverlayCollapseSmoke -ScreenSize $ScreenSize
    $checks += Invoke-LandscapeFinderSmoke -ScreenSize $ScreenSize
    $checks += Invoke-EdgeToEdgeSmoke

    [pscustomobject]@{
        checks = $checks
        result = if (@($checks | Where-Object { -not $_.pass }).Count -eq 0) { "pass" } else { "fail" }
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
$interactionSmoke = Invoke-InteractionSmoke -ScreenSize $screenSize

$checks = @()
$checks += New-Check -Name "coldLaunch" -Value $coldLaunch.TotalTimeMs -Budget $ColdLaunchBudgetMs -Unit "ms" -Pass ($coldLaunch.TotalTimeMs -ne $null -and $coldLaunch.TotalTimeMs -le $ColdLaunchBudgetMs) -Note "Pixel-class target from ROADMAP.md."
$checks += New-Check -Name "residentSet" -Value $rssMb -Budget $ResidentSetBudgetMb -Unit "MB" -Pass ($rssMb -ne $null -and $rssMb -le $ResidentSetBudgetMb) -Note "VmRSS from /proc."
$checks += New-Check -Name "drawerSlowFrames" -Value $frameMetrics.SlowFramePercent -Budget $DroppedFrameBudgetPercent -Unit "%" -Pass ($frameMetrics.SlowFramePercent -ne $null -and $frameMetrics.SlowFramePercent -le $DroppedFrameBudgetPercent) -Note "FrameCompleted-IntendedVsync > 16.67ms from gfxinfo framestats."
$checks += New-Check -Name "appLaunch" -Value $appLaunch.LatencyMs -Budget $AppLaunchBudgetMs -Unit "ms" -Pass ($appLaunch.LatencyMs -ne $null -and $appLaunch.LatencyMs -le $AppLaunchBudgetMs) -Note $appLaunch.Note
foreach ($smokeCheck in $interactionSmoke.checks) {
    $checks += New-Check -Name $smokeCheck.name -Value $smokeCheck.value -Budget 1 -Unit "" -Pass $smokeCheck.pass -Note $smokeCheck.note
}

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
        interactionSmoke = $interactionSmoke
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
