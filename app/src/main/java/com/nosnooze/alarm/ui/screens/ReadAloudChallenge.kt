package com.nosnooze.alarm.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nosnooze.alarm.ui.theme.*
import java.util.Locale

private val passages = listOf(
    "The morning is here and I am fully awake. I will put both feet down and start my day.",
    "I choose to get up now and welcome this morning. My eyes are open and my mind is ready.",
    "Today begins with one strong and deliberate step. I am awake, alert, and ready to move."
)

@Composable fun ReadAloudChallenge(alarmId: Long, onComplete: () -> Unit, notice: String? = null) {
    val context = LocalContext.current
    val passage = remember(alarmId) { passages[(kotlin.math.abs(alarmId) % passages.size).toInt()] }
    var listening by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("Tap the microphone, then read both lines clearly.") }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val recognizer = remember { if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null }

    DisposableEffect(recognizer, passage) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true; result = "Listening…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false; result = "Checking your words…" }
            override fun onError(error: Int) { listening = false; result = if (error == SpeechRecognizer.ERROR_NO_MATCH) "I couldn't catch that. Speak slowly and try again." else "Voice check paused. Tap to try again." }
            override fun onResults(results: Bundle?) {
                listening = false
                val options = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val best = options.maxOfOrNull { spokenScore(passage, it) } ?: 0f
                if (best >= .72f) onComplete() else result = "Almost there (${(best * 100).toInt()}%). Read every word and try once more."
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer?.destroy() }
    }

    fun begin() {
        if (!granted) { permission.launch(Manifest.permission.RECORD_AUDIO); return }
        if (recognizer == null) { result = "Speech recognition is unavailable on this phone."; return }
        recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        })
    }

    Column(Modifier.fillMaxSize()) {
        ChallengeHeader("MISSION 02", "Say it out loud", "Read both lines — clear and confident")
        Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.Center) {
            notice?.let { Text(it, color = Orange, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp)) }
            Surface(shape = RoundedCornerShape(28.dp), color = Color.White) {
                Text(passage.replace(". ", ".\n"), modifier = Modifier.padding(24.dp), color = Ink, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 34.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(result, color = Color.White.copy(.75f), modifier = Modifier.fillMaxWidth(), fontSize = 13.sp)
        }
        Button(onClick = ::begin, enabled = !listening, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp).height(62.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange)) {
            Icon(Icons.Outlined.Mic, null); Spacer(Modifier.width(10.dp)); Text(if (listening) "LISTENING…" else "START READING", fontWeight = FontWeight.Black)
        }
    }
}

private fun spokenScore(expected: String, spoken: String): Float {
    fun words(value: String) = value.lowercase().replace(Regex("[^a-z0-9 ]"), " ").split(Regex("\\s+")).filter { it.isNotBlank() }
    val target = words(expected)
    val heard = words(spoken).toMutableList()
    var found = 0
    target.forEach { word -> val index = heard.indexOf(word); if (index >= 0) { found++; heard.removeAt(index) } }
    return found / target.size.toFloat()
}
