package com.septaalfauzan.saku.ui.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.septaalfauzan.saku.domain.model.KeywordType
import com.septaalfauzan.saku.notification.model.NotificationData
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.notification.provider.ConfigurableParser
import com.septaalfauzan.saku.ui.components.EmptyState
import com.septaalfauzan.saku.ui.components.LabelCaps
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import kotlin.time.Clock
import org.koin.androidx.compose.koinViewModel

@Composable
fun ConfigureParserRoute(onBack: () -> Unit) {
    val viewModel: ConfigureParserViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SakuDp.screenEdgePadding),
    ) {
        Spacer(Modifier.height(SakuDp.spaceSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(SakuIcons.Back, contentDescription = stringResource(R.string.common_back), tint = palette.ink)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.configure_title), style = type.headlineLg, color = palette.ink)
                Text(stringResource(R.string.configure_subtitle), style = type.bodySm, color = palette.slate)
            }
            if (state.selectedPackage != null) {
                TextButton(onClick = { viewModel.onSave(); focusManager.clearFocus() }) {
                    Text(stringResource(R.string.configure_done), color = palette.crimson, style = type.labelMd)
                }
            }
        }

        Spacer(Modifier.height(SakuDp.spaceMd))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.configure_search_hint), style = type.bodyMd) },
            leadingIcon = { Icon(SakuIcons.Search, contentDescription = null, tint = palette.slate) },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = palette.chalk,
                focusedContainerColor = palette.chalk,
            ),
            singleLine = true,
        )

        if (state.filteredApps.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.canvas),
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(state.filteredApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onSelectApp(app.packageName) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(app.label, style = type.labelMd, color = palette.ink)
                                Text(app.packageName, style = type.bodySm, color = palette.slate)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(SakuDp.spaceMd))

        if (state.selectedPackage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.canvas),
            ) {
                Row(
                    modifier = Modifier.padding(SakuDp.spaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(palette.ink, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            state.selectedLabel.take(2).uppercase(),
                            color = palette.canvas,
                            style = type.labelMd,
                        )
                    }
                    Spacer(Modifier.width(SakuDp.spaceSm))
                    Column(Modifier.weight(1f)) {
                        Text(state.selectedLabel, style = type.headlineSm, color = palette.ink)
                        Text(state.selectedPackage!!, style = type.bodySm, color = palette.slate)
                    }
                    TextButton(onClick = { viewModel.onDismissApp() }) {
                        Text(stringResource(R.string.configure_change), style = type.labelMd, color = palette.slate)
                    }
                }
            }

            Spacer(Modifier.height(SakuDp.spaceMd))

            KeywordSection(
                title = stringResource(R.string.configure_expense_keywords),
                description = stringResource(R.string.configure_expense_desc),
                dotColor = palette.crimson,
                keywords = state.expenseWords,
                onAdd = { viewModel.onAddKeyword(KeywordType.EXPENSE, it) },
                onRemove = { viewModel.onRemoveKeyword(KeywordType.EXPENSE, it) },
            )
            Spacer(Modifier.height(SakuDp.spaceSm))
            KeywordSection(
                title = stringResource(R.string.configure_income_keywords),
                description = stringResource(R.string.configure_income_desc),
                dotColor = palette.slate,
                keywords = state.incomeWords,
                onAdd = { viewModel.onAddKeyword(KeywordType.INCOME, it) },
                onRemove = { viewModel.onRemoveKeyword(KeywordType.INCOME, it) },
            )
            Spacer(Modifier.height(SakuDp.spaceSm))
            KeywordSection(
                title = stringResource(R.string.configure_merchant_prefixes),
                description = stringResource(R.string.configure_merchant_desc),
                dotColor = palette.slate,
                keywords = state.merchantWords,
                onAdd = { viewModel.onAddKeyword(KeywordType.MERCHANT, it) },
                onRemove = { viewModel.onRemoveKeyword(KeywordType.MERCHANT, it) },
            )

            Spacer(Modifier.height(SakuDp.spaceMd))

            LiveTestSection(
                text = state.testNotificationText,
                onTextChange = { viewModel.onTestTextChanged(it) },
                selectedPackage = state.selectedPackage!!,
                expenseWords = state.expenseWords,
                incomeWords = state.incomeWords,
                merchantWords = state.merchantWords,
            )
        } else {
            EmptyState(
                title = stringResource(R.string.configure_select_app),
                body = stringResource(R.string.configure_select_app_body),
            )
        }

        Spacer(Modifier.height(SakuDp.spaceMd))

        LabelCaps(stringResource(R.string.configure_configured_apps), color = palette.slate)
        Spacer(Modifier.height(SakuDp.spaceXs))

        val configuredSources = sources.filter { source ->
            state.installedApps.any { it.packageName == source.packageName }
        }

        if (configuredSources.isEmpty()) {
            EmptyState(title = stringResource(R.string.configure_no_apps), body = stringResource(R.string.configure_no_apps_body))
        } else {
            configuredSources.forEach { source ->
                ConfiguredAppRow(
                    appName = state.installedApps.find { it.packageName == source.packageName }?.label
                        ?: source.providerId,
                    packageName = source.packageName,
                    isSelected = source.packageName == state.selectedPackage,
                    onClick = { viewModel.onSelectApp(source.packageName) },
                    onDelete = { viewModel.onDeleteApp(source.packageName) },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(SakuDp.bottomSafeClearance))
    }
}

