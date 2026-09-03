package `in`.heyluna.omludo

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
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
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val socketClient = GameSocketClient()
    private val myUserId = UUID.randomUUID().toString().take(8)
    private var mySeat = 0
    private var currentPhase = "WAITING_FOR_PLAYERS"

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
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

                binding.tvTurnStatus.text = "Turn: P${state.currentTurn}"

                // Update Jackaroo Board
                binding.boardView.updateMarbles(state.marbles)

                // Render Hand Cards
                renderHandCards(state.myHand)
            }
        }
    }

    private fun renderHandCards(cards: List<Card>) {
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
                cardElevation = 8f
                setCardBackgroundColor(Color.parseColor("#FFFDF9"))
                layoutParams = LinearLayout.LayoutParams(170, 240).apply {
                    setMargins(10, 8, 10, 8)
                }
                setOnClickListener {
                    handleCardClick(card)
                }

                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(12, 12, 12, 12)

                    // Card Rank & Suit Top
                    addView(TextView(context).apply {
                        text = "${card.rank} $suitSymbol"
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(if (isRedSuit) Color.parseColor("#C62828") else Color.parseColor("#1B1B1B"))
                        gravity = Gravity.CENTER
                    })

                    // Center Action Graphic/Subtitle matching Jackaroo cards
                    val actionDescription = when (card.rank) {
                        "A" -> "✈\n1 / 11"
                        "4" -> "⬅\n-4"
                        "7" -> "🧩\nSplit 7"
                        "J" -> "🔄\nSwap"
                        "K" -> "👑\n13"
                        "Q" -> "12"
                        else -> card.rank
                    }
                    addView(TextView(context).apply {
                        text = actionDescription
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.parseColor("#5D4037"))
                        gravity = Gravity.CENTER
                        setPadding(0, 6, 0, 0)
                    })
                }
                addView(layout)
            }

            binding.llHandCards.addView(cardContainer)
        }
    }

    private fun handleCardClick(card: Card) {
        if (currentPhase == "PARTNER_SWAP") {
            socketClient.swapCard(card.id)
            Toast.makeText(this, "Swapped ${card.rank} with partner!", Toast.LENGTH_SHORT).show()
        } else if (currentPhase == "PLAYING") {
            val action = MoveAction(
                player = mySeat,
                cardId = card.id,
                marbleIndex = 0
            )
            socketClient.playCard(action)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socketClient.disconnect()
    }
}
