package com.septaalfauzan.saku.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Column(Modifier.fillMaxSize().padding(horizontal = SakuDp.screenEdgePadding)) {
        Spacer(Modifier.height(SakuDp.spaceLg))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs),
        ) {
            Icon(
                SakuIcons.Back, contentDescription = stringResource(R.string.common_back),
                tint = palette.ink, modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() }
                    .padding(SakuDp.spaceXs),
            )
            Text(stringResource(R.string.settings_title), style = type.headlineLg, color = palette.ink)
        }
        Spacer(Modifier.height(SakuDp.spaceLg))

        SettingsRow(
            title = stringResource(R.string.settings_export_transactions),
            body = stringResource(R.string.settings_export_body),
            onTap = onOpenExport,
        )
        SettingsRow(
            title = stringResource(R.string.settings_import_transactions),
            body = stringResource(R.string.settings_import_body),
            onTap = onOpenImport,
        )
    }
}

@Composable
private fun SettingsRow(title: String, body: String, onTap: () -> Unit) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = SakuDp.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = type.headlineSm, color = palette.ink)
            Text(body, style = type.bodySm, color = palette.slate)
        }
        Icon(
            SakuIcons.ChevronRight, contentDescription = null,
            tint = palette.slate, modifier = Modifier.size(20.dp),
        )
    }
}