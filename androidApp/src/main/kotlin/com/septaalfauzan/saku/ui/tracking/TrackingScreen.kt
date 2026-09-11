package com.septaalfauzan.saku.ui.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.notification.NotificationAccessManager
import com.septaalfauzan.saku.ui.components.EmptyState
import com.septaalfauzan.saku.ui.components.LabelCaps
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun TrackingRoute(onBack: (() -> Unit)?, onConfigureParser: (() -> Unit)? = null) {
    val viewModel: TrackingViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val context = LocalContext.current
    val accessGranted by produceState(initialValue = false) {
        value = withContext(Dispatchers.Default) { NotificationAccessManager.isListening(context) }
    }
    val scrollState = rememberScrollState()


    Column(Modifier.fillMaxSize().padding(horizontal = SakuDp.screenEdgePadding).verticalScroll(scrollState)) {
        Spacer(Modifier.height(SakuDp.spaceSm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(SakuIcons.Back, contentDescription = "Back", tint = palette.ink)
                }
            }
            Text(
                if (onBack != null) "Automatic Tracking" else "Rules",
                style = type.headlineLg,
                color = palette.ink,
            )
        }
        Spacer(Modifier.height(SakuDp.spaceMd))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.canvas, RoundedCornerShape(28.dp))
                .border(1.dp, palette.hairline, RoundedCornerShape(28.dp))
                .padding(SakuDp.spaceMd),
            verticalArrangement = Arrangement.spacedBy(SakuDp.spaceSm),
        ) {
            Text(
                "Reads notifications from selected financial apps to automatically record transactions. Notifications are processed locally whenever possible.",
                style = type.bodyMd,
                color = palette.slate,
            )
            if (accessGranted) {
                Text("Notification access granted", style = type.labelCaps, color = palette.crimson)
            } else {
                Text("Notification access not granted", style = type.labelCaps, color = palette.crimson)
                PillButton(
                    "Enable Notification Access",
                    { NotificationAccessManager.openSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = PillButtonVariant.PRIMARY,
                )
            }
        }

        Spacer(Modifier.height(SakuDp.spaceMd))
        RuleRow("Automatic Tracking", state.trackingEnabled, viewModel::toggleTrackingEnabled)
        RuleRow("Auto-confirm high confidence", state.autoConfirm, viewModel::toggleAutoConfirm)

        Spacer(Modifier.height(SakuDp.spaceMd))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabelCaps("Monitored Apps", color = palette.slate)
            if (onConfigureParser != null) {
                TextButton(onClick = onConfigureParser) {
                    Text("Configure", style = type.labelMd, color = palette.crimson)
                }
            }
        }
        Spacer(Modifier.height(SakuDp.spaceXs))
        if (state.sources.isEmpty()) {
            EmptyState(title = "No apps", body = "Enable a financial app to track.")
        } else {
            state.sources.forEach { source ->
                RuleRow(
                    label = source.providerId,
                    sublabel = source.packageName,
                    checked = source.enabled,
                    onCheckedChange = { enabled -> viewModel.toggleSourceEnabled(source.packageName, enabled) },
                )
            }
        }
        Spacer(Modifier.height(SakuDp.bottomSafeClearance))
    }
}

@Composable
private fun RuleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    sublabel: String = "",
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = type.labelMd, color = palette.ink)
            if (sublabel.isNotBlank()) {
                Text(sublabel, style = type.bodySm, color = palette.slate)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.canvas,
                checkedTrackColor = palette.crimson,
                uncheckedThumbColor = palette.canvas,
                uncheckedTrackColor = palette.chalk,
                uncheckedBorderColor = palette.hairline,
            ),
        )
    }
}

