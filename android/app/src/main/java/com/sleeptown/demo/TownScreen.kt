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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TownScreen() {
    val context = LocalContext.current
    val dataManager = remember { SleepDataManager.get(context) }
    val stats = remember { dataManager.getStats() }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("我的小镇", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(20.dp))

        // 统计卡片
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(modifier = Modifier.weight(1f), title = "总建筑", value = stats.totalBuildings.toString(), color = Color(0xFF5C6BC0))
            StatCard(modifier = Modifier.weight(1f), title = "成功率", value = "${stats.successRate}%", color = Color(0xFF4CAF50))
            StatCard(modifier = Modifier.weight(1f), title = "最长连胜", value = "${stats.longestStreak}天", color = Color(0xFFFF9800))
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(modifier = Modifier.weight(1f), title = "当前连胜", value = "${stats.currentStreak}天", color = Color(0xFFEF6C00))
            StatCard(modifier = Modifier.weight(1f), title = "总尝试", value = stats.totalAttempts.toString(), color = Color(0xFF78909C))
            StatCard(modifier = Modifier.weight(1f), title = "成功次数", value = stats.successCount.toString(), color = Color(0xFF66BB6A))
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("建筑图鉴", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(12.dp))

        // 建筑图鉴网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(BuildingType.entries.toList()) { building ->
                val count = stats.collectedBuildings[building.name] ?: 0
                BuildingCard(building = building, count = count)
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 12.sp, color = Color(0xFF888888))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun BuildingCard(building: BuildingType, count: Int) {
    val locked = count == 0
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (locked) Color(0xFFF5F5F5) else Color(building.color).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (locked) "❓" else building.emoji,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (locked) "???" else building.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (locked) Color(0xFFBDBDBD) else Color(building.color),
                textAlign = TextAlign.Center
            )
            if (!locked) {
                Text("x$count", fontSize = 10.sp, color = Color(0xFF888888))
            }
        }
    }
}
