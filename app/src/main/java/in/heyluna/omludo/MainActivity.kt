package `in`.heyluna.omludo

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            val cardView = TextView(this).apply {
                text = "${card.rank}\n${card.suit.take(1)}"
                textSize = 14f
                setTextColor(if (card.suit == "HEARTS" || card.suit == "DIAMONDS") Color.RED else Color.BLACK)
                setBackgroundColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(24, 16, 24, 16)
                layoutParams = android.widget.LinearLayout.LayoutParams(140, 180).apply {
                    setMargins(8, 0, 8, 0)
                }
                setOnClickListener {
                    handleCardClick(card)
                }
            }
            binding.llHandCards.addView(cardView)
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
