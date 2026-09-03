package `in`.heyluna.omludo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

class GameSocketClient(
    private val baseUrl: String = "https://omludo.mvimalkrpal.workers.dev"
) {
    private val client = OkHttpClient.Builder().build()
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
                val json = JSONObject(body)
                return@withContext CreateRoomResponse(
                    roomCode = json.getString("roomCode"),
                    roomId = json.getString("roomId")
                )
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
                val json = JSONObject(body)
                return@withContext CreateRoomResponse(
                    roomCode = json.getString("roomCode"),
                    roomId = json.getString("roomId")
                )
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
                        if (json.optString("type") == "ROOM_STATE") {
                            val parsedState = parseRoomState(json)
                            _roomState.value = parsedState
                        } else {
                            _events.emit(text)
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

    private fun parseRoomState(json: JSONObject): RoomStatePayload {
        val myHandArray = json.optJSONArray("myHand") ?: JSONArray()
        val hand = mutableListOf<Card>()
        for (i in 0 until myHandArray.length()) {
            val c = myHandArray.getJSONObject(i)
            hand.add(Card(id = c.getString("id"), suit = c.getString("suit"), rank = c.getString("rank")))
        }

        val marblesArray = json.optJSONArray("marbles") ?: JSONArray()
        val allMarbles = mutableListOf<List<MarblePosition>>()
        for (p in 0 until marblesArray.length()) {
            val pArray = marblesArray.getJSONArray(p)
            val pMarbles = mutableListOf<MarblePosition>()
            for (m in 0 until pArray.length()) {
                val mObj = pArray.getJSONObject(m)
                pMarbles.add(
                    MarblePosition(
                        player = mObj.getInt("player"),
                        marbleIndex = mObj.getInt("marbleIndex"),
                        zone = mObj.getString("zone"),
                        position = mObj.getInt("position")
                    )
                )
            }
            allMarbles.add(pMarbles)
        }

        val playersArray = json.optJSONArray("players") ?: JSONArray()
        val players = mutableListOf<PublicPlayerInfo?>()
        for (i in 0 until playersArray.length()) {
            if (playersArray.isNull(i)) {
                players.add(null)
            } else {
                val p = playersArray.getJSONObject(i)
                players.add(
                    PublicPlayerInfo(
                        seat = p.getInt("seat"),
                        userId = p.getString("userId"),
                        name = p.getString("name"),
                        isReady = p.getBoolean("isReady"),
                        isConnected = p.getBoolean("isConnected"),
                        isMuted = p.optBoolean("isMuted", false),
                        isSpeaking = p.optBoolean("isSpeaking", false),
                        cardCount = p.getInt("cardCount"),
                        hasSwappedCard = p.getBoolean("hasSwappedCard"),
                        hasFinishedAllMarbles = p.getBoolean("hasFinishedAllMarbles"),
                        voiceSessionId = p.optString("voiceSessionId", null)
                    )
                )
            }
        }

        return RoomStatePayload(
            type = "ROOM_STATE",
            roomId = json.optString("roomId"),
            phase = json.getString("phase"),
            mySeat = json.getInt("mySeat"),
            currentTurn = json.getInt("currentTurn"),
            turnDeadline = if (json.has("turnDeadline") && !json.isNull("turnDeadline")) json.getLong("turnDeadline") else null,
            players = players,
            myHand = hand,
            marbles = allMarbles,
            winningTeam = if (json.has("winningTeam") && !json.isNull("winningTeam")) json.getInt("winningTeam") else null
        )
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
