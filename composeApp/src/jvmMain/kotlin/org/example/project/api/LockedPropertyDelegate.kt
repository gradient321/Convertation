package org.example.project.api

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LockedPropertyDelegate<T>(val default: T, val lock: () -> Boolean, val onError: () -> Nothing = ::defaultOnError) : ReadWriteProperty<Any?, T> {
	var value: T = default
	override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return value
	}
	
	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		if (lock()) onError()
		this.value = value
	}
	
	companion object {
		
		fun defaultOnError(): Nothing {
			throw IllegalStateException("Property modification is locked")
		}
		
		fun <T> locked(default: T, lock: () -> Boolean)= LockedPropertyDelegate(default, lock)
		fun <T> locked(default: T, lock: () -> Boolean, onError: () -> Nothing) = LockedPropertyDelegate(default, lock, onError)
		
	}
}

class CustomLockedPropertyDelegate<T>(
	val lock: () -> Boolean, val onError: () -> Nothing = ::defaultOnError,
	val getter: () -> T, val setter: (T) -> Unit
) : ReadWriteProperty<Any?, T> {
	
	override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return getter()
	}
	
	override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
		if (lock()) onError()
		setter(value)
	}
	
	companion object {
		
		fun defaultOnError(): Nothing {
			throw IllegalStateException("Property modification is locked")
		}
		
		fun <T> locked(lock: () -> Boolean, getter: () -> T, setter: (T) -> Unit)
			= CustomLockedPropertyDelegate(lock, getter = getter, setter = setter)
		fun <T> locked(lock: () -> Boolean, onError: () -> Nothing, getter: () -> T, setter: (T) -> Unit)
			= CustomLockedPropertyDelegate(lock, onError, getter, setter)
	}
	
}