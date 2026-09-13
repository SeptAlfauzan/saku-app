package com.septaalfauzan.saku.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.components.BalanceCard
import com.septaalfauzan.saku.ui.components.EmptyState
import com.septaalfauzan.saku.ui.components.LabelCaps
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.components.TransactionRow
import androidx.compose.ui.res.stringResource
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardRoute(
    onAdd: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenTracking: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onOpenAll: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: DashboardViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val burnRateState by viewModel.burnRateState.collectAsState()
    val itemsNeedReview by viewModel.needReviewState.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val isNotificationListenerState by viewModel.notificationListenerState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SakuDp.screenEdgePadding),
        contentPadding = PaddingValues(
            top = SakuDp.spaceLg,
            bottom = SakuDp.bottomSafeClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(SakuDp.spaceMd),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.canvas, RoundedCornerShape(28.dp))
                    .padding(SakuDp.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(palette.chalk, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isNotificationListenerState) SakuIcons.Listener else SakuIcons.ListenerDisabled,
                        contentDescription = null,
                        tint = palette.crimson,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(Modifier
                    .weight(1f)
                    .padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier
                            .size(8.dp)
                            .background(palette.crimson, CircleShape))
                        Text(
                            "  " + stringResource(if (isNotificationListenerState) R.string.dashboard_smart_listener_active else R.string.dashboard_smart_listener_disabled),
                            style = type.labelCaps,
                            color = palette.crimson,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    Text(
                        if (isNotificationListenerState) stringResource(R.string.dashboard_listener_body_active) else stringResource(R.string.dashboard_listener_body_disabled),
                        style = type.bodySm,
                        color = palette.slate,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        item {
            BalanceCard(
                monthLabel = state.monthLabel,
                balance = state.balance,
                income = state.income,
                expense = state.expense,
                burnRate = burnRateState
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LabelCaps(stringResource(R.string.dashboard_review), color = palette.slate)
                Text(stringResource(R.string.dashboard_pending_items), style = type.bodySm, color = palette.slate)
            }
            Spacer(Modifier.height(SakuDp.spaceXs))
            Row(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
                BadgedBox(
                    badge = {
                        if (itemsNeedReview == 0) Spacer(modifier = Modifier) else Badge {
                            Text(itemsNeedReview.toString())
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {

                    PillButton(
                        stringResource(R.string.dashboard_review_queue), onOpenReview,
                        variant = PillButtonVariant.GHOST
                    )
                }
                PillButton(
                    stringResource(R.string.dashboard_tracking),
                    icon = Icons.Default.Apps,
                    onClick =
                        onOpenTracking,
                    modifier = Modifier.weight(1f),
                    variant = PillButtonVariant.GHOST
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.dashboard_recent_activity), style = type.headlineSm, color = palette.ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.dashboard_view_all),
                        style = type.labelMd,
                        color = palette.crimson,
                        modifier = Modifier.clickable { onOpenAll() },
                    )
                    Icon(
                        SakuIcons.Settings, contentDescription = stringResource(R.string.settings_title),
                        tint = palette.slate,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(start = 8.dp)
                            .clickable { onOpenSettings() },
                    )
                }
            }
            Spacer(Modifier.height(SakuDp.spaceXs))
        }

        if (state.recentTransactions.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.dashboard_no_transactions),
                    body = stringResource(R.string.dashboard_no_transactions_body),
                    ctaLabel = stringResource(R.string.dashboard_add_transaction),
                    onCta = onAdd,
                )
            }
        } else {
            items(state.recentTransactions, key = { it.id }) { tx ->
                TransactionRow(tx, onClick = { onOpenTransaction(tx.id) })
            }
        }
    }
}

