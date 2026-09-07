param(
    [string]$ImageMagickPath = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$brandRoot = Join-Path $repoRoot "assets\brand"
$sourcePath = Join-Path $brandRoot "one-ui-home-mark-source.png"
$iconPackRoot = Join-Path $repoRoot "one_ui_home_clone_icon_pack"
$resRoot = Join-Path $repoRoot "prototype-android\app\src\main\res"
$boldFont = (Join-Path $env:SystemRoot "Fonts\seguisb.ttf").Replace("\", "/")
$regularFont = (Join-Path $env:SystemRoot "Fonts\segoeui.ttf").Replace("\", "/")

if ([string]::IsNullOrWhiteSpace($ImageMagickPath)) {
    $magickCommand = Get-Command magick.exe -ErrorAction SilentlyContinue
    if ($null -eq $magickCommand) { $magickCommand = Get-Command magick -ErrorAction SilentlyContinue }
    if ($null -eq $magickCommand) { throw "ImageMagick is required to build the One UI Home Clone brand assets." }
    $ImageMagickPath = $magickCommand.Source
}

foreach ($required in @($sourcePath, $boldFont, $regularFont, $ImageMagickPath)) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) {
        throw "Missing brand build input: $required"
    }
}

function Invoke-Magick {
    param([string[]]$Arguments)
    & $ImageMagickPath @Arguments
    if ($LASTEXITCODE -ne 0) { throw "ImageMagick failed with exit code $LASTEXITCODE." }
}

function Get-ImageProperty {
    param([string]$Path, [string]$Format)
    $value = & $ImageMagickPath identify -format $Format $Path
    if ($LASTEXITCODE -ne 0) { throw "ImageMagick could not inspect $Path." }
    [string]$value
}

function Assert-RgbaPng {
    param(
        [string]$Path,
        [string]$ExpectedGeometry,
        [switch]$RequireTransparentCorner
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf) -or (Get-Item -LiteralPath $Path).Length -lt 1KB) {
        throw "Brand output is missing or unexpectedly small: $Path"
    }
    $geometry = Get-ImageProperty -Path $Path -Format "%wx%h"
    $channels = Get-ImageProperty -Path $Path -Format "%[channels]"
    if ($geometry -ne $ExpectedGeometry -or $channels -notmatch "a") {
        throw "Expected $ExpectedGeometry RGBA PNG: $Path"
    }
    if ($RequireTransparentCorner) {
        $cornerAlpha = [double](Get-ImageProperty -Path $Path -Format "%[fx:p{0,0}.a]")
        if ($cornerAlpha -ne 0) { throw "Expected a fully transparent corner: $Path" }
    }
}

New-Item -ItemType Directory -Path $brandRoot -Force | Out-Null
New-Item -ItemType Directory -Path $iconPackRoot -Force | Out-Null
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) "OneUiHome-Brand-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null

