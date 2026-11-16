package com.holdup.app.ui.pet

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.model.PetStage

internal fun DrawScope.drawSpiritAuraHalo(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    stage: PetStage
) {
    if (stage != PetStage.ADULT) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
            center = center,
            radius = radius * 1.38f
        ),
        radius = radius * 1.38f,
        center = center
    )
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(primary.copy(alpha = 0.38f), secondary.copy(alpha = 0.55f), primary.copy(alpha = 0.38f)),
            center = center
        ),
        radius = radius * 1.18f,
        center = center,
        style = Stroke(width = radius * 0.045f)
    )
}

internal fun DrawScope.drawGlowingBody(
    center: Offset,
    rx: Float,
    ry: Float,
    primary: Color,
    secondary: Color
) {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), primary, secondary.copy(alpha = 0.92f)),
            center = Offset(center.x - rx * 0.25f, center.y - ry * 0.3f),
            radius = maxOf(rx, ry) * 1.45f
        ),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f)
    )
    drawOval(
        color = Color.White.copy(alpha = 0.14f),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = minOf(rx, ry) * 0.06f)
    )
}

internal fun DrawScope.drawSpiritFace(
    faceCenter: Offset,
    eyeSpread: Float,
    eyeRadius: Float,
    mood: PetMood,
    blinkOpen: Boolean
) {
    val leftEye = Offset(faceCenter.x - eyeSpread, faceCenter.y)
    val rightEye = Offset(faceCenter.x + eyeSpread, faceCenter.y)
    val eyeInk = Color(0xFF10131A)

    when {
        mood == PetMood.RADIANT -> {
            drawHappyEyeArc(leftEye, eyeRadius, eyeInk)
            drawHappyEyeArc(rightEye, eyeRadius, eyeInk)
            drawCircle(Color(0xFFFF7A90).copy(alpha = 0.55f), eyeRadius * 0.85f, Offset(leftEye.x - eyeRadius * 0.8f, faceCenter.y + eyeRadius * 1.1f))
            drawCircle(Color(0xFFFF7A90).copy(alpha = 0.55f), eyeRadius * 0.85f, Offset(rightEye.x + eyeRadius * 0.8f, faceCenter.y + eyeRadius * 1.1f))
        }
        mood == PetMood.SAD -> {
            drawSadEye(leftEye, eyeRadius, eyeInk)
            drawSadEye(rightEye, eyeRadius, eyeInk)
            drawCircle(Color(0xFF7DD3FC).copy(alpha = 0.8f), eyeRadius * 0.42f, Offset(rightEye.x + eyeRadius * 1.1f, faceCenter.y + eyeRadius * 0.9f))
        }
        !blinkOpen -> {
            drawLine(eyeInk, Offset(leftEye.x - eyeRadius, leftEye.y), Offset(leftEye.x + eyeRadius, leftEye.y), strokeWidth = eyeRadius * 0.45f, cap = StrokeCap.Round)
            drawLine(eyeInk, Offset(rightEye.x - eyeRadius, rightEye.y), Offset(rightEye.x + eyeRadius, rightEye.y), strokeWidth = eyeRadius * 0.45f, cap = StrokeCap.Round)
        }
        else -> {
            listOf(leftEye, rightEye).forEach { eye ->
                drawCircle(eyeInk, eyeRadius, eye)
                drawCircle(Color.White, eyeRadius * 0.36f, Offset(eye.x - eyeRadius * 0.28f, eye.y - eyeRadius * 0.28f))
            }
        }
    }
    drawMouthArc(faceCenter, eyeRadius, mood, eyeInk)
}

