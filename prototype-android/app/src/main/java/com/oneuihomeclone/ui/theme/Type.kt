package com.oneuihomeclone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// One UI-inspired type scale. Samsung's system typography is denser than Material default:
// smaller display sizes, tighter letter-spacing at display tier, medium weight bias, and
// sentence-case body text. Values tuned against OneUI 7 reference screenshots.
private val OneUiFontFamily = FontFamily.Default

private val displayStyle = TextStyle(
    fontFamily = OneUiFontFamily,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 1.2.em,
    letterSpacing = (-0.02).em,
)

private val headlineStyle = TextStyle(
    fontFamily = OneUiFontFamily,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 1.25.em,
    letterSpacing = (-0.015).em,
)

private val titleStyle = TextStyle(
    fontFamily = OneUiFontFamily,
    fontWeight = FontWeight.Medium,
    lineHeight = 1.35.em,
    letterSpacing = 0.em,
)

private val bodyStyle = TextStyle(
    fontFamily = OneUiFontFamily,
    fontWeight = FontWeight.Normal,
    lineHeight = 1.45.em,
    letterSpacing = 0.005.em,
)

private val labelStyle = TextStyle(
    fontFamily = OneUiFontFamily,
    fontWeight = FontWeight.Medium,
    lineHeight = 1.4.em,
    letterSpacing = 0.01.em,
)

val Typography: Typography = Typography(
    displayLarge = displayStyle.copy(fontSize = 48.sp),
    displayMedium = displayStyle.copy(fontSize = 38.sp),
    displaySmall = displayStyle.copy(fontSize = 32.sp),
    headlineLarge = headlineStyle.copy(fontSize = 28.sp),
    headlineMedium = headlineStyle.copy(fontSize = 24.sp),
    headlineSmall = headlineStyle.copy(fontSize = 20.sp),
    titleLarge = titleStyle.copy(fontSize = 18.sp),
    titleMedium = titleStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = titleStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = bodyStyle.copy(fontSize = 16.sp),
    bodyMedium = bodyStyle.copy(fontSize = 14.sp),
    bodySmall = bodyStyle.copy(fontSize = 12.sp),
    labelLarge = labelStyle.copy(fontSize = 14.sp),
    labelMedium = labelStyle.copy(fontSize = 12.sp),
    labelSmall = labelStyle.copy(fontSize = 11.sp),
)
