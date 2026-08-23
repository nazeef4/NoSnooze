package com.nosnooze.alarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosnooze.alarm.ui.theme.Orange

@Composable fun ChallengeHeader(kicker: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).background(Orange, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Alarm, null, tint = Color.White) }
        Spacer(Modifier.height(12.dp))
        Text(kicker, color = Orange, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 2.sp)
        Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 30.sp)
        Text(subtitle, color = Color.White.copy(.6f), fontSize = 13.sp)
    }
}
