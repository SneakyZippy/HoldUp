package com.holdup.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.holdup.app.data.model.DailyStats
import com.holdup.app.data.model.PetInstance
import com.holdup.app.data.model.PetMood
import com.holdup.app.data.model.PetSpecies
import com.holdup.app.data.model.PetStage
import com.holdup.app.data.model.PetTreat
import com.holdup.app.data.model.ZooState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FeedResult(
    val success: Boolean,
    val xpGained: Int = 0,
    val didEvolveTo: PetStage? = null,
    val wasCheerUp: Boolean = false
)

data class AdoptResult(
    val success: Boolean,
    val pet: PetInstance? = null
)

class PetPreferencesRepository(
    private val context: Context,
    private val json: Json
) {
    companion object {
        const val MYSTERY_EGG_COST = 100
        const val SPECIFIC_EGG_COST = 400
        const val WALK_AWAY_REWARD_POINTS = 10
        const val MAX_DAILY_WALK_AWAY_REWARD_COUNT = 3
        const val MAX_DAILY_WALK_AWAY_POINTS = WALK_AWAY_REWARD_POINTS * MAX_DAILY_WALK_AWAY_REWARD_COUNT
    }

    private fun todayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    internal fun readSpendablePoints(prefs: Preferences): Int {
        return prefs[PreferencesManager.KEY_SPENDABLE_POINTS]
            ?: (prefs[PreferencesManager.KEY_LIFETIME_POINTS] ?: 0)
    }

    internal fun readZooState(prefs: Preferences): ZooState {
        val raw = prefs[PreferencesManager.KEY_ZOO_STATE_JSON] ?: return ZooState()
        if (raw.isBlank()) return ZooState()
        return try {
            json.decodeFromString<ZooState>(raw)
        } catch (_: Exception) {
            ZooState()
        }
    }

    internal fun writeZooState(prefs: MutablePreferences, state: ZooState) {
        prefs[PreferencesManager.KEY_ZOO_STATE_JSON] = json.encodeToString(state)
    }

    val zooState: Flow<ZooState> = context.dataStore.data.map { prefs ->
        readZooState(prefs)
    }

    suspend fun ensureStarterEgg(): PetInstance? {
        var starter: PetInstance? = null
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val current = readZooState(prefs)
            if (current.ownedPets.isNotEmpty() || current.hasClaimedStarterEgg) {
                starter = current.activePet
                return@edit
            }
            val species = current.pickMysterySpecies()
            val newPet = PetInstance(
                id = "pet_${now}_1",
                species = species,
                customName = species.defaultPetName,
                xp = 0,
                adoptedAtMillis = now
            )
            val updated = current.copy(
                activePetId = newPet.id,
                ownedPets = listOf(newPet),
                hasClaimedStarterEgg = true
            )
            writeZooState(prefs, updated)
            starter = newPet
        }
        return starter
    }

    suspend fun feedActivePet(treat: PetTreat): FeedResult {
        var result = FeedResult(success = false)
        val today = todayString()
        val now = System.currentTimeMillis()

        context.dataStore.edit { prefs ->
            val zoo = readZooState(prefs)
            val pet = zoo.activePet ?: return@edit
            val spendable = readSpendablePoints(prefs)
            if (spendable < treat.costPoints) return@edit

            val snapshotStats = snapshotTodayMoodStats(prefs, today)
            val currentMood = pet.resolveMood(snapshotStats, today)
            val prevStage = pet.stage
            val (updatedPet, xpGained) = pet.applyFeeding(
                treat = treat,
                currentMood = currentMood,
                todayDateString = today,
                nowMillis = now
            )

            prefs[PreferencesManager.KEY_SPENDABLE_POINTS] = spendable - treat.costPoints
            writeZooState(prefs, zoo.updatePet(updatedPet))

            val nextStage = updatedPet.stage
            result = FeedResult(
                success = true,
                xpGained = xpGained,
                didEvolveTo = if (nextStage != prevStage) nextStage else null,
                wasCheerUp = currentMood == PetMood.SAD
            )
        }
        return result
    }

    suspend fun adoptEgg(species: PetSpecies? = null): AdoptResult {
        var result = AdoptResult(success = false)
        val now = System.currentTimeMillis()

        context.dataStore.edit { prefs ->
            val zoo = readZooState(prefs)
            if (!zoo.canAdoptNewEgg) return@edit

            val isStarter = !zoo.hasClaimedStarterEgg && zoo.ownedPets.isEmpty()
            val cost = when {
                isStarter -> 0
                species == null -> MYSTERY_EGG_COST
                else -> SPECIFIC_EGG_COST
            }

            val spendable = readSpendablePoints(prefs)
            if (spendable < cost) return@edit

            val chosenSpecies = species ?: zoo.pickMysterySpecies()
            val newPet = PetInstance(
                id = "pet_${now}_${zoo.ownedPets.size + 1}",
                species = chosenSpecies,
                customName = chosenSpecies.defaultPetName,
                xp = 0,
                adoptedAtMillis = now
            )
            prefs[PreferencesManager.KEY_SPENDABLE_POINTS] = spendable - cost
            val updatedZoo = zoo.copy(
                activePetId = newPet.id,
                ownedPets = zoo.ownedPets + newPet,
                hasClaimedStarterEgg = true
            )
            writeZooState(prefs, updatedZoo)
            result = AdoptResult(success = true, pet = newPet)
        }
        return result
    }

    suspend fun renamePet(petId: String, newName: String) {
        val trimmed = newName.trim().take(24)
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val zoo = readZooState(prefs)
            val target = zoo.ownedPets.firstOrNull { it.id == petId } ?: return@edit
            writeZooState(prefs, zoo.updatePet(target.copy(customName = trimmed)))
        }
    }

    suspend fun selectActivePet(petId: String) {
        context.dataStore.edit { prefs ->
            val zoo = readZooState(prefs)
            if (zoo.ownedPets.any { it.id == petId }) {
                writeZooState(prefs, zoo.copy(activePetId = petId))
            }
        }
    }

    suspend fun grantDebugSpendablePoints(targetPoints: Int = 999_999) {
        context.dataStore.edit { prefs ->
            val current = readSpendablePoints(prefs)
            prefs[PreferencesManager.KEY_SPENDABLE_POINTS] = maxOf(current, targetPoints)
        }
    }

    internal fun markActivePetSad(prefs: MutablePreferences) {
        val zoo = readZooState(prefs)
        val active = zoo.activePet ?: return
        writeZooState(prefs, zoo.updatePet(active.markSadOnOverBudget()))
    }

    internal fun clearActivePetSadOnCleanFocusHour(prefs: MutablePreferences, today: String) {
        val zoo = readZooState(prefs)
        val active = zoo.activePet ?: return
        if (active.isSadFromOverBudget) {
            writeZooState(prefs, zoo.updatePet(active.clearSadOnCleanFocusHour(today)))
        }
    }

    private fun snapshotTodayMoodStats(prefs: Preferences, today: String): DailyStats {
        val isToday = (prefs[PreferencesManager.KEY_TODAY_DATE] ?: "") == today
        return DailyStats(
            dateString = today,
            graceMinutesUsedToday = if (isToday) prefs[PreferencesManager.KEY_GRACE_USED_TODAY] ?: 0 else 0,
            overBudgetOpensToday = if (isToday) prefs[PreferencesManager.KEY_OVER_BUDGET_OPENS_TODAY] ?: 0 else 0
        )
    }
}
