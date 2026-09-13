package com.septaalfauzan.saku.ui.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.domain.importexport.TransactionDraft
import com.septaalfauzan.saku.domain.model.TransactionType
import com.septaalfauzan.saku.ui.components.FilterChip
import com.septaalfauzan.saku.ui.components.LabelCaps
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.util.formatRupiah
import com.septaalfauzan.saku.util.formatShortDate
import org.koin.androidx.compose.koinViewModel

private val importMimeTypes = arrayOf("text/*", "text/csv", "text/comma-separated-values")

@Composable
fun ImportCsvRoute(onBack: () -> Unit, vm: ImportCsvViewModel = koinViewModel()) {
    val state by vm.uiState.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                vm.onFilePicked(uri.lastPathSegment ?: "file.csv", bytes)
            }
        }
    }
    val openPicker = { launcher.launch(importMimeTypes) }

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
            Text(stringResource(R.string.import_title), style = type.headlineLg, color = palette.ink)
        }

        when (val current = state) {
            is ImportCsvUiState.Idle -> ImportIdleContent(openPicker)
            is ImportCsvUiState.Parsing -> ImportProcessingContent()
            is ImportCsvUiState.Preview -> ImportPreviewContent(current, vm)
            is ImportCsvUiState.Applying -> ImportProcessingContent()
            is ImportCsvUiState.Done -> ImportDoneContent(current, vm)
            is ImportCsvUiState.Failed -> ImportFailedContent(current, onBack)
        }
    }
}

@Composable
private fun ImportIdleContent(openPicker: () -> Unit) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Spacer(Modifier.height(SakuDp.spaceLg))
    Column(
        Modifier
            .fillMaxWidth()
            .background(palette.surfaceLow, RoundedCornerShape(SakuDp.radiusDefault))
            .clickable { openPicker() }
            .padding(vertical = SakuDp.space3xl, horizontal = SakuDp.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(SakuIcons.FileOpen, contentDescription = null, tint = palette.slate, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(SakuDp.spaceSm))
        Text(
            stringResource(R.string.import_pick_title),
            style = type.headlineSm,
            color = palette.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SakuDp.spaceXs))
        Text(
            stringResource(R.string.import_pick_body),
            style = type.bodyMd,
            color = palette.slate,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(SakuDp.spaceLg))
    PillButton(text = stringResource(R.string.import_pick_button), onClick = openPicker)
    Spacer(Modifier.height(SakuDp.spaceXl))
}

@Composable
private fun ImportProcessingContent() {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Spacer(Modifier.height(SakuDp.space3xl))
    Box(
        Modifier.fillMaxWidth().height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = palette.ink)
            Spacer(Modifier.height(SakuDp.spaceMd))
            Text(stringResource(R.string.import_processing), style = type.bodyMd, color = palette.slate)
        }
    }
}

@Composable
private fun ImportPreviewContent(preview: ImportCsvUiState.Preview, vm: ImportCsvViewModel) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Spacer(Modifier.height(SakuDp.spaceLg))

    LabelCaps(preview.fileName)
    Spacer(Modifier.height(SakuDp.spaceSm))
    Row(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXl)) {
        CountPod(preview.newCount, stringResource(R.string.import_preview_summary_new), palette.ink)
        CountPod(preview.updateCount, stringResource(R.string.import_preview_summary_update), palette.slate)
        if (!preview.skipDuplicates) {
            CountPod(preview.duplicateCount, stringResource(R.string.import_preview_summary_duplicate), palette.crimson)
        }
    }

    if (preview.blocked) {
        Spacer(Modifier.height(SakuDp.spaceLg))
        Text(
            stringResource(R.string.import_preview_blocked, preview.errors.size),
            style = type.bodySm,
            color = palette.crimson,
        )
        preview.errors.forEach { error ->
            Text(
                stringResource(R.string.import_preview_error_row, error.row, error.field, error.reason),
                style = type.bodySm,
                color = palette.crimson,
            )
        }
    } else {
        if (preview.sample.isNotEmpty()) {
            Spacer(Modifier.height(SakuDp.spaceLg))
            LabelCaps(stringResource(R.string.import_preview_title))
            Spacer(Modifier.height(SakuDp.spaceXs))
            preview.sample.forEach { draft -> ImportSampleRow(draft) }
        }
        Spacer(Modifier.height(SakuDp.spaceMd))
        FilterChip(
            stringResource(R.string.import_skip_duplicates),
            preview.skipDuplicates,
            onClick = { vm.setSkipDuplicates(!preview.skipDuplicates) },
        )
    }

    Spacer(Modifier.height(SakuDp.spaceLg))
    PillButton(
        text = stringResource(R.string.import_confirm_button, preview.importableCount),
        onClick = { vm.confirmImport() },
        enabled = !preview.blocked,
        icon = SakuIcons.Save,
    )
    Spacer(Modifier.height(SakuDp.spaceXl))
}

@Composable
private fun CountPod(count: Int, label: String, color: Color) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Column {
        Text("$count", style = type.headlineSm, color = color)
        Text(label, style = type.bodySm, color = palette.slate)
    }
}

@Composable
private fun ImportSampleRow(draft: TransactionDraft) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val title = draft.merchant ?: stringResource(R.string.detail_fallback_title)
    val date = formatShortDate(draft.occurredAt.toEpochMilliseconds())
    Row(
        Modifier.fillMaxWidth().padding(vertical = SakuDp.spaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = type.headlineSm, color = palette.ink, maxLines = 1)
            Text(date, style = type.bodySm, color = palette.slate)
        }
        Text(
            (if (draft.type == TransactionType.INCOME) "+" else "-") + formatRupiah(draft.amount),
            style = type.numericTable,
            color = palette.ink,
        )
    }
}

@Composable
private fun ImportDoneContent(done: ImportCsvUiState.Done, vm: ImportCsvViewModel) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Spacer(Modifier.height(SakuDp.spaceLg))
    Text(stringResource(R.string.import_done_title), style = type.headlineSm, color = palette.ink)
    Spacer(Modifier.height(SakuDp.spaceXs))
    Text(
        stringResource(R.string.import_done_body, done.newCount, done.updateCount),
        style = type.bodySm,
        color = palette.slate,
    )
    if (done.skippedDuplicates > 0) {
        Spacer(Modifier.height(SakuDp.space2xs))
        Text(
            stringResource(R.string.import_done_skipped, done.skippedDuplicates),
            style = type.bodySm,
            color = palette.slate,
        )
    }
    Spacer(Modifier.height(SakuDp.spaceMd))
    PillButton(
        text = stringResource(R.string.import_undo),
        onClick = { vm.undo() },
        variant = PillButtonVariant.GHOST,
    )
    Spacer(Modifier.height(SakuDp.spaceXl))
}

@Composable
private fun ImportFailedContent(failed: ImportCsvUiState.Failed, onBack: () -> Unit) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    Spacer(Modifier.height(SakuDp.spaceLg))
    Text(stringResource(R.string.import_failed_title), style = type.headlineSm, color = palette.ink)
    if (failed.reason.isNotBlank()) {
        Spacer(Modifier.height(SakuDp.spaceXs))
        Text(failed.reason, style = type.bodySm, color = palette.crimson)
    }
    Spacer(Modifier.height(SakuDp.spaceLg))
    PillButton(text = stringResource(R.string.common_back), onClick = onBack)
    Spacer(Modifier.height(SakuDp.spaceXl))
}
