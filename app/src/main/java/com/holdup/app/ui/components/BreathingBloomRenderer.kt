package com.holdup.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure Canvas rendering helpers for the Sacred Lotus mindful breathing visuals.
 * Renders glowing ambient auras, multi-layered rotating organic petals, floating stardust,
 * and a perimeter progress halo.
 */
object BreathingBloomRenderer {

    data class ParticleSpec(
        val baseAngle: Float,
        val distanceFraction: Float,
        val radiusDp: Float,
        val alpha: Float,
        val speedMultiplier: Float
    )

    val defaultParticles = listOf(
        ParticleSpec(0.35f, 0.72f, 2.2f, 0.40f, 1.0f),
        ParticleSpec(1.10f, 0.88f, 1.8f, 0.35f, 0.8f),
        ParticleSpec(1.95f, 0.65f, 2.5f, 0.50f, 1.2f),
        ParticleSpec(2.80f, 0.92f, 1.6f, 0.30f, 0.9f),
        ParticleSpec(3.60f, 0.76f, 2.0f, 0.45f, 1.1f),
        ParticleSpec(4.45f, 0.84f, 2.4f, 0.38f, 0.85f),
        ParticleSpec(5.30f, 0.68f, 1.9f, 0.42f, 1.15f),
        ParticleSpec(5.95f, 0.90f, 2.1f, 0.36f, 0.95f)
    )

    fun drawAmbientBloom(
        scope: DrawScope,
        center: Offset,
        baseRadius: Float,
        scale: Float,
        primary: Color,
        secondary: Color
    ) = with(scope) {
        val outerRadius = baseRadius * scale * 1.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = 0.28f * scale),
                    secondary.copy(alpha = 0.12f * scale),
                    Color.Transparent
                ),
                center = center,
                radius = outerRadius
            ),
            radius = outerRadius,
            center = center
        )
    }

    fun drawFloatingParticles(
        scope: DrawScope,
        center: Offset,
        maxRadius: Float,
        rotationRad: Float,
        scale: Float,
        primary: Color
    ) = with(scope) {
        for (p in defaultParticles) {
            val angle = p.baseAngle + (rotationRad * p.speedMultiplier)
            val dist = maxRadius * p.distanceFraction * (0.85f + 0.25f * scale)
            val px = center.x + dist * cos(angle.toDouble()).toFloat()
            val py = center.y + dist * sin(angle.toDouble()).toFloat()
            val particleAlpha = (p.alpha * (0.4f + 0.6f * scale)).coerceIn(0f, 1f)
            drawCircle(
                color = primary.copy(alpha = particleAlpha),
                radius = p.radiusDp.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }

    fun drawLotusPetals(
        scope: DrawScope,
        center: Offset,
        petalRadius: Float,
        petalSpread: Float,
        rotationDeg: Float,
        primary: Color,
        secondary: Color,
        petalCount: Int = 6
    ) = with(scope) {
        val angleStep = (2.0 * PI / petalCount).toFloat()
        val baseRotationRad = Math.toRadians(rotationDeg.toDouble()).toFloat()

        for (i in 0 until petalCount) {
            val petalAngle = baseRotationRad + i * angleStep
            val petalCenter = Offset(
                x = center.x + petalSpread * cos(petalAngle.toDouble()).toFloat(),
                y = center.y + petalSpread * sin(petalAngle.toDouble()).toFloat()
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.42f),
                        secondary.copy(alpha = 0.24f),
                        Color.Transparent
                    ),
                    center = petalCenter,
                    radius = petalRadius
                ),
                radius = petalRadius,
                center = petalCenter
            )
        }
    }

    fun drawCoreAuraAndOrb(
        scope: DrawScope,
        center: Offset,
        coreRadius: Float,
        primary: Color,
        secondary: Color
    ) = with(scope) {
        // Core glowing body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    primary.copy(alpha = 0.85f),
                    secondary.copy(alpha = 0.65f),
                    primary.copy(alpha = 0.35f)
                ),
                center = center,
                radius = coreRadius
            ),
            radius = coreRadius,
            center = center
        )

        // Specular 3D highlight
        val highlightCenter = Offset(center.x, center.y - coreRadius * 0.32f)
        val highlightRadius = coreRadius * 0.65f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.38f),
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = highlightCenter,
                radius = highlightRadius
            ),
            radius = highlightRadius,
            center = highlightCenter
        )
    }

    fun drawProgressHalo(
        scope: DrawScope,
        center: Offset,
        ringRadius: Float,
        sweepAngle: Float,
        primary: Color
    ) = with(scope) {
        // Delicate background track
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = ringRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Smooth active progress arc
        if (sweepAngle > 0.5f) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.4f),
                        primary,
                        Color.White
                    ),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Luminous head bead
            val headRad = Math.toRadians((-90.0 + sweepAngle))
            val hx = center.x + ringRadius * cos(headRad).toFloat()
            val hy = center.y + ringRadius * sin(headRad).toFloat()

            drawCircle(
                color = primary.copy(alpha = 0.5f),
                radius = 6.dp.toPx(),
                center = Offset(hx, hy)
            )
            drawCircle(
                color = Color.White,
                radius = 2.8.dp.toPx(),
                center = Offset(hx, hy)
            )
        }
    }
}
