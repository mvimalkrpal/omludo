package in.heyluna.omludo.ui.board

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import in.heyluna.omludo.data.model.MarblePosition
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class JackarooBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#CCCCCC")
    }

    private val trackSlotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E0E0E0")
    }

    private val playerPaints = arrayOf(
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E53935") }, // 0: Red
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E88E5") }, // 1: Blue
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FDD835") }, // 2: Yellow
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#43A047") }  // 3: Green
    )

    private var marbles: List<List<MarblePosition>>? = null

    fun updateMarbles(marblesList: List<List<MarblePosition>>) {
        this.marbles = marblesList
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = size * 0.38f
        val slotRadius = size * 0.022f

        // Draw outer 64 track circular ring
        for (i in 0 until 64) {
            val angle = Math.toRadians((i * (360.0 / 64.0) - 90))
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()

            // Exit spots have player colors
            val playerExitIndex = i / 16
            if (i % 16 == 0) {
                canvas.drawCircle(x, y, slotRadius * 1.3f, playerPaints[playerExitIndex])
            } else {
                canvas.drawCircle(x, y, slotRadius, trackSlotPaint)
                canvas.drawCircle(x, y, slotRadius, trackPaint)
            }
        }

        // Draw 4 Player Bases (Quadrant corners)
        val baseOffset = size * 0.36f
        val baseOffsets = arrayOf(
            Pair(-baseOffset, -baseOffset), // P0: Top-Left
            Pair(baseOffset, -baseOffset),  // P1: Top-Right
            Pair(baseOffset, baseOffset),   // P2: Bottom-Right
            Pair(-baseOffset, baseOffset)   // P3: Bottom-Left
        )

        for (p in 0..3) {
            val bx = centerX + baseOffsets[p].first
            val by = centerY + baseOffsets[p].second
            val pPaint = playerPaints[p]

            // Draw base housing ring
            canvas.drawCircle(bx, by, slotRadius * 2.8f, trackPaint)

            // Draw 4 slots inside base
            for (slot in 0..3) {
                val sAngle = Math.toRadians(slot * 90.0)
                val sx = bx + slotRadius * 1.5f * cos(sAngle).toFloat()
                val sy = by + slotRadius * 1.5f * sin(sAngle).toFloat()
                canvas.drawCircle(sx, sy, slotRadius, trackSlotPaint)
            }
        }

        // Draw Marbles on top
        marbles?.let { allMarbles ->
            for (playerIdx in allMarbles.indices) {
                val playerMarbles = allMarbles[playerIdx]
                val pPaint = playerPaints[playerIdx]

                for (marble in playerMarbles) {
                    var mx = centerX
                    var my = centerY

                    when (marble.zone) {
                        "TRACK" -> {
                            val angle = Math.toRadians((marble.position * (360.0 / 64.0) - 90))
                            mx = centerX + radius * cos(angle).toFloat()
                            my = centerY + radius * sin(angle).toFloat()
                        }
                        "BASE" -> {
                            val bx = centerX + baseOffsets[playerIdx].first
                            val by = centerY + baseOffsets[playerIdx].second
                            val sAngle = Math.toRadians(marble.position * 90.0)
                            mx = bx + slotRadius * 1.5f * cos(sAngle).toFloat()
                            my = by + slotRadius * 1.5f * sin(sAngle).toFloat()
                        }
                        "HOME" -> {
                            // Steps moving toward board center
                            val homeRadius = radius * (0.8f - marble.position * 0.15f)
                            val entranceAngle = Math.toRadians(((playerIdx * 16 - 1) * (360.0 / 64.0) - 90))
                            mx = centerX + homeRadius * cos(entranceAngle).toFloat()
                            my = centerY + homeRadius * sin(entranceAngle).toFloat()
                        }
                    }

                    // Draw marble with border
                    canvas.drawCircle(mx, my, slotRadius * 1.1f, pPaint)
                    canvas.drawCircle(mx, my, slotRadius * 1.1f, trackPaint)
                }
            }
        }
    }
}
