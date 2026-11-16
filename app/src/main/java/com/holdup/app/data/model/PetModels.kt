package com.holdup.app.data.model

import kotlinx.serialization.Serializable

/**
 * Six Zen Spirit companion species rendered procedurally on Compose Canvas.
 */
@Serializable
enum class PetSpecies(
    val displayName: String,
    val defaultPetName: String,
    val spiritTitle: String,
    val biome: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long
) {
    MOSS_TURTLE("Moss Turtle", "Kame", "Ancient Island Guardian", "Emerald Grove", 0xFF4EE1A0, 0xFF36B37E),
    EMBER_FOX("Ember Fox", "Kitsune", "Nine-Tail Hearth Spirit", "Amber Shrine", 0xFFFF7A66, 0xFFFFB26B),
    CLOUD_OWL("Cloud Owl", "Fukuro", "Astral Night Watcher", "Starlit Canopy", 0xFFA594F9, 0xFF7C69EF),
    LOTUS_AXOLOTL("Lotus Axolotl", "Mizuki", "Sacred Pond Mystic", "Lotus Lagoon", 0xFF38D39F, 0xFFFF8DA1),
    BAMBOO_PANDA("Bamboo Panda", "Takeshi", "Serene Zen Monk", "Jade Bamboo", 0xFF6EE7B7, 0xFFE2E8F0),
    COSMIC_KOI("Cosmic Koi", "Ryujin", "Celestial River Dragon", "Nebula Stream", 0xFFFFB26B, 0xFF38BDF8)
}

/**
 * Evolution stages spanning roughly two weeks of mindful usage (~2,100 XP total).
 */
@Serializable
enum class PetStage(
    val level: Int,
    val label: String,
    val minXp: Int,
    val nextStageXp: Int?
) {
    EGG(1, "Spirit Egg", 0, 300),
    BABY(2, "Hatchling", 300, 900),
    TEEN(3, "Juvenile", 900, 2100),
    ADULT(4, "Awakened", 2100, null);

    companion object {
        fun fromXp(xp: Int): PetStage = when {
            xp >= ADULT.minXp -> ADULT
            xp >= TEEN.minXp -> TEEN
            xp >= BABY.minXp -> BABY
            else -> EGG
        }
    }
}

@Serializable
enum class PetMood(
    val label: String,
    val xpMultiplier: Float,
    val subtitle: String
) {
    RADIANT("Radiant", 1.25f, "1.25× Evolution XP • Pure Zen aura"),
    CONTENT("Content", 1.0f, "1.0× Evolution XP • Within Daily Grace"),
    SAD("Sluggish", 0.0f, "Evolution paused • Feed or complete 1h Clean Focus")
}

enum class PetTreat(
    val id: String,
    val title: String,
    val costPoints: Int,
    val baseXp: Int,
    val description: String
) {
    DEWDROP_BERRY("berry", "Dewdrop Berry", 25, 25, "+25 XP or cheers up a Sluggish pet"),
    SUNLIT_LOTUS("lotus", "Sunlit Lotus", 60, 65, "+65 XP & restores Content mood"),
    CELESTIAL_NECTAR("nectar", "Celestial Nectar", 120, 140, "+140 XP & boosts to Radiant mood")
}

