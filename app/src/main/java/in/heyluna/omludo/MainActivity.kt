package `in`.heyluna.omludo

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
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
        // Enforce Day Mode Only across the entire application
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

                // Update room code and phase banner
                binding.tvTurnStatus.text = "Phase: ${state.phase} | Turn: P${state.currentTurn}"

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
                radius = 16f
                cardElevation = 6f
                setCardBackgroundColor(Color.WHITE)
                layoutParams = android.widget.LinearLayout.LayoutParams(140, 190).apply {
                    setMargins(10, 8, 10, 8)
                }
                setOnClickListener {
                    handleCardClick(card)
                }

                val textView = TextView(context).apply {
                    text = "${card.rank}\n$suitSymbol"
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (isRedSuit) Color.parseColor("#D32F2F") else Color.parseColor("#212121"))
                    gravity = Gravity.CENTER
                }
                addView(textView)
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
