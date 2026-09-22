package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.util.formatRupiah

@Composable
fun BalanceCard(
    monthLabel: String,
    balance: Long,
    income: Long,
    expense: Long,
    burnRate: List<Float>,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.canvas, RoundedCornerShape(32.dp))
            .border(1.dp, palette.hairline, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp)),
        verticalArrangement = Arrangement.spacedBy(SakuDp.spaceMd),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SakuDp.spaceLg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                LabelCaps(stringResource(R.string.balance_net_balance, monthLabel))
                Text(
                    formatRupiah(balance, absoluteValue = false),
                    style = type.displayCurrencyMobile.copy(fontFeatureSettings = "tnum"),
                    color = palette.ink,
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = SakuDp.spaceLg),
        ) {
            item {
                MetricPod(
                    label = stringResource(R.string.common_income),
                    text = "+${formatRupiah(income)}",
                    icon = Icons.Outlined.ArrowDownward,
                    modifier = Modifier
                        .padding(end = SakuDp.spaceSm)
                        .weight(1f)
                )
                MetricPod(
                    label = stringResource(R.string.common_expense),
                    text = "-${formatRupiah(expense)}",
                    icon = Icons.Outlined.ArrowUpward,
                    modifier = Modifier
                        .padding(end = SakuDp.spaceSm)
                        .weight(1f),
                    accent = true,
                )
            }
        }
        Column {
            LabelCaps(
                stringResource(R.string.balance_daily_burn_rate),
                color = palette.slate,
                modifier = Modifier.padding(horizontal = SakuDp.spaceLg)
            )
            Spacer(Modifier.height(SakuDp.spaceXs))
            BurnRateCurve(burnRate)
        }
    }
}
