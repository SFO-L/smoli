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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoomScreen(onStartSleep: () -> Unit) {
    val context = LocalContext.current
    val dataManager = remember { SleepDataManager.get(context) }
    val mp = remember { MultiplayerManager.get() }
    val roomState by mp.roomState.collectAsState()

    var serverUrl by remember { mutableStateOf(dataManager.serverUrl) }
    var joinCode by remember { mutableStateOf("") }

    // 联机游戏开始时自动启动 SleepService
    LaunchedEffect(roomState.isStarted) {
        if (roomState.isStarted && roomState.roomCode.isNotEmpty()) {
            onStartSleep()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("联机睡眠", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(8.dp))
        Text("和好友一起放下手机，共同建造小镇", fontSize = 14.sp, color = Color(0xFF888888))
        Spacer(modifier = Modifier.height(24.dp))

        when {
            // 错误提示
            roomState.errorMessage != null -> {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("出错了", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        Text(roomState.errorMessage!!, fontSize = 14.sp, color = Color(0xFFE53935))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { mp.leaveRoom() }) { Text("返回") }
                    }
                }
            }

            // 未连接服务器
            !roomState.connected -> {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("服务器设置", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("服务器地址 (ws://ip:port)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                dataManager.serverUrl = serverUrl
                                mp.connect(context)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
                        ) { Text("连接服务器", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // 已连接但未在房间中
            roomState.roomCode.isEmpty() -> {
                Text("已连接服务器 ✓", fontSize = 14.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))

                // 创建房间
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏠", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("创建房间", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
                        Text("成为房主，好友通过房间号加入", fontSize = 13.sp, color = Color(0xFF888888))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { mp.createRoom() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
                        ) { Text("创建房间", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 加入房间
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("🔑", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("加入房间", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                        Text("输入房主分享的4位房间号", fontSize = 13.sp, color = Color(0xFF888888))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { if (it.length <= 4) joinCode = it.filter { c -> c.isDigit() } },
                            label = { Text("房间号 (4位数字)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (joinCode.length == 4) {
                                    mp.joinRoom(joinCode)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) { Text("加入", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // 在房间大厅中
            !roomState.isStarted -> {
                RoomLobby(
                    roomState = roomState,
                    dataManager = dataManager,
                    onStart = {
                        val duration = dataManager.sleepDurationSeconds()
                        val countdown = dataManager.countdownDuration
                        mp.startGame(duration, countdown)
                    },
                    onLeave = { mp.leaveRoom() }
                )
            }

            // 游戏进行中
            roomState.isStarted -> {
                RoomInGame(roomState = roomState, onLeave = { mp.leaveRoom() })
            }
        }
    }
}

@Composable
private fun RoomLobby(
    roomState: RoomState,
    dataManager: SleepDataManager,
    onStart: () -> Unit,
    onLeave: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("房间大厅", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
            Spacer(modifier = Modifier.height(16.dp))

            Text("房间号: ${roomState.roomCode}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            if (roomState.isHost) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("把房间号告诉好友，让他们加入", fontSize = 13.sp, color = Color(0xFFAAAAAA))
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text("已连接到房主", fontSize = 14.sp, color = Color(0xFF4CAF50))
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text("成员 (${roomState.members.size}人)", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(12.dp))
            roomState.members.forEach { member ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(member.avatar, fontSize = 28.sp)
                    Spacer(modifier = Modifier.padding(8.dp))
                    Column {
                        Text(
                            member.nickname + if (member.isHost) " (房主)" else "",
                            fontSize = 15.sp,
                            fontWeight = if (member.isHost) FontWeight.Bold else FontWeight.Normal,
                            color = Color(0xFF333333)
                        )
                        Text(if (member.hasLeft) "已离开App" else "在线", fontSize = 11.sp, color = if (member.hasLeft) Color(0xFFE53935) else Color(0xFF4CAF50))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (roomState.isHost) {
                Text("睡眠设置: ${dataManager.sleepTimeStr()} → ${dataManager.wakeTimeStr()} (${dataManager.sleepDurationStr()})", fontSize = 13.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("开始联机睡眠", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            } else {
                Text("等待房主开始...", fontSize = 14.sp, color = Color(0xFF888888), modifier = Modifier.padding(vertical = 16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLeave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935))
            ) { Text("离开房间") }
        }
    }
}

@Composable
private fun RoomInGame(roomState: RoomState, onLeave: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("联机睡眠进行中", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(16.dp))

            roomState.members.forEach { member ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(member.avatar, fontSize = 24.sp)
                    Spacer(modifier = Modifier.padding(6.dp))
                    Column {
                        Text(member.nickname + if (member.isHost) " (房主)" else "", fontSize = 14.sp, color = Color(0xFF333333))
                        Text(
                            if (member.hasLeft) "已离开App!" else "坚持中",
                            fontSize = 12.sp,
                            color = if (member.hasLeft) Color(0xFFE53935) else Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            roomState.penaltyReason?.let { reason ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💥", fontSize = 32.sp)
                        Text("全员惩罚!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        Text(reason, fontSize = 12.sp, color = Color(0xFFE53935))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) { Text("退出房间") }
        }
    }
}
