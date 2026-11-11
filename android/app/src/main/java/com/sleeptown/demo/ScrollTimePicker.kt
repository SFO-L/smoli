package com.sleeptown.demo

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 上下滚动时间选择器 - 小时(0-23) + 分钟(0-59)
 */
@Composable
fun ScrollTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelColumn(
            items = (0..23).toList(),
            initial = hour,
            label = "时"
        ) { selected ->
            if (selected != hour) {
                hour = selected
                onTimeChanged(hour, minute)
            }
        }
        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C6BC0))
        WheelColumn(
            items = (0..59).toList(),
            initial = minute,
            label = "分"
        ) { selected ->
            if (selected != minute) {
                minute = selected
                onTimeChanged(hour, minute)
            }
        }
    }
}

@Composable
private fun WheelColumn(
    items: List<Int>,
    initial: Int,
    label: String,
    onSelected: (Int) -> Unit
) {
    val itemHeight = 44.dp
    val visibleCount = 5
    val state = rememberLazyListState(initial.coerceIn(0, items.size - 1))
    val scope = rememberCoroutineScope()

    // 滚动停止时 snap 到最近项
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .collect {
                val offset = state.firstVisibleItemScrollOffset
                val idx = state.firstVisibleItemIndex
                if (offset > 0) {
                    state.animateScrollToItem(idx + 1)
                }
            }
    }

    // 实时更新选中值
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val safe = index.coerceIn(0, items.size - 1)
                onSelected(items[safe])
            }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LazyColumn(
            state = state,
            modifier = Modifier
                .height(itemHeight * visibleCount)
                .width(72.dp),
            contentPadding = PaddingValues(
                top = itemHeight * ((visibleCount - 1) / 2),
                bottom = itemHeight * ((visibleCount - 1) / 2)
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = rememberSnapFlingBehavior(state)
        ) {
            items(items.size) { i ->
                val isSelected = i == state.firstVisibleItemIndex
                Text(
                    text = String.format("%02d", items[i]),
                    fontSize = if (isSelected) 24.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF5C6BC0) else Color(0xFFBDBDBD),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.height(itemHeight)
                )
            }
        }
        Text(label, fontSize = 12.sp, color = Color(0xFF888888))
    }
}
