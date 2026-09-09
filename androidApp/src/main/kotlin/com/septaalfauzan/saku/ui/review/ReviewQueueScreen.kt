package com.septaalfauzan.saku.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.septaalfauzan.saku.ui.components.EmptyState
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.components.TransactionRow
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReviewQueueRoute(onEdit: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: ReviewQueueViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type

    Column(Modifier.fillMaxSize().padding(horizontal = SakuDp.screenEdgePadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(SakuIcons.Back, contentDescription = "Back", tint = palette.ink)
            }
            Text("Pending Review", style = type.headlineLg, color = palette.ink)
        }

        if (state.pending.isEmpty()) {
            EmptyState(
                title = "Nothing to review",
                body = "Captured transactions above 80% confidence are confirmed automatically. Low-confidence ones appear here.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = SakuDp.bottomSafeClearance),
            ) {
                items(state.pending, key = { it.id }) { tx ->
                    Column {
                        TransactionRow(tx)
                        Row(Modifier.padding(start = SakuDp.spaceXs, top = SakuDp.spaceXs), horizontalArrangement = Arrangement.spacedBy(space = SakuDp.spaceSm)) {
                            PillButton("Confirm",
                                icon = Icons.Default.CheckCircle,
                                onClick = { viewModel.confirm(tx.id) }, modifier = Modifier.weight(2f), variant = PillButtonVariant.ACCENT)
                            PillButton("Edit",
                                icon = Icons.Default.Edit,
                                onClick = { onEdit(tx.id) }, modifier = Modifier.weight(2f), variant = PillButtonVariant.GHOST)
                            PillButton(null,
                                icon = Icons.Default.Delete,
                                onClick ={ viewModel.ignore(tx.id) }, modifier = Modifier.weight(1f), variant = PillButtonVariant.GHOST)
                        }
                    }
                }
            }
        }
    }
}

