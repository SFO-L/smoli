package com.sleeptown.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SleepScreen(onStartSleep: () -> Unit, onReset: () -> Unit) {
    val state by SleepService.state.collectAsState()
    val buildingRemaining by SleepService.buildingRemaining.collectAsState()
    val countdownRemaining by SleepService.countdownRemaining.collectAsState()
    val resultBuilding by SleepService.resultBuilding.collectAsState()

    val context = LocalContext.current
    val dataManager = SleepDataManager.get(context)
    val buildingDuration = dataManager.sleepDurationSeconds()
    val countdownDuration = dataManager.countdownDuration
    val sleepTime = dataManager.sleepTimeStr()
    val wakeTime = dataManager.wakeTimeStr()
    val durationStr = dataManager.sleepDurationStr()
    val stats = dataManager.getStats()

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        when (state) {
            SleepService.SleepState.IDLE -> IdleContent(
                sleepTime = sleepTime,
                wakeTime = wakeTime,
                durationStr = durationStr,
                countdownDuration = countdownDuration,
                currentStreak = stats.currentStreak,
                avatar = dataManager.userAvatar,
                nickname = dataManager.userNickname,
                onStartSleep = onStartSleep
            )
            SleepService.SleepState.BUILDING -> BuildingContent(
                remaining = buildingRemaining,
                total = buildingDuration,
                onReset = onReset
            )
            SleepService.SleepState.COUNTDOWN -> CountdownContent(
                remaining = countdownRemaining,
                total = countdownDuration
            )
            SleepService.SleepState.COLLAPSED -> CollapsedContent(onReset = onReset)
            SleepService.SleepState.SUCCESS -> SuccessContent(
                building = resultBuilding,
                streak = stats.currentStreak,
                onReset = onReset
            )
        }
    }
}

@Composable
private fun IdleContent(
    sleepTime: String,
    wakeTime: String,
    durationStr: String,
    countdownDuration: Int,
    currentStreak: Int,
    avatar: String,
    nickname: String,
    onStartSleep: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        // 用户头像和昵称
        Text(avatar, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(nickname, fontSize = 16.sp, color = Color(0xFF666666))
        Spacer(modifier = Modifier.height(24.dp))

        Text("睡眠小镇", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(8.dp))
        Text("放下手机，建造你的小镇", fontSize = 15.sp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(32.dp))

        if (currentStreak > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("连续打卡", fontSize = 13.sp, color = Color(0xFF888888))
                    Text("$currentStreak 天", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 睡眠时间段卡片
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("睡眠时段", fontSize = 13.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(sleepTime, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
                    Text(" → ", fontSize = 28.sp, color = Color(0xFFAAAAAA))
                    Text(wakeTime, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(durationStr, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(4.dp))
                Text("期间不能切出 App，切出宽限 ${countdownDuration}s", fontSize = 12.sp, color = Color(0xFF888888))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartSleep,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
        ) {
            Text("开始睡眠", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BuildingContent(remaining: Int, total: Int, onReset: () -> Unit) {
    val progress = (total - remaining).toFloat() / total
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🏗️", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("正在建造房屋...", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(24.dp))
        Text("$remaining", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
        Text("秒", fontSize = 16.sp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFFE0E0E0)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text("切出应用将触发倒计时!", fontSize = 14.sp, color = Color(0xFFE53935))
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onReset) { Text("取消建造") }
    }
}

@Composable
private fun CountdownContent(remaining: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("⚠️", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("你离开了应用!", fontSize = 22.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Text("$remaining", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
        Text("秒后房屋倒塌", fontSize = 16.sp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(32.dp))
        Text("快回到 App 拯救你的房屋!", fontSize = 15.sp, color = Color(0xFF666666))
    }
}

@Composable
private fun CollapsedContent(onReset: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("💥", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("房屋已倒塌", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
        Spacer(modifier = Modifier.height(8.dp))
        Text("你在建造期间离开了应用太久", fontSize = 14.sp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(8.dp))
        Text("连续打卡已中断", fontSize = 13.sp, color = Color(0xFFBDBDBD))
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("重新开始", fontSize = 18.sp) }
    }
}

@Composable
private fun SuccessContent(building: BuildingType?, streak: Int, onReset: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(building?.emoji ?: "🏠", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("建造成功!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
        Spacer(modifier = Modifier.height(8.dp))
        if (building != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(building.color).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(building.displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(building.color))
                    Text(building.rarity, fontSize = 13.sp, color = Color(0xFF888888))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("连续打卡 $streak 天", fontSize = 16.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) { Text("再来一次", fontSize = 18.sp) }
    }
}
