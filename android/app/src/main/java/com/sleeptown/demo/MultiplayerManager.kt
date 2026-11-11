package com.sleeptown.demo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 房间成员
 */
data class RoomMember(
    val userId: String,
    val nickname: String,
    val avatar: String,
    val isHost: Boolean = false,
    var hasLeft: Boolean = false
)

/**
 * 联机房间状态
 */
data class RoomState(
    val roomCode: String = "",
    val isHost: Boolean = false,
    val members: List<RoomMember> = emptyList(),
    val isStarted: Boolean = false,
    val durationSeconds: Int = 0,
    val countdownSeconds: Int = 0,
    val penaltyReason: String? = null,
    val errorMessage: String? = null,
    val connected: Boolean = false
)

/**
 * 联机管理器 - WebSocket 客户端
 * 连接到公网服务器，通过房间号创建/加入房间
 */
class MultiplayerManager private constructor() {

    companion object {
        @Volatile
        private var instance: MultiplayerManager? = null
        fun get(): MultiplayerManager = instance ?: synchronized(this) {
            instance ?: MultiplayerManager().also { instance = it }
        }
    }

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    private var myUserId = ""
    private var myNickname = ""
    private var myAvatar = ""

    val roomState = MutableStateFlow(RoomState())

    var multiplayerDuration: Int = 0
        private set
    var multiplayerCountdown: Int = 0
        private set
    var sharedBuildingType: String? = null
        private set

    /** 连接到服务器 */
    fun connect(context: Context, onConnected: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val dataManager = SleepDataManager.get(context)
        myUserId = dataManager.ensureUserId()
        myNickname = dataManager.userNickname
        myAvatar = dataManager.userAvatar
        val url = dataManager.serverUrl

        if (client == null) {
            client = OkHttpClient.Builder()
                .pingInterval(10, TimeUnit.SECONDS)
                .build()
        }

        val request = Request.Builder().url(url).build()
        webSocket = client!!.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                // 注册用户
                send(JSONObject().apply {
                    put("type", "REGISTER")
                    put("userId", myUserId)
                    put("nickname", myNickname)
                    put("avatar", myAvatar)
                })
                roomState.value = roomState.value.copy(connected = true, errorMessage = null)
                onConnected()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerMessage(JSONObject(text))
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                roomState.value = roomState.value.copy(connected = false)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                roomState.value = roomState.value.copy(
                    connected = false,
                    errorMessage = "连接失败: ${t.message}"
                )
                onError(t.message ?: "未知错误")
            }
        })
    }

    private fun send(msg: JSONObject) {
        webSocket?.send(msg.toString())
    }

    private fun handleServerMessage(msg: JSONObject) {
        when (msg.getString("type")) {
            "ROOM_CREATED" -> {
                roomState.value = roomState.value.copy(
                    roomCode = msg.getString("roomCode"),
                    isHost = true
                )
            }
            "ROOM_JOINED" -> {
                roomState.value = roomState.value.copy(
                    roomCode = msg.getString("roomCode"),
                    isHost = false
                )
            }
            "MEMBER_LIST" -> {
                val arr = msg.getJSONArray("members")
                val list = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    RoomMember(
                        userId = o.getString("userId"),
                        nickname = o.getString("nickname"),
                        avatar = o.getString("avatar"),
                        isHost = o.optBoolean("isHost"),
                        hasLeft = o.optBoolean("hasLeft")
                    )
                }
                roomState.value = roomState.value.copy(members = list)
            }
            "GAME_STARTED" -> {
                multiplayerDuration = msg.getInt("durationSeconds")
                multiplayerCountdown = msg.getInt("countdownSeconds")
                sharedBuildingType = msg.optString("buildingType", null)
                roomState.value = roomState.value.copy(
                    isStarted = true,
                    durationSeconds = multiplayerDuration,
                    countdownSeconds = multiplayerCountdown
                )
            }
            "MEMBER_LEFT_APP" -> {
                val uid = msg.getString("userId")
                val updated = roomState.value.members.map {
                    if (it.userId == uid) it.copy(hasLeft = true) else it
                }
                roomState.value = roomState.value.copy(members = updated)
            }
            "MEMBER_BACK_APP" -> {
                val uid = msg.getString("userId")
                val updated = roomState.value.members.map {
                    if (it.userId == uid) it.copy(hasLeft = false) else it
                }
                roomState.value = roomState.value.copy(members = updated)
            }
            "PENALTY" -> {
                roomState.value = roomState.value.copy(
                    penaltyReason = msg.optString("reason", "有人离开太久")
                )
                // 联机惩罚：所有人的房屋坍塌
                val s = SleepService.state.value
                if (s == SleepService.SleepState.BUILDING || s == SleepService.SleepState.COUNTDOWN) {
                    SleepService.forceCollapse()
                }
            }
            "ERROR" -> {
                roomState.value = roomState.value.copy(
                    errorMessage = msg.optString("message", "未知错误")
                )
            }
            "ROOM_DISBANDED" -> {
                roomState.value = RoomState()
            }
        }
    }

    // ==================== 房间操作 ====================

    fun createRoom() {
        send(JSONObject().apply {
            put("type", "CREATE_ROOM")
            put("nickname", myNickname)
            put("avatar", myAvatar)
        })
    }

    fun joinRoom(roomCode: String) {
        send(JSONObject().apply {
            put("type", "JOIN_ROOM")
            put("roomCode", roomCode)
            put("nickname", myNickname)
            put("avatar", myAvatar)
        })
    }

    fun startGame(durationSeconds: Int, countdownSeconds: Int) {
        multiplayerDuration = durationSeconds
        multiplayerCountdown = countdownSeconds
        send(JSONObject().apply {
            put("type", "START_GAME")
            put("durationSeconds", durationSeconds)
            put("countdownSeconds", countdownSeconds)
        })
    }

    fun leaveRoom() {
        send(JSONObject().apply { put("type", "LEAVE_ROOM") })
        roomState.value = RoomState(connected = roomState.value.connected)
    }

    fun reportLeftApp() {
        send(JSONObject().apply { put("type", "LEFT_APP") })
    }

    fun reportBackApp() {
        send(JSONObject().apply { put("type", "BACK_APP") })
    }

    fun reportPenalty(reason: String) {
        send(JSONObject().apply {
            put("type", "PENALTY_REPORT")
            put("reason", reason)
        })
    }

    fun reportGameEnd(success: Boolean, buildingType: String?, duration: Int) {
        send(JSONObject().apply {
            put("type", "GAME_END")
            put("success", success)
            put("buildingType", buildingType)
            put("duration", duration)
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "bye")
        webSocket = null
        roomState.value = RoomState()
    }

    fun isConnected(): Boolean = roomState.value.connected
}
