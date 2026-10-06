package com.holdup.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class InterventionType(val displayName: String, val description: String) {
    BREATHING("Mindful Breathing", "Take a rhythmic breath pause with haptics"),
    PHOTO("Loved One's Photo", "Remember what truly matters with a personal photo & message"),
    VIDEO("Friend's Video", "Watch a short video message from a loved one or friend"),
    COUNTDOWN("Calm Countdown", "A silent pause to let the dopamine rush dissipate"),
    ALTERNATIVES("Healthy Swaps", "Quick suggestions of rewarding things to do right now")
}

@Serializable
enum class RuleMode(val displayName: String) {
    SHUFFLE("Shuffle (Randomized)"),
    SPECIFIC("Specific Intervention"),
    SEQUENCE("Multi-Step Pause")
}
