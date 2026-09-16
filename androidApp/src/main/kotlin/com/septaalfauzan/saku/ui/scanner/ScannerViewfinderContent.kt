package com.septaalfauzan.saku.ui.scanner

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.septaalfauzan.saku.R
import com.septaalfauzan.saku.ui.components.PillButton
import com.septaalfauzan.saku.ui.components.PillButtonVariant
import com.septaalfauzan.saku.ui.designsystem.SakuDp
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import java.io.File
import java.util.concurrent.Executor

@Composable
internal fun ScannerViewfinderContent(
    hasPermission: Boolean,
    flashMode: ScannerFlash,
    shutterToken: Int,
    scanning: Boolean,
    onCaptured: (String) -> Unit,
    onGrantPermission: () -> Unit,
    onShutter: () -> Unit,
    onPickImage: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (hasPermission) {
            CameraView(
                flashMode = flashMode,
                shutterToken = shutterToken,
                onCaptured = onCaptured,
            )
        } else {
            PermissionPrompt(onGrant = onGrantPermission)
        }
        ShutterButtons(
            scanning = scanning,
            onClick = onShutter,
            onPickImage = onPickImage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
    }
}

@Composable
private fun ShutterButtons(scanning: Boolean, onClick: () -> Unit, onPickImage: () -> Unit, modifier: Modifier = Modifier) {
    val palette = SakuTheme.palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = modifier
                .size(72.dp)
                .border(3.dp, Color.White, CircleShape)
                .clickable(enabled = !scanning, onClick = onClick),
            contentAlignment = Alignment.BottomCenter,
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
        IconButton(
            onClick = onPickImage,
            colors = IconButtonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContentColor = Color.White,
                disabledContainerColor = Color.DarkGray
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 52.dp, bottom = 52.dp)
        ) {
            Icon(Icons.Default.Image, "open galery")
        }
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
        Text(
            stringResource(R.string.scanner_camera_permission_title),
            style = type.headlineSm,
            color = Color.White
        )
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
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(2400, 3200),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            )
                        )
                        .build()
                )
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