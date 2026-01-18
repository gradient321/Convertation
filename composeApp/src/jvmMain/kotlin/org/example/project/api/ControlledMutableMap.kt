package org.example.project.api

class ControlledMutableMap<K, V>(
	private val lock: () -> Boolean,
	private val onError: (ChangeEvent<K, V>) -> Nothing = { throw IllegalStateException("Map is locked") },
	private val innerMap: MutableMap<K, V> = mutableMapOf()
) : MutableMap<K, V> by innerMap {
	
	sealed class ChangeEvent<out K, out V> {
		data class Put<K, V>(val key: K, val value: V) : ChangeEvent<K, V>()
		data class PutAll<K, V>(val from: Map<out K, V>) : ChangeEvent<K, V>()
		data class Remove<K>(val key: K) : ChangeEvent<K, Nothing>()
		object Clear : ChangeEvent<Nothing, Nothing>()
	}
	
	override fun put(key: K, value: V): V? {
		if (lock()) onError(ChangeEvent.Put(key, value))
		return innerMap.put(key, value)
	}
	
	override fun putAll(from: Map<out K, V>) {
		if (lock()) onError(ChangeEvent.PutAll(from))
		innerMap.putAll(from)
	}
	
	override fun remove(key: K): V? {
		if (lock()) onError(ChangeEvent.Remove(key))
		return innerMap.remove(key)
	}
	
	override fun clear() {
		if (lock()) onError(ChangeEvent.Clear)
		innerMap.clear()
	}
}