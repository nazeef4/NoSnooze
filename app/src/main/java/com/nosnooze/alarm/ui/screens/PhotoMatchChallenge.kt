package com.nosnooze.alarm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nosnooze.alarm.util.ImageSimilarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable fun PhotoMatchChallenge(referencePath: String, onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var feedback by remember { mutableStateOf("Go to the place or object you selected and frame it like your reference photo.") }
    var processing by remember { mutableStateOf(false) }
    val candidate = remember { File(context.cacheDir, "photo_match_${System.currentTimeMillis()}.jpg") }
    Column(Modifier.fillMaxSize()) {
        ChallengeHeader("MISSION 01", "Find your photo target", "The alarm stops only after a visual match")
        Box(Modifier.weight(1f)) {
            CameraCapture(candidate, false, if (processing) "Checking…" else "Match my photo", onCaptured = { file ->
                if (!processing) scope.launch {
                    processing = true
                    val score = withContext(Dispatchers.Default) { ImageSimilarity.compare(referencePath, file.absolutePath) }
                    processing = false
                    if (score >= .66f) onComplete() else feedback = "Not a close enough match (${(score * 100).toInt()}%). Match the original angle and make sure the target is visible, then try again."
                }
            }, onError = { feedback = it })
        }
        Text(feedback, color = Color.White, modifier = Modifier.padding(20.dp).navigationBarsPadding())
    }
}
