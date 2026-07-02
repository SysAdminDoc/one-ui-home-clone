package com.oneuihomeclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.ui.theme.OneUiAccentSoft
import com.oneuihomeclone.ui.theme.OneUiCard
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiSurfaceSoft
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

@Composable
internal fun ContextActionSheet(
    title: String,
    summary: String,
    app: CloneApp?,
    widget: WidgetTemplateModel?,
    actions: List<LauncherContextAction>,
    onAction: (LauncherContextAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) },
            shape = OneUiPanelShape,
            color = OneUiCard.copy(alpha = 0.98f),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ContextActionHeader(title = title, summary = summary, app = app, widget = widget)
                actions.forEach { action ->
                    ContextActionRow(action = action, onClick = { onAction(action) })
                }
            }
        }
    }
}

@Composable
private fun ContextActionHeader(
    title: String,
    summary: String,
    app: CloneApp?,
    widget: WidgetTemplateModel?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            app != null -> AppIconBubble(app = app, size = 54.dp)
            widget != null -> {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(OneUiIconShape)
                        .background(widget.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = widget.title.take(1),
                        color = OneUiText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = OneUiText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = OneUiTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ContextActionRow(
    action: LauncherContextAction,
    onClick: () -> Unit,
) {
    val actionDescription = "${action.title}, ${action.summary}"
    Surface(
        shape = OneUiControlShape,
        color = if (action.enabled) OneUiSurface else OneUiSurfaceSoft,
        shadowElevation = if (action.enabled) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = actionDescription }
                .then(if (action.enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(OneUiMicroShape)
                    .background(if (action.enabled) OneUiAccentSoft else OneUiTextSecondary.copy(alpha = 0.28f)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    color = if (action.enabled) OneUiText else OneUiTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = action.summary,
                    color = OneUiTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