try {
    $sourceWidth = [int](Get-ImageProperty -Path $sourcePath -Format "%w")
    $sourceHeight = [int](Get-ImageProperty -Path $sourcePath -Format "%h")
    $sourceChannels = Get-ImageProperty -Path $sourcePath -Format "%[channels]"
    $cornerAlpha = [double](Get-ImageProperty -Path $sourcePath -Format "%[fx:p{0,0}.a]")
    if ($sourceWidth -ne $sourceHeight -or $sourceWidth -lt 1024 -or $sourceChannels -notmatch "a" -or $cornerAlpha -ne 0) {
        throw "The brand master must be a square RGBA PNG at least 1024 pixels wide with a fully transparent corner."
    }

    $markPath = Join-Path $brandRoot "one-ui-home-mark.png"
    $appIconPath = Join-Path $brandRoot "one-ui-home-app-icon.png"
    $wordmarkPath = Join-Path $brandRoot "one-ui-home-wordmark.png"
    $bannerPath = Join-Path $brandRoot "one-ui-home-banner.png"
    $socialPath = Join-Path $brandRoot "social-preview.png"
    $rootLogoPath = Join-Path $repoRoot "logo.png"
    $primaryPackPath = Join-Path $iconPackRoot "one_ui_home_clone_primary.png"
    $darkPackPath = Join-Path $iconPackRoot "one_ui_home_clone_dark.png"
    $foregroundPackPath = Join-Path $iconPackRoot "one_ui_home_clone_foreground.png"
    $monochromePackPath = Join-Path $iconPackRoot "one_ui_home_clone_monochrome.png"
    $iconBackground = Join-Path $tempRoot "icon-background.png"
    $bannerBackground = Join-Path $tempRoot "banner-background.png"
    $socialBackground = Join-Path $tempRoot "social-background.png"

    Invoke-Magick @($sourcePath, "-resize", "1024x1024!", "-strip", "PNG32:$markPath")

    Invoke-Magick @(
        "-size", "1024x1024", "xc:none",
        "-fill", "#0A1B36", "-draw", "roundrectangle 22,22 1002,1002 218,218",
        "-fill", "rgba(0,174,255,0.10)", "-draw", "circle 805,210 970,210",
        "-fill", "rgba(12,82,255,0.10)", "-draw", "circle 250,740 380,740",
        "PNG32:$iconBackground"
    )
    Invoke-Magick @(
        $iconBackground,
        "(", $markPath, "-resize", "760x760", ")", "-gravity", "center", "-composite",
        "-strip", "PNG32:$appIconPath"
    )
    Copy-Item -LiteralPath $appIconPath -Destination $rootLogoPath -Force
    Copy-Item -LiteralPath $appIconPath -Destination $primaryPackPath -Force

    Invoke-Magick @(
        "-size", "1024x1024", "xc:none",
        "-fill", "#050A13", "-draw", "roundrectangle 22,22 1002,1002 218,218",
        "-fill", "rgba(0,174,255,0.07)", "-draw", "circle 805,210 970,210",
        "(", $markPath, "-resize", "760x760", ")", "-gravity", "center", "-composite",
        "-strip", "PNG32:$darkPackPath"
    )
    Invoke-Magick @(
        "-size", "1024x1024", "xc:none",
        "(", $markPath, "-resize", "760x760", ")", "-gravity", "center", "-composite",
        "-strip", "PNG32:$foregroundPackPath"
    )
    Invoke-Magick @(
        "-size", "1024x1024", "xc:none",
        "(", $markPath, "-resize", "760x760", "-channel", "RGB", "-fill", "white", "-colorize", "100", "+channel", ")",
        "-gravity", "center", "-composite", "-strip", "PNG32:$monochromePackPath"
    )

    Invoke-Magick @(
        "-size", "1400x300", "xc:none",
        "(", $appIconPath, "-resize", "238x238", ")", "-geometry", "+18+31", "-composite",
        "-font", $boldFont, "-fill", "#F5F8FF", "-pointsize", "76", "-annotate", "+292+142", "One UI Home Clone",
        "-font", $regularFont, "-fill", "#86C9FF", "-pointsize", "28", "-annotate", "+297+202", "A familiar launcher flow, rebuilt in Kotlin and Compose",
        "-strip", "PNG32:$wordmarkPath"
    )

    Invoke-Magick @(
        "-size", "1600x500", "gradient:#06101E-#123D78", "-rotate", "90", "-resize", "1600x500!",
        "-fill", "rgba(0,174,255,0.10)", "-draw", "circle 1400,70 1770,70",
        "-fill", "rgba(22,85,255,0.13)", "-draw", "circle 1180,500 1600,500",
        "-strip", $bannerBackground
    )
    Invoke-Magick @(
        $bannerBackground,
        "(", $appIconPath, "-resize", "324x324", ")", "-geometry", "+104+88", "-composite",
        "-font", $boldFont, "-fill", "#F8FAFF", "-pointsize", "72", "-annotate", "+490+200", "One UI Home Clone",
        "-font", $regularFont, "-fill", "#BED8F6", "-pointsize", "29", "-annotate", "+496+268", "A familiar home screen, rebuilt in Kotlin and Compose",
        "-fill", "#64C7FF", "-pointsize", "21", "-annotate", "+498+330", "Private by design  |  Android 9+  |  Signed APK",
        "-strip", "PNG24:$bannerPath"
    )

    Invoke-Magick @(
        "-size", "1280x640", "gradient:#06101E-#123D78", "-rotate", "90", "-resize", "1280x640!",
        "-fill", "rgba(0,174,255,0.10)", "-draw", "circle 1120,90 1460,90",
        "-fill", "rgba(22,85,255,0.13)", "-draw", "circle 980,630 1390,630",
        "-strip", $socialBackground
    )
    Invoke-Magick @(
        $socialBackground,
        "(", $appIconPath, "-resize", "350x350", ")", "-geometry", "+86+145", "-composite",
        "-font", $boldFont, "-fill", "#F8FAFF", "-pointsize", "64", "-annotate", "+486+275", "One UI Home Clone",
        "-font", $regularFont, "-fill", "#BED8F6", "-pointsize", "27", "-annotate", "+491+343", "A familiar Android launcher flow",
        "-fill", "#64C7FF", "-pointsize", "22", "-annotate", "+491+397", "Private by design  |  Kotlin  |  Compose",
        "-strip", "PNG24:$socialPath"
    )

    $densityOutputs = [ordered]@{
        "mipmap-mdpi" = @{ Canvas = 108; Mark = 64 }
        "mipmap-hdpi" = @{ Canvas = 162; Mark = 96 }
        "mipmap-xhdpi" = @{ Canvas = 216; Mark = 128 }
        "mipmap-xxhdpi" = @{ Canvas = 324; Mark = 192 }
        "mipmap-xxxhdpi" = @{ Canvas = 432; Mark = 256 }
    }
    foreach ($entry in $densityOutputs.GetEnumerator()) {
        $densityDir = Join-Path $resRoot $entry.Key
        $canvas = [int]$entry.Value.Canvas
        $markSize = [int]$entry.Value.Mark
        $foregroundPath = Join-Path $densityDir "ic_launcher_foreground.png"
        $monochromePath = Join-Path $densityDir "ic_launcher_monochrome.png"

        Invoke-Magick @(
            "-size", "${canvas}x${canvas}", "xc:none",
            "(", $markPath, "-resize", "${markSize}x${markSize}", ")", "-gravity", "center", "-composite",
            "-strip", "PNG32:$foregroundPath"
        )
        Invoke-Magick @(
            "-size", "${canvas}x${canvas}", "xc:none",
            "(", $markPath, "-resize", "${markSize}x${markSize}", "-channel", "RGB", "-fill", "white", "-colorize", "100", "+channel", ")",
            "-gravity", "center", "-composite", "-strip", "PNG32:$monochromePath"
        )
        Assert-RgbaPng -Path $foregroundPath -ExpectedGeometry "${canvas}x${canvas}" -RequireTransparentCorner
        Assert-RgbaPng -Path $monochromePath -ExpectedGeometry "${canvas}x${canvas}" -RequireTransparentCorner
    }

    Assert-RgbaPng -Path $markPath -ExpectedGeometry "1024x1024" -RequireTransparentCorner
    Assert-RgbaPng -Path $appIconPath -ExpectedGeometry "1024x1024" -RequireTransparentCorner
    Assert-RgbaPng -Path $wordmarkPath -ExpectedGeometry "1400x300" -RequireTransparentCorner
    Assert-RgbaPng -Path $rootLogoPath -ExpectedGeometry "1024x1024" -RequireTransparentCorner
    foreach ($iconPackOutput in @($primaryPackPath, $darkPackPath, $foregroundPackPath, $monochromePackPath)) {
        Assert-RgbaPng -Path $iconPackOutput -ExpectedGeometry "1024x1024" -RequireTransparentCorner
    }
    foreach ($opaqueOutput in @($bannerPath, $socialPath)) {
        if (-not (Test-Path -LiteralPath $opaqueOutput -PathType Leaf) -or (Get-Item -LiteralPath $opaqueOutput).Length -lt 1KB) {
            throw "Brand output is missing or unexpectedly small: $opaqueOutput"
        }
    }

    [PSCustomObject]@{
        Mark = $markPath
        AppIcon = $appIconPath
        Wordmark = $wordmarkPath
        Banner = $bannerPath
        SocialPreview = $socialPath
        IconPack = $iconPackRoot
        LauncherIconDensities = $densityOutputs.Keys.Count
    }
} finally {
    $fullTempRoot = [System.IO.Path]::GetFullPath($tempRoot)
    $tempBase = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
    if ($fullTempRoot.StartsWith($tempBase, [System.StringComparison]::OrdinalIgnoreCase) -and
        [System.IO.Path]::GetFileName($fullTempRoot) -like "OneUiHome-Brand-*") {
        Remove-Item -LiteralPath $fullTempRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
