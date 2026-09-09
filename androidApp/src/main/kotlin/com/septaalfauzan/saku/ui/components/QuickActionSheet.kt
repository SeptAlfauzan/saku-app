package com.septaalfauzan.saku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionSheet(
    sheetState: SheetState,
    onScanReceipt: () -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.canvas,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(4.dp).background(palette.chalk, CircleShape),
            )
            Text("Add Transaction", style = type.headlineSm, color = palette.ink)
            Text("Select a recording method", style = type.bodyMd, color = palette.slate)
            SheetAction("Scan Receipt", SakuIcons.Scanner, palette.crimson, onScanReceipt)
            SheetAction("Manual Entry", SakuIcons.Edit, palette.ink, onManualEntry)
        }
    }
}

@Composable
private fun SheetAction(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.chalk, CircleShape)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(palette.crimson.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Text(label, style = type.labelMd, color = palette.ink)
    }
}
