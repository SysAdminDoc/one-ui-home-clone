package com.oneuihomeclone.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oneuihomeclone.R

@Composable
internal fun HomeLayoutMode.localizedTitle(): String = stringResource(
    when (this) {
        HomeLayoutMode.HOME_AND_APPS_SCREENS -> R.string.mode_home_and_apps_screens
        HomeLayoutMode.HOME_SCREEN_ONLY -> R.string.mode_home_screen_only
    },
)

@Composable
internal fun DrawerSortMode.localizedTitle(): String = stringResource(
    when (this) {
        DrawerSortMode.CUSTOM_ORDER -> R.string.drawer_sort_custom_order
        DrawerSortMode.ALPHABETICAL -> R.string.drawer_sort_alphabetical
    },
)

@Composable
internal fun MotionPresetMode.localizedTitle(): String = stringResource(
    when (this) {
        MotionPresetMode.STANDARD -> R.string.motion_standard
        MotionPresetMode.REDUCED -> R.string.motion_reduced
    },
)

@Composable
internal fun FolderGridMode.localizedTitle(): String = stringResource(
    when (this) {
        FolderGridMode.GRID_3X4 -> R.string.folder_grid_3x4
        FolderGridMode.GRID_4X4 -> R.string.folder_grid_4x4
        FolderGridMode.GRID_5X5 -> R.string.folder_grid_5x5
    },
)
