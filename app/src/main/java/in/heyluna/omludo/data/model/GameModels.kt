package in.heyluna.omludo.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Card(
    val id: String,
    val suit: String, // HEARTS, DIAMONDS, CLUBS, SPADES
    val rank: String  // A, 2..10, J, Q, K
)

@JsonClass(generateAdapter = true)
data class MarblePosition(
    val player: Int,       // 0..3
    val marbleIndex: Int,  // 0..3
    val zone: String,      // BASE, TRACK, HOME
    val position: Int      // 0..63 for TRACK, 0..3 for BASE/HOME
)

@JsonClass(generateAdapter = true)
data class PublicPlayerInfo(
    val seat: Int,
    val userId: String,
    val name: String,
    val isReady: Boolean,
    val isConnected: Boolean,
    val isMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val cardCount: Int,
    val hasSwappedCard: Boolean,
    val hasFinishedAllMarbles: Boolean,
    val voiceSessionId: String? = null
)

@JsonClass(generateAdapter = true)
data class MoveAction(
    val player: Int,
    val cardId: String,
    val marbleIndex: Int? = null,
    val aceChoice: Int? = null,
    val isDiscard: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class RoomStatePayload(
    val type: String,
    val roomId: String? = null,
    val phase: String, // WAITING_FOR_PLAYERS, PARTNER_SWAP, PLAYING, GAME_OVER
    val mySeat: Int,
    val currentTurn: Int,
    val turnDeadline: Long? = null,
    val players: List<PublicPlayerInfo?>,
    val myHand: List<Card> = emptyList(),
    val marbles: List<List<MarblePosition>>,
    val lastDiscardedCard: Card? = null,
    val winningTeam: Int? = null
)

@JsonClass(generateAdapter = true)
data class CreateRoomResponse(
    val roomCode: String,
    val roomId: String
)
