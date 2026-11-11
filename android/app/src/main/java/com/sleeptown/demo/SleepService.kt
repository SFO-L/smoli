package com.sleeptown.demo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SleepService : Service() {

    enum class SleepState { IDLE, BUILDING, COUNTDOWN, COLLAPSED, SUCCESS }

    companion object {
        private const val CHANNEL_ID = "sleep_channel"
        private const val NOTIFICATION_ID = 1

        val state = MutableStateFlow(SleepState.IDLE)
        val buildingRemaining = MutableStateFlow(0)
        val countdownRemaining = MutableStateFlow(0)
        val resultBuilding = MutableStateFlow<BuildingType?>(null)

        private var instance: SleepService? = null

        @Volatile
        var isScreenOff = false

        fun onAppBackgrounded() { instance?.handleAppBackgrounded() }
        fun onAppForegrounded() { instance?.handleAppForegrounded() }

        fun forceCollapse() {
            instance?.let { svc ->
                svc.buildingJob?.cancel()
                svc.countdownJob?.cancel()
                svc.removeOverlay()
                state.value = SleepState.COLLAPSED
                svc.updateNotification("联机惩罚! 房屋已倒塌!")
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, SleepService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SleepService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: TextView? = null
    private var serviceScope: CoroutineScope? = null
    private var buildingJob: Job? = null
    private var countdownJob: Job? = null
    private lateinit var dataManager: SleepDataManager
    private var buildingDuration = 30
    private var countdownDuration = 10

    override fun onCreate() {
        super.onCreate()
        instance = this
        dataManager = SleepDataManager.get(this)
        val mp = MultiplayerManager.get()
        if (mp.multiplayerDuration > 0) {
            buildingDuration = mp.multiplayerDuration
            countdownDuration = mp.multiplayerCountdown
        } else {
            buildingDuration = dataManager.sleepDurationSeconds()
            countdownDuration = dataManager.countdownDuration
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("正在建造房屋..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startBuilding()
        return START_NOT_STICKY
    }

    private fun startBuilding() {
        state.value = SleepState.BUILDING
        buildingRemaining.value = buildingDuration
        resultBuilding.value = null
        buildingJob?.cancel()
        buildingJob = serviceScope?.launch {
            while (buildingRemaining.value > 0) {
                delay(1000)
                buildingRemaining.value--
                updateNotification("正在建造房屋... 剩余 ${buildingRemaining.value}s")
            }
            // 建造成功
            val building = determineBuilding()
            resultBuilding.value = building
            dataManager.addRecord(BuildingRecord(
                date = dataManager.todayStr(),
                timestamp = System.currentTimeMillis(),
                success = true,
                buildingType = building.name,
                duration = buildingDuration
            ))
            state.value = SleepState.SUCCESS
            updateNotification("建造成功! 获得${building.displayName}")
            delay(3000)
            stopSelf()
        }
    }

    private fun determineBuilding(): BuildingType {
        // 联机模式：使用服务器指定的共同建筑类型
        val shared = MultiplayerManager.get().sharedBuildingType
        if (!shared.isNullOrEmpty()) {
            return try { BuildingType.valueOf(shared) } catch (e: Exception) { BuildingType.random() }
        }
        return BuildingType.random()
    }

    private fun handleAppBackgrounded() {
        // 用PowerManager同步检查屏幕状态，息屏不触发倒计时
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (state.value == SleepState.BUILDING && pm.isInteractive) {
            buildingJob?.cancel()
            state.value = SleepState.COUNTDOWN
            countdownRemaining.value = countdownDuration
            showOverlay()
            startCountdown()
        }
    }

    private fun handleAppForegrounded() {
        if (state.value == SleepState.COUNTDOWN) {
            countdownJob?.cancel()
            removeOverlay()
            countdownRemaining.value = 0
            state.value = SleepState.BUILDING
            buildingJob = serviceScope?.launch {
                while (buildingRemaining.value > 0) {
                    delay(1000)
                    buildingRemaining.value--
                    updateNotification("正在建造房屋... 剩余 ${buildingRemaining.value}s")
                }
                val building = BuildingType.random()
                resultBuilding.value = building
                dataManager.addRecord(BuildingRecord(
                    date = dataManager.todayStr(),
                    timestamp = System.currentTimeMillis(),
                    success = true,
                    buildingType = building.name,
                    duration = buildingDuration
                ))
                state.value = SleepState.SUCCESS
                updateNotification("建造成功! 获得${building.displayName}")
                delay(3000)
                stopSelf()
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = serviceScope?.launch {
            while (countdownRemaining.value > 0) {
                updateOverlayText()
                delay(1000)
                countdownRemaining.value--
            }
            // 倒计时结束 -> 房屋倒塌
            dataManager.addRecord(BuildingRecord(
                date = dataManager.todayStr(),
                timestamp = System.currentTimeMillis(),
                success = false,
                buildingType = null,
                duration = buildingDuration
            ))
            // 联机模式：上报惩罚
            val mp = MultiplayerManager.get()
            if (mp.roomState.value.roomCode.isNotEmpty()) {
                mp.reportPenalty("有人离开App超过${countdownDuration}秒")
            }
            state.value = SleepState.COLLAPSED
            removeOverlay()
            updateNotification("房屋已倒塌!")
            delay(3000)
            stopSelf()
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) return
        val textView = TextView(this).apply {
            text = "快回到睡眠小镇! 倒计时: ${countdownRemaining.value}s"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(220, 200, 0, 0))
            setPadding(48, 48, 48, 48)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP }
        try {
            windowManager.addView(textView, params)
            overlayView = textView
        } catch (e: Exception) {}
    }

    private fun updateOverlayText() {
        overlayView?.text = "快回到睡眠小镇! 倒计时: ${countdownRemaining.value}s"
    }

    private fun removeOverlay() {
        overlayView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        overlayView = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "睡眠监督", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("睡眠小镇")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        buildingJob?.cancel()
        countdownJob?.cancel()
        serviceScope?.cancel()
        removeOverlay()
        state.value = SleepState.IDLE
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
