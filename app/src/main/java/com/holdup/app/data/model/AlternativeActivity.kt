package com.holdup.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AlternativeActivity(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val category: String = "Mind & Body"
)

val defaultAlternativeActivities = listOf(
    AlternativeActivity("1", "Drink a glass of water", "💧", "Wellness"),
    AlternativeActivity("2", "Take 5 deep breaths", "🌬️", "Mindfulness"),
    AlternativeActivity("3", "Do 10 pushups or a quick stretch", "🧘", "Movement"),
    AlternativeActivity("4", "Text a good friend or family member", "💬", "Connection"),
    AlternativeActivity("5", "Read 2 pages of a book", "📖", "Learning"),
    AlternativeActivity("6", "Step outside for fresh air", "☀️", "Nature"),
    AlternativeActivity("7", "Tidy up your immediate workspace", "✨", "Focus")
)
