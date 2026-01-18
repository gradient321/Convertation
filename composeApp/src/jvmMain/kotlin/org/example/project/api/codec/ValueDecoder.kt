package ru_lonya.util.codec

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

abstract class ValueDecoder(private val value: Any?, val serializerModuleBuilder: SerializerModuleBuilder) : Decoder {
	override val serializersModule: SerializersModule = serializerModuleBuilder.build()
	
	@OptIn(ExperimentalSerializationApi::class)
	override fun decodeNotNullMark(): Boolean {
		return value != null
	}
	
	@OptIn(ExperimentalSerializationApi::class)
	override fun decodeNull(): Nothing? {
		if (value == null) return null
		throw SerializationException("Expected null, but got value of type ${value::class.simpleName}")
	}
	
	override fun decodeBoolean(): Boolean {
		return when (value) {
			is Boolean -> value
			is String -> value.trim().let {
				when (it.lowercase()) {
					"true", "yes", "on", "1" -> true
					"false", "no", "off", "0" -> false
					else -> throw IllegalArgumentException("Cannot convert string '$it' to Boolean")
				}
			}
			is Number -> value.toDouble() != 0.0
			null -> throw IllegalArgumentException("Expected Boolean, got null")
			else -> throw IllegalArgumentException("Expected Boolean, got '${value::class.simpleName}' value '$value'")
		}
	}
	
	override fun decodeByte(): Byte {
		return convertToNumber("Byte") { it.toByte() }
	}
	
	override fun decodeShort(): Short {
		return convertToNumber("Short") { it.toShort() }
	}
	
	override fun decodeChar(): Char {
		return when (value) {
			is Char -> value
			is String -> if (value.length == 1) value[0] else throw IllegalArgumentException("String '$value' cannot be converted to Char (expected single character)")
			is Number -> value.toInt().toChar()
			null -> throw IllegalArgumentException("Expected Char, got null")
			else -> throw IllegalArgumentException("Expected Char, got '${value::class.simpleName}' value '$value'")
		}
	}
	
	override fun decodeInt(): Int {
		return convertToNumber("Int") { it.toInt() }
	}
	
	override fun decodeLong(): Long {
		return convertToNumber("Long") { it.toLong() }
	}
	
	override fun decodeFloat(): Float {
		return convertToNumber("Float") { it.toFloat() }
	}
	
	override fun decodeDouble(): Double {
		return convertToNumber("Double") { it.toDouble() }
	}
	
	override fun decodeString(): String {
		return when (value) {
			is String -> value
			null -> throw IllegalArgumentException("Expected String, got null")
			else -> value.toString()
		}
	}
	
	override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
		val stringValue = decodeString()
		val index = enumDescriptor.getElementIndex(stringValue)
		if (index == -1) {
			throw SerializationException("Unknown enum value '$stringValue' for enum ${enumDescriptor.serialName}. Valid values are: ${
				(0 until enumDescriptor.elementsCount).joinToString { enumDescriptor.getElementName(it) }
			}")
		}
		return index
	}
	
	override fun decodeInline(descriptor: SerialDescriptor): Decoder {
		return this
	}
	
	@OptIn(ExperimentalSerializationApi::class)
	override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
		return when (val kind = descriptor.kind) {
			StructureKind.LIST -> {
				val listValue = value as? List<*> ?: throw SerializationException(
					"Expected List for ${descriptor.serialName}, but got ${value?.javaClass?.simpleName ?: "null"}"
				)
				listDecoder(listValue, descriptor)
			}
			StructureKind.MAP -> {
				val mapValue = value as? Map<Any?, Any?> ?: throw SerializationException(
					"Expected Map for ${descriptor.serialName}, but got ${value?.javaClass?.simpleName ?: "null"}"
				)
				mapDecoder(mapValue, descriptor)
			}
			StructureKind.CLASS, StructureKind.OBJECT -> {
				val mapValue = value as? Map<Any?, Any?> ?: throw SerializationException(
					"Expected Map for ${descriptor.serialName}, but got ${value?.javaClass?.simpleName ?: "null"}"
				)
				classDecoder(mapValue, descriptor)
			}
			is PrimitiveKind -> throw SerializationException("Primitive type ${kind} cannot begin structure")
			is PolymorphicKind -> throw SerializationException("Polymorphic type $kind is not supported in this decoder")
			else -> throw SerializationException("Unsupported structure kind: $kind")
		}
	}
	
	abstract fun listDecoder(list: List<Any?>, descriptor: SerialDescriptor): CompositeDecoder
	abstract fun mapDecoder(map: Map<Any?, Any?>, descriptor: SerialDescriptor): CompositeDecoder
	abstract fun classDecoder(obj: Map<Any?, Any?>, descriptor: SerialDescriptor): CompositeDecoder
	
	private inline fun <reified T : Number> convertToNumber(typeName: String, converter: (Number) -> T): T {
		return when (value) {
			is Number -> converter(value)
			is String -> try {
				converter(value.toDouble())
			} catch (e: NumberFormatException) {
				throw IllegalArgumentException("Cannot convert string '$value' to $typeName")
			}
			null -> throw IllegalArgumentException("Expected $typeName, got null")
			else -> throw IllegalArgumentException("Expected $typeName, got '${value::class.simpleName}' value '$value'")
		}
	}
}