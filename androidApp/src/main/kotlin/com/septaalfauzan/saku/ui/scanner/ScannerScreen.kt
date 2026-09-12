package com.septaalfauzan.saku.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.septaalfauzan.saku.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.septaalfauzan.saku.domain.model.Receipt
import org.koin.androidx.compose.koinViewModel
import com.septaalfauzan.saku.ui.components.LabelCaps
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import com.septaalfauzan.saku.ui.state.StateUi
import com.septaalfauzan.saku.util.formatRupiah
import java.io.File
import java.util.concurrent.Executor

@Composable
fun ScannerScreen(onBack: () -> Unit, onEdit: (Receipt) -> Unit) {
    val context = LocalContext.current
    val viewModel: ScannerViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val palette = SakuTheme.palette
    val type = SakuTheme.type
    var shutterToken by remember { mutableStateOf(0) }
    val scanOcrState by viewModel.scanOcrState.collectAsState()
    val event by viewModel.event.collectAsState()

    LaunchedEffect(event) {
        when (event) {
            ScannerEvent.Saved -> onBack()
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
        }

    val onScanningState = scanOcrState == StateUi.Idle || scanOcrState == StateUi.Loading

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(SakuIcons.ChevronLeft, contentDescription = stringResource(R.string.common_back), tint = Color.White)
                }
                Text(stringResource(R.string.scanner_title), style = type.headlineLg, color = Color.White)
                Spacer(Modifier.weight(1f))
                if (state.phase == ScannerPhase.VIEWFINDER || state.phase == ScannerPhase.SCANNING)
                    IconButton(onClick = viewModel::toggleFlash) {
                        Icon(
                            if (state.flash == ScannerFlash.OFF) SakuIcons.FlashOff else SakuIcons.FlashOn,
                            contentDescription = stringResource(R.string.scanner_flash_format, state.flash),
                            tint = Color.White,
                        )
                    }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            when (state.phase) {
                ScannerPhase.VIEWFINDER, ScannerPhase.SCANNING -> {
                    if (hasPermission) {
                        CameraView(
                            flashMode = state.flash,
                            shutterToken = shutterToken,
                            onCaptured = viewModel::onCaptured,
                        )
                    } else {
                        PermissionPrompt(onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) })
                    }
                }

                ScannerPhase.RESULT -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(SakuDp.screenEdgePadding),
                        verticalArrangement = Arrangement.spacedBy(SakuDp.spaceMd),
                    ) {
                        Spacer(Modifier.height(SakuDp.spaceSm))
                        Text(stringResource(R.string.scanner_review_title), style = type.headlineSm, color = Color.White)
                        state.capturedPath?.let { path ->
                            val bitmap =
                                remember(path) { decodeWithExifRotation(path)?.asImageBitmap() }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = stringResource(R.string.scanner_captured_receipt),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                                )
                            }
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1A1F), RoundedCornerShape(20.dp))
                                .padding(SakuDp.spaceMd),
                            verticalArrangement = Arrangement.spacedBy(SakuDp.spaceXs),
                        ) {
                            val scanOcrResult = scanOcrState
                            when (scanOcrResult) {
                                is StateUi.Error -> {
                                    LabelCaps((scanOcrState as StateUi.Error).message, color = palette.crimson)

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
                                    val data = scanOcrResult.data
                                    LabelCaps(stringResource(R.string.scanner_detected), color = palette.crimson)
                                    SampleField(stringResource(R.string.common_merchant), data.merchantName)
                                    SampleField(stringResource(R.string.common_date), data.transactionDate)
                                    SampleField(stringResource(R.string.scanner_provider), data.paymentMethod)
                                    SampleField(
                                        stringResource(R.string.scanner_items),
                                        data.items.map {
                                            "${it.quantity}x ${it.name} (${
                                                formatRupiah(
                                                    it.totalPrice
                                                )
                                            })"
                                        }.joinToString(", "),
                                    )
                                    SampleField(stringResource(R.string.scanner_discount), formatRupiah(data.discount))
                                    SampleField(stringResource(R.string.scanner_total), formatRupiah(data.total), fontSize = 20.sp)
                                }
                            }


                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(SakuDp.spaceXs)) {
                            PillButton(
                                stringResource(R.string.scanner_approve_log),
                                viewModel::approve,
                                modifier = Modifier.weight(1f),
                                enabled = !onScanningState,
                                variant = PillButtonVariant.ACCENT
                            )
                            PillButton(
                                stringResource(R.string.common_edit),
                                {
                                    (scanOcrState as? StateUi.Success<Receipt>)?.data?.let(onEdit)
                                },
                                enabled = !onScanningState,
                                modifier = Modifier.weight(1f),
                                variant = PillButtonVariant.GHOST,
                            )
                        }
                        PillButton(
                            stringResource(R.string.scanner_rescan),
                            viewModel::retake,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !onScanningState,
                            variant = PillButtonVariant.PRIMARY
                        )
                    }
                }
            }



            if (state.phase == ScannerPhase.VIEWFINDER || state.phase == ScannerPhase.SCANNING) {
                ShutterButton(
                    scanning = state.phase == ScannerPhase.SCANNING,
                    onClick = { shutterToken++ },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                )
            }

            state.message?.let { msg ->
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .background(Color(0xFF1A1A1F), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                ) {
                    Text(msg, style = type.bodySm, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(scanning: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = SakuTheme.palette
    Box(
        modifier = modifier
            .size(72.dp)
            .border(3.dp, Color.White, CircleShape)
            .clickable(enabled = !scanning, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .padding(8.dp)
                .size(56.dp)
                .background(
                    if (scanning) palette.crimson.copy(alpha = 0.4f) else palette.crimson,
                    CircleShape
                ),
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
            color = Color.White,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    val type = SakuTheme.type
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.scanner_camera_permission_title), style = type.headlineSm, color = Color.White)
        Spacer(Modifier.height(SakuDp.spaceSm))
        Text(
            stringResource(R.string.scanner_camera_permission_body),
            style = type.bodyMd,
            color = Color(0xFF9C9CA4)
        )
        Spacer(Modifier.height(SakuDp.spaceMd))
        PillButton(
            stringResource(R.string.scanner_grant_access),
            onGrant,
            modifier = Modifier.fillMaxWidth(0.6f),
            variant = PillButtonVariant.ACCENT
        )
    }
}

private fun newReceiptFile(context: Context): File =
    File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")

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

private fun ScannerFlash.toCameraXFlash(): Int = when (this) {
    ScannerFlash.AUTO -> ImageCapture.FLASH_MODE_AUTO
    ScannerFlash.ON -> ImageCapture.FLASH_MODE_ON
    ScannerFlash.OFF -> ImageCapture.FLASH_MODE_OFF
}

@Composable
private fun CameraView(
    flashMode: ScannerFlash,
    shutterToken: Int,
    onCaptured: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val executor: Executor = remember { ContextCompat.getMainExecutor(context) }
    val onCapturedRef = rememberUpdatedState(onCaptured)

    LaunchedEffect(lifecycleOwner) {
        val provider = ProcessCameraProvider.getInstance(context)
        provider.addListener({
            val cameraProvider = provider.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode.toCameraXFlash())
                .build()
            imageCapture = capture
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )
        }, executor)
    }

    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode.toCameraXFlash()
    }

    LaunchedEffect(shutterToken) {
        if (shutterToken == 0) return@LaunchedEffect
        val capture = imageCapture ?: return@LaunchedEffect
        val file = newReceiptFile(context)
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onCapturedRef.value(file.absolutePath)
                }

                override fun onError(exc: ImageCaptureException) {
                    onCapturedRef.value(file.absolutePath)
                }
            },
        )
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
    )

    DisposableEffect(Unit) {
        onDispose {
            val provider = ProcessCameraProvider.getInstance(context)
            provider.addListener({
                provider.get().unbindAll()
            }, executor)
        }
    }
}
