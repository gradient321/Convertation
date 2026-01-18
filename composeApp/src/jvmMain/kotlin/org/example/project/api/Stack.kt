package org.example.project.api

/**
 * Реализация стека (LIFO) на основе [MutableList].
 *
 * @param T тип элементов в стеке.
 */
class Stack<T> : Iterable<T> {
	private val elements = mutableListOf<T>()
	
	/**
	 * Добавляет элемент на вершину стека.
	 *
	 * @param item элемент для добавления.
	 */
	fun push(item: T) {
		elements.add(item)
	}
	
	/**
	 * Удаляет и возвращает элемент с вершины стека.
	 *
	 * @return верхний элемент стека.
	 * @throws NoSuchElementException если стек пуст.
	 */
	fun pop(): T {
		if (isEmpty()) {
			throw NoSuchElementException("Стек пуст")
		}
		return elements.removeAt(elements.size - 1)
	}
	
	/**
	 * Возвращает элемент с вершины стека без удаления.
	 *
	 * @return верхний элемент стека или `null`, если стек пуст.
	 */
	fun peek(): T? {
		return elements.lastOrNull()
	}
	
	/**
	 * Проверяет, пуст ли стек.
	 *
	 * @return `true`, если стек пуст, иначе `false`.
	 */
	fun isEmpty(): Boolean = elements.isEmpty()
	fun isNotEmpty(): Boolean = !isEmpty()
	/**
	 * Возвращает количество элементов в стеке.
	 *
	 * @return размер стека.
	 */
	val size: Int get() = elements.size
	
	/**
	 * Очищает стек.
	 */
	fun clear() = elements.clear()
	
	override fun toString(): String = elements.toString()
	
	override fun iterator(): Iterator<T> = elements.iterator()
}

fun <T> Iterable<T>.toStack(): Stack<T> {
	return Stack<T>().apply {
		this@toStack.forEach { push(it) }
	}
}

fun <T> Iterable<T>.toStackReversed(): Stack<T> {
	if (this is List<*>) {
		return asReversed().toStack() as Stack<T>
	}
	return toList().asReversed().toStack()
}