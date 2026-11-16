package com.holdup.app.ui.pet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdup.app.R
import com.holdup.app.data.model.DailyStats
import com.holdup.app.data.model.PetInstance
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.model.PetSpecies
import com.holdup.app.data.model.ZooState
import com.holdup.app.ui.components.zenGlassBorder
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class RoamingPetState(
    var x: Float,
    var y: Float,
    var targetX: Float,
    var targetY: Float,
    var pauseSec: Float,
    var phaseOffset: Float,
    var facingLeft: Boolean = false
)

private data class BiomeBounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

@Composable
fun ZooHabitatView(
    zooState: ZooState,
    todayStats: DailyStats,
    onPetClick: (PetInstance) -> Unit,
    modifier: Modifier = Modifier
) {
    val pets = zooState.ownedPets
    val states = remember { mutableStateMapOf<String, RoamingPetState>() }
    var elapsedSec by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pets.map { it.id }) {
        pets.forEach { pet ->
            if (!states.containsKey(pet.id)) {
                val bounds = biomeBoundsFor(pet.species)
                val sx = lerp(bounds.minX, bounds.maxX, Random.nextFloat())
                val sy = lerp(bounds.minY, bounds.maxY, Random.nextFloat())
                states[pet.id] = RoamingPetState(
                    x = sx,
                    y = sy,
                    targetX = lerp(bounds.minX, bounds.maxX, Random.nextFloat()),
                    targetY = lerp(bounds.minY, bounds.maxY, Random.nextFloat()),
                    pauseSec = Random.nextFloat() * 1.5f,
                    phaseOffset = Random.nextFloat() * 6.28f
                )
            }
        }
    }

    LaunchedEffect(pets.size) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (lastNanos != 0L) {
                    val dt = ((nanos - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                    elapsedSec += dt
                    stepRoamingPets(pets, states, dt)
                }
                lastNanos = nanos
            }
        }
    }

    val activePet = zooState.activePet
    val primary = activePet?.let { Color(it.species.primaryColorHex) } ?: MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0B0E17),
        border = zenGlassBorder(isHighlighted = true, accentColor = primary),
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            Image(
                painter = painterResource(R.drawable.bg_zen_sanctuary),
                contentDescription = "Zen Sanctuary",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawAmbientFireflies(elapsedSec, primary)
            }

            val sortedPets = pets.sortedBy { states[it.id]?.y ?: 0.5f }
            sortedPets.forEach { pet ->
                val st = states[pet.id] ?: return@forEach
                val mood = pet.resolveMood(todayStats, todayStats.dateString)
                val isActive = pet.id == activePet?.id
                HabitatPetNode(
                    pet = pet,
                    state = st,
                    mood = mood,
                    isActive = isActive,
                    timeSec = elapsedSec,
                    containerWidthPx = widthPx,
                    containerHeightPx = heightPx,
                    onClick = { onPetClick(pet) }
                )
            }
        }
    }
}

@Composable
private fun HabitatPetNode(
    pet: PetInstance,
    state: RoamingPetState,
    mood: PetMood,
    isActive: Boolean,
    timeSec: Float,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onClick: () -> Unit
) {
    val spriteSizeDp = if (isActive) 74.dp else 62.dp
    val density = LocalDensity.current
    val spritePx = with(density) { spriteSizeDp.toPx() }
    val bobPx = sin(timeSec * 2.6f + state.phaseOffset) * 3.5f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (state.x * containerWidthPx - spritePx / 2f).roundToInt(),
                    y = (state.y * containerHeightPx - spritePx * 0.72f + bobPx).roundToInt()
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = if (isActive) "★ ${pet.customName}" else pet.customName,
            color = if (isActive) Color(0xFFFFF3C4) else Color.White.copy(alpha = 0.92f),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier
                .background(
                    color = Color(0xAA0B0E17),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 7.dp, vertical = 2.dp)
        )

        ZenSpiritCanvas(
            pet = pet,
            mood = mood,
            isCompact = true,
            modifier = Modifier
                .size(spriteSizeDp)
                .graphicsLayer {
                    scaleX = if (state.facingLeft) -1f else 1f
                }
        )

        Box(
            modifier = Modifier
                .offset(y = (-6).dp)
                .size(width = spriteSizeDp * 0.56f, height = 8.dp)
                .background(Color.Black.copy(alpha = 0.35f), shape = CircleShape)
        )
    }
}

private fun stepRoamingPets(
    pets: List<PetInstance>,
    states: MutableMap<String, RoamingPetState>,
    dt: Float
) {
    pets.forEach { pet ->
        val st = states[pet.id] ?: return@forEach
        if (st.pauseSec > 0f) {
            st.pauseSec -= dt
            return@forEach
        }
        val dx = st.targetX - st.x
        val dy = st.targetY - st.y
        val dist = hypot(dx, dy)
        val speed = if (pet.species == PetSpecies.CLOUD_OWL) 0.085f else 0.055f
        if (dist < 0.015f) {
            val b = biomeBoundsFor(pet.species)
            st.targetX = lerp(b.minX, b.maxX, Random.nextFloat())
            st.targetY = lerp(b.minY, b.maxY, Random.nextFloat())
            st.pauseSec = 1.4f + Random.nextFloat() * 2.4f
        } else {
            if (dx < -0.001f) st.facingLeft = true
            else if (dx > 0.001f) st.facingLeft = false
            st.x += (dx / dist) * speed * dt
            st.y += (dy / dist) * speed * dt
        }
    }
}

private fun DrawScope.drawAmbientFireflies(timeSec: Float, accent: Color) {
    for (i in 0 until 7) {
        val fx = ((0.14f + i * 0.13f + sin(timeSec * 0.35f + i) * 0.05f) % 1f) * size.width
        val fy = (0.35f + (i % 3) * 0.18f + sin(timeSec * 0.6f + i * 2f) * 0.04f) * size.height
        val alpha = 0.25f + 0.35f * (0.5f + 0.5f * sin(timeSec * 2.1f + i))
        drawCircle(accent.copy(alpha = alpha), radius = 2.4f, center = Offset(fx, fy))
    }
}

private fun biomeBoundsFor(species: PetSpecies): BiomeBounds = when (species) {
    PetSpecies.CLOUD_OWL -> BiomeBounds(0.16f, 0.84f, 0.22f, 0.44f)
    PetSpecies.LOTUS_AXOLOTL, PetSpecies.COSMIC_KOI -> BiomeBounds(0.45f, 0.86f, 0.70f, 0.86f)
    PetSpecies.MOSS_TURTLE, PetSpecies.EMBER_FOX, PetSpecies.BAMBOO_PANDA -> BiomeBounds(0.14f, 0.54f, 0.60f, 0.84f)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
