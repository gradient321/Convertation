package ru_lonya.util.codec

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import java.util.UUID

object UtilSerializersModule {
	val all by lazy {
		SerializersModule {
			include(java)
		}
	}
	
	val java by lazy {
		SerializersModule {
			contextual(UUID::class, UUIDSerializer)
			
		}
	}
}

object UUIDSerializer : KSerializer<UUID> {
	override val descriptor = PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)
	
	override fun deserialize(decoder: Decoder): UUID {
		return UUID.fromString(decoder.decodeString())
	}
	
	override fun serialize(encoder: Encoder, value: UUID) {
		encoder.encodeString(value.toString())
	}
}