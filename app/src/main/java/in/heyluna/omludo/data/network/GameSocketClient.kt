package in.heyluna.omludo.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import in.heyluna.omludo.data.model.CreateRoomResponse
import in.heyluna.omludo.data.model.MoveAction
import in.heyluna.omludo.data.model.RoomStatePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class GameSocketClient(
    private val baseUrl: String = "https://omludo.mvimalkrpal.workers.dev"
) {
    private val client = OkHttpClient.Builder().build()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val roomStateAdapter = moshi.adapter(RoomStatePayload::class.java)

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _roomState = MutableStateFlow<RoomStatePayload?>(null)
    val roomState = _roomState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    suspend fun createRoom(): CreateRoomResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/rooms/create")
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                return@withContext moshi.adapter(CreateRoomResponse::class.java).fromJson(body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun lookupRoom(roomCode: String): CreateRoomResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/rooms/$roomCode")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                return@withContext moshi.adapter(CreateRoomResponse::class.java).fromJson(body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun connectToRoom(roomId: String, userId: String, userName: String, preferredSeat: Int? = null) {
        val wsUrl = baseUrl.replace("https://", "wss://").replace("http://", "ws://") + "/ws/room/$roomId"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Send JOIN message
                val joinMsg = JSONObject().apply {
                    put("type", "JOIN")
                    put("userId", userId)
                    put("name", userName)
                    if (preferredSeat != null) put("preferredSeat", preferredSeat)
                }
                webSocket.send(joinMsg.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    try {
                        val json = JSONObject(text)
                        when (json.optString("type")) {
                            "ROOM_STATE" -> {
                                val state = roomStateAdapter.fromJson(text)
                                _roomState.value = state
                            }
                            else -> {
                                _events.emit(text)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch {
                    _events.emit(JSONObject().put("type", "CONNECTION_ERROR").put("message", t.message).toString())
                }
            }
        })
    }

    fun setReady(isReady: Boolean) {
        val msg = JSONObject().apply {
            put("type", "READY")
            put("isReady", isReady)
        }
        webSocket?.send(msg.toString())
    }

    fun swapCard(cardId: String) {
        val msg = JSONObject().apply {
            put("type", "SWAP_CARD")
            put("cardId", cardId)
        }
        webSocket?.send(msg.toString())
    }

    fun playCard(action: MoveAction) {
        val actionJson = JSONObject().apply {
            put("player", action.player)
            put("cardId", action.cardId)
            if (action.marbleIndex != null) put("marbleIndex", action.marbleIndex)
            if (action.aceChoice != null) put("aceChoice", action.aceChoice)
            if (action.isDiscard != null) put("isDiscard", action.isDiscard)
        }
        val msg = JSONObject().apply {
            put("type", "PLAY_CARD")
            put("action", actionJson)
        }
        webSocket?.send(msg.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User Left")
        webSocket = null
    }
}
