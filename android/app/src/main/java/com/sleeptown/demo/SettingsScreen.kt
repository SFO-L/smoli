package com.sleeptown.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AVATAR_OPTIONS = listOf("😴", "🦉", "🌙", "⭐", "🛏️", "🐼", "🦊", "🐰", "🐯", "🦁")

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val dataManager = remember { SleepDataManager.get(context) }

    var sleepHour by remember { mutableStateOf(dataManager.sleepStartHour) }
    var sleepMinute by remember { mutableStateOf(dataManager.sleepStartMinute) }
    var wakeHour by remember { mutableStateOf(dataManager.wakeUpHour) }
    var wakeMinute by remember { mutableStateOf(dataManager.wakeUpMinute) }
    var countdownDuration by remember { mutableStateOf(dataManager.countdownDuration) }
    var nickname by remember { mutableStateOf(dataManager.userNickname) }
    var avatar by remember { mutableStateOf(dataManager.userAvatar) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(24.dp))

        // ========== 用户档案 ==========
        Text("个人档案", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(avatar, fontSize = 40.sp)
                    Spacer(modifier = Modifier.padding(8.dp))
                    Column {
                        Text(nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                        Text("ID: ${dataManager.ensureUserId().take(12)}...", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        if (it.length <= 12) {
                            nickname = it
                            dataManager.userNickname = it
                        }
                    },
                    label = { Text("昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("选择头像", fontSize = 14.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AVATAR_OPTIONS.take(5).forEach { emoji ->
                        AvatarChip(emoji, emoji == avatar) {
                            avatar = emoji
                            dataManager.userAvatar = emoji
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AVATAR_OPTIONS.drop(5).forEach { emoji ->
                        AvatarChip(emoji, emoji == avatar) {
                            avatar = emoji
                            dataManager.userAvatar = emoji
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ========== 睡眠起始时间 ==========
        Text("睡眠时间设定", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
        Spacer(modifier = Modifier.height(4.dp))
        Text("从睡眠起始到起床时间，期间不能切出 App", fontSize = 13.sp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(12.dp))

        // 睡眠起始时间
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("入睡时间", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF5C6BC0))
                Spacer(modifier = Modifier.height(12.dp))
                ScrollTimePicker(
                    initialHour = sleepHour,
                    initialMinute = sleepMinute,
                    onTimeChanged = { h, m ->
                        sleepHour = h
                        sleepMinute = m
                        dataManager.sleepStartHour = h
                        dataManager.sleepStartMinute = m
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 起床时间
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("起床时间", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFF9800))
                Spacer(modifier = Modifier.height(12.dp))
                ScrollTimePicker(
                    initialHour = wakeHour,
                    initialMinute = wakeMinute,
                    onTimeChanged = { h, m ->
                        wakeHour = h
                        wakeMinute = m
                        dataManager.wakeUpHour = h
                        dataManager.wakeUpMinute = m
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 睡眠时长汇总
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("睡眠时长", fontSize = 13.sp, color = Color(0xFF666666))
                    Text(dataManager.sleepDurationStr(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${dataManager.sleepTimeStr()} → ${dataManager.wakeTimeStr()}", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ========== 切出宽限时间 ==========
        Text("切出宽限时间", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("切出 App 后的倒计时，超过则房屋倒塌", fontSize = 13.sp, color = Color(0xFF888888))
                Spacer(modifier = Modifier.height(16.dp))
                Text("${countdownDuration} 秒", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                Slider(
                    value = countdownDuration.toFloat(),
                    onValueChange = { countdownDuration = it.toInt() },
                    onValueChangeFinished = { dataManager.countdownDuration = countdownDuration },
                    valueRange = 5f..60f,
                    steps = 0
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("5s (严格)", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    Text("60s (宽松)", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = { dataManager.resetAll() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
        ) {
            Text("重置所有数据", fontSize = 15.sp)
        }
    }
}

@Composable
private fun AvatarChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF5C6BC0) else Color(0xFFE0E0E0)
        )
    ) {
        Text(emoji, fontSize = 20.sp)
    }
}
