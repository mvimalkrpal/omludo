package `in`.heyluna.omludo

import android.content.Context
import android.graphics.*
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

    // Ornate Wooden Border & Felt Fill
    private val boardGoldTrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        color = Color.parseColor("#D4AF37") // Gold carved border
    }

    private val boardWoodRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#B8860B") // Golden Oak wood edge
    }

    private val boardInnerFeltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F5E6CA") // Premium Sand Beige Felt
    }

    private val boardCenterEmbossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#E5D3B3")
    }

    // Carved Pit Holes (Wooden Board indents)
    private val pitShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#A67C52")
    }

    private val pitInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#8C6239")
    }

    private val basePlateBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#EAD7B8")
    }

    private val basePlateBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#C8AC85")
    }

    // 4 Team Colors
    private val playerColors = arrayOf(
        Color.parseColor("#43A047"), // P0 (Bottom): Emerald Green
        Color.parseColor("#E53935"), // P1 (Right): Ruby Red
        Color.parseColor("#1E88E5"), // P2 (Top): Sapphire Blue
        Color.parseColor("#FDD835")  // P3 (Left): Amber Yellow
    )

    private val playerHighlightColors = arrayOf(
        Color.parseColor("#A5D6A7"),
        Color.parseColor("#EF9A9A"),
        Color.parseColor("#90CAF9"),
        Color.parseColor("#FFF59D")
    )

    private val marbleHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(220, 255, 255, 255)
    }

    private val marbleShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(100, 0, 0, 0)
    }

    private val turnTimerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#4CAF50")
    }

    private val turnTimerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        color = Color.parseColor("#423020")
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
        val R = size * 0.46f // Board Radius

        // 1. Draw Ornate Moroccan/Arabesque 4-Lobed Wood & Gold Trim Board
        val boardPath = Path()
        val petalCount = 8
        for (i in 0..360 step 5) {
            val rad = Math.toRadians(i.toDouble())
            // Subtle petal curvature matching reference screenshot
            val rCurv = R + (size * 0.025f) * cos(petalCount * rad).toFloat()
            val x = centerX + rCurv * cos(rad).toFloat()
            val y = centerY + rCurv * sin(rad).toFloat()
            if (i == 0) boardPath.moveTo(x, y) else boardPath.lineTo(x, y)
        }
        boardPath.close()

        // Draw Board Shadow
        canvas.drawCircle(centerX, centerY + 8f, R, marbleShadowPaint)
        // Draw Outer Wood Trim
        canvas.drawPath(boardPath, boardWoodRimPaint)
        // Draw Gold Trim Edge
        canvas.drawPath(boardPath, boardGoldTrimPaint)

        // Draw Inner Warm Felt Playing Field
        val innerPath = Path()
        for (i in 0..360 step 5) {
            val rad = Math.toRadians(i.toDouble())
            val rCurv = (R - 14f) + (size * 0.024f) * cos(petalCount * rad).toFloat()
            val x = centerX + rCurv * cos(rad).toFloat()
            val y = centerY + rCurv * sin(rad).toFloat()
            if (i == 0) innerPath.moveTo(x, y) else innerPath.lineTo(x, y)
        }
        innerPath.close()
        canvas.drawPath(innerPath, boardInnerFeltPaint)

        // 2. Draw Center Medallion with Card Suits Embossing
        val centerRadius = size * 0.16f
        canvas.drawCircle(centerX, centerY, centerRadius, basePlateBackgroundPaint)
        canvas.drawCircle(centerX, centerY, centerRadius, boardCenterEmbossPaint)
        canvas.drawCircle(centerX, centerY, centerRadius * 0.85f, boardCenterEmbossPaint)

        // Draw Turn Countdown Arc in the Center (Matching reference)
        val timerRect = RectF(
            centerX - centerRadius * 0.72f,
            centerY - centerRadius * 0.72f,
            centerX + centerRadius * 0.72f,
            centerY + centerRadius * 0.72f
        )
        canvas.drawArc(timerRect, 120f, 100f, false, turnTimerBgPaint)
        canvas.drawArc(timerRect, 120f, 65f, false, turnTimerPaint)

        // 3. Draw 4 Corner Flower Base Camps (Top-Left, Top-Right, Bottom-Right, Bottom-Left)
        val baseDist = size * 0.29f
        val baseCoords = arrayOf(
            Pair(-baseDist, baseDist),  // P0 (Bottom-Left): Green
            Pair(baseDist, baseDist),   // P1 (Bottom-Right): Red
            Pair(baseDist, -baseDist),  // P2 (Top-Right): Blue
            Pair(-baseDist, -baseDist)  // P3 (Top-Left): Yellow
        )

        val slotR = size * 0.016f

        for (p in 0..3) {
            val bx = centerX + baseCoords[p].first
            val by = centerY + baseCoords[p].second

            // Draw Flower/Clover shape for base
            canvas.drawCircle(bx, by, slotR * 3.4f, basePlateBackgroundPaint)
            canvas.drawCircle(bx, by, slotR * 3.4f, basePlateBorderPaint)

            // 4 Base Slots inside flower
            for (slot in 0..3) {
                val sAngle = Math.toRadians(slot * 90.0 + 45.0)
                val sx = bx + slotR * 1.6f * cos(sAngle).toFloat()
                val sy = by + slotR * 1.6f * sin(sAngle).toFloat()

                canvas.drawCircle(sx, sy, slotR, pitShadowPaint)
                canvas.drawCircle(sx, sy, slotR * 0.82f, pitInnerPaint)
            }
        }

        // 4. Draw Jackaroo Cross-Track and Home Goal Paths
        val stepSpacing = size * 0.039f
        val innerTrackOffset = size * 0.075f

        for (quadrant in 0..3) {
            val angleDeg = quadrant * 90.0

            // Draw 4 Colored Home Slots (Column heading into the center)
            for (h in 0..3) {
                val dist = (h + 2) * stepSpacing + size * 0.05f
                val hAngle = Math.toRadians(angleDeg + 90.0)
                val hx = centerX + dist * cos(hAngle).toFloat()
                val hy = centerY + dist * sin(hAngle).toFloat()

                val homeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = playerColors[quadrant]
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }
                canvas.drawCircle(hx, hy, slotR * 1.15f, pitShadowPaint)
                canvas.drawCircle(hx, hy, slotR * 0.95f, homeFill)
            }

            // Draw 2 Parallel Rows of 8 Pits each along the 4 Cross Wings
            for (row in listOf(-innerTrackOffset, innerTrackOffset)) {
                for (step in 0..7) {
                    val dist = (step + 2) * stepSpacing + size * 0.05f
                    val rad = Math.toRadians(angleDeg + 90.0)

                    val px = centerX + dist * cos(rad).toFloat() - row * sin(rad).toFloat()
                    val py = centerY + dist * sin(rad).toFloat() + row * cos(rad).toFloat()

                    // Check if Base Exit Diamond Slot
                    if (step == 7 && row == innerTrackOffset) {
                        val diamondPath = Path().apply {
                            moveTo(px, py - slotR * 1.5f)
                            lineTo(px + slotR * 1.5f, py)
                            lineTo(px, py + slotR * 1.5f)
                            lineTo(px - slotR * 1.5f, py)
                            close()
                        }
                        val exitFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = playerColors[quadrant]
                            style = Paint.Style.FILL
                        }
                        canvas.drawPath(diamondPath, exitFill)
                        canvas.drawPath(diamondPath, boardCenterEmbossPaint)
                    } else {
                        canvas.drawCircle(px, py, slotR, pitShadowPaint)
                        canvas.drawCircle(px, py, slotR * 0.82f, pitInnerPaint)
                    }
                }
            }
        }

        // 5. Draw 3D Jewel/Glass Spherical Marbles
        marbles?.let { allMarbles ->
            for (playerIdx in allMarbles.indices) {
                val playerMarbles = allMarbles[playerIdx]
                val baseColor = playerColors[playerIdx]
                val highlightColor = playerHighlightColors[playerIdx]

                for (marble in playerMarbles) {
                    var mx = centerX
                    var my = centerY

                    when (marble.zone) {
                        "BASE" -> {
                            val bx = centerX + baseCoords[playerIdx].first
                            val by = centerY + baseCoords[playerIdx].second
                            val sAngle = Math.toRadians(marble.position * 90.0 + 45.0)
                            mx = bx + slotR * 1.6f * cos(sAngle).toFloat()
                            my = by + slotR * 1.6f * sin(sAngle).toFloat()
                        }
                        "HOME" -> {
                            val angleDeg = playerIdx * 90.0
                            val dist = (marble.position + 2) * stepSpacing + size * 0.05f
                            val hAngle = Math.toRadians(angleDeg + 90.0)
                            mx = centerX + dist * cos(hAngle).toFloat()
                            my = centerY + dist * sin(hAngle).toFloat()
                        }
                        "TRACK" -> {
                            // Map 0..63 onto the 4 cross wings
                            val quad = (marble.position / 16)
                            val stepInQuad = marble.position % 16
                            val angleDeg = quad * 90.0
                            val rad = Math.toRadians(angleDeg + 90.0)

                            val (dist, row) = if (stepInQuad < 8) {
                                Pair((stepInQuad + 2) * stepSpacing + size * 0.05f, -innerTrackOffset)
                            } else {
                                Pair((15 - stepInQuad + 2) * stepSpacing + size * 0.05f, innerTrackOffset)
                            }

                            mx = centerX + dist * cos(rad).toFloat() - row * sin(rad).toFloat()
                            my = centerY + dist * sin(rad).toFloat() + row * cos(rad).toFloat()
                        }
                    }

                    val mRadius = slotR * 1.45f

                    // Marble Drop Shadow
                    canvas.drawCircle(mx + 3f, my + 5f, mRadius, marbleShadowPaint)

                    // 3D Glass Radial Gradient
                    val gradient = RadialGradient(
                        mx - mRadius * 0.35f,
                        my - mRadius * 0.35f,
                        mRadius * 1.25f,
                        highlightColor,
                        baseColor,
                        Shader.TileMode.CLAMP
                    )
                    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = gradient
                    }
                    canvas.drawCircle(mx, my, mRadius, bodyPaint)

                    // Inner Rim & Glint
                    canvas.drawCircle(
                        mx - mRadius * 0.32f,
                        my - mRadius * 0.32f,
                        mRadius * 0.35f,
                        marbleHighlightPaint
                    )
                }
            }
        }
    }
}
