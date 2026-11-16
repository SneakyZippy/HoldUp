package com.holdup.app.data.model

import com.holdup.app.data.preferences.PetPreferencesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PetSystemTest {

    private fun samplePet(
        xp: Int = 0,
        isSad: Boolean = false
    ) = PetInstance(
        id = "pet_1",
        species = PetSpecies.MOSS_TURTLE,
        customName = "Kame",
        xp = xp,
        adoptedAtMillis = 1000L,
        isSadFromOverBudget = isSad
    )

    @Test
    fun `stage thresholds map accurately across all 4 stages`() {
        assertEquals(PetStage.EGG, PetStage.fromXp(0))
        assertEquals(PetStage.EGG, PetStage.fromXp(299))
        assertEquals(PetStage.BABY, PetStage.fromXp(300))
        assertEquals(PetStage.BABY, PetStage.fromXp(899))
        assertEquals(PetStage.TEEN, PetStage.fromXp(900))
        assertEquals(PetStage.TEEN, PetStage.fromXp(2099))
        assertEquals(PetStage.ADULT, PetStage.fromXp(2100))
        assertEquals(PetStage.ADULT, PetStage.fromXp(2600))
    }

    @Test
    fun `stage progress and graduation flags compute cleanly`() {
        val eggMid = samplePet(xp = 150)
        assertEquals(0.5f, eggMid.stageProgressFraction, 0.001f)
        assertFalse(eggMid.isGraduated)

        val babyMid = samplePet(xp = 600)
        assertEquals(300, babyMid.xpIntoCurrentStage)
        assertEquals(600, babyMid.xpRequiredForNextStage)
        assertEquals(0.5f, babyMid.stageProgressFraction, 0.001f)

        val adult = samplePet(xp = 2100)
        assertTrue(adult.isGraduated)
        assertEquals(1f, adult.stageProgressFraction, 0.001f)
        assertNull(adult.xpRequiredForNextStage)
    }

    @Test
    fun `mood resolves from daily grace and over-budget state`() {
        val today = "2026-11-15"
        val pet = samplePet()

        assertEquals(
            PetMood.RADIANT,
            pet.resolveMood(DailyStats(dateString = today, overBudgetOpensToday = 0, graceMinutesUsedToday = 0), today)
        )
        assertEquals(
            PetMood.CONTENT,
            pet.resolveMood(DailyStats(dateString = today, overBudgetOpensToday = 0, graceMinutesUsedToday = 5), today)
        )
        assertEquals(
            PetMood.SAD,
            pet.resolveMood(DailyStats(dateString = today, overBudgetOpensToday = 1), today)
        )
        assertEquals(
            PetMood.SAD,
            samplePet(isSad = true).resolveMood(DailyStats(dateString = today), today)
        )
    }

    @Test
    fun `feeding radiant pet applies 1_25x bonus and graduates at 2100 xp`() {
        val today = "2026-11-15"
        val (fedBerry, berryXp) = samplePet().applyFeeding(PetTreat.DEWDROP_BERRY, PetMood.RADIANT, today, 5000L)
        assertEquals(31, berryXp)
        assertEquals(31, fedBerry.xp)

        val (_, lotusXp) = samplePet().applyFeeding(PetTreat.SUNLIT_LOTUS, PetMood.RADIANT, today, 5000L)
        assertEquals(81, lotusXp)

        val (graduated, nectarXp) = samplePet(xp = 2000).applyFeeding(PetTreat.CELESTIAL_NECTAR, PetMood.CONTENT, today, 9999L)
        assertEquals(175, nectarXp)
        assertTrue(graduated.isGraduated)
        assertNotNull(graduated.graduatedAtMillis)
        assertEquals(PetMood.RADIANT, graduated.resolveMood(DailyStats(graceMinutesUsedToday = 10), today))
    }

    @Test
    fun `feeding sad pet cheers up first and resets cleanly on new over-budget open`() {
        val today = "2026-11-15"
        val overBudgetStats = DailyStats(dateString = today, overBudgetOpensToday = 1)
        val sadPet = samplePet(xp = 400, isSad = true)

        val (cheered, berryXp) = sadPet.applyFeeding(PetTreat.DEWDROP_BERRY, PetMood.SAD, today, 5000L)
        assertEquals(0, berryXp)
        assertEquals(400, cheered.xp)
        assertFalse(cheered.isSadFromOverBudget)
        assertEquals(PetMood.CONTENT, cheered.resolveMood(overBudgetStats, today))

        val relapsed = cheered.markSadOnOverBudget()
        assertEquals(PetMood.SAD, relapsed.resolveMood(overBudgetStats, today))

        val (nectarCheered, nectarXp) = relapsed.applyFeeding(PetTreat.CELESTIAL_NECTAR, PetMood.SAD, today, 6000L)
        assertEquals(175, nectarXp)
        assertEquals(PetMood.RADIANT, nectarCheered.resolveMood(overBudgetStats, today))
    }

    @Test
    fun `zoo state adopts mystery eggs from unowned pool and enforces egg pricing`() {
        assertEquals(100, PetPreferencesRepository.MYSTERY_EGG_COST)
        assertEquals(400, PetPreferencesRepository.SPECIFIC_EGG_COST)

        val growingPet = samplePet(xp = 500)
        val growingZoo = ZooState(activePetId = growingPet.id, ownedPets = listOf(growingPet), hasClaimedStarterEgg = true)
        assertFalse(growingZoo.canAdoptNewEgg)

        val adultPet = samplePet(xp = 2100, isSad = false).copy(graduatedAtMillis = 8000L)
        val readyZoo = growingZoo.updatePet(adultPet)
        assertTrue(readyZoo.canAdoptNewEgg)

        val picked = readyZoo.pickMysterySpecies { 0 }
        assertEquals(PetSpecies.EMBER_FOX, picked)
    }
}
