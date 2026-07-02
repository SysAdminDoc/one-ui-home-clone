package com.oneuihomeclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.R
import com.oneuihomeclone.ui.theme.OneUiBackground
import com.oneuihomeclone.ui.theme.OneUiCard
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

@Composable
internal fun FolderOverlay(
    folder: FolderModel,
    appLabelsEnabled: Boolean,
    folderGrid: FolderGridMode,
    onOpenApp: (CloneApp) -> Unit,
    onRenameFolder: (String) -> Unit,
    onClose: () -> Unit,
) {
    var titleDraft by remember(folder.title) { mutableStateOf(folder.title) }
    val sanitizedTitleDraft = titleDraft.trim()
    val hasTitleChanges = sanitizedTitleDraft.isNotBlank() && sanitizedTitleDraft != folder.title

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClose)
            .background(Color(0x32000000)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
            shape = OneUiPanelShape,
            color = OneUiCard.copy(alpha = 0.98f),
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = titleDraft,
                            onValueChange = { titleDraft = it.take(24) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = OneUiText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            placeholder = {
                                Text(stringResource(R.string.folder_name), color = OneUiTextSecondary)
                            },
                            shape = OneUiControlShape,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(folder.summary, color = OneUiTextSecondary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    SettingsCapsule(label = stringResource(R.string.action_close), onClick = onClose, accent = false)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsCapsule(
                        label = if (hasTitleChanges) stringResource(R.string.folder_save_name) else stringResource(R.string.folder_name),
                        onClick = {
                            val updatedTitle = sanitizedTitleDraft.ifBlank { folder.title }
                            if (updatedTitle != folder.title) {
                                onRenameFolder(updatedTitle)
                            }
                            titleDraft = updatedTitle
                        },
                        accent = hasTitleChanges,
                    )
                    DrawerPill(stringResource(R.string.folder_label))
                }
                Spacer(Modifier.height(18.dp))
                val folderGridHeight = (folderGrid.rows * 78).dp
                LazyVerticalGrid(
                    columns = GridCells.Fixed(folderGrid.columns),
                    modifier = Modifier.height(folderGridHeight),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(folder.apps) { app ->
                        Column(
                            modifier = Modifier
                                .semantics { contentDescription = app.name }
                                .clickable(role = Role.Button) { onOpenApp(app) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppIconBubble(app = app, size = 60.dp)
                            if (appLabelsEnabled) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = app.name,
                                    color = OneUiText,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.folder_summary),
                    color = OneUiTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
internal fun HideAppsOverlay(
    apps: List<CloneApp>,
    hiddenAppIds: Set<String>,
    onToggleHidden: (CloneApp) -> Unit,
    onClose: () -> Unit,
) {
    val hiddenApps = remember(apps, hiddenAppIds) { apps.filter { it.id in hiddenAppIds } }
    val visibleApps = remember(apps, hiddenAppIds) { apps.filterNot { it.id in hiddenAppIds } }

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
                Text(stringResource(R.string.settings_hide_apps), color = OneUiText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SettingsCapsule(label = stringResource(R.string.action_close), onClick = onClose, accent = false)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.hide_apps_summary),
                color = OneUiTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(16.dp))
            if (hiddenApps.isNotEmpty()) {
                FinderSectionHeader(stringResource(R.string.hide_apps_hidden_now))
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    hiddenApps.forEach { app ->
                        SettingsCapsule(
                            label = app.name,
                            onClick = { onToggleHidden(app) },
                            accent = true,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            FinderSectionHeader(stringResource(R.string.hide_apps_tap_to_toggle))
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                lazyItems(visibleApps + hiddenApps) { app ->
                    val appState = if (app.id in hiddenAppIds) {
                        stringResource(R.string.hide_apps_hidden_state)
                    } else {
                        stringResource(R.string.hide_apps_visible_state)
                    }
                    val appDescription = stringResource(R.string.a11y_hide_app_state, app.name, appState)
                    Surface(
                        shape = OneUiPanelShape,
                        color = OneUiSurface,
                        shadowElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = appDescription }
                                .clickable(role = Role.Button) { onToggleHidden(app) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIconBubble(app = app, size = 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.name, color = OneUiText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    appState,
                                    color = OneUiTextSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                            SettingsCapsule(
                                label = if (app.id in hiddenAppIds) stringResource(R.string.action_restore) else stringResource(R.string.action_hide),
                                onClick = { onToggleHidden(app) },
                                accent = app.id !in hiddenAppIds,
                            )
                        }
                    }
                }
            }
        }
    }
}
