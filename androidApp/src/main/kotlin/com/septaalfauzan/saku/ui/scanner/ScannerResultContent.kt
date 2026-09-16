package com.septaalfauzan.saku.ui.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.septaalfauzan.saku.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.septaalfauzan.saku.domain.model.Receipt
import com.septaalfauzan.saku.ui.components.LabelCaps
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.ui.state.StateUi
import com.septaalfauzan.saku.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerResultContent(
    capturedPath: String?,
    scanOcrState: StateUi<Receipt>,
    onApprove: () -> Unit,
    onEdit: (Receipt) -> Unit,
    onRetake: () -> Unit,
) {
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    val onScanningState = scanOcrState == StateUi.Idle || scanOcrState == StateUi.Loading
    var showFullImage by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(SakuDp.screenEdgePadding),
        verticalArrangement = Arrangement.spacedBy(SakuDp.spaceMd),
    ) {
        Spacer(Modifier.height(SakuDp.spaceSm))
        Text(
            stringResource(R.string.scanner_review_title),
            style = type.headlineSm,
        )
        capturedPath?.let { path ->
            val bitmap =
                remember(path) { decodeWithExifRotation(path)?.asImageBitmap() }
            if (bitmap != null) {
                Box {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.scanner_captured_receipt),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                    IconButton(onClick = { showFullImage = true }) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom button",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(SakuDp.spaceMd),
            verticalArrangement = Arrangement.spacedBy(SakuDp.spaceXs),
        ) {
            when (scanOcrState) {
                is StateUi.Error -> {
                    LabelCaps(scanOcrState.message, color = palette.crimson)
                }

                StateUi.Idle, StateUi.Loading -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.scanner_processing))
                    CircularProgressIndicator()
                }

                is StateUi.Success<Receipt> -> {
                    val data = scanOcrState.data
                    LabelCaps(
                        stringResource(R.string.scanner_detected),
                        color = palette.crimson
                    )
                    SampleField(
                        stringResource(R.string.common_merchant),
                        data.merchantName
                    )
                    SampleField(
                        stringResource(R.string.common_date),
                        data.transactionDate
                    )
                    SampleField(
                        stringResource(R.string.scanner_provider),
                        data.paymentMethod
                    )
                    SampleField(
                        stringResource(R.string.scanner_items),
                        data.items.map {
                            "${it.quantity}x ${it.name} (${formatRupiah(it.totalPrice)})"
                        }.joinToString(", "),
                    )
                    SampleField(
                        stringResource(R.string.scanner_discount),
                        formatRupiah(data.discount)
                    )
                    SampleField(
                        stringResource(R.string.scanner_total),
                        formatRupiah(data.total),
                        fontSize = 20.sp
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
            PillButton(
                stringResource(R.string.scanner_approve_log),
                onApprove,
                modifier = Modifier.weight(1f),
                enabled = !onScanningState,
                variant = PillButtonVariant.ACCENT
            )
            PillButton(
                stringResource(R.string.common_edit),
                { (scanOcrState as? StateUi.Success<Receipt>)?.data?.let(onEdit) },
                enabled = !onScanningState,
                modifier = Modifier.weight(1f),
                variant = PillButtonVariant.GHOST,
            )
        }
        PillButton(
            stringResource(R.string.scanner_rescan),
            onRetake,
            modifier = Modifier.fillMaxWidth(),
            enabled = !onScanningState,
            variant = PillButtonVariant.PRIMARY
        )
    }

    capturedPath?.let { path ->
        val bitmap =
            remember(path) { decodeWithExifRotation(path)?.asImageBitmap() }
        if (bitmap != null && showFullImage)
            BasicAlertDialog(
                onDismissRequest = { showFullImage = false },
                content = {
                    Box {
                        Image(
                            bitmap = bitmap,
                            contentDescription = stringResource(R.string.scanner_captured_receipt),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .scale(1.2f)
                                .horizontalScroll(rememberScrollState())
                                .verticalScroll(rememberScrollState())
                                .clip(RoundedCornerShape(20.dp)),
                        )
                        IconButton(onClick = { showFullImage = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close button",
                                tint = Color.White,
                            )
                        }
                    }
                },
            )
    }
}

@Composable
private fun SampleField(label: String, value: String, fontSize: TextUnit = 14.sp) {
    val type = SakuTheme.type
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = type.labelMd, color = Color(0xFF9C9CA4))
        Text(
            value,
            style = type.labelMd.copy(fontSize = fontSize),
            textAlign = TextAlign.End
        )
    }
}

private fun decodeWithExifRotation(path: String): Bitmap? {
    val original = BitmapFactory.decodeFile(path) ?: return null
    val orientation = try {
        ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    } catch (e: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270f
        else -> 0f
    }
    if (degrees == 0f) return original
    val matrix = Matrix().apply { postRotate(degrees) }
    val rotated = Bitmap.createBitmap(
        original, 0, 0, original.width, original.height, matrix, true,
    )
    if (rotated != original) original.recycle()
    return rotated
}