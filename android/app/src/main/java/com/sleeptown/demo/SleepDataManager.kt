package com.sleeptown.demo

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 建筑类型定义 - 10种建筑，从普通到稀有
 */
enum class BuildingType(
    val displayName: String,
    val emoji: String,
    val rarity: String,
    val color: Long
) {
    COTTAGE("小木屋", "🏠", "普通", 0xFF8D6E63),
    BRICK_HOUSE("砖房", "🏡", "普通", 0xFFB71C1C),
    TENT("帐篷", "⛺", "普通", 0xFF6D4C41),
    TREEHOUSE("树屋", "🌲", "普通", 0xFF2E7D32),
    LIGHTHOUSE("灯塔", "🗼", "稀有", 0xFF1565C0),
    WINDMILL("风车", "🌬️", "稀有", 0xFF558B2F),
    CASTLE("城堡", "🏰", "稀有", 0xFF37474F),
    PYRAMID("金字塔", "🔺", "史诗", 0xFFD84315),
    PALACE("宫殿", "🏛️", "史诗", 0xFFAD1457),
    WONDER("世界奇观", "🌟", "传说", 0xFFFFD700),
    ;

    companion object {
        fun random(): BuildingType {
            val weights = listOf(30, 25, 20, 15, 5, 3, 2, 1, 1, 1)
            val total = weights.sum()
            var r = (Math.random() * total).toInt()
            for (i in entries.indices) {
                r -= weights[i]
                if (r < 0) return entries[i]
            }
            return COTTAGE
        }
    }
}

/**
 * 建筑记录 - 每次睡眠的结果
 */
data class BuildingRecord(
    val date: String,
    val timestamp: Long,
    val success: Boolean,
    val buildingType: String?,
    val duration: Int
)

/**
 * 睡眠统计数据
 */
data class SleepStats(
    val totalAttempts: Int,
    val successCount: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val collectedBuildings: Map<String, Int>
) {
    val successRate: Int get() = if (totalAttempts == 0) 0 else (successCount * 100 / totalAttempts)
    val totalBuildings: Int get() = collectedBuildings.values.sum()
}

/**
 * 数据管理器 - 本地持久化（用 SharedPreferences + org.json）
 */
class SleepDataManager private constructor(private val prefs: SharedPreferences) {

