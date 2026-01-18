package org.example.project.api

import ru_lonya.util.interfaces.IId

interface HybridCollection<V> : Iterable<V> {
	operator fun get(key: String): V?
	operator fun get(index: Int): V?
	operator fun contains(key: String): Boolean
	operator fun contains(value: V): Boolean
	fun getById(id: String): V?
	val values: List<V>
	val keys: Set<String>
	fun hasId(index: Int): Boolean
	fun getId(index: Int): String?
	fun getIndex(id: String): Int?
	fun containsKey(key: String): Boolean
	fun containsValue(value: V): Boolean
}

interface MutableHybridCollection<V> : HybridCollection<V> {
	fun add(value: V)
	fun add(index: Int, value: V)
	operator fun plusAssign(value: V)
}

class HybridCollectionImpl<V> : MutableHybridCollection<V>, Iterable<V> {
	private val ordered = mutableListOf<V>()
	private val byId = mutableMapOf<String, V>()
	
	override fun add(value: V) {
		ordered.add(value)
		if (value is IId) byId[value.id] = value
	}
	override fun add(index: Int, value: V) {
		ordered.add(index, value)
		if (value is IId) byId[value.id] = value
	}
	
	override fun getById(id: String): V? = byId[id]
	override val values: List<V> = ordered
	override val keys: Set<String> = byId.keys
	override fun hasId(index: Int): Boolean = ordered[index] is IId
	override fun getId(index: Int): String? = ordered[index]?.let { if (it is IId) it.id else null }
	override fun getIndex(id: String): Int? = byId[id]?.let { ordered.withIndex().find { e -> e.value === it }?.index }
	override fun containsKey(key: String): Boolean = byId.containsKey(key)
	override fun containsValue(value: V): Boolean = ordered.contains(value)
	
	override operator fun get(key: String): V? = getById(key)
	override operator fun get(index: Int): V? = ordered[index]
	override operator fun plusAssign(value: V) { add(value) }
	override operator fun contains(key: String): Boolean = containsKey(key)
	override operator fun contains(value: V): Boolean = containsValue(value)
	
	override fun iterator(): Iterator<V> {
		return ordered.iterator()
	}
}


class SingletonHybridCollection<V> private constructor(private val element: V) : HybridCollection<V> {
	
	private val cachedValues by lazy { listOf(element) }
	private val cachedKeys by lazy {
		if (element is IId) setOf(element.id) else emptySet<String>()
	}
	private val cachedById by lazy {
		if (element is IId) mapOf(element.id to element) else emptyMap<String, V>()
	}
	
	override fun get(key: String): V? = cachedById[key]
	override fun get(index: Int): V? = if (index == 0) element else null
	override fun contains(key: String): Boolean = cachedById.containsKey(key)
	override fun contains(value: V): Boolean = element == value
	
	override fun getById(id: String): V? = cachedById[id]
	override val values: List<V> get() = cachedValues
	override val keys: Set<String> get() = cachedKeys
	override fun hasId(index: Int): Boolean = index == 0 && element is IId
	override fun getId(index: Int): String? = if (index == 0 && element is IId) element.id else null
	override fun getIndex(id: String): Int? = if (element is IId && element.id == id) 0 else null
	override fun containsKey(key: String): Boolean = cachedById.containsKey(key)
	override fun containsValue(value: V): Boolean = element == value
	
	override fun iterator(): Iterator<V> = cachedValues.iterator()
	
	companion object {
		fun <V> of(element: V): HybridCollection<V> {
			return SingletonHybridCollection(element)
		}
	}
}


fun <V> MutableHybridCollection<V>.addAll(collection: Iterable<V>) {
	collection.forEach { add(it) }
}

fun <V> MutableHybridCollection<V>.addAll(vararg elements: V) {
	elements.forEach { add(it) }
}

inline fun <V, R> HybridCollection<V>.mapHybrid(transform: (V) -> R): HybridCollection<R> {
	return HybridCollectionImpl<R>().apply {
		for (element in this@mapHybrid) {
			add(transform(element))
		}
	}
}

fun <V> hybridOf(element: V): HybridCollection<V> {
	return SingletonHybridCollection.of(element)
}
fun <V> hybridOf(vararg elements: V): HybridCollection<V> {
	return HybridCollectionImpl<V>().apply { addAll(*elements) }
}

fun <V> mutableHybridOf(vararg elements: V): MutableHybridCollection<V> {
	return HybridCollectionImpl<V>().apply { addAll(*elements) }
}