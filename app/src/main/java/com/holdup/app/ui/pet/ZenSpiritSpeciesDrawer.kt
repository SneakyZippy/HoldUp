package com.holdup.app.ui.pet

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
import com.holdup.app.data.model.PetSpecies
import com.holdup.app.data.model.PetStage

internal fun DrawScope.drawZenSpirit(
    species: PetSpecies,
    stage: PetStage,
    mood: PetMood,
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    eggProgress: Float,
    wobbleDeg: Float,
    blinkOpen: Boolean
) {
    if (stage == PetStage.EGG) {
        drawEggStage(center, radius, primary, secondary, eggProgress, wobbleDeg, mood)
        return
    }

    drawSpiritAuraHalo(center, radius, primary, secondary, stage)
    when (species) {
        PetSpecies.MOSS_TURTLE -> drawMossTurtle(center, radius, stage, mood, primary, secondary, blinkOpen)
        PetSpecies.EMBER_FOX -> drawEmberFox(center, radius, stage, mood, primary, secondary, blinkOpen)
        PetSpecies.CLOUD_OWL -> drawCloudOwl(center, radius, stage, mood, primary, secondary, blinkOpen)
        PetSpecies.LOTUS_AXOLOTL -> drawLotusAxolotl(center, radius, stage, mood, primary, secondary, blinkOpen)
        PetSpecies.BAMBOO_PANDA -> drawBambooPanda(center, radius, stage, mood, primary, secondary, blinkOpen)
        PetSpecies.COSMIC_KOI -> drawCosmicKoi(center, radius, stage, mood, primary, secondary, blinkOpen)
    }
}

