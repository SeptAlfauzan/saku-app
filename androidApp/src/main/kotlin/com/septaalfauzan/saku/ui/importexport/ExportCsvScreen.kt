package com.septaalfauzan.saku.ui.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.domain.importexport.ExportTransactions
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.ui.components.FilterChip
import com.septaalfauzan.saku.ui.components.LabelCaps
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportCsvRoute(onBack: () -> Unit, vm: ExportCsvViewModel = koinViewModel()) {
    val state by vm.uiState.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val context = LocalContext.current
    var done by remember { mutableStateOf<ExportTransactions.Result?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            vm.buildCsv { result ->
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write("\uFEFF".toByteArray(Charsets.UTF_8))
                    os.write(result.csv.toByteArray(Charsets.UTF_8))
                }
                done = result
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SakuDp.screenEdgePadding),
    ) {
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
            Text(stringResource(R.string.export_title), style = type.headlineLg, color = palette.ink)
        }
        Spacer(Modifier.height(SakuDp.spaceLg))

        LabelCaps(stringResource(R.string.export_date_range))
        Spacer(Modifier.height(SakuDp.spaceSm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
            FilterChip(stringResource(R.string.export_all), state.datePreset == DatePreset.ALL_TIME, onClick = {
                vm.setDatePreset(DatePreset.ALL_TIME)
            })
            FilterChip(stringResource(R.string.export_this_month), state.datePreset == DatePreset.THIS_MONTH, onClick = {
                vm.setDatePreset(DatePreset.THIS_MONTH)
            })
            FilterChip(stringResource(R.string.export_this_year), state.datePreset == DatePreset.THIS_YEAR, onClick = {
                vm.setDatePreset(DatePreset.THIS_YEAR)
            })
        }

        Spacer(Modifier.height(SakuDp.spaceLg))
        LabelCaps(stringResource(R.string.export_type))
        Spacer(Modifier.height(SakuDp.spaceSm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
            FilterChip(stringResource(R.string.export_all), state.type == null, onClick = { vm.setType(null) })
            FilterChip(stringResource(R.string.common_income), state.type == TransactionType.INCOME, onClick = {
                vm.setType(TransactionType.INCOME)
            })
            FilterChip(stringResource(R.string.common_expense), state.type == TransactionType.EXPENSE, onClick = {
                vm.setType(TransactionType.EXPENSE)
            })
        }

        Spacer(Modifier.height(SakuDp.spaceLg))
        LabelCaps(stringResource(R.string.export_category))
        Spacer(Modifier.height(SakuDp.spaceSm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
            FilterChip(stringResource(R.string.export_all), state.categoryId == null, onClick = { vm.setCategory(null) })
            state.categories.forEach { category ->
                FilterChip(category.name, state.categoryId == category.id, onClick = { vm.setCategory(category.id) })
            }
        }

        Spacer(Modifier.height(SakuDp.spaceXl))

        val result = done
        if (result == null) {
            val message = state.message
            PillButton(
                text = stringResource(R.string.export_export_button),
                onClick = { launcher.launch("saku-export.csv") },
                enabled = !state.building,
            )
            if (message != null) {
                Spacer(Modifier.height(SakuDp.spaceSm))
                Text(message, style = type.bodySm, color = palette.crimson)
            }
        } else {
            Text(stringResource(R.string.export_done_title), style = type.headlineSm, color = palette.ink)
            Spacer(Modifier.height(SakuDp.spaceXs))
            Text(
                stringResource(R.string.export_done_body, result.exportedCount),
                style = type.bodySm,
                color = palette.slate,
            )
            if (result.excludedTransfers > 0) {
                Spacer(Modifier.height(SakuDp.space2xs))
                Text(
                    stringResource(R.string.export_excluded_note, result.excludedTransfers),
                    style = type.bodySm,
                    color = palette.slate,
                )
            }
            Spacer(Modifier.height(SakuDp.spaceMd))
            PillButton(
                text = stringResource(R.string.export_export_again),
                onClick = { done = null },
                variant = PillButtonVariant.GHOST,
            )
        }

        Spacer(Modifier.height(SakuDp.spaceXl))
    }
}