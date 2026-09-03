package `in`.heyluna.omludo

data class Card(
    val id: String,
    val suit: String,
    val rank: String
)

data class MarblePosition(
    val player: Int,
    val marbleIndex: Int,
    val zone: String,
    val position: Int
)

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

data class MoveAction(
    val player: Int,
    val cardId: String,
    val marbleIndex: Int? = null,
    val aceChoice: Int? = null,
    val isDiscard: Boolean? = null
)

data class RoomStatePayload(
    val type: String,
    val roomId: String? = null,
    val phase: String,
    val mySeat: Int,
    val currentTurn: Int,
    val turnDeadline: Long? = null,
    val players: List<PublicPlayerInfo?>,
    val myHand: List<Card> = emptyList(),
    val marbles: List<List<MarblePosition>>,
    val lastDiscardedCard: Card? = null,
    val winningTeam: Int? = null
)

data class CreateRoomResponse(
    val roomCode: String,
    val roomId: String
)
