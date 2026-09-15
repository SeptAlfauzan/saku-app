package com.septaalfauzan.saku.ui.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.getAppVersion
import com.septaalfauzan.saku.notification.NotificationAccessManager
import com.septaalfauzan.saku.ui.components.EmptyState
import com.septaalfauzan.saku.ui.components.LabelCaps
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.components.TopBar
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


    Scaffold(
        topBar = {
            TopBar(
                title = if (onBack != null) stringResource(R.string.tracking_automated) else stringResource(
                    R.string.tracking_rules
                ),
                onBack = { if (onBack != null) onBack() },
                action = {}
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = SakuDp.screenEdgePadding)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.canvas, RoundedCornerShape(28.dp))
                    .border(1.dp, palette.hairline, RoundedCornerShape(28.dp))
                    .padding(SakuDp.spaceMd),
                verticalArrangement = Arrangement.spacedBy(SakuDp.spaceSm),
            ) {
                Text(
                    stringResource(R.string.tracking_intro),
                    style = type.bodyMd,
                    color = palette.slate,
                )
                if (accessGranted) {
                    Text(
                        stringResource(R.string.tracking_access_granted),
                        style = type.labelCaps,
                        color = palette.crimson
                    )
                } else {
                    Text(
                        stringResource(R.string.tracking_access_not_granted),
                        style = type.labelCaps,
                        color = palette.crimson
                    )
                    PillButton(
                        stringResource(R.string.tracking_enable_access),
                        { NotificationAccessManager.openSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = PillButtonVariant.PRIMARY,
                    )
                }
            }

            Spacer(Modifier.height(SakuDp.spaceMd))
            RuleRow(
                stringResource(R.string.tracking_automated),
                state.trackingEnabled,
                viewModel::toggleTrackingEnabled
            )
            RuleRow(
                stringResource(R.string.tracking_auto_confirm),
                state.autoConfirm,
                viewModel::toggleAutoConfirm
            )

            Spacer(Modifier.height(SakuDp.spaceMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabelCaps(stringResource(R.string.tracking_monitored_apps), color = palette.slate)
                if (onConfigureParser != null) {
                    TextButton(onClick = onConfigureParser) {
                        Text(
                            stringResource(R.string.tracking_configure),
                            style = type.labelMd,
                            color = palette.crimson
                        )
                    }
                }
            }
            Spacer(Modifier.height(SakuDp.spaceXs))
            if (state.sources.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.tracking_no_apps),
                    body = stringResource(R.string.tracking_no_apps_body)
                )
            } else {
                state.sources.forEach { source ->
                    RuleRow(
                        label = source.providerId,
                        sublabel = source.packageName,
                        checked = source.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleSourceEnabled(
                                source.packageName,
                                enabled
                            )
                        },
                    )
                }
            }
            Text("App Version v${getAppVersion()}", modifier = Modifier.fillMaxWidth(), color = palette.slate,  textAlign = TextAlign.Center)
            Spacer(Modifier.height(SakuDp.bottomSafeClearance))
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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

