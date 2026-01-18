package org.example.project.api

/**
 * Очень компактно хранит множество из двух enum-классов.
 * Ограничение: в сумме не более 32 элементов.
 */
class OptimizedCombinedSmallIntEnumSet<E1 : Enum<E1>, E2 : Enum<E2>>(
	private val enumClass1: Class<E1>,
	private val enumClass2: Class<E2>
) {
	private var backingSet: Int = 0
	private val offset = enumClass1.enumConstants.size
	
	init {
		require(enumClass1.enumConstants.size + enumClass2.enumConstants.size <= 32) {
			"Total enum elements exceed 32 (${enumClass1.enumConstants.size} + ${enumClass2.enumConstants.size})"
		}
	}
	
	// === Методы для E1 ===
	fun addEnum1(element: E1): Boolean {
		val mask = 1 shl element.ordinal
		val prev = backingSet
		backingSet = prev or mask
		return (prev and mask) == 0
	}
	
	fun removeEnum1(element: E1): Boolean {
		val mask = 1 shl element.ordinal
		val prev = backingSet
		backingSet = prev and mask.inv()
		return (prev and mask) != 0
	}
	
	fun hasEnum1(element: E1): Boolean {
		return (backingSet and (1 shl element.ordinal)) != 0
	}
	
	// === Методы для E2 ===
	fun addEnum2(element: E2): Boolean {
		val mask = 1 shl (element.ordinal + offset)
		val prev = backingSet
		backingSet = prev or mask
		return (prev and mask) == 0
	}
	
	fun removeEnum2(element: E2): Boolean {
		val mask = 1 shl (element.ordinal + offset)
		val prev = backingSet
		backingSet = prev and mask.inv()
		return (prev and mask) != 0
	}
	
	fun hasEnum2(element: E2): Boolean {
		return (backingSet and (1 shl (element.ordinal + offset))) != 0
	}
	
	// === Общие методы ===
	fun clear() {
		backingSet = 0
	}
	
	fun size(): Int = Integer.bitCount(backingSet)
	
	fun isEmpty(): Boolean = backingSet == 0
	
	// Быстрая проверка на пересечение с другим множеством
	fun hasCommonElements(other: OptimizedCombinedSmallIntEnumSet<E1, E2>): Boolean {
		return (backingSet and other.backingSet) != 0
	}
	
	// Получение всех элементов (если нужно)
	fun getAllEnum1(): Set<E1> {
		return enumClass1.enumConstants
			.filter { hasEnum1(it) }
			.toSet()
	}
	
	fun getAllEnum2(): Set<E2> {
		return enumClass2.enumConstants
			.filter { hasEnum2(it) }
			.toSet()
	}
}