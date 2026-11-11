package com.sleeptown.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.sleeptown.demo.ui.theme.SleepTownDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SleepTownDemoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(onStartSleep = { startSleep() }, onReset = { resetState() })
                }
            }
        }
    }

    private fun startSleep() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            return
        }
        SleepService.start(this)
    }

    private fun resetState() {
        SleepService.stop(this)
        SleepService.state.value = SleepService.SleepState.IDLE
        SleepService.buildingRemaining.value = 0
        SleepService.countdownRemaining.value = 0
        SleepService.resultBuilding.value = null
    }

    override fun onStart() {
        super.onStart()
        SleepService.onAppForegrounded()
        MultiplayerManager.get().reportBackApp()
    }
    override fun onStop() {
        super.onStop()
        SleepService.onAppBackgrounded()
        // 用PowerManager同步检查屏幕状态，息屏不上报离开App
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isInteractive) {
            MultiplayerManager.get().reportLeftApp()
        }
    }
}

enum class Tab(val label: String, val icon: String) {
    SLEEP("睡眠", "🌙"), TOWN("小镇", "🏘️"), MP("联机", "🔗"), SETTINGS("设置", "⚙️")
}

@Composable
fun AppNavigation(onStartSleep: () -> Unit, onReset: () -> Unit) {
    var currentTab by remember { mutableStateOf(Tab.SLEEP) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        label = { Text(tab.label) },
                        icon = { Text(tab.icon) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (currentTab) {
                Tab.SLEEP -> SleepScreen(onStartSleep = onStartSleep, onReset = onReset)
                Tab.TOWN -> TownScreen()
                Tab.MP -> RoomScreen(onStartSleep = onStartSleep)
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}