@Serializable
data class PetInstance(
    val id: String,
    val species: PetSpecies,
    val customName: String,
    val xp: Int = 0,
    val adoptedAtMillis: Long = 0L,
    val graduatedAtMillis: Long? = null,
    val isSadFromOverBudget: Boolean = false,
    val isRadiantBoostedDate: String = "",
    val cheeredUpDate: String = ""
) {
    val stage: PetStage
        get() = PetStage.fromXp(xp)

    val isGraduated: Boolean
        get() = xp >= PetStage.ADULT.minXp

    val xpIntoCurrentStage: Int
        get() = (xp - stage.minXp).coerceAtLeast(0)

    val xpRequiredForNextStage: Int?
        get() = stage.nextStageXp?.let { it - stage.minXp }

    val stageProgressFraction: Float
        get() {
            val next = stage.nextStageXp ?: return 1f
            val span = (next - stage.minXp).coerceAtLeast(1)
            return ((xp - stage.minXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        }

    fun resolveMood(todayStats: DailyStats, todayDateString: String): PetMood {
        if (isSadFromOverBudget) return PetMood.SAD
        if (isRadiantBoostedDate == todayDateString) return PetMood.RADIANT
        val isOverBudgetWithoutRecovery = todayStats.overBudgetOpensToday > 0 && cheeredUpDate != todayDateString
        if (isOverBudgetWithoutRecovery) return PetMood.SAD
        return if (todayStats.overBudgetOpensToday == 0 && todayStats.graceMinutesUsedToday == 0) {
            PetMood.RADIANT
        } else {
            PetMood.CONTENT
        }
    }

    fun applyFeeding(
        treat: PetTreat,
        currentMood: PetMood,
        todayDateString: String,
        nowMillis: Long
    ): Pair<PetInstance, Int> {
        val xpGained = when {
            currentMood == PetMood.SAD && treat == PetTreat.DEWDROP_BERRY -> 0
            currentMood == PetMood.SAD && treat == PetTreat.SUNLIT_LOTUS -> treat.baseXp
            treat == PetTreat.CELESTIAL_NECTAR || currentMood == PetMood.RADIANT ->
                (treat.baseXp * PetMood.RADIANT.xpMultiplier).toInt()
            else -> treat.baseXp
        }

        val updatedXp = xp + xpGained
        val nextGraduatedAt = if (updatedXp >= PetStage.ADULT.minXp && graduatedAtMillis == null) {
            nowMillis
        } else {
            graduatedAtMillis
        }

        val updated = copy(
            xp = updatedXp,
            graduatedAtMillis = nextGraduatedAt,
            isSadFromOverBudget = false,
            cheeredUpDate = if (currentMood == PetMood.SAD) todayDateString else cheeredUpDate,
            isRadiantBoostedDate = if (treat == PetTreat.CELESTIAL_NECTAR) todayDateString else isRadiantBoostedDate
        )
        return updated to xpGained
    }

    fun markSadOnOverBudget(): PetInstance {
        return copy(
            isSadFromOverBudget = true,
            cheeredUpDate = "",
            isRadiantBoostedDate = ""
        )
    }

    fun clearSadOnCleanFocusHour(todayDateString: String): PetInstance {
        return copy(
            isSadFromOverBudget = false,
            cheeredUpDate = todayDateString
        )
    }
}

@Serializable
data class ZooState(
    val activePetId: String? = null,
    val ownedPets: List<PetInstance> = emptyList(),
    val hasClaimedStarterEgg: Boolean = false
) {
    val activePet: PetInstance?
        get() = ownedPets.firstOrNull { it.id == activePetId } ?: ownedPets.firstOrNull()

    val graduatedSpecies: Set<PetSpecies>
        get() = ownedPets.filter { it.isGraduated }.map { it.species }.toSet()

    val ownedSpecies: Set<PetSpecies>
        get() = ownedPets.map { it.species }.toSet()

    val canAdoptNewEgg: Boolean
        get() {
            val current = activePet
            return !hasClaimedStarterEgg || current == null || current.isGraduated
        }

    fun updatePet(updated: PetInstance): ZooState {
        return copy(ownedPets = ownedPets.map { if (it.id == updated.id) updated else it })
    }

    fun pickMysterySpecies(randomIndexSelector: (Int) -> Int = { size -> kotlin.random.Random.nextInt(size) }): PetSpecies {
        val unowned = PetSpecies.entries.filter { it !in ownedSpecies }
        val pool = unowned.ifEmpty { PetSpecies.entries }
        return pool[randomIndexSelector(pool.size)]
    }
}
