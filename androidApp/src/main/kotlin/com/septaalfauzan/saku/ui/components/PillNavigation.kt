package com.septaalfauzan.saku.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

enum class PillDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    Dashboard("dashboard", SakuIcons.Dashboard, R.string.nav_dashboard),
    Transactions("transactions", SakuIcons.Wallet, R.string.nav_transactions),
    ScanAdd("scan-add", SakuIcons.Scanner, R.string.nav_scan),
    Rules("rules", SakuIcons.Rules, R.string.nav_rules),
}

@Composable
fun PillNavigation(
    currentRoute: String?,
    onSelect: (PillDestination) -> Unit,
    onScanAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = SakuTheme.palette

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SakuDp.floatingNavHeight)
            .shadow(16.dp, RoundedCornerShape(50), clip = false)
            .background(palette.canvas.copy(alpha = 0.92f), RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = SakuDp.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        PillDestination.entries.forEach { dest ->
            if (dest == PillDestination.ScanAdd) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .size(44.dp)
                        .background(palette.ink, CircleShape)
                        .clickable { onScanAdd() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(SakuIcons.Add, contentDescription = stringResource(R.string.common_add), tint = Color.White, modifier = Modifier.size(22.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(dest) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            dest.icon,
                            contentDescription = stringResource(dest.labelRes),
                            tint = if (currentRoute == dest.route) palette.ink else palette.slate,
                            modifier = Modifier.size(24.dp),
                        )
                        Box(
                            Modifier
                                .size(4.dp)
                                .background(
                                    if (currentRoute == dest.route) palette.crimson else Color.Transparent,
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}
