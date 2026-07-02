package com.oneuihomeclone.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class LauncherFormFactor {
    PHONE_PORTRAIT,
    PHONE_LANDSCAPE,
    FOLDABLE,
    TABLET,
}

internal data class LauncherLayoutContract(
    val formFactor: LauncherFormFactor,
    val isLandscape: Boolean,
    val homeHorizontalPadding: Dp,
    val homeVerticalPadding: Dp,
    val homeMaxWidth: Dp,
    val homeGridColumns: Int,
    val homeGridRows: Int,
    val homeGridHeight: Dp,
    val compactHomeGridHeight: Dp,
    val widgetGridColumns: Int,
    val widgetGridMaxRows: Int,
    val widgetGridCellHeight: Dp,
    val appsGridColumns: Int,
    val appsGridRows: Int,
    val overlayHorizontalPadding: Dp,
    val drawerMaxWidth: Dp,
    val settingsMaxWidth: Dp,
    val folderMaxWidth: Dp,
    val folderGridMaxHeight: Dp,
    val widgetPickerMaxWidth: Dp,
    val editTrayMaxWidth: Dp,
) {
    val homeGridLabel: String = "${homeGridColumns}x$homeGridRows"
    val appsGridLabel: String = "${appsGridColumns}x$appsGridRows"
    val appsPageSize: Int = appsGridColumns * appsGridRows
}

internal fun resolveLauncherLayoutContract(widthDp: Int, heightDp: Int): LauncherLayoutContract {
    val safeWidth = widthDp.coerceAtLeast(1)
    val safeHeight = heightDp.coerceAtLeast(1)
    val isLandscape = safeWidth > safeHeight
    val shortestSide = minOf(safeWidth, safeHeight)
    val formFactor = when {
        shortestSide >= 720 || safeWidth >= 960 -> LauncherFormFactor.TABLET
        shortestSide >= 600 || (safeWidth >= 600 && safeHeight >= 480) -> LauncherFormFactor.FOLDABLE
        isLandscape -> LauncherFormFactor.PHONE_LANDSCAPE
        else -> LauncherFormFactor.PHONE_PORTRAIT
    }

    return when (formFactor) {
        LauncherFormFactor.PHONE_PORTRAIT -> LauncherLayoutContract(
            formFactor = formFactor,
            isLandscape = isLandscape,
            homeHorizontalPadding = 18.dp,
            homeVerticalPadding = 12.dp,
            homeMaxWidth = 440.dp,
            homeGridColumns = 4,
            homeGridRows = 5,
            homeGridHeight = 356.dp,
            compactHomeGridHeight = 304.dp,
            widgetGridColumns = 4,
            widgetGridMaxRows = 6,
            widgetGridCellHeight = 88.dp,
            appsGridColumns = 4,
            appsGridRows = 5,
            overlayHorizontalPadding = 18.dp,
            drawerMaxWidth = 560.dp,
            settingsMaxWidth = 560.dp,
            folderMaxWidth = 520.dp,
            folderGridMaxHeight = 390.dp,
            widgetPickerMaxWidth = 600.dp,
            editTrayMaxWidth = 560.dp,
        )
        LauncherFormFactor.PHONE_LANDSCAPE -> LauncherLayoutContract(
            formFactor = formFactor,
            isLandscape = true,
            homeHorizontalPadding = 28.dp,
            homeVerticalPadding = 8.dp,
            homeMaxWidth = 860.dp,
            homeGridColumns = 5,
            homeGridRows = 3,
            homeGridHeight = 214.dp,
            compactHomeGridHeight = 184.dp,
            widgetGridColumns = 5,
            widgetGridMaxRows = 3,
            widgetGridCellHeight = 72.dp,
            appsGridColumns = 5,
            appsGridRows = 3,
            overlayHorizontalPadding = 28.dp,
            drawerMaxWidth = 680.dp,
            settingsMaxWidth = 620.dp,
            folderMaxWidth = 520.dp,
            folderGridMaxHeight = 238.dp,
            widgetPickerMaxWidth = 680.dp,
            editTrayMaxWidth = 720.dp,
        )
        LauncherFormFactor.FOLDABLE -> LauncherLayoutContract(
            formFactor = formFactor,
            isLandscape = isLandscape,
            homeHorizontalPadding = 28.dp,
            homeVerticalPadding = 16.dp,
            homeMaxWidth = 760.dp,
            homeGridColumns = 5,
            homeGridRows = 5,
            homeGridHeight = 394.dp,
            compactHomeGridHeight = 334.dp,
            widgetGridColumns = 5,
            widgetGridMaxRows = 5,
            widgetGridCellHeight = 86.dp,
            appsGridColumns = 5,
            appsGridRows = 5,
            overlayHorizontalPadding = 24.dp,
            drawerMaxWidth = 680.dp,
            settingsMaxWidth = 640.dp,
            folderMaxWidth = 560.dp,
            folderGridMaxHeight = 390.dp,
            widgetPickerMaxWidth = 680.dp,
            editTrayMaxWidth = 680.dp,
        )
        LauncherFormFactor.TABLET -> LauncherLayoutContract(
            formFactor = formFactor,
            isLandscape = isLandscape,
            homeHorizontalPadding = 36.dp,
            homeVerticalPadding = 18.dp,
            homeMaxWidth = 920.dp,
            homeGridColumns = 6,
            homeGridRows = 5,
            homeGridHeight = 420.dp,
            compactHomeGridHeight = 360.dp,
            widgetGridColumns = 6,
            widgetGridMaxRows = 5,
            widgetGridCellHeight = 86.dp,
            appsGridColumns = 6,
            appsGridRows = 5,
            overlayHorizontalPadding = 28.dp,
            drawerMaxWidth = 760.dp,
            settingsMaxWidth = 720.dp,
            folderMaxWidth = 620.dp,
            folderGridMaxHeight = 420.dp,
            widgetPickerMaxWidth = 760.dp,
            editTrayMaxWidth = 760.dp,
        )
    }
}
