package com.oneuihomeclone.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.R
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiAccentSoft
import com.oneuihomeclone.ui.theme.OneUiBorder
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiSurfaceSoft
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

@Composable
internal fun EditModeTray(
    layoutContract: LauncherLayoutContract,
    pages: List<HomePageModel>,
    pageIndex: Int,
    mediaPageEnabled: Boolean,
    defaultHomePageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onToggleMediaPage: () -> Unit,
    onAddPage: () -> Unit,
    onMoveCurrentPageLeft: () -> Unit,
    onMoveCurrentPageRight: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    currentWidgetCount: Int,
    onRemoveLastWidget: () -> Unit,
    onRemoveCurrentPage: () -> Unit,
    onSetCurrentPageAsDefault: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val selectedHomePageIndex = homePageIndexFromVisual(pageIndex, mediaPageEnabled)
    val selectedIsMedia = mediaPageEnabled && pageIndex == 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x20000000)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = layoutContract.editTrayMaxWidth)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            shape = OneUiPanelShape,
            color = OneUiSurface,
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.edit_home_screen), color = OneUiText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    SettingsCapsule(label = stringResource(R.string.action_done), onClick = onClose, accent = false)
                }
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditTile(
                            title = stringResource(R.string.edit_wallpapers_style),
                            icon = Icons.Default.Image,
                            modifier = Modifier.weight(1f),
                            enabled = false,
                            supportingText = stringResource(R.string.state_unavailable),
                        )
                        EditTile(stringResource(R.string.widgets_title), Icons.Default.Widgets, modifier = Modifier.weight(1f), onClick = onOpenWidgetPicker)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EditTile(stringResource(R.string.edit_home_settings), Icons.Default.Settings, modifier = Modifier.weight(1f), onClick = onOpenSettings)
                        EditTile(
                            title = stringResource(R.string.edit_page_manager),
                            icon = Icons.Default.Tune,
                            modifier = Modifier.weight(1f),
                            enabled = false,
                            supportingText = stringResource(R.string.edit_controls_below),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                PageManagerPanel(
                    pages = pages,
                    pageIndex = pageIndex,
                    mediaPageEnabled = mediaPageEnabled,
                    defaultHomePageIndex = defaultHomePageIndex,
                    onSelectPage = onSelectPage,
                    onAddPage = onAddPage,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsCapsule(
                        label = if (mediaPageEnabled) stringResource(R.string.edit_hide_media_page) else stringResource(R.string.edit_show_media_page),
                        onClick = onToggleMediaPage,
                        accent = mediaPageEnabled,
                    )
                    if (!selectedIsMedia) {
                        SettingsCapsule(
                            label = if (selectedHomePageIndex == defaultHomePageIndex) stringResource(R.string.edit_default_home) else stringResource(R.string.edit_set_as_home),
                            onClick = onSetCurrentPageAsDefault,
                            accent = selectedHomePageIndex != defaultHomePageIndex,
                        )
                        if (selectedHomePageIndex != null && selectedHomePageIndex > 0) {
                            SettingsCapsule(
                                label = stringResource(R.string.edit_move_left),
                                onClick = onMoveCurrentPageLeft,
                                accent = false,
                            )
                        }
                        if (selectedHomePageIndex != null && selectedHomePageIndex < pages.lastIndex) {
                            SettingsCapsule(
                                label = stringResource(R.string.edit_move_right),
                                onClick = onMoveCurrentPageRight,
                                accent = false,
                            )
                        }
                        if (currentWidgetCount > 0) {
                            SettingsCapsule(
                                label = stringResource(R.string.edit_remove_widget),
                                onClick = onRemoveLastWidget,
                                accent = false,
                            )
                        }
                        if (pages.size > 1) {
                            SettingsCapsule(
                                label = stringResource(R.string.edit_remove_page),
                                onClick = onRemoveCurrentPage,
                                accent = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageManagerPanel(
    pages: List<HomePageModel>,
    pageIndex: Int,
    mediaPageEnabled: Boolean,
    defaultHomePageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
) {
    Surface(
        shape = OneUiPanelShape,
        color = OneUiSurfaceSoft,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text(stringResource(R.string.edit_pages), color = OneUiText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.edit_pages_summary),
                color = OneUiTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (mediaPageEnabled) {
                    PagePreviewTile(
                        title = stringResource(R.string.edit_media_preview_title),
                        subtitle = stringResource(R.string.home_media_hub),
                        selected = pageIndex == 0,
                        onClick = { onSelectPage(0) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(OneUiPanelShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFEEF4FF), Color(0xFFDDEBFF)),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = OneUiAccent)
                        }
                    }
                }

                pages.forEachIndexed { index, page ->
                    PagePreviewTile(
                        title = page.label,
                        subtitle = if (index == defaultHomePageIndex) stringResource(R.string.edit_default_home) else stringResource(R.string.edit_tap_to_preview),
                        selected = pageIndex == visualIndexForHomePage(index, mediaPageEnabled),
                        onClick = { onSelectPage(visualIndexForHomePage(index, mediaPageEnabled)) },
                    ) {
                        HomePagePreview(
                            items = page.items.take(4),
                            isDefaultHome = index == defaultHomePageIndex,
                        )
                    }
                }

                PagePreviewTile(
                    title = stringResource(R.string.edit_new_page),
                    subtitle = stringResource(R.string.action_add),
                    selected = false,
                    onClick = onAddPage,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(OneUiPanelShape)
                            .background(OneUiAccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = OneUiAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun PagePreviewTile(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.width(132.dp),
        shape = OneUiPanelShape,
        color = if (selected) Color.White else OneUiSurface,
        border = if (selected) BorderStroke(1.dp, OneUiAccent.copy(alpha = 0.32f)) else null,
        shadowElevation = if (selected) 4.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier
                .semantics {
                    contentDescription = title
                }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
            ) {
                preview()
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = OneUiText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = if (selected) OneUiAccent else OneUiTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomePagePreview(
    items: List<HomeGridItemModel>,
    isDefaultHome: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = OneUiPanelShape,
        color = Color(0xFFF3F6FB),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "8:42", color = OneUiTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (isDefaultHome) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = OneUiAccent, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(modifier = Modifier.fillMaxWidth(), shape = OneUiPanelShape, color = Color.White) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(stringResource(R.string.home_widget_preview), color = OneUiTextSecondary, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(2).forEach { item -> PreviewItem(item = item, modifier = Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.drop(2).take(2).forEach { item -> PreviewItem(item = item, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PreviewItem(item: HomeGridItemModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when (item) {
            is AppItemModel -> {
                if (item.app.icon != null) {
                    Image(bitmap = item.app.icon, contentDescription = item.app.accessibilityLabel(), modifier = Modifier.size(22.dp), contentScale = ContentScale.Fit)
                } else {
                    Box(modifier = Modifier.size(22.dp).clip(OneUiMicroShape).background(item.app.color))
                }
            }
            is FolderModel -> {
                Surface(modifier = Modifier.size(22.dp), shape = OneUiMicroShape, color = Color.White, border = BorderStroke(1.dp, OneUiBorder)) {
                    Column(Modifier.padding(3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(2) { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                item.apps.drop(row * 2).take(2).forEach { app ->
                                    Box(modifier = Modifier.weight(1f).height(6.dp), contentAlignment = Alignment.Center) {
                                        if (app.icon != null) {
                                            Image(bitmap = app.icon, contentDescription = app.accessibilityLabel(), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize().clip(OneUiMicroShape).background(app.color))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth(0.8f).height(3.dp).clip(OneUiMicroShape).background(Color(0x22000000)))
    }
}

@Composable
internal fun EditTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.widthIn(min = 150.dp),
        shape = OneUiPanelShape,
        color = if (enabled) OneUiSurfaceSoft else OneUiSurfaceSoft.copy(alpha = 0.76f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = title }
                .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(OneUiIconShape)
                    .background(if (enabled) OneUiAccentSoft else OneUiAccentSoft.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (enabled) OneUiAccent else OneUiTextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = if (enabled) OneUiText else OneUiTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                supportingText?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, color = OneUiTextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}
