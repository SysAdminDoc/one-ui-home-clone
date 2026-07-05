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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.DefaultLauncherState
import com.oneuihomeclone.R
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiAccentSoft
import com.oneuihomeclone.ui.theme.OneUiBackground
import com.oneuihomeclone.ui.theme.OneUiBorder
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

@Composable
internal fun SettingsOverlay(
    layoutContract: LauncherLayoutContract,
    mediaPageEnabled: Boolean,
    appsButtonEnabled: Boolean,
    appLabelsEnabled: Boolean,
    widgetLabelsEnabled: Boolean,
    swipeDownForNotifications: Boolean,
    addNewAppsToHomeScreen: Boolean,
    notificationBadgeMode: NotificationBadgeMode,
    notificationBadgePermissionGranted: Boolean,
    notificationBadgeActiveAppCount: Int,
    notificationBadgeActiveCount: Int,
    homeLayoutMode: HomeLayoutMode,
    lockHomeScreenLayout: Boolean,
    motionPreset: MotionPresetMode,
    folderGrid: FolderGridMode,
    defaultHomePageLabel: String,
    homePageCount: Int,
    appsScreenSortTitle: String,
    hiddenAppCount: Int,
    boundWidgetCount: Int,
    backupFileName: String,
    diagnosticsFileName: String,
    defaultLauncherState: DefaultLauncherState,
    focusedSettingTitle: String?,
    onClose: () -> Unit,
    onMediaPageChange: (Boolean) -> Unit,
    onAppsButtonChange: (Boolean) -> Unit,
    onAppLabelsChange: (Boolean) -> Unit,
    onWidgetLabelsChange: (Boolean) -> Unit,
    onSwipeDownChange: (Boolean) -> Unit,
    onAddNewAppsToHomeScreenChange: (Boolean) -> Unit,
    onNotificationBadgeModeChange: (NotificationBadgeMode) -> Unit,
    onOpenNotificationBadgeSettings: () -> Unit,
    onHomeLayoutModeChange: (HomeLayoutMode) -> Unit,
    onLockHomeScreenLayoutChange: (Boolean) -> Unit,
    onMotionPresetChange: (MotionPresetMode) -> Unit,
    onFolderGridChange: (FolderGridMode) -> Unit,
    onResetWidgets: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportDiagnostics: () -> Unit,
    onOpenDefaultLauncherSettings: () -> Unit,
) {
    val homeScreenLayoutLabel = stringResource(R.string.settings_home_screen_layout)
    val homeScreenGridLabel = stringResource(R.string.settings_home_screen_grid)
    val appsScreenGridLabel = stringResource(R.string.settings_apps_screen_grid)
    val appsScreenSortLabel = stringResource(R.string.settings_apps_screen_sort)
    val appsScreenSortUnavailable = stringResource(R.string.settings_sort_unavailable_home_only)
    val hideAppsLabel = stringResource(R.string.settings_hide_apps)
    val hiddenAppsValue = if (hiddenAppCount == 0) {
        stringResource(R.string.settings_value_none)
    } else {
        stringResource(R.string.settings_value_hidden_count, hiddenAppCount)
    }
    val folderGridLabel = stringResource(R.string.settings_folder_grid)
    val homeLayoutModeTitle = homeLayoutMode.localizedTitle()
    val folderGridTitle = folderGrid.localizedTitle()
    val notificationBadgeModeTitle = notificationBadgeMode.localizedTitle()
    val homeGridLabel = layoutContract.homeGridLabel
    val appsGridLabel = layoutContract.appsGridLabel
    val defaultHomePageLabelText = stringResource(R.string.settings_default_home_page)
    val visiblePagesLabel = stringResource(R.string.settings_visible_pages)
    val resetWidgetsDescription = when {
        boundWidgetCount == 0 -> stringResource(R.string.settings_reset_widgets_empty)
        boundWidgetCount == 1 -> stringResource(R.string.settings_reset_widgets_one)
        else -> stringResource(R.string.settings_reset_widgets_many, boundWidgetCount)
    }
    val defaultLauncherDescription = if (defaultLauncherState.isDefaultLauncher) {
        stringResource(R.string.settings_default_launcher_current)
    } else {
        stringResource(R.string.settings_default_launcher_not_current)
    }
    val notificationBadgeAccessDescription = when {
        notificationBadgeMode == NotificationBadgeMode.OFF -> stringResource(R.string.settings_badge_access_off_summary)
        notificationBadgePermissionGranted -> {
            val base = stringResource(R.string.settings_badge_access_granted_summary)
            if (notificationBadgeActiveCount > 0) {
                "$base\n${stringResource(R.string.settings_badge_count_summary, notificationBadgeActiveCount, notificationBadgeActiveAppCount)}"
            } else {
                base
            }
        }
        else -> stringResource(R.string.settings_badge_access_denied_summary)
    }
    val behaviorRows = listOf(
        SettingRowState(
            stringResource(R.string.settings_add_new_apps),
            stringResource(if (addNewAppsToHomeScreen) R.string.settings_value_on else R.string.state_off),
        ),
        SettingRowState(stringResource(R.string.settings_badge_notifications), notificationBadgeModeTitle),
        SettingRowState(stringResource(R.string.settings_about_home_screen), stringResource(R.string.settings_about_version)),
    )
    val privacyPoints = listOf(
        stringResource(R.string.settings_privacy_network),
        stringResource(R.string.settings_privacy_apps),
        stringResource(R.string.settings_privacy_search_layout),
        stringResource(R.string.settings_privacy_widgets_wallpaper),
        stringResource(R.string.settings_privacy_exports),
    )
    val layoutRows = remember(
        defaultHomePageLabel,
        homePageCount,
        homeLayoutMode,
        appsScreenSortTitle,
        folderGrid,
        homeScreenLayoutLabel,
        homeLayoutModeTitle,
        homeScreenGridLabel,
        appsScreenGridLabel,
        appsScreenSortLabel,
        appsScreenSortUnavailable,
        hideAppsLabel,
        hiddenAppsValue,
        homeGridLabel,
        appsGridLabel,
        folderGridLabel,
        folderGridTitle,
        defaultHomePageLabelText,
        visiblePagesLabel,
    ) {
        listOf(
            SettingRowState(homeScreenLayoutLabel, homeLayoutModeTitle),
            SettingRowState(homeScreenGridLabel, homeGridLabel),
            SettingRowState(appsScreenGridLabel, appsGridLabel),
            SettingRowState(
                appsScreenSortLabel,
                if (homeLayoutMode == HomeLayoutMode.HOME_SCREEN_ONLY) appsScreenSortUnavailable else appsScreenSortTitle,
            ),
            SettingRowState(hideAppsLabel, hiddenAppsValue),
            SettingRowState(folderGridLabel, folderGridTitle),
            SettingRowState(defaultHomePageLabelText, defaultHomePageLabel),
            SettingRowState(visiblePagesLabel, homePageCount.toString()),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUiBackground),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = layoutContract.settingsMaxWidth)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = layoutContract.overlayHorizontalPadding, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_title_home_screen), color = OneUiText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SettingsCapsule(label = stringResource(R.string.action_close), onClick = onClose, accent = false)
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
                                Text(stringResource(R.string.settings_finder_result), color = OneUiAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    focusedSettingTitle,
                                    color = OneUiText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.settings_finder_result_review),
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
                        title = stringResource(R.string.settings_section_layout),
                        summary = stringResource(R.string.settings_summary_layout),
                        rows = layoutRows,
                    )
                }
                item {
                    SettingsModeCard(
                        title = stringResource(R.string.settings_home_screen_layout),
                        selectedMode = homeLayoutMode,
                        onSelectMode = onHomeLayoutModeChange,
                    )
                }
                item {
                    SettingsToggleCard(
                        title = stringResource(R.string.settings_media_page),
                        checked = mediaPageEnabled,
                        onCheckedChange = onMediaPageChange,
                        summary = stringResource(R.string.settings_media_page_summary),
                    )
                }
                if (homeLayoutMode == HomeLayoutMode.HOME_AND_APPS_SCREENS) {
                    item {
                        SettingsToggleCard(
                            title = stringResource(R.string.settings_apps_button),
                            checked = appsButtonEnabled,
                            onCheckedChange = onAppsButtonChange,
                            summary = stringResource(R.string.settings_apps_button_summary),
                        )
                    }
                }
                item {
                    SettingsToggleCard(
                        stringResource(R.string.settings_app_labels),
                        appLabelsEnabled,
                        onAppLabelsChange,
                        stringResource(R.string.settings_app_labels_summary),
                    )
                }
                item {
                    SettingsToggleCard(
                        stringResource(R.string.settings_widget_labels),
                        widgetLabelsEnabled,
                        onWidgetLabelsChange,
                        stringResource(R.string.settings_widget_labels_summary),
                    )
                }
                item {
                    SettingsToggleCard(
                        stringResource(R.string.settings_swipe_notifications),
                        swipeDownForNotifications,
                        onSwipeDownChange,
                        stringResource(R.string.settings_swipe_notifications_summary),
                    )
                }
                item {
                    SettingsToggleCard(
                        stringResource(R.string.settings_add_new_apps),
                        addNewAppsToHomeScreen,
                        onAddNewAppsToHomeScreenChange,
                        stringResource(R.string.settings_add_new_apps_summary),
                    )
                }
                item {
                    SettingsSelectorCard(
                        title = stringResource(R.string.settings_badge_notifications),
                        description = stringResource(R.string.settings_badge_notifications_description),
                        entries = NotificationBadgeMode.entries,
                        selectedEntry = notificationBadgeMode,
                        labelOf = { it.localizedTitle() },
                        onSelect = onNotificationBadgeModeChange,
                    )
                }
                item {
                    SettingsActionCard(
                        title = stringResource(R.string.settings_badge_access),
                        description = notificationBadgeAccessDescription,
                        actionLabel = stringResource(R.string.action_open_settings),
                        onClick = onOpenNotificationBadgeSettings,
                    )
                }
                item {
                    SettingsToggleCard(
                        stringResource(R.string.settings_lock_layout),
                        lockHomeScreenLayout,
                        onLockHomeScreenLayoutChange,
                        stringResource(R.string.settings_lock_layout_summary),
                    )
                }
                item {
                    SettingsSelectorCard(
                        title = stringResource(R.string.settings_folder_grid),
                        description = stringResource(R.string.settings_folder_grid_description),
                        entries = FolderGridMode.entries,
                        selectedEntry = folderGrid,
                        labelOf = { it.localizedTitle() },
                        onSelect = onFolderGridChange,
                    )
                }
                item {
                    SettingsSelectorCard(
                        title = stringResource(R.string.settings_motion),
                        description = stringResource(R.string.settings_motion_description),
                        entries = MotionPresetMode.entries,
                        selectedEntry = motionPreset,
                        labelOf = { it.localizedTitle() },
                        onSelect = onMotionPresetChange,
                    )
                }
                item {
                    SettingsActionCard(
                        title = stringResource(R.string.settings_reset_widgets),
                        description = resetWidgetsDescription,
                        actionLabel = stringResource(R.string.action_reset),
                        onClick = onResetWidgets,
                    )
                }
                item {
                    SettingsActionCard(
                        title = stringResource(R.string.settings_export_backup),
                        description = stringResource(R.string.settings_export_backup_summary, backupFileName),
                        actionLabel = stringResource(R.string.action_export),
                        onClick = onExportBackup,
                    )
                }
                item {
                    SettingsActionCard(
                        title = stringResource(R.string.settings_restore_backup),
                        description = stringResource(R.string.settings_restore_backup_summary, backupFileName),
                        actionLabel = stringResource(R.string.action_import),
                        onClick = onImportBackup,
                    )
                }
                item {
                    SettingsActionCard(
                        title = stringResource(R.string.settings_export_diagnostics),
                        description = stringResource(R.string.settings_export_diagnostics_summary, diagnosticsFileName),
                        actionLabel = stringResource(R.string.action_export),
                        onClick = onExportDiagnostics,
                    )
                }
                item {
                    SettingsPrivacyCard(
                        title = stringResource(R.string.settings_privacy_title),
                        summary = stringResource(R.string.settings_privacy_summary),
                        points = privacyPoints,
                    )
                }
                item {
                    SettingsActionCard(
                        title = stringResource(R.string.settings_default_launcher),
                        description = defaultLauncherDescription,
                        actionLabel = stringResource(R.string.default_launcher_prompt_action),
                        onClick = onOpenDefaultLauncherSettings,
                        enabled = defaultLauncherState.canOpenSettings,
                    )
                }
                item {
                    SettingsSection(
                        title = stringResource(R.string.settings_section_behavior),
                        summary = stringResource(R.string.settings_summary_behavior),
                        rows = behaviorRows,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPrivacyCard(
    title: String,
    summary: String,
    points: List<String>,
) {
    Surface(
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(title, color = OneUiText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(summary, color = OneUiTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(14.dp))
            points.forEach { point ->
                Text(
                    "- $point",
                    color = OneUiText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        shape = OneUiPanelShape,
        color = OneUiSurface,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(description, color = OneUiTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
            SettingsCapsule(label = actionLabel, onClick = onClick, accent = true, enabled = enabled)
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
                val rowDescription = stringResource(R.string.a11y_setting_row, row.title, row.value)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = rowDescription },
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
                stringResource(R.string.settings_mode_description),
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
                        label = mode.localizedTitle(),
                        onClick = { onSelectMode(mode) },
                        accent = selectedMode == mode,
                    )
                }
            }
        }
    }
}
