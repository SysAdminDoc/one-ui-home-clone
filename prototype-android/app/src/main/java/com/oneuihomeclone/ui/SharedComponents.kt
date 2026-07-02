package com.oneuihomeclone.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiAccentSoft
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiSurfaceSoft
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

internal val OneUiPanelShape = RoundedCornerShape(12.dp)
internal val OneUiControlShape = RoundedCornerShape(10.dp)
internal val OneUiIconShape = RoundedCornerShape(12.dp)
internal val OneUiMicroShape = RoundedCornerShape(4.dp)

@Composable
internal fun SettingsCapsule(
    label: String,
    onClick: () -> Unit = {},
    accent: Boolean = true,
    enabled: Boolean = true,
    selectedState: Boolean? = null,
) {
    Surface(
        shape = OneUiControlShape,
        color = when {
            !enabled -> OneUiSurfaceSoft
            accent -> OneUiAccentSoft
            else -> OneUiSurface
        },
        shadowElevation = if (accent || !enabled) 0.dp else 1.dp,
    ) {
        Text(
            label,
            color = when {
                !enabled -> OneUiTextSecondary
                accent -> OneUiAccent
                else -> OneUiText
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .defaultMinSize(minHeight = 44.dp)
                .semantics {
                    contentDescription = label
                    selectedState?.let { selected = it }
                }
                .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
internal fun <T> SettingsSelectorCard(
    title: String,
    description: String,
    entries: List<T>,
    selectedEntry: T,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Surface(
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                color = OneUiTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entries.forEach { entry ->
                    SettingsCapsule(
                        label = labelOf(entry),
                        onClick = { onSelect(entry) },
                        accent = entry == selectedEntry,
                        selectedState = entry == selectedEntry,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppIconBubble(app: CloneApp, size: Dp) {
    if (app.icon != null) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = app.icon,
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        Surface(
            modifier = Modifier.size(size),
            shape = OneUiIconShape,
            color = app.color,
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = app.name.take(1),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun DrawerPill(label: String) {
    Surface(
        shape = OneUiControlShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Text(
            label,
            color = OneUiText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
internal fun FinderSectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        color = OneUiTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
    )
}

@Composable
internal fun SettingsToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
) {
    Surface(
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                summary?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = OneUiTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
internal fun SystemFeedbackBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = OneUiControlShape,
        color = OneUiText.copy(alpha = 0.92f),
        shadowElevation = 6.dp,
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}