    companion object {
        private const val PREFS_NAME = "sleep_town_data"
        private const val KEY_RECORDS = "building_records"
        private const val KEY_BUILDING_DURATION = "building_duration"
        private const val KEY_COUNTDOWN_DURATION = "countdown_duration"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_SLEEP_HOUR = "sleep_start_hour"
        private const val KEY_SLEEP_MINUTE = "sleep_start_minute"
        private const val KEY_USER_NICKNAME = "user_nickname"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_USER_ID = "user_id"

        @Volatile
        private var instance: SleepDataManager? = null

        fun get(context: Context): SleepDataManager {
            return instance ?: synchronized(this) {
                instance ?: SleepDataManager(
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }

    // ==================== 睡眠起始时间 ====================

    var sleepStartHour: Int
        get() = prefs.getInt(KEY_SLEEP_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_SLEEP_HOUR, value.coerceIn(0, 23)).apply()

    var sleepStartMinute: Int
        get() = prefs.getInt(KEY_SLEEP_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_SLEEP_MINUTE, value.coerceIn(0, 59)).apply()

    var wakeUpHour: Int
        get() = prefs.getInt("wake_up_hour", 7)
        set(value) = prefs.edit().putInt("wake_up_hour", value.coerceIn(0, 23)).apply()

    var wakeUpMinute: Int
        get() = prefs.getInt("wake_up_minute", 0)
        set(value) = prefs.edit().putInt("wake_up_minute", value.coerceIn(0, 59)).apply()

    fun sleepTimeStr(): String = String.format("%02d:%02d", sleepStartHour, sleepStartMinute)
    fun wakeTimeStr(): String = String.format("%02d:%02d", wakeUpHour, wakeUpMinute)

    /** 计算睡眠时长（分钟），支持跨午夜 */
    fun sleepDurationMinutes(): Int {
        val start = sleepStartHour * 60 + sleepStartMinute
        val end = wakeUpHour * 60 + wakeUpMinute
        var diff = (end - start + 1440) % 1440
        if (diff == 0) diff = 1
        return diff
    }

    /** 睡眠时长（秒），用于建造计时 */
    fun sleepDurationSeconds(): Int = sleepDurationMinutes() * 60

    /** 格式化睡眠时长，如 "8小时30分钟" */
    fun sleepDurationStr(): String {
        val mins = sleepDurationMinutes()
        val h = mins / 60
        val m = mins % 60
        return if (h > 0 && m > 0) "${h}小时${m}分钟"
        else if (h > 0) "${h}小时"
        else "${m}分钟"
    }

    // ==================== 用户档案（联机准备） ====================

    var userNickname: String
        get() = prefs.getString(KEY_USER_NICKNAME, "睡眠建造师") ?: "睡眠建造师"
        set(value) = prefs.edit().putString(KEY_USER_NICKNAME, value).apply()

    var userAvatar: String
        get() = prefs.getString(KEY_USER_AVATAR, "😴") ?: "😴"
        set(value) = prefs.edit().putString(KEY_USER_AVATAR, value).apply()

    var userId: String
        get() = prefs.getString(KEY_USER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    /** 服务器地址（WebSocket） */
    var serverUrl: String
        get() = prefs.getString("server_url", "ws://YOUR_SERVER_IP:8080") ?: "ws://YOUR_SERVER_IP:8080"
        set(value) = prefs.edit().putString("server_url", value).apply()

    fun ensureUserId(): String {
        val id = userId
        if (id.isEmpty()) {
            val newId = "u_" + System.currentTimeMillis().toString(36)
            userId = newId
            return newId
        }
        return id
    }

    // ==================== 建造时长（保留用于实际计时） ====================

    var buildingDuration: Int
        get() = prefs.getInt(KEY_BUILDING_DURATION, 30)
        set(value) = prefs.edit().putInt(KEY_BUILDING_DURATION, value).apply()

    var countdownDuration: Int
        get() = prefs.getInt(KEY_COUNTDOWN_DURATION, 10)
        set(value) = prefs.edit().putInt(KEY_COUNTDOWN_DURATION, value).apply()

    fun getRecords(): List<BuildingRecord> {
        val json = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                BuildingRecord(
                    date = o.getString("date"),
                    timestamp = o.getLong("timestamp"),
                    success = o.getBoolean("success"),
                    buildingType = if (o.has("type") && !o.isNull("type")) o.getString("type") else null,
                    duration = o.getInt("duration")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun addRecord(record: BuildingRecord) {
        val records = getRecords().toMutableList()
        records.add(record)
        val arr = JSONArray()
        for (r in records) {
            arr.put(JSONObject().apply {
                put("date", r.date)
                put("timestamp", r.timestamp)
                put("success", r.success)
                put("type", r.buildingType)
                put("duration", r.duration)
            })
        }
        prefs.edit().putString(KEY_RECORDS, arr.toString()).apply()

        if (record.success) {
            val streak = prefs.getInt(KEY_CURRENT_STREAK, 0) + 1
            prefs.edit()
                .putInt(KEY_CURRENT_STREAK, streak)
                .putInt(KEY_LONGEST_STREAK, maxOf(streak, prefs.getInt(KEY_LONGEST_STREAK, 0)))
                .apply()
        } else {
            prefs.edit().putInt(KEY_CURRENT_STREAK, 0).apply()
        }
    }

    fun getStats(): SleepStats {
        val records = getRecords()
        val collected = mutableMapOf<String, Int>()
        for (r in records) {
            if (r.success && r.buildingType != null) {
                collected[r.buildingType] = (collected[r.buildingType] ?: 0) + 1
            }
        }
        return SleepStats(
            totalAttempts = records.size,
            successCount = records.count { it.success },
            currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0),
            longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0),
            collectedBuildings = collected
        )
    }

    fun todayStr(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun resetAll() {
        prefs.edit().clear().apply()
    }
}
