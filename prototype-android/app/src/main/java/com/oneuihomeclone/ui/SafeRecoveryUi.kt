package com.oneuihomeclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneuihomeclone.PreviousCrashSummary
import com.oneuihomeclone.R
import com.oneuihomeclone.ui.theme.OneUiAccent
import com.oneuihomeclone.ui.theme.OneUiBackground
import com.oneuihomeclone.ui.theme.OneUiCard
import com.oneuihomeclone.ui.theme.OneUiSurface
import com.oneuihomeclone.ui.theme.OneUiText
import com.oneuihomeclone.ui.theme.OneUiTextSecondary

@Composable
internal fun SafeRecoveryCheckingScreen() {
    SafeRecoveryScaffold {
        Surface(
            shape = OneUiPanelShape,
            color = OneUiCard,
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(color = OneUiAccent)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.safe_mode_checking_title),
                        color = OneUiText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.safe_mode_checking_summary),
                        color = OneUiTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SafeRecoveryScreen(
    summary: PreviousCrashSummary,
    actionMessage: String?,
    actionInProgress: Boolean,
    onResetLayout: () -> Unit,
    onResetSettings: () -> Unit,
    onClearWidgets: () -> Unit,
    onExportDiagnostics: () -> Unit,
    onContinue: () -> Unit,
) {
    SafeRecoveryScaffold {
        Surface(
            shape = OneUiPanelShape,
            color = OneUiCard,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.safe_mode_title),
                        color = OneUiText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.safe_mode_summary),
                        color = OneUiTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }

                Surface(shape = OneUiPanelShape, color = OneUiSurface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RecoveryDetailRow(stringResource(R.string.safe_mode_detail_exception), summary.exceptionClass)
                        RecoveryDetailRow(stringResource(R.string.safe_mode_detail_when), summary.timestamp)
                        RecoveryDetailRow(stringResource(R.string.safe_mode_detail_version), summary.versionName)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecoveryActionRow(
                        primary = stringResource(R.string.safe_mode_reset_layout),
                        secondary = stringResource(R.string.safe_mode_reset_settings),
                        enabled = !actionInProgress,
                        onPrimary = onResetLayout,
                        onSecondary = onResetSettings,
                    )
                    RecoveryActionRow(
                        primary = stringResource(R.string.safe_mode_clear_widgets),
                        secondary = stringResource(R.string.safe_mode_export_diagnostics),
                        enabled = !actionInProgress,
                        onPrimary = onClearWidgets,
                        onSecondary = onExportDiagnostics,
                    )
                    Button(
                        onClick = onContinue,
                        enabled = !actionInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.safe_mode_continue))
                    }
                }

                actionMessage?.let { message ->
                    Surface(shape = OneUiControlShape, color = OneUiSurface) {
                        Text(
                            text = message,
                            color = OneUiText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SafeRecoveryScaffold(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        OneUiBackground,
                        Color(0xFFEFF5FF),
                        Color(0xFFF8FBFF),
                    ),
                ),
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun RecoveryActionRow(
    primary: String,
    secondary: String,
    enabled: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onPrimary,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Text(primary)
        }
        OutlinedButton(
            onClick = onSecondary,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Text(secondary)
        }
    }
}

@Composable
private fun RecoveryDetailRow(label: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = OneUiTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.36f),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = value?.takeIf(String::isNotBlank) ?: stringResource(R.string.state_unavailable),
            color = OneUiText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.64f),
        )
    }
}
