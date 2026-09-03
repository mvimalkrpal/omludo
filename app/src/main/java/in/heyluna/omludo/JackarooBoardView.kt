package `in`.heyluna.omludo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class JackarooBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Wooden Board Background Paints
    private val woodOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4E342E") // Dark Walnut rim
    }

    private val woodInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#8D6E63") // Warm Maple wood board
    }

    private val woodBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#3E2723")
    }

    // Carved Track Slots (engraved wooden holes)
    private val holeShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#3E2723") // Deep engraved shadow
    }

    private val holeInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#D7CCC8") // Light carved groove
    }

    private val trackLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#5D4037")
    }

    // Vibrant 3D Glossy Marble Paints
    private val playerColors = arrayOf(
        Color.parseColor("#D32F2F"), // 0: Ruby Red
        Color.parseColor("#1976D2"), // 1: Sapphire Blue
        Color.parseColor("#FBC02D"), // 2: Amber Yellow
        Color.parseColor("#388E3C")  // 3: Emerald Green
    )

    private val playerExitColors = arrayOf(
        Color.parseColor("#FF5252"),
        Color.parseColor("#448AFF"),
        Color.parseColor("#FFD740"),
        Color.parseColor("#69F0AE")
    )

    private val marbleHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(180, 255, 255, 255) // Glossy specular reflection
    }

    private val marbleShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(90, 0, 0, 0)
    }

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
        val boardRadius = size * 0.47f
        val trackRadius = size * 0.35f
        val holeRadius = size * 0.020f

        // 1. Draw Luxurious Wooden Board with Rounded Edges & Bevel
        val boardRect = RectF(
            centerX - boardRadius,
            centerY - boardRadius,
            centerX + boardRadius,
            centerY + boardRadius
        )
        canvas.drawRoundRect(boardRect, 48f, 48f, woodOuterPaint)

        val innerRect = RectF(
            centerX - boardRadius + 14f,
            centerY - boardRadius + 14f,
            centerX + boardRadius - 14f,
            centerY + boardRadius - 14f
        )
        canvas.drawRoundRect(innerRect, 40f, 40f, woodInnerPaint)
        canvas.drawRoundRect(innerRect, 40f, 40f, woodBevelPaint)

        // 2. Draw Track Connecting Ring Line
        canvas.drawCircle(centerX, centerY, trackRadius, trackLinePaint)

        // 3. Draw 64 Track Holes (Engraved Wooden Slots)
        for (i in 0 until 64) {
            val angle = Math.toRadians((i * (360.0 / 64.0) - 90))
            val x = centerX + trackRadius * cos(angle).toFloat()
            val y = centerY + trackRadius * sin(angle).toFloat()

            // Outer carved hole shadow
            canvas.drawCircle(x + 1f, y + 2f, holeRadius, holeShadowPaint)

            // Hole base
            val playerExitIndex = i / 16
            if (i % 16 == 0) {
                // Colored Base Exit Spot
                val exitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = playerExitColors[playerExitIndex]
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(x, y, holeRadius * 1.25f, exitPaint)
                canvas.drawCircle(x, y, holeRadius * 1.25f, woodBevelPaint)
            } else {
                canvas.drawCircle(x, y, holeRadius, holeInnerPaint)
            }
        }

        // 4. Draw 4 Corner Team Base Camps
        val baseOffset = size * 0.32f
        val baseOffsets = arrayOf(
            Pair(-baseOffset, -baseOffset), // P0: Top-Left
            Pair(baseOffset, -baseOffset),  // P1: Top-Right
            Pair(baseOffset, baseOffset),   // P2: Bottom-Right
            Pair(-baseOffset, baseOffset)   // P3: Bottom-Left
        )

        for (p in 0..3) {
            val bx = centerX + baseOffsets[p].first
            val by = centerY + baseOffsets[p].second

            // Carved circular recessed base plate
            val basePlatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4E342E")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(bx, by, holeRadius * 3.2f, basePlatePaint)

            val basePlateBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = playerExitColors[p]
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawCircle(bx, by, holeRadius * 3.2f, basePlateBorder)

            // 4 Base Slots
            for (slot in 0..3) {
                val sAngle = Math.toRadians(slot * 90.0 + 45.0)
                val sx = bx + holeRadius * 1.6f * cos(sAngle).toFloat()
                val sy = by + holeRadius * 1.6f * sin(sAngle).toFloat()

                canvas.drawCircle(sx + 1f, sy + 1f, holeRadius * 0.9f, holeShadowPaint)
                canvas.drawCircle(sx, sy, holeRadius * 0.9f, holeInnerPaint)
            }
        }

        // 5. Draw Home Goal Safety Paths (4 slots per player moving toward center)
        for (p in 0..3) {
            val entranceAngle = Math.toRadians(((p * 16 - 1) * (360.0 / 64.0) - 90))
            for (step in 0..3) {
                val homeRadius = trackRadius * (0.80f - step * 0.16f)
                val hx = centerX + homeRadius * cos(entranceAngle).toFloat()
                val hy = centerY + homeRadius * sin(entranceAngle).toFloat()

                // Colored Home slot
                val homeSlotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = playerExitColors[p]
                    style = Paint.Style.FILL
                    alpha = 180
                }
                canvas.drawCircle(hx + 1f, hy + 1f, holeRadius * 0.95f, holeShadowPaint)
                canvas.drawCircle(hx, hy, holeRadius * 0.95f, homeSlotPaint)
                canvas.drawCircle(hx, hy, holeRadius * 0.95f, woodBevelPaint)
            }
        }

        // 6. Draw 3D Glossy Marbles
        marbles?.let { allMarbles ->
            for (playerIdx in allMarbles.indices) {
                val playerMarbles = allMarbles[playerIdx]
                val baseColor = playerColors[playerIdx]

                for (marble in playerMarbles) {
                    var mx = centerX
                    var my = centerY

                    when (marble.zone) {
                        "TRACK" -> {
                            val angle = Math.toRadians((marble.position * (360.0 / 64.0) - 90))
                            mx = centerX + trackRadius * cos(angle).toFloat()
                            my = centerY + trackRadius * sin(angle).toFloat()
                        }
                        "BASE" -> {
                            val bx = centerX + baseOffsets[playerIdx].first
                            val by = centerY + baseOffsets[playerIdx].second
                            val sAngle = Math.toRadians(marble.position * 90.0 + 45.0)
                            mx = bx + holeRadius * 1.6f * cos(sAngle).toFloat()
                            my = by + holeRadius * 1.6f * sin(sAngle).toFloat()
                        }
                        "HOME" -> {
                            val homeRadius = trackRadius * (0.80f - marble.position * 0.16f)
                            val entranceAngle = Math.toRadians(((playerIdx * 16 - 1) * (360.0 / 64.0) - 90))
                            mx = centerX + homeRadius * cos(entranceAngle).toFloat()
                            my = centerY + homeRadius * sin(entranceAngle).toFloat()
                        }
                    }

                    val marbleR = holeRadius * 1.25f

                    // Drop Shadow
                    canvas.drawCircle(mx + 2f, my + 4f, marbleR, marbleShadowPaint)

                    // 3D Spherical Radial Shader
                    val marbleShader = RadialGradient(
                        mx - marbleR * 0.35f,
                        my - marbleR * 0.35f,
                        marbleR * 1.1f,
                        Color.WHITE,
                        baseColor,
                        Shader.TileMode.CLAMP
                    )
                    val marbleBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = marbleShader
                    }
                    canvas.drawCircle(mx, my, marbleR, marbleBodyPaint)

                    // Glass Specular Glint
                    canvas.drawCircle(
                        mx - marbleR * 0.3f,
                        my - marbleR * 0.3f,
                        marbleR * 0.28f,
                        marbleHighlightPaint
                    )
                }
            }
        }
    }
}
