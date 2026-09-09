package com.septaalfauzan.saku.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.domain.model.TransactionStatus
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuPalette
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.ui.designsystem.SakuType
import com.septaalfauzan.saku.ui.designsystem.categoryGlyph
import com.septaalfauzan.saku.ui.designsystem.categoryLabel
import com.septaalfauzan.saku.util.formatRupiah
import com.septaalfauzan.saku.util.formatShortDate
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailRoute(transactionId: String, onEdit: () -> Unit, onDeleted: () -> Unit) {
    val viewModel: TransactionDetailViewModel = koinViewModel(parameters = { parametersOf(transactionId) })
    val tx by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type

    LaunchedEffect(event) {
        when (event) {
            DetailEvent.Deleted -> onDeleted()
            DetailEvent.EditRequested -> onEdit()
            null -> Unit
        }
        viewModel.consumeEvent()
    }

    val transaction = tx
    if (transaction == null) {
        Text("Transaction not found", modifier = Modifier.padding(24.dp), color = palette.ink)
        return
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = SakuDp.screenEdgePadding, vertical = SakuDp.spaceLg)) {
        Box(
            Modifier.size(56.dp).background(palette.chalk, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(categoryGlyph(transaction.categoryId), contentDescription = null, tint = palette.ink, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(SakuDp.spaceMd))
        Text(transaction.merchant ?: transaction.description ?: "Transaction", style = type.headlineMd, color = palette.ink)
        Spacer(Modifier.height(SakuDp.spaceXs))
        Text(
            text = (if (transaction.isIncome) "+" else "-") + formatRupiah(transaction.amount),
            style = type.displayCurrencyMobile.copy(fontFeatureSettings = "tnum"),
            color = palette.ink,
        )
        Spacer(Modifier.height(SakuDp.spaceMd))
        Row(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
            DetailTag(categoryLabel(transaction.categoryId))
            DetailTag(transaction.source.name.lowercase().replaceFirstChar { it.uppercase() })
            if (transaction.status == TransactionStatus.PENDING_REVIEW) DetailTag("Pending review", accent = true)
        }

        Spacer(Modifier.height(SakuDp.space2xl))
        DividerRow("Date", formatShortDate(transaction.occurredAt.toEpochMilliseconds()), palette, type)
        DividerRow("Amount", formatRupiah(transaction.amount), palette, type)
        DividerRow("Source", transaction.sourcePackage ?: transaction.source.name, palette, type)

        Spacer(Modifier.weight(1f))
        PillButton("Edit", viewModel::requestEdit, icon = SakuIcons.Edit, variant = PillButtonVariant.GHOST)
        Spacer(Modifier.height(SakuDp.spaceXs))
        PillButton(
            text = "Delete",
            onClick = { showDeleteDialog = true },
            icon = SakuIcons.Delete,
            variant = PillButtonVariant.PRIMARY,
        )
        Spacer(Modifier.height(SakuDp.bottomSafeClearance))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete transaction?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                Button(onClick = { showDeleteDialog = false; viewModel.requestDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailTag(label: String, accent: Boolean = false) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Text(
        label.uppercase(),
        style = type.labelCaps,
        color = if (accent) Color.White else palette.ink,
        modifier = Modifier
            .background(if (accent) palette.crimson else palette.chalk, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun DividerRow(label: String, value: String, palette: SakuPalette, type: SakuType) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = type.labelMd, color = palette.slate)
            Text(value, style = type.labelMd, color = palette.ink)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.hairline))
    }
}
