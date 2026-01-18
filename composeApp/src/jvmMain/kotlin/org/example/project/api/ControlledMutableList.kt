package org.example.project.api

import java.util.function.IntFunction

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
class ControlledMutableList<T>(
	private val lock: () -> Boolean,
	private val onError: () -> Nothing = { throw IllegalStateException("List is locked") },
	private val innerList: MutableList<T> = mutableListOf()
) : MutableList<T> by innerList {
	
	override fun add(element: T): Boolean {
		if (lock()) onError()
		return innerList.add(element)
	}
	
	override fun add(index: Int, element: T) {
		if (lock()) onError()
		innerList.add(index, element)
	}
	
	override fun addAll(elements: Collection<T>): Boolean {
		if (lock()) onError()
		return innerList.addAll(elements)
	}
	
	override fun addAll(index: Int, elements: Collection<T>): Boolean {
		if (lock()) onError()
		return innerList.addAll(index, elements)
	}
	
	override fun remove(element: T): Boolean {
		if (lock()) onError()
		return innerList.remove(element)
	}
	
	override fun removeAt(index: Int): T {
		if (lock()) onError()
		return innerList.removeAt(index)
	}
	
	override fun removeAll(elements: Collection<T>): Boolean {
		if (lock()) onError()
		return innerList.removeAll(elements)
	}
	
	override fun retainAll(elements: Collection<T>): Boolean {
		if (lock()) onError()
		return innerList.retainAll(elements)
	}
	
	override fun set(index: Int, element: T): T {
		if (lock()) onError()
		return innerList.set(index, element)
	}
	
	override fun clear() {
		if (lock()) onError()
		innerList.clear()
	}
	
	override fun iterator(): MutableIterator<T> =
		ControlledMutableIterator(innerList.iterator())
	
	override fun listIterator(): MutableListIterator<T> =
		ControlledMutableListIterator(innerList.listIterator())
	
	override fun listIterator(index: Int): MutableListIterator<T> =
		ControlledMutableListIterator(innerList.listIterator(index))
	
	private inner class ControlledMutableIterator(
		private val delegate: MutableIterator<T>
	) : MutableIterator<T> by delegate {
		override fun remove() {
			if (lock()) onError()
			delegate.remove()
		}
	}
	
	private inner class ControlledMutableListIterator(
		private val delegate: MutableListIterator<T>
	) : MutableListIterator<T> by delegate {
		override fun add(element: T) {
			if (lock()) onError()
			delegate.add(element)
		}
		
		override fun remove() {
			if (lock()) onError()
			delegate.remove()
		}
		
		override fun set(element: T) {
			if (lock()) onError()
			delegate.set(element)
		}
	}
}