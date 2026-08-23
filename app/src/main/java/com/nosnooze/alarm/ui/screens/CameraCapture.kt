package com.nosnooze.alarm.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun CameraCapture(
    file: File,
    frontFacing: Boolean,
    buttonText: String,
    onCaptured: (File) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CameraAlt, null); Text("Camera access is needed for this mission")
            Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
        }
        return
    }

    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    LaunchedEffect(previewView, lifecycle, frontFacing) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                val selector = CameraSelector.Builder().requireLensFacing(if (frontFacing) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK).build()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycle, selector, preview, imageCapture)
                capture = imageCapture
            }.onFailure { onError(it.message ?: "Camera unavailable") }
        }, ContextCompat.getMainExecutor(context))
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.Center).fillMaxWidth(.82f).aspectRatio(1f).background(Color.Transparent))
        Button(
            onClick = {
                file.parentFile?.mkdirs()
                capture?.takePicture(
                    ImageCapture.OutputFileOptions.Builder(file).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(result: ImageCapture.OutputFileResults) = onCaptured(file)
                        override fun onError(exception: ImageCaptureException) = onError(exception.message ?: "Could not take photo")
                    }
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp).height(58.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) { Icon(Icons.Outlined.CameraAlt, null); Spacer(Modifier.width(10.dp)); Text(buttonText) }
    }
}
