package com.nosnooze.alarm.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.io.File

@Composable fun SelfieChallenge(onComplete: () -> Unit) {
    val context = LocalContext.current
    var feedback by remember { mutableStateOf("Face the camera in good light. Keep both eyes open.") }
    var processing by remember { mutableStateOf(false) }
    val candidate = remember { File(context.cacheDir, "awake_selfie_${System.currentTimeMillis()}.jpg") }
    val detector = remember {
        FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build())
    }
    DisposableEffect(detector) { onDispose { detector.close() } }

    Column(Modifier.fillMaxSize()) {
        ChallengeHeader("MISSION 03", "Show you're awake", "Both eyes open. Face centered.")
        Box(Modifier.weight(1f)) {
            CameraCapture(candidate, true, if (processing) "Checking…" else "Take awake selfie", onCaptured = { file ->
                if (!processing) {
                    processing = true
                    val image = runCatching { InputImage.fromFilePath(context, Uri.fromFile(file)) }.getOrElse { feedback = "Could not read selfie. Try again."; processing = false; return@CameraCapture }
                    detector.process(image).addOnSuccessListener { faces ->
                        processing = false
                        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                        val left = face?.leftEyeOpenProbability ?: 0f
                        val right = face?.rightEyeOpenProbability ?: 0f
                        when {
                            face == null -> feedback = "No face found. Center your face and use more light."
                            left >= .62f && right >= .62f -> onComplete()
                            else -> feedback = "Eyes don't look fully open yet. Look straight at the lens and try again."
                        }
                    }.addOnFailureListener { processing = false; feedback = "Face check failed. Hold still and try again." }
                }
            }, onError = { feedback = it })
        }
        Text(feedback, color = Color.White, modifier = Modifier.padding(20.dp).navigationBarsPadding())
    }
}