//@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordSection(
    title: String,
    description: String,
    dotColor: androidx.compose.ui.graphics.Color,
    keywords: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.canvas),
    ) {
        Column(modifier = Modifier.padding(SakuDp.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(dotColor, RoundedCornerShape(4.dp)),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(title, style = type.headlineSm, color = palette.ink)
                    }
                    Text(description, style = type.bodySm, color = palette.slate)
                }
                Text(stringResource(R.string.configure_rules_count, keywords.size), style = type.labelCaps, color = palette.slate)
            }

            if (keywords.isNotEmpty()) {
                Spacer(Modifier.height(SakuDp.spaceXs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    keywords.forEach { word ->
                        AssistChip(
                            onClick = { onRemove(word) },
                            label = { Text(word, style = type.labelMd) },
                            trailingIcon = {
                                Icon(
                                    SakuIcons.Close,
                                    contentDescription = stringResource(R.string.common_remove),
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(SakuDp.spaceXs))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.configure_add_keyword_hint), style = type.bodySm) },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (inputText.isNotBlank()) {
                            onAdd(inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = palette.chalk,
                        focusedContainerColor = palette.chalk,
                    ),
                )
                Spacer(Modifier.width(SakuDp.spaceXs))
                PillButton(
                    stringResource(R.string.configure_add),
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onAdd(inputText)
                            inputText = ""
                        }
                    },
                    variant = PillButtonVariant.PRIMARY,
                    modifier = Modifier.width(80.dp),
                )
            }
        }
    }
}

data class TestResult(
    val type: String?,
    val amount: Long?,
    val merchant: String?,
    val matched: Boolean,
)

@Composable
private fun LiveTestSection(
    text: String,
    onTextChange: (String) -> Unit,
    selectedPackage: String,
    expenseWords: List<String>,
    incomeWords: List<String>,
    merchantWords: List<String>,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type

    val testResult = remember(text, expenseWords, incomeWords, merchantWords) {
        if (text.isBlank()) null
        else {
            val parser = ConfigurableParser(
                packageName = selectedPackage,
                expenseWords = expenseWords,
                incomeWords = incomeWords,
                merchantWords = merchantWords,
            )
            val notification = NotificationData(
                packageName = selectedPackage,
                title = null,
                body = text,
                postedAt = Clock.System.now(),
                notificationId = 0,
            )
            val parsed = parser.parse(notification)
            if (parsed != null) TestResult(
                type = parsed.type?.name,
                amount = parsed.amount,
                merchant = parsed.rawMerchant,
                matched = true,
            )
            else TestResult(type = null, amount = null, merchant = null, matched = false)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.chalk),
    ) {
        Column(modifier = Modifier.padding(SakuDp.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabelCaps(stringResource(R.string.configure_live_simulation))
                if (testResult?.matched == true) {
                    Text(stringResource(R.string.configure_auto_matched), style = type.labelMd, color = palette.crimson)
                }
            }

            Spacer(Modifier.height(SakuDp.spaceXs))

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.configure_paste_hint), style = type.bodySm) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = palette.canvas,
                    focusedContainerColor = palette.canvas,
                ),
            )

            if (testResult != null && testResult.matched) {
                Spacer(Modifier.height(SakuDp.spaceXs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TestResultChip(stringResource(R.string.configure_type), testResult.type ?: "-", Modifier.weight(1f))
                    TestResultChip(stringResource(R.string.common_amount), testResult.amount?.let { "Rp $it" } ?: "-", Modifier.weight(1f))
                    TestResultChip(stringResource(R.string.common_merchant), testResult.merchant ?: "-", Modifier.weight(1f))
                }
            } else if (testResult != null && !testResult.matched) {
                Spacer(Modifier.height(SakuDp.spaceXs))
                Text(stringResource(R.string.configure_no_match), style = type.labelMd, color = palette.slate)
            }
        }
    }
}

@Composable
private fun TestResultChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SakuTheme.palette.canvas),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            LabelCaps(label, color = SakuTheme.palette.slate)
            Text(
                value,
                style = SakuTheme.type.labelMd,
                color = SakuTheme.palette.ink,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ConfiguredAppRow(
    appName: String,
    packageName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) palette.chalk else palette.canvas,
        ),
    ) {
        Row(
            modifier = Modifier.padding(SakuDp.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) palette.ink else palette.chalk,
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    appName.take(2).uppercase(),
                    color = if (isSelected) palette.canvas else palette.ink,
                    style = type.labelMd,
                )
            }
            Spacer(Modifier.width(SakuDp.spaceSm))
            Column(Modifier.weight(1f)) {
                Text(appName, style = type.labelMd, color = palette.ink)
                Text(packageName, style = type.bodySm, color = palette.slate)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(SakuIcons.Delete, contentDescription = stringResource(R.string.common_delete), tint = palette.slate)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.configure_delete_app_title, appName)) },
            text = { Text(stringResource(R.string.configure_delete_app_body)) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_delete), color = SakuTheme.palette.crimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
