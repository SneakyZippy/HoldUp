package com.holdup.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AlternativeActivity(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val category: String = "Mind & Body",
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false
)

val presetSwapEmojis = listOf(
    "💧", "🌬️", "🧘", "💬", "📖", "☀️",
    "✨", "🚶", "📝", "🫖", "🎵", "🍎",
    "🪴", "🎨", "🧩", "💪", "🧹", "🌳"
)

val defaultAlternativeActivities = listOf(
    AlternativeActivity("1", "Drink a glass of water", "💧", "Wellness"),
    AlternativeActivity("2", "Take 5 deep breaths", "🌬️", "Mindfulness"),
    AlternativeActivity("3", "Do 10 pushups or a quick stretch", "🧘", "Movement"),
    AlternativeActivity("4", "Text a good friend or family member", "💬", "Connection"),
    AlternativeActivity("5", "Read 2 pages of a book", "📖", "Learning"),
    AlternativeActivity("6", "Step outside for fresh air", "☀️", "Nature"),
    AlternativeActivity("7", "Tidy up your immediate workspace", "✨", "Focus"),
    AlternativeActivity("8", "Go for a brisk 2-minute walk", "🚶", "Movement"),
    AlternativeActivity("9", "Write down one thought or priority", "📝", "Focus"),
    AlternativeActivity("10", "Brew a calming cup of tea", "🫖", "Wellness"),
    AlternativeActivity("11", "Listen to a favorite song with eyes closed", "🎵", "Relaxation"),
    AlternativeActivity("12", "Look 20 feet away for 20 seconds", "👀", "Eye Health")
)
