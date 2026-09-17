package com.septaalfauzan.saku.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.septaalfauzan.saku.R
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.septaalfauzan.saku.domain.model.Receipt
import org.koin.androidx.compose.koinViewModel
import com.septaalfauzan.saku.ui.components.TopBar
import com.septaalfauzan.saku.ui.designsystem.SakuIcons
import com.septaalfauzan.saku.ui.designsystem.SakuTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    saveJsonEdit: String?,
    onBack: () -> Unit, onEdit: (Receipt) -> Unit,
    sharedImagUri: String? = null,
) {
    val context = LocalContext.current
    val viewModel: ScannerViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val type = SakuTheme.type
    var shutterToken by remember { mutableStateOf(0) }
    val scanOcrState by viewModel.scanOcrState.collectAsState()
    val event by viewModel.event.collectAsState()
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = File(context.cacheDir, "pick_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            val path = file.absolutePath
            viewModel.onCaptured(path, context)
        }
    }

    LaunchedEffect(event) {
        when (event) {
            ScannerEvent.Saved -> onBack()
            null -> Unit
        }
        if (event != null) viewModel.consumeEvent()
    }
    LaunchedEffect(Unit) {
        if(saveJsonEdit == null) return@LaunchedEffect
        viewModel.updateStateFromEditValue(saveJsonEdit)
    }

    LaunchedEffect(sharedImagUri) {
        sharedImagUri?.takeIf { it.isNotBlank() }?.let { raw ->
            val uri = Uri.parse(raw)
            viewModel.onCaptured(uri, context)
        }
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

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.scanner_title),
                onBack = onBack,
                action = {
                    if (state.phase == ScannerPhase.VIEWFINDER || state.phase == ScannerPhase.SCANNING)
                        IconButton(onClick = viewModel::toggleFlash) {
                            Icon(
                                if (state.flash == ScannerFlash.OFF) SakuIcons.FlashOff else SakuIcons.FlashOn,
                                contentDescription = stringResource(
                                    R.string.scanner_flash_format,
                                    state.flash
                                ),
                                tint = Color.White,
                            )
                        }
                }
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.phase) {
                ScannerPhase.VIEWFINDER, ScannerPhase.SCANNING -> ScannerViewfinderContent(
                    hasPermission = hasPermission,
                    flashMode = state.flash,
                    shutterToken = shutterToken,
                    scanning = state.phase == ScannerPhase.SCANNING,
                    onCaptured = { viewModel.onCaptured(it, context) },
                    onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onShutter = { shutterToken++ },
                    onPickImage = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )

                ScannerPhase.RESULT -> ScannerResultContent(
                    capturedPath = state.capturedPath,
                    scanOcrState = scanOcrState,
                    onApprove = viewModel::approve,
                    onEdit = onEdit,
                    onRetake = viewModel::retake,
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