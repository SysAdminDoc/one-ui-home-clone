package com.oneuihomeclone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.R
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiBackground
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary
import com.oneuihomeclone.widgets.PreviewSource

@Composable
internal fun WidgetPickerOverlay(
    categories: List<String>,
    selectedCategory: String,
    widgets: List<WidgetTemplateModel>,
    targetPageLabel: String,
    onSelectCategory: (String) -> Unit,
    onAddWidget: (WidgetTemplateModel) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUiBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.widgets_title), color = OneUiText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SettingsCapsule(label = stringResource(R.string.action_close), onClick = onClose, accent = false)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.widgets_intro),
                color = OneUiTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.widgets_adding_to, targetPageLabel),
                color = OneUiAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    SettingsCapsule(
                        label = category,
                        onClick = { onSelectCategory(category) },
                        accent = category == selectedCategory,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                if (widgets.isEmpty()) {
                    item {
                        WidgetPickerEmptyState(selectedCategory = selectedCategory)
                    }
                } else {
                    lazyItems(widgets) { widget ->
                        WidgetTemplateCard(widget = widget, onAddWidget = onAddWidget)
                    }
                }
            }
        }
    }
}

@Composable
internal fun WidgetTemplateCard(
    widget: WidgetTemplateModel,
    onAddWidget: (WidgetTemplateModel) -> Unit,
) {
    Surface(
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(widget.title, color = OneUiText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(widget.summary, color = OneUiTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    SettingsCapsule(label = widget.span, accent = false, enabled = false)
                    Spacer(Modifier.height(8.dp))
                    SettingsCapsule(label = stringResource(R.string.action_add), onClick = { onAddWidget(widget) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (widget.spanY == 1) 86.dp else 126.dp),
                shape = OneUiPanelShape,
                color = Color.White,
                border = BorderStroke(1.dp, widget.accent.copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(widget.accent.copy(alpha = 0.18f), Color.White),
                                start = Offset.Zero,
                                end = Offset(900f, 400f),
                            ),
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    if (widget.previewSource == PreviewSource.Empty && widget.providerInfo == null) {
                        Column {
                            Text(widget.category, color = widget.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(widget.title, color = OneUiText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            SyntheticWidgetPreview(
                                widget = widget,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (widget.spanY == 1) 30.dp else 42.dp),
                                compact = false,
                            )
                        }
                    } else {
                        WidgetPreviewPane(
                            widget = widget,
                            modifier = Modifier.fillMaxSize(),
                            compact = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerEmptyState(selectedCategory: String) {
    Surface(
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            Text(stringResource(R.string.widgets_empty_title, selectedCategory), color = OneUiText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.widgets_empty_summary),
                color = OneUiTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}
