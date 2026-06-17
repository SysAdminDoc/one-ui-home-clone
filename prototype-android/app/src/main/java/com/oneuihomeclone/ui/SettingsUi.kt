package com.oneuihomeclone.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiAccentSoft
import com.oneuihomeclone.ui.theme.OneUiBackground
import com.oneuihomeclone.ui.theme.OneUiBorder
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

@Composable
internal fun SettingsOverlay(
    mediaPageEnabled: Boolean,
    appsButtonEnabled: Boolean,
    appLabelsEnabled: Boolean,
    widgetLabelsEnabled: Boolean,
    swipeDownForNotifications: Boolean,
    homeLayoutMode: HomeLayoutMode,
    lockHomeScreenLayout: Boolean,
    motionPreset: MotionPresetMode,
    folderGrid: FolderGridMode,
    defaultHomePageLabel: String,
    homePageCount: Int,
    appsScreenSortTitle: String,
    hiddenAppCount: Int,
    focusedSettingTitle: String?,
    onClose: () -> Unit,
    onMediaPageChange: (Boolean) -> Unit,
    onAppsButtonChange: (Boolean) -> Unit,
    onAppLabelsChange: (Boolean) -> Unit,
    onWidgetLabelsChange: (Boolean) -> Unit,
    onSwipeDownChange: (Boolean) -> Unit,
    onHomeLayoutModeChange: (HomeLayoutMode) -> Unit,
    onLockHomeScreenLayoutChange: (Boolean) -> Unit,
    onMotionPresetChange: (MotionPresetMode) -> Unit,
    onFolderGridChange: (FolderGridMode) -> Unit,
) {
    val layoutRows = remember(defaultHomePageLabel, homePageCount, homeLayoutMode, appsScreenSortTitle, hiddenAppCount, folderGrid) {
        listOf(
            SettingRowState("Home screen layout", homeLayoutMode.title),
            SettingRowState("Home screen grid", "4x6"),
            SettingRowState("Apps screen grid", "4x6"),
            SettingRowState(
                "Apps screen sort",
                if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) "Unavailable in Home screen only mode" else appsScreenSortTitle,
            ),
            SettingRowState("Hide apps", if (hiddenAppCount == 0) "None" else "$hiddenAppCount hidden"),
            SettingRowState("Folder grid", folderGrid.title),
            SettingRowState("Default home page", defaultHomePageLabel),
            SettingRowState("Visible pages", homePageCount.toString()),
        )
    }

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
                Text("Home screen settings", color = OneUiText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SettingsCapsule(label = "Close", onClick = onClose, accent = false)
            }
            Spacer(Modifier.height(18.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
            ) {
                if (!focusedSettingTitle.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = OneUiPanelShape,
                            color = OneUiAccentSoft,
                        ) {
                            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                                Text("Finder result", color = OneUiAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    focusedSettingTitle,
                                    color = OneUiText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Review the matching setting below.",
                                    color = OneUiTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                    }
                }
                item {
                    SettingsSection(
                        title = "Layout",
                        summary = "Default launcher structure and page behavior",
                        rows = layoutRows,
                    )
                }
                item {
                    SettingsModeCard(
                        title = "Home screen layout",
                        selectedMode = homeLayoutMode,
                        onSelectMode = onHomeLayoutModeChange,
                    )
                }
                item {
                    SettingsToggleCard(
                        title = "Media page",
                        checked = mediaPageEnabled,
                        onCheckedChange = onMediaPageChange,
                        summary = "Show a left-side page for news, media, and daily cards.",
                    )
                }
                if (homeLayoutMode == HomeLayoutMode.HOME_AND_APPS_SCREENS) {
                    item {
                        SettingsToggleCard(
                            title = "Apps button on Home screen",
                            checked = appsButtonEnabled,
                            onCheckedChange = onAppsButtonChange,
                            summary = "Keep an explicit Apps entry in the dock.",
                        )
                    }
                }
                item {
                    SettingsToggleCard(
                        "App labels",
                        appLabelsEnabled,
                        onAppLabelsChange,
                        "Show names under Home, dock, and Apps screen icons.",
                    )
                }
                item {
                    SettingsToggleCard(
                        "Widget labels",
                        widgetLabelsEnabled,
                        onWidgetLabelsChange,
                        "Show provider labels on compact widget previews.",
                    )
                }
                item {
                    SettingsToggleCard(
                        "Swipe down for notification panel",
                        swipeDownForNotifications,
                        onSwipeDownChange,
                        "Open the notification shade from empty Home screen space.",
                    )
                }
                item {
                    SettingsToggleCard(
                        "Lock Home screen layout",
                        lockHomeScreenLayout,
                        onLockHomeScreenLayoutChange,
                        "Prevent accidental page, folder, and widget changes.",
                    )
                }
                item {
                    SettingsSelectorCard(
                        title = "Folder grid",
                        description = "Controls how many apps appear per folder page. The default is 3x4.",
                        entries = FolderGridMode.entries,
                        selectedEntry = folderGrid,
                        labelOf = { it.title },
                        onSelect = onFolderGridChange,
                    )
                }
                item {
                    SettingsSelectorCard(
                        title = "Motion",
                        description = "Standard uses spring overshoot for One UI feel. Reduced softens transitions for accessibility.",
                        entries = MotionPresetMode.entries,
                        selectedEntry = motionPreset,
                        labelOf = { it.title },
                        onSelect = onMotionPresetChange,
                    )
                }
                item {
                    SettingsSection(
                        title = "Behavior",
                        summary = "Expected launcher defaults",
                        rows = listOf(
                            SettingRowState("Add new apps to Home screen", "On"),
                            SettingRowState("Badge notifications", "Dots and number"),
                            SettingRowState("About Home screen", "One UI Home Clone 0.2.1"),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    summary: String,
    rows: List<SettingRowState>,
) {
    Surface(
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(title, color = OneUiText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(summary, color = OneUiTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        row.title,
                        color = OneUiText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        row.value,
                        color = OneUiAccent,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = 180.dp),
                    )
                }
                if (index != rows.lastIndex) {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(OneUiBorder))
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsModeCard(
    title: String,
    selectedMode: HomeLayoutMode,
    onSelectMode: (HomeLayoutMode) -> Unit,
) {
    Surface(
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Switch between the traditional Home and Apps screens setup and the simpler Home screen only layout.",
                color = OneUiTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeLayoutMode.entries.forEach { mode ->
                    SettingsCapsule(
                        label = mode.title,
                        onClick = { onSelectMode(mode) },
                        accent = selectedMode == mode,
                    )
                }
            }
        }
    }
}
