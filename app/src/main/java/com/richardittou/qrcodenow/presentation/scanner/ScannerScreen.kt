package com.richardittou.qrcodenow.presentation.scanner

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.richardittou.qrcodenow.domain.action.AndroidExternalActionLauncher
import com.richardittou.qrcodenow.domain.model.QrContent
import com.richardittou.qrcodenow.ui.theme.BrandRed
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    onCreateQr: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appSettings by viewModel.settings.collectAsStateWithLifecycle()
    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var hasRequestedCamera by rememberSaveable { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasRequestedCamera = true
        cameraGranted = it
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.scanImage(context, it) }
    }
    val haptics = LocalHapticFeedback.current
    val currentSettings by rememberUpdatedState(appSettings)

    LaunchedEffect(Unit) {
        viewModel.feedback.collect {
            if (currentSettings.vibrationEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (cameraGranted) {
            if (cameraError) {
                CameraUnavailableContent(
                    onRetry = { cameraError = false },
                    onPickImage = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )
            } else {
                CameraPreview(
                    paused = state.analysisPaused,
                    analyze = viewModel::analyze,
                    onError = { cameraError = true },
                    onCameraReady = { camera ->
                        boundCamera = camera
                        if (camera == null) torchEnabled = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
                ScannerOverlay(
                    onPickImage = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onCreateQr = onCreateQr,
                    flashAvailable = boundCamera?.cameraInfo?.hasFlashUnit() == true,
                    torchEnabled = torchEnabled,
                    onToggleTorch = {
                        val enable = !torchEnabled
                        boundCamera?.cameraControl?.enableTorch(enable)
                        torchEnabled = enable
                    }
                )
            }
        } else {
            PermissionContent(
                permanentlyDenied = hasRequestedCamera && activity?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } == true,
                onRequest = {
                    hasRequestedCamera = true
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onSettings = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                },
                onPickImage = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
        }
    }

    if (state.results.size > 1 && state.selected == null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text("${state.results.size} QR Codes encontrados") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.results, key = { it.raw }) { result ->
                        FilledTonalButton(onClick = { viewModel.select(result) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${result.type.label}: ${result.title}", maxLines = 2)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissResult) { Text("Fechar") } }
        )
    }
    state.selected?.let { content ->
        ResultDialog(content, appSettings.autoOpenSafeUrls, onDismiss = viewModel::dismissResult)
    }
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Não foi possível concluir") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
        )
    }
}

@Composable
private fun CameraPreview(
    paused: Boolean,
    analyze: (androidx.camera.core.ImageProxy) -> Unit,
    onError: (Throwable) -> Unit,
    onCameraReady: (Camera?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val currentPaused by rememberUpdatedState(paused)
    val currentAnalyze by rememberUpdatedState(analyze)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnCameraReady by rememberUpdatedState(onCameraReady)

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor) { image -> if (currentPaused) image.close() else currentAnalyze(image) } }
                provider.unbindAll()
                currentOnCameraReady(provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis))
            }.onFailure(currentOnError)
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            currentOnCameraReady(null)
            if (providerFuture.isDone) runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
        }
    }

    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ScannerOverlay(
    onPickImage: () -> Unit,
    onCreateQr: () -> Unit,
    flashAvailable: Boolean,
    torchEnabled: Boolean,
    onToggleTorch: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "line")
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            val side = minOf(size.width - 48.dp.toPx(), size.height * .58f)
            val left = (size.width - side) / 2f
            val headerClearance = if (flashAvailable) 160.dp.toPx() else 96.dp.toPx()
            val top = ((size.height - side) / 2f - 28.dp.toPx()).coerceAtLeast(headerClearance)
            drawRect(Color.Black.copy(alpha = .28f))
            drawRoundRect(Color.Transparent, Offset(left, top), Size(side, side), cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()), blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
            val corner = 42.dp.toPx()
            val stroke = 5.dp.toPx()
            listOf(
                Offset(left, top) to Offset(left + corner, top), Offset(left, top) to Offset(left, top + corner),
                Offset(left + side, top) to Offset(left + side - corner, top), Offset(left + side, top) to Offset(left + side, top + corner),
                Offset(left, top + side) to Offset(left + corner, top + side), Offset(left, top + side) to Offset(left, top + side - corner),
                Offset(left + side, top + side) to Offset(left + side - corner, top + side), Offset(left + side, top + side) to Offset(left + side, top + side - corner)
            ).forEach { (start, end) -> drawLine(BrandRed, start, end, stroke, StrokeCap.Round) }
            val lineY = top + side * progress
            drawLine(BrandRed.copy(alpha = .88f), Offset(left + 18.dp.toPx(), lineY), Offset(left + side - 18.dp.toPx(), lineY), 2.dp.toPx())
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (flashAvailable) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = .62f)) {
                        IconButton(onClick = onToggleTorch) {
                            Icon(
                                if (torchEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                if (torchEnabled) "Desligar lanterna" else "Ligar lanterna",
                                tint = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Surface(color = Color.Black.copy(alpha = .62f), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("QR Code Now", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Posicione o QR Code dentro da moldura", color = Color.White.copy(alpha = .88f))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onPickImage) { Icon(Icons.Outlined.Image, null); Spacer(Modifier.size(8.dp)); Text("Imagem") }
                Button(onClick = onCreateQr) { Icon(Icons.Outlined.AddBox, null); Spacer(Modifier.size(8.dp)); Text("Criar") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PermissionContent(permanentlyDenied: Boolean, onRequest: () -> Unit, onSettings: () -> Unit, onPickImage: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Image, null, tint = BrandRed, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text("Permissão da câmera", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("A câmera é usada somente para reconhecer QR Codes. As imagens não saem do aparelho.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = if (permanentlyDenied) onSettings else onRequest) { Text(if (permanentlyDenied) "Abrir configurações" else "Permitir câmera") }
        TextButton(onClick = onPickImage) { Text("Escanear uma imagem") }
    }
}

@Composable
private fun CameraUnavailableContent(onRetry: () -> Unit, onPickImage: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Image, null, tint = BrandRed, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text("Câmera indisponível", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("Não foi possível iniciar a câmera. Você ainda pode escolher uma imagem do aparelho.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Tentar novamente") }
        TextButton(onClick = onPickImage) { Text("Escanear uma imagem") }
    }
}

@Composable
private fun ResultDialog(content: QrContent, autoOpen: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val launcher = remember { AndroidExternalActionLauncher() }
    var externalError by remember { mutableStateOf(false) }
    val wifiPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && content is QrContent.Wifi) {
            launcher.connectWifi(context, content).onFailure { externalError = true }
        } else if (!granted) {
            externalError = true
        }
    }
    val wifiSupported = content !is QrContent.Wifi || content.security.uppercase() in setOf("", "NOPASS", "OPEN", "WPA", "WPA2", "WPA/WPA2", "WPA3", "SAE")
    val canOpen = content is QrContent.Url || content is QrContent.Phone || content is QrContent.Sms ||
        content is QrContent.Email || content is QrContent.Location || content is QrContent.AppLink ||
        (content is QrContent.Wifi && wifiSupported)
    LaunchedEffect(content.raw, autoOpen) {
        if (autoOpen && content is QrContent.Url && !content.suspicious) {
            launcher.open(context, content).onSuccess { onDismiss() }.onFailure { externalError = true }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(content.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(content.type.label, color = MaterialTheme.colorScheme.primary)
                if (content is QrContent.Url) Text("Domínio: ${content.domain}")
                if (content is QrContent.Url && content.suspicious) Text("Atenção: confira este endereço antes de abrir.", color = MaterialTheme.colorScheme.error)
                if (content is QrContent.AppLink) Text("Este link usa o esquema ${content.scheme}. Abra somente se reconhecer a origem.", color = MaterialTheme.colorScheme.error)
                if (content is QrContent.Wifi && !wifiSupported) Text("Esta segurança Wi-Fi não permite conexão direta pelo Android. Copie os dados e conecte pelas configurações.", color = MaterialTheme.colorScheme.error)
                Text(content.raw, maxLines = 8)
                if (externalError) Text("Não foi possível concluir esta ação. Verifique as permissões e os aplicativos disponíveis.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Row {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("QR Code", content.raw))
                }) { Icon(Icons.Outlined.ContentCopy, "Copiar") }
                IconButton(onClick = { launcher.shareText(context, content.raw).onFailure { externalError = true } }) { Icon(Icons.Outlined.Share, "Compartilhar") }
                if (canOpen) IconButton(onClick = {
                    val result = if (content is QrContent.Wifi) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED) {
                            launcher.connectWifi(context, content)
                        } else {
                            wifiPermissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                            Result.success(Unit)
                        }
                    } else {
                        launcher.open(context, content)
                    }
                    result.onFailure { externalError = true }
                }) { Icon(Icons.AutoMirrored.Outlined.Launch, if (content is QrContent.Wifi) "Conectar" else "Abrir") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