private fun DrawScope.drawEggStage(
    center: Offset,
    r: Float,
    primary: Color,
    secondary: Color,
    eggProgress: Float,
    wobbleDeg: Float,
    mood: PetMood
) {
    val eggW = r * 1.12f
    val eggH = r * 1.42f
    rotate(degrees = if (mood == PetMood.SAD) 0f else wobbleDeg, pivot = center) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.45f), primary, secondary),
                center = Offset(center.x - eggW * 0.18f, center.y - eggH * 0.2f),
                radius = eggH * 0.9f
            ),
            topLeft = Offset(center.x - eggW / 2f, center.y - eggH / 2f),
            size = Size(eggW, eggH)
        )
        // Mystic band pattern
        drawArc(
            color = Color.White.copy(alpha = 0.38f),
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(center.x - eggW * 0.42f, center.y - eggH * 0.12f),
            size = Size(eggW * 0.84f, eggH * 0.28f),
            style = Stroke(width = r * 0.08f, cap = StrokeCap.Round)
        )
        // Glowing hatch fissures when close to hatching
        if (eggProgress >= 0.7f) {
            val crack = Path().apply {
                moveTo(center.x - eggW * 0.12f, center.y - eggH * 0.18f)
                lineTo(center.x + eggW * 0.04f, center.y - eggH * 0.04f)
                lineTo(center.x - eggW * 0.08f, center.y + eggH * 0.08f)
            }
            drawPath(crack, Color.White.copy(alpha = 0.85f), style = Stroke(width = r * 0.05f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawMossTurtle(
    center: Offset,
    r: Float,
    stage: PetStage,
    mood: PetMood,
    primary: Color,
    secondary: Color,
    blinkOpen: Boolean
) {
    val s = stageScale(stage)
    val bodyR = r * 0.62f * s
    // Flippers
    listOf(-1f, 1f).forEach { dir ->
        drawCircle(secondary, bodyR * 0.24f, Offset(center.x + dir * bodyR * 0.76f, center.y + bodyR * 0.52f))
    }
    // Domed moss shell
    drawArc(
        brush = Brush.verticalGradient(listOf(primary, secondary)),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - bodyR, center.y - bodyR * 0.95f),
        size = Size(bodyR * 2f, bodyR * 1.9f)
    )
    // Sprout / bonsai crest
    drawLine(Color(0xFF8D6E63), Offset(center.x, center.y - bodyR * 0.9f), Offset(center.x, center.y - bodyR * (if (stage == PetStage.BABY) 1.2f else 1.4f)), strokeWidth = bodyR * 0.1f, cap = StrokeCap.Round)
    drawCircle(primary, bodyR * (if (stage == PetStage.ADULT) 0.36f else 0.22f), Offset(center.x, center.y - bodyR * (if (stage == PetStage.BABY) 1.25f else 1.48f)))
    if (stage == PetStage.ADULT) {
        drawCircle(Color(0xFFA7F3D0), bodyR * 0.24f, Offset(center.x - bodyR * 0.28f, center.y - bodyR * 1.32f))
        drawCircle(Color(0xFFA7F3D0), bodyR * 0.24f, Offset(center.x + bodyR * 0.28f, center.y - bodyR * 1.32f))
    }
    drawGlowingBody(Offset(center.x, center.y + bodyR * 0.24f), bodyR * 0.88f, bodyR * 0.56f, Color(0xFFA7F3D0), primary)
    drawSpiritFace(Offset(center.x, center.y + bodyR * 0.24f), bodyR * 0.34f, bodyR * 0.1f, mood, blinkOpen)
}

private fun DrawScope.drawEmberFox(
    center: Offset,
    r: Float,
    stage: PetStage,
    mood: PetMood,
    primary: Color,
    secondary: Color,
    blinkOpen: Boolean
) {
    val s = stageScale(stage)
    val headR = r * 0.58f * s
    // Spirit tails
    val tailAngles = when (stage) {
        PetStage.BABY -> listOf(35f)
        PetStage.TEEN -> listOf(22f, 50f)
        else -> listOf(15f, 40f, 65f)
    }
    tailAngles.forEach { angle ->
        rotate(angle, pivot = Offset(center.x + headR * 0.45f, center.y + headR * 0.45f)) {
            drawOval(
                brush = Brush.verticalGradient(listOf(secondary, primary)),
                topLeft = Offset(center.x + headR * 0.25f, center.y - headR * 0.45f),
                size = Size(headR * 0.5f, headR * 1.15f)
            )
        }
    }
    // Fox ears
    listOf(-1f, 1f).forEach { dir ->
        val ear = Path().apply {
            moveTo(center.x + dir * headR * 0.2f, center.y - headR * 0.55f)
            lineTo(center.x + dir * headR * 0.78f, center.y - headR * 1.22f)
            lineTo(center.x + dir * headR * 0.88f, center.y - headR * 0.25f)
            close()
        }
        drawPath(ear, primary)
    }
    drawGlowingBody(center, headR, headR * 0.88f, primary, secondary)
    drawSpiritFace(Offset(center.x, center.y + headR * 0.08f), headR * 0.38f, headR * 0.11f, mood, blinkOpen)
}

private fun DrawScope.drawCloudOwl(
    center: Offset,
    r: Float,
    stage: PetStage,
    mood: PetMood,
    primary: Color,
    secondary: Color,
    blinkOpen: Boolean
) {
    val s = stageScale(stage)
    val bodyR = r * 0.62f * s
    // Wings
    val wingSpread = if (stage == PetStage.ADULT) 1.2f else if (stage == PetStage.TEEN) 1.0f else 0.85f
    listOf(-1f, 1f).forEach { dir ->
        rotate(dir * (if (stage == PetStage.ADULT) 28f else 12f), Offset(center.x + dir * bodyR * 0.75f, center.y + bodyR * 0.1f)) {
            drawOval(
                color = secondary.copy(alpha = 0.88f),
                topLeft = Offset(center.x + dir * bodyR * 0.55f - bodyR * 0.28f, center.y - bodyR * 0.3f),
                size = Size(bodyR * 0.56f * wingSpread, bodyR * 0.88f)
            )
        }
    }
    // Ear tufts
    listOf(-1f, 1f).forEach { dir ->
        val tuft = Path().apply {
            moveTo(center.x + dir * bodyR * 0.32f, center.y - bodyR * 0.68f)
            lineTo(center.x + dir * bodyR * 0.72f, center.y - bodyR * 1.15f)
            lineTo(center.x + dir * bodyR * 0.75f, center.y - bodyR * 0.45f)
            close()
        }
        drawPath(tuft, secondary)
    }
    drawGlowingBody(center, bodyR * 0.94f, bodyR * 0.96f, primary, secondary)
    // Cloud belly
    drawOval(
        color = Color.White.copy(alpha = 0.26f),
        topLeft = Offset(center.x - bodyR * 0.52f, center.y + bodyR * 0.14f),
        size = Size(bodyR * 1.04f, bodyR * 0.64f)
    )
    drawSpiritFace(Offset(center.x, center.y - bodyR * 0.08f), bodyR * 0.36f, bodyR * 0.12f, mood, blinkOpen)
}
