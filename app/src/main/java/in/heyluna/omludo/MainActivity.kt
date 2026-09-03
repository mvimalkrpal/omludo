package `in`.heyluna.omludo

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import `in`.heyluna.omludo.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val socketClient = GameSocketClient()
    private val myUserId = UUID.randomUUID().toString().take(8)
    private var mySeat = 0
    private var currentPhase = "WAITING_FOR_PLAYERS"
    private var selectedMarbleIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
        observeEvents()
    }

    private fun setupListeners() {
        binding.btnCreateRoom.setOnClickListener {
            lifecycleScope.launch {
                val res = socketClient.createRoom()
                if (res != null) {
                    binding.tvRoomCode.text = "Room: ${res.roomCode}"
                    socketClient.connectToRoom(res.roomId, myUserId, "Player 1")
                    Toast.makeText(this@MainActivity, "Room ${res.roomCode} Created!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to create room", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnReady.setOnClickListener {
            Log.d("OmLudo", "Ready button clicked!")
            socketClient.setReady(true)
            binding.btnReady.isEnabled = false
            binding.btnReady.text = "Ready!"
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            socketClient.roomState.collectLatest { state ->
                state ?: return@collectLatest

                mySeat = state.mySeat
                currentPhase = state.phase

                if (state.phase == "PARTNER_SWAP") {
                    binding.tvTurnStatus.text = "Pass a card to partner"
                } else if (state.phase == "PLAYING") {
                    val turnText = if (state.currentTurn == mySeat) "YOUR TURN!" else "P${state.currentTurn}'s Turn"
                    binding.tvTurnStatus.text = turnText
                } else {
                    binding.tvTurnStatus.text = "Waiting for players"
                }

                // Update Jackaroo Board
                binding.boardView.updateMarbles(state.marbles)

                // Render Hand Cards
                renderHandCards(state.myHand, state.currentTurn == mySeat || state.phase == "PARTNER_SWAP")
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            socketClient.events.collectLatest { eventText ->
                try {
                    val json = JSONObject(eventText)
                    val type = json.optString("type")
                    when (type) {
                        "CARDS_DEALT" -> {
                            Toast.makeText(this@MainActivity, "Cards dealt! Tap a card to pass to your partner.", Toast.LENGTH_SHORT).show()
                        }
                        "CARD_SWAPPED_RECEIVED" -> {
                            val card = json.optJSONObject("receivedCard")?.optString("rank") ?: ""
                            Toast.makeText(this@MainActivity, "Received $card from partner!", Toast.LENGTH_SHORT).show()
                        }
                        "MOVE_PLAYED" -> {
                            val player = json.optInt("player")
                            val card = json.optJSONObject("card")?.optString("rank") ?: ""
                            Toast.makeText(this@MainActivity, "P$player played $card", Toast.LENGTH_SHORT).show()
                        }
                        "ERROR" -> {
                            val msg = json.optString("message")
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OmLudo", "Event error: ${e.message}")
                }
            }
        }
    }

    private fun renderHandCards(cards: List<Card>, isInteractive: Boolean) {
        binding.llHandCards.removeAllViews()

        for (card in cards) {
            val suitSymbol = when (card.suit) {
                "HEARTS" -> "♥"
                "DIAMONDS" -> "♦"
                "CLUBS" -> "♣"
                "SPADES" -> "♠"
                else -> card.suit.take(1)
            }
            val isRedSuit = card.suit == "HEARTS" || card.suit == "DIAMONDS"

            val cardContainer = CardView(this).apply {
                radius = 18f
                cardElevation = if (isInteractive) 10f else 4f
                setCardBackgroundColor(if (isInteractive) Color.parseColor("#FFFDF9") else Color.parseColor("#ECEFF1"))
                layoutParams = LinearLayout.LayoutParams(160, 230).apply {
                    setMargins(10, 8, 10, 8)
                }
                setOnClickListener {
                    handleCardAction(card)
                }

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(10, 10, 10, 10)

                    addView(TextView(context).apply {
                        text = "${card.rank} $suitSymbol"
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(if (isRedSuit) Color.parseColor("#C62828") else Color.parseColor("#1B1B1B"))
                        gravity = Gravity.CENTER
                    })

                    val actionDescription = if (currentPhase == "PARTNER_SWAP") {
                        "🤝 Pass"
                    } else {
                        when (card.rank) {
                            "A" -> "✈ Exit/11"
                            "4" -> "⬅ Back 4"
                            "7" -> "🧩 Split 7"
                            "J" -> "🔄 Swap"
                            "K" -> "👑 Exit/13"
                            "Q" -> "12"
                            else -> card.rank
                        }
                    }
                    addView(TextView(context).apply {
                        text = actionDescription
                        textSize = 13f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.parseColor("#5D4037"))
                        gravity = Gravity.CENTER
                        setPadding(0, 4, 0, 0)
                    })
                }
                addView(layout)
            }

            binding.llHandCards.addView(cardContainer)
        }
    }

    private fun handleCardAction(card: Card) {
        if (currentPhase == "PARTNER_SWAP") {
            socketClient.swapCard(card.id)
            Toast.makeText(this, "Passed ${card.rank} to partner!", Toast.LENGTH_SHORT).show()
        } else if (currentPhase == "PLAYING") {
            val action = MoveAction(
                player = mySeat,
                cardId = card.id,
                marbleIndex = selectedMarbleIndex,
                aceChoice = 11
            )
            socketClient.playCard(action)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socketClient.disconnect()
    }
}
