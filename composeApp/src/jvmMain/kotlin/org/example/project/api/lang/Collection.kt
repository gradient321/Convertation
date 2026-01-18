package ru_lonya.util.extend.lang


inline fun <T> mutableList(initialCapacity: Int): MutableList<T> = ArrayList(initialCapacity)
inline fun <T> list(initialCapacity: Int): List<T> = ArrayList(initialCapacity)

fun <T> MutableList<T>.exclude(element: T): MutableList<T> {
	this.remove(element)
	return this
}

fun <T> MutableList<T>.excludeAll(elements: Collection<T>): MutableList<T> {
	this.removeAll(elements)
	return this
}

// Перегрузка для vararg
fun <T> MutableList<T>.excludeAll(vararg elements: T): MutableList<T> {
	this.removeAll(elements)
	return this
}


@Suppress("ClassName")
object collection {
	enum class ForEachOut {
		NONE,
		BREAK,
		CONTINUE
	}
	
	inline fun <T> Iterable<T>.forEachWithControl(action: (T) -> ForEachOut?) {
		for (item in this) {
			when (action(item)) {
				ForEachOut.BREAK -> return // Прерываем итерацию
				ForEachOut.CONTINUE -> continue // Переходим к следующей итерации
				ForEachOut.NONE -> {} // Ничего не делаем, продолжаем
				else -> {}
			}
		}
	}
	
	inline fun <T> Iterable<T>.forEachIndexedWithControl(action: (index: Int, item: T) -> ForEachOut?) {
		var index = 0
		for (item in this) {
			when (action(index, item)) {
				ForEachOut.BREAK -> return // Прерываем итерацию
				ForEachOut.CONTINUE -> continue // Переходим к следующей итерации
				ForEachOut.NONE -> {} // Ничего не делаем, продолжаем
				else -> {}
			}
			index++
		}
	}
	
	inline fun <T> Iterable<T>.forEachWithBreak(action: (T) -> Boolean) {
		for (item in this) {
			if (!action(item)) {
				break
			}
		}
	}
	
	inline fun <T> Iterable<T>.forEachIndexedWithBreak(action: (index: Int, item: T) -> Boolean) {
		var index = 0
		for (item in this) {
			if (!action(index, item)) {
				break
			}
			index++
		}
	}
	
	inline fun <C : Collection<*>> C?.ifNotNullOrEmpty(action: (C) -> Unit): C? {
		if (!isNullOrEmpty()) {
			action(this)
		}
		return this
	}
	
	inline fun <C : Collection<*>> C.ifNotEmpty(action: (C) -> Unit): C {
		if (isNotEmpty()) {
			action(this)
		}
		return this
	}
	
	fun <C : Collection<*>> C.takeIfNotEmpty(): C? = takeIf { isNotEmpty() }
	
	fun <C : Collection<*>> C.takeIfEmpty(): C? = takeIf { isEmpty() }
	
	fun <C : Collection<*>, R> C.takeIfNotEmpty(block: (C) -> R): R? = takeIf { isNotEmpty() }?.let(block)
	
	fun <C : Collection<*>, R> C.takeIfEmpty(block: (C) -> R): R? = takeIf { isEmpty() }?.let(block)
	
	fun <T> Collection<T>.toLinkedHashSet(): LinkedHashSet<T> {
		return LinkedHashSet(this)
	}
	
	fun <T> Collection<T>.toHashSet(): HashSet<T> {
		return HashSet(this)
	}
	
	inline fun <T> Iterable<T>.forEachThis(action: T.() -> Unit) = forEach { it.action() }
	inline fun <T> Iterable<T>.forEachThisIndexed(action: T.(Int) -> Unit) = forEachIndexed { index, t -> t.action(index) }
	
	
	inline fun <T> Pair<T, T>.forEachThis(block: T.() -> Unit) {
		this.first.block()
		this.second.block()
	}
	
	inline fun <T> Pair<T, T>.forEach(block: (T) -> Unit) {
		block(this.first)
		block(this.second)
	}
	
	inline fun <E : Any?, T : Collection<E>> Collection<T>.getAll(block: (E) -> Unit) {
	
	}
	
	inline fun <T> Iterable<T>.sumOf(selector: (T) -> String): String {
		var sum = ""
		for (element in this) {
			sum += selector(element)
		}
		return sum
	}
}