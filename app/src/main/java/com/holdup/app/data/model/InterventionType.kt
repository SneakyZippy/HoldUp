package com.holdup.app.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = InterventionTypeSerializer::class)
enum class InterventionType(val displayName: String, val description: String) {
    BREATHING("Mindful Breathing", "Take a rhythmic breath pause with haptics"),
    REFLECTION("Mindful Reflection", "Grounding thoughts and reality checks to snap out of autopilot"),
    PHOTO("Loved One's Photo", "Remember what truly matters with a personal photo & message"),
    VIDEO("Friend's Video", "Watch a short video message from a loved one or friend"),
    ALTERNATIVES("Healthy Swaps", "Quick suggestions of rewarding things to do right now")
}

object InterventionTypeSerializer : KSerializer<InterventionType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("InterventionType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InterventionType) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): InterventionType {
        val name = decoder.decodeString()
        return when (name) {
            "COUNTDOWN" -> InterventionType.REFLECTION
            else -> try {
                InterventionType.valueOf(name)
            } catch (_: Exception) {
                InterventionType.REFLECTION
            }
        }
    }
}

@Serializable
enum class RuleMode(val displayName: String) {
    SHUFFLE("Shuffle (Randomized)"),
    SPECIFIC("Specific Intervention"),
    SEQUENCE("Multi-Step Pause")
}
