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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                            shape = RoundedCornerShape(24.dp),
                            color = OneUiAccentSoft,
                        ) {
                            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                                Text("Jumped from Finder", color = OneUiAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    focusedSettingTitle,
                                    color = OneUiText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "A later pass can scroll directly to this section or row, but the prototype now preserves the Finder handoff.",
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
                        summary = "Match Samsung defaults first",
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
                item { SettingsToggleCard("Media page", mediaPageEnabled, onMediaPageChange) }
                if (homeLayoutMode == HomeLayoutMode.HOME_AND_APPS_SCREENS) {
                    item { SettingsToggleCard("Apps button on Home screen", appsButtonEnabled, onAppsButtonChange) }
                }
                item { SettingsToggleCard("App labels", appLabelsEnabled, onAppLabelsChange) }
                item { SettingsToggleCard("Widget labels", widgetLabelsEnabled, onWidgetLabelsChange) }
                item { SettingsToggleCard("Swipe down for notification panel", swipeDownForNotifications, onSwipeDownChange) }
                item { SettingsToggleCard("Lock Home screen layout", lockHomeScreenLayout, onLockHomeScreenLayoutChange) }
                item {
                    SettingsSelectorCard(
                        title = "Folder grid",
                        description = "Controls how many apps appear per folder page. Samsung defaults to 3x4.",
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
                        summary = "Samsung naming and expected defaults",
                        rows = listOf(
                            SettingRowState("Add new apps to Home screen", "On"),
                            SettingRowState("Badge notifications", "Dots and number"),
                            SettingRowState("About Home screen", "One UI Home clone prototype"),
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
        shape = RoundedCornerShape(28.dp),
        color = OneUiSurface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(title, color = OneUiText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(summary, color = OneUiTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            rows.forEachIndexed { index, row ->
                Column {
                    Text(row.title, color = OneUiText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(row.value, color = OneUiAccent, fontSize = 12.sp)
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
        shape = RoundedCornerShape(24.dp),
        color = OneUiSurface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Switch between Samsung's traditional Home and Apps screens setup and the simpler Home screen only layout.",
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