private fun DrawScope.drawHappyEyeArc(center: Offset, radius: Float, ink: Color) {
    drawArc(
        color = ink,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius * 0.5f),
        size = Size(radius * 2f, radius * 1.6f),
        style = Stroke(width = radius * 0.48f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawSadEye(center: Offset, radius: Float, ink: Color) {
    drawCircle(ink, radius * 0.85f, center)
    drawLine(
        color = ink,
        start = Offset(center.x - radius * 1.1f, center.y - radius * 0.4f),
        end = Offset(center.x + radius * 1.1f, center.y - radius * 0.7f),
        strokeWidth = radius * 0.42f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawMouthArc(faceCenter: Offset, eyeRadius: Float, mood: PetMood, ink: Color) {
    val mouthY = faceCenter.y + eyeRadius * 1.15f
    val mouthW = eyeRadius * 1.6f
    val mouthH = eyeRadius * 1.0f
    drawArc(
        color = ink,
        startAngle = if (mood == PetMood.SAD) 200f else 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(faceCenter.x - mouthW / 2f, mouthY - if (mood == PetMood.SAD) 0f else mouthH * 0.4f),
        size = Size(mouthW, mouthH),
        style = Stroke(width = eyeRadius * 0.35f, cap = StrokeCap.Round)
    )
}

internal fun DrawScope.drawLotusAxolotl(
    center: Offset,
    r: Float,
    stage: PetStage,
    mood: PetMood,
    primary: Color,
    secondary: Color,
    blinkOpen: Boolean
) {
    val scale = stageScale(stage)
    val bodyR = r * 0.62f * scale
    // External lotus gill frills
    listOf(-1f, 1f).forEach { dir ->
        listOf(-0.28f, 0f, 0.28f).forEachIndexed { idx, angleOffset ->
            val gillAngle = dir * (28f + idx * 18f)
            rotate(gillAngle, pivot = Offset(center.x + dir * bodyR * 0.75f, center.y - bodyR * 0.1f)) {
                drawRoundRect(
                    color = secondary.copy(alpha = 0.9f),
                    topLeft = Offset(center.x + dir * bodyR * 0.75f - bodyR * 0.12f, center.y - bodyR * (0.48f - idx * 0.06f)),
                    size = Size(bodyR * 0.44f, bodyR * 0.2f),
                    cornerRadius = CornerRadius(bodyR * 0.12f)
                )
            }
        }
    }
    if (stage >= PetStage.TEEN) {
        drawOval(
            color = primary.copy(alpha = 0.75f),
            topLeft = Offset(center.x - bodyR * 0.45f, center.y + bodyR * 0.48f),
            size = Size(bodyR * 0.9f, bodyR * 0.55f)
        )
    }
    drawGlowingBody(center, bodyR * 1.08f, bodyR * 0.88f, primary, primary.copy(alpha = 0.75f))
    if (stage == PetStage.ADULT) {
        drawLotusCrown(Offset(center.x, center.y - bodyR * 0.9f), bodyR * 0.38f, secondary)
    }
    drawSpiritFace(Offset(center.x, center.y + bodyR * 0.05f), bodyR * 0.42f, bodyR * 0.12f, mood, blinkOpen)
}

internal fun DrawScope.drawBambooPanda(
    center: Offset,
    r: Float,
    stage: PetStage,
    mood: PetMood,
    primary: Color,
    secondary: Color,
    blinkOpen: Boolean
) {
    val scale = stageScale(stage)
    val headR = r * 0.64f * scale
    val ink = Color(0xFF1A202C)

    // Ears
    listOf(-1f, 1f).forEach { dir ->
        drawCircle(ink, headR * 0.3f, Offset(center.x + dir * headR * 0.68f, center.y - headR * 0.65f))
    }
    if (stage >= PetStage.TEEN) {
        drawRoundRect(
            color = primary.copy(alpha = 0.85f),
            topLeft = Offset(center.x - headR * 0.75f, center.y + headR * 0.55f),
            size = Size(headR * 1.5f, headR * 0.45f),
            cornerRadius = CornerRadius(headR * 0.22f)
        )
    }
    drawGlowingBody(center, headR, headR * 0.92f, secondary, Color(0xFFCBD5E1))
    // Eye patches
    listOf(-1f, 1f).forEach { dir ->
        rotate(dir * 16f, Offset(center.x + dir * headR * 0.36f, center.y)) {
            drawOval(
                color = ink.copy(alpha = 0.88f),
                topLeft = Offset(center.x + dir * headR * 0.36f - headR * 0.22f, center.y - headR * 0.16f),
                size = Size(headR * 0.44f, headR * 0.34f)
            )
        }
    }
    if (stage == PetStage.ADULT) {
        drawLotusCrown(Offset(center.x, center.y - headR * 0.98f), headR * 0.34f, primary)
    }
    // White inner eye dots over dark panda patches
    val faceCenter = Offset(center.x, center.y + headR * 0.02f)
    listOf(-1f, 1f).forEach { dir ->
        val ex = faceCenter.x + dir * headR * 0.36f
        val ey = faceCenter.y
        if (mood == PetMood.RADIANT) {
            drawHappyEyeArc(Offset(ex, ey), headR * 0.11f, primary)
        } else if (!blinkOpen && mood == PetMood.CONTENT) {
            drawLine(Color.White, Offset(ex - headR * 0.1f, ey), Offset(ex + headR * 0.1f, ey), strokeWidth = headR * 0.06f, cap = StrokeCap.Round)
        } else {
            drawCircle(Color.White, headR * 0.09f, Offset(ex, ey))
        }
    }
    drawMouthArc(faceCenter, headR * 0.12f, mood, ink)
}

internal fun DrawScope.drawCosmicKoi(
    center: Offset,
    r: Float,
    stage: PetStage,
    mood: PetMood,
    primary: Color,
    secondary: Color,
    blinkOpen: Boolean
) {
    val scale = stageScale(stage)
    val bodyR = r * 0.62f * scale
    // Flowing tail fin
    val tailPath = Path().apply {
        moveTo(center.x - bodyR * 0.4f, center.y + bodyR * 0.55f)
        quadraticTo(center.x - bodyR * 1.15f, center.y + bodyR * 0.95f, center.x - bodyR * 0.85f, center.y + bodyR * 0.25f)
        quadraticTo(center.x - bodyR * 0.3f, center.y + bodyR * 0.35f, center.x - bodyR * 0.4f, center.y + bodyR * 0.55f)
        close()
    }
    drawPath(tailPath, secondary.copy(alpha = 0.85f))

    // Side pectoral fins
    listOf(-1f, 1f).forEach { dir ->
        rotate(dir * 32f, Offset(center.x + dir * bodyR * 0.85f, center.y + bodyR * 0.1f)) {
            drawOval(
                color = secondary.copy(alpha = 0.78f),
                topLeft = Offset(center.x + dir * bodyR * 0.85f - bodyR * 0.25f, center.y),
                size = Size(bodyR * 0.5f, bodyR * 0.26f)
            )
        }
    }
    drawGlowingBody(center, bodyR * 0.94f, bodyR * 0.86f, primary, secondary)
    if (stage >= PetStage.TEEN) {
        drawCircle(secondary.copy(alpha = 0.75f), bodyR * 0.2f, Offset(center.x - bodyR * 0.25f, center.y - bodyR * 0.45f))
    }
    if (stage == PetStage.ADULT) {
        listOf(-1f, 1f).forEach { dir ->
            drawLine(
                color = Color(0xFFFFE082),
                start = Offset(center.x + dir * bodyR * 0.3f, center.y - bodyR * 0.75f),
                end = Offset(center.x + dir * bodyR * 0.52f, center.y - bodyR * 1.18f),
                strokeWidth = bodyR * 0.1f,
                cap = StrokeCap.Round
            )
        }
    }
    drawSpiritFace(Offset(center.x, center.y + bodyR * 0.08f), bodyR * 0.38f, bodyR * 0.11f, mood, blinkOpen)
}

private fun DrawScope.drawLotusCrown(topCenter: Offset, size: Float, color: Color) {
    listOf(-28f, 0f, 28f).forEach { angle ->
        rotate(angle, pivot = topCenter) {
            drawOval(
                color = color.copy(alpha = 0.92f),
                topLeft = Offset(topCenter.x - size * 0.35f, topCenter.y - size),
                size = Size(size * 0.7f, size * 1.1f)
            )
        }
    }
}

internal fun stageScale(stage: PetStage): Float = when (stage) {
    PetStage.EGG -> 0.82f
    PetStage.BABY -> 0.84f
    PetStage.TEEN -> 0.96f
    PetStage.ADULT -> 1.06f
}
