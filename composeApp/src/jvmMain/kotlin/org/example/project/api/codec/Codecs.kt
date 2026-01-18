package ru_lonya.util.codec

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object Codecs {
	val enumsCodecs = ConcurrentHashMap<KClass<*>, AnyCodec<*, String>>()
	
	@Suppress("UNCHECKED_CAST", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
	inline fun <reified E : Enum<E>> enumCodec(): AnyCodec<E, String> = enumsCodecs.getOrPut(E::class) {
		object : AnyCodec<E, String> {
			private val enumValues = enumValues<E>().associateBy { it.name }
			
			override fun encode(obj: E) = obj.name
			override fun decode(name: String) = enumValues[name] ?: throw IllegalArgumentException("No enum constant ${E::class.simpleName}.$name")
		}
	} as AnyCodec<E, String>
	
	val UuidToBytes = object : AnyCodec<java.util.UUID, ByteArray> {
		override fun encode(uuid: java.util.UUID): ByteArray {
			val bytes = ByteArray(16)
			val msb = uuid.mostSignificantBits
			val lsb = uuid.leastSignificantBits
			
			for (i in 0..7) {
				bytes[i] = (msb shr (56 - i * 8)).toByte()
			}
			for (i in 8..15) {
				bytes[i] = (lsb shr (56 - i * 8)).toByte()
			}
			
			return bytes
		}
		override fun decode(bytes: ByteArray): java.util.UUID {
			require(bytes.size == 16) { "Byte array must be 16 bytes long" }
			
			var msb = 0L
			var lsb = 0L
			
			for (i in 0..7)
				msb = msb shl 8 or (bytes[i].toInt() and 0xFF).toLong()
			for (i in 8..15)
				lsb = lsb shl 8 or (bytes[i].toInt() and 0xFF).toLong()
			
			return UUID(msb, lsb)
		}
	}
	
}