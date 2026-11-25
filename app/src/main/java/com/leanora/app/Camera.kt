package com.leanora.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal enum class CameraMode { FOOD_PHOTO, BARCODE }

private val RETAIL_FORMATS = intArrayOf(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)

@Composable
internal fun CameraCaptureDialog(mode: CameraMode, onClose: () -> Unit, onBarcode: (String) -> Unit, onPhoto: (Bitmap) -> Unit) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    LaunchedEffect(Unit) { if (!allowed) request.launch(Manifest.permission.CAMERA) }
    if (allowed) {
        CameraPreviewDialog(mode, onClose, onBarcode, onPhoto)
    } else {
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Camera access needed") },
            text = { Text("Allow camera access to scan food barcodes or take a meal photo.") },
            confirmButton = { TextButton(onClick = { request.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") } },
            dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun ImageProxy.toInputImage(): InputImage? = image?.let { InputImage.fromMediaImage(it, imageInfo.rotationDegrees) }

@Composable
private fun CameraPreviewDialog(mode: CameraMode, onClose: () -> Unit, onBarcode: (String) -> Unit, onPhoto: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(true) }
    val scanner = remember(mode) {
        if (mode == CameraMode.BARCODE) {
            BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(RETAIL_FORMATS[0], *RETAIL_FORMATS.drop(1).toIntArray()).build())
        } else null
    }
    val latestBarcode = rememberUpdatedState(onBarcode)
    val latestPhoto = rememberUpdatedState(onPhoto)
    val scanGate = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner, mode) {
        onDispose {
            runCatching {
                ProcessCameraProvider.getInstance(context).addListener(
                    { runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() } }, mainExecutor,
                )
            }
            scanner?.close()
            analyzerExecutor.shutdown()
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(14.dp), shape = RoundedCornerShape(28.dp), color = Color(0xFF191817)) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (mode == CameraMode.BARCODE) "Scan product barcode" else "Food photo estimate", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(if (mode == CameraMode.BARCODE) "Point at a UPC or EAN label" else "Fit the full meal in the frame", fontSize = 13.sp, color = Color(0xFFE3D8CF))
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close camera", tint = Color.White) }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AndroidView(modifier = Modifier.fillMaxSize(), factory = { androidContext ->
                        val view = PreviewView(androidContext)
                        val providerFuture = ProcessCameraProvider.getInstance(context)
                        providerFuture.addListener({
                            runCatching {
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                                val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                                imageCapture = capture
                                provider.unbindAll()
                                if (mode == CameraMode.BARCODE && scanner != null) {
                                    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                                    analysis.setAnalyzer(analyzerExecutor) { proxy ->
                                        val input = proxy.toInputImage()
                                        if (input == null) {
                                            proxy.close()
                                        } else {
                                            scanner.process(input)
                                                .addOnSuccessListener { codes ->
                                                    val found = codes.firstOrNull { code ->
                                                        val raw = code.rawValue.orEmpty()
                                                        code.format in RETAIL_FORMATS && raw.length in setOf(8, 12, 13, 14) && raw.all(Char::isDigit)
                                                    }
                                                    if (found != null && scanGate.compareAndSet(false, true)) {
                                                        scanning = false
                                                        latestBarcode.value(found.rawValue.orEmpty())
                                                    }
                                                }
                                                .addOnCompleteListener { proxy.close() }
                                        }
                                    }
                                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture, analysis)
                                } else {
                                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                                }
                            }.onFailure { cameraError = it.localizedMessage ?: "Unable to start the camera" }
                        }, mainExecutor)
                        view.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        view.scaleType = PreviewView.ScaleType.FILL_CENTER
                        view
                    })
                    if (mode == CameraMode.BARCODE) ScannerFrame()
                    cameraError?.let {
                        Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center).background(Color(0xAA000000), RoundedCornerShape(12.dp)).padding(12.dp))
                    }
                }
                if (mode == CameraMode.FOOD_PHOTO) {
                    Button(
                        onClick = {
                            val capture = imageCapture
                            if (capture == null) {
                                cameraError = "Camera is starting. Try again in a moment."
                            } else {
                                val file = File(context.cacheDir, "leanora-food-${System.currentTimeMillis()}.jpg")
                                capture.takePicture(
                                    ImageCapture.OutputFileOptions.Builder(file).build(), mainExecutor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                            file.delete()
                                            if (bitmap != null) latestPhoto.value(bitmap) else cameraError = "Could not read the captured photo."
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            file.delete()
                                            cameraError = exception.localizedMessage ?: "Photo capture failed."
                                        }
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Capture food photo") }
                } else {
                    Text(if (scanning) "Scanning for a food product barcode…" else "Barcode found", color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Text(
                    if (mode == CameraMode.BARCODE) "Scans product UPC/EAN codes only. No QR codes or links." else "Your photo is sent for this estimate only and is not saved to your gallery.",
                    fontSize = 12.sp, color = Color(0xFFD0C5BC),
                )
            }
        }
    }
}

@Composable
private fun ScannerFrame() {
    Canvas(Modifier.fillMaxWidth(.78f).height(150.dp)) {
        val color = Color(0xFFFFA36F)
        val stroke = 5.dp.toPx()
        val corner = 28.dp.toPx()
        val w = size.width
        val h = size.height
        listOf(Offset(0f, 0f), Offset(w, 0f), Offset(0f, h), Offset(w, h)).forEachIndexed { index, point ->
            val sx = if (index % 2 == 0) 1f else -1f
            val sy = if (index < 2) 1f else -1f
            drawLine(color, point, Offset(point.x + sx * corner, point.y), stroke, cap = StrokeCap.Round)
            drawLine(color, point, Offset(point.x, point.y + sy * corner), stroke, cap = StrokeCap.Round)
        }
    }
}
