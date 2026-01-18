@file:Suppress("NOTHING_TO_INLINE")

package ru_lonya.util.extend.lang

import kotlin.collections.LinkedHashMap
import kotlin.collections.iterator

typealias MapData = Map<String, Any>
typealias MutableMapData = MutableMap<String, Any>

inline fun <K, V> mutableMap(initialCapacity: Int): MutableMap<K, V> = LinkedHashMap(initialCapacity)
inline fun <K, V> map(initialCapacity: Int): Map<K, V> = LinkedHashMap(initialCapacity)


fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> {
	return this.filterValues { it != null }.mapValues { it.value!! }
}

inline fun <T, K, V> Iterable<T>.associateNotNull(
	transform: (T) -> Pair<K, V>?,
): Map<K, V> {
	return mapNotNull { item ->
		transform(item)
	}.toMap()
}

inline fun <T, K, V> Iterable<T>.associateAllNotNull(
	transform: (T) -> Pair<K?, V?>?,
): Map<K, V> {
	return mapNotNull { item ->
		transform(item)?.let { (k, v) ->
			if (k != null && v != null) k to v
			else null
		}
	}.toMap()
}

inline fun <T, K, V> Iterable<T>.associateValueNotNull(
	transform: (T) -> Pair<K, V?>,
): Map<K, V> {
	return mapNotNull { item ->
		transform(item).let { (k, v) ->
			if (v != null) k to v
			else null
		}
	}.toMap()
}

fun <K, V, M : MutableMap<K, V>> M.put(pair: Pair<K, V>): V? =
	put(pair.first, pair.second)

fun <K, V, M : MutableMap<K, V>> M.put(entry: Map.Entry<K, V>): V? =
	put(entry.key, entry.value)

inline fun <K, V, M : Map<K, V>> M.filter(
	predicate: (Map.Entry<K, V>) -> Boolean,
): Map<K, V> {
	val result = LinkedHashMap<K, V>()
	for (entry in this) {
		if (predicate(entry)) {
			result.put(entry)
		}
	}
	return result
}

inline fun <K, V, M : Map<K, V>, RK, RV> M.associate(
	transform: (Map.Entry<K, V>) -> Pair<RK, RV>,
): Map<RK, RV> {
	val result = mutableMapOf<RK, RV>()
	for (entry in this) {
		result.put(transform(entry))
	}
	return result
}

inline fun <K, V, M : Map<K, V>, R> M.associateValues(
	transform: (V) -> R,
): Map<K, R> {
	val result = mutableMapOf<K, R>()
	for (entry in this) {
		result.put(entry.key to transform(entry.value))
	}
	return result
}

inline fun <K, V, M : Map<K, V>, RK, RV> M.associateFilter(
	predicate: (Map.Entry<K, V>) -> Boolean,
	transform: (Map.Entry<K, V>) -> Pair<RK, RV>,
): Map<RK, RV> {
	val result = LinkedHashMap<RK, RV>()
	for (entry in this) {
		if (predicate(entry)) {
			result.put(transform(entry))
		}
	}
	return result
	
}

fun <K, M : Map<K, *>> M.containsAllKeys(vararg keys: K): Boolean {
	return keys.all { this.containsKey(it) }
}

fun <K, M : Map<K, *>> M.containsAnyKeys(vararg keys: K): Boolean {
	return keys.any { this.containsKey(it) }
}

inline fun <M : Map<*, *>> M?.ifNotNullOrEmpty(action: (M) -> Unit): M? {
	if (!isNullOrEmpty()) {
		action(this)
	}
	return this
}

inline fun <M : Map<*, *>> M.ifNotEmpty(action: (M) -> Unit): M {
	if (isNotEmpty()) {
		action(this)
	}
	return this
}

fun <K, V> Map<K, V>.toLinkedHashMap(): LinkedHashMap<K, V> = LinkedHashMap(this)