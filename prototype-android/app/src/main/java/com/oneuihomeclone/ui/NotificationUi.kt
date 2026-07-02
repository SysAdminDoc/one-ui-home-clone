package com.oneuihomeclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.R
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiAccentSoft
import com.oneuihomeclone.ui.theme.OneUiBackground
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiSurfaceSoft
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

@Composable
internal fun NotificationShadeOverlay(
    clock: StatusClock,
    onClose: () -> Unit,
) {
    val notifications = listOf(
        NotificationCardModel(
            title = stringResource(R.string.notification_home_ready_title),
            summary = stringResource(R.string.notification_home_ready_summary),
            timestamp = stringResource(R.string.notification_timestamp_now),
        ),
        NotificationCardModel(
            title = stringResource(R.string.notification_finder_active_title),
            summary = stringResource(R.string.notification_finder_active_summary),
            timestamp = stringResource(R.string.notification_timestamp_two_min),
        ),
        NotificationCardModel(
            title = stringResource(R.string.notification_gesture_active_title),
            summary = stringResource(R.string.notification_gesture_active_summary),
            timestamp = stringResource(R.string.notification_timestamp_five_min),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUiBackground.copy(alpha = 0.98f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(clock.timeText, color = OneUiText, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(clock.fullDateText, color = OneUiTextSecondary, fontSize = 13.sp)
                }
                SettingsCapsule(label = stringResource(R.string.action_close), onClick = onClose, accent = false)
            }
            Spacer(Modifier.height(18.dp))
            Surface(
                shape = OneUiPanelShape,
                color = OneUiSurfaceSoft,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(OneUiIconShape)
                            .background(OneUiAccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = OneUiAccent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.notification_panel_title), color = OneUiText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.notification_panel_summary),
                            color = OneUiTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                lazyItems(notifications) { notification ->
                    Surface(
                        shape = OneUiPanelShape,
                        color = OneUiSurface,
                        shadowElevation = 1.dp,
                    ) {
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    notification.title,
                                    color = OneUiText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(notification.timestamp, color = OneUiTextSecondary, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                notification.summary,
                                color = OneUiTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
