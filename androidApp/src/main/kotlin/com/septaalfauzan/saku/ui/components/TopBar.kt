package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme.palette
import com.septaalfauzan.saku.ui.designsystem.SakuTheme.type

@Composable
fun TopBar(
    title: String,
    subTitle: String? = null,
    onBack: () -> Unit,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(end = 20.dp)
        ,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                SakuIcons.ChevronLeft,
                contentDescription = stringResource(R.string.common_back),
                tint = Color.White
            )
        }
        Column{
            Text(
                title,
                style = type.headlineLg,
                color = Color.White
            )
            if (subTitle != null)
                Text(
                    subTitle,
                    style = type.bodySm,
                    color = palette.slate
                )
        }
        Spacer(Modifier.weight(1f))
        action()
    }
}