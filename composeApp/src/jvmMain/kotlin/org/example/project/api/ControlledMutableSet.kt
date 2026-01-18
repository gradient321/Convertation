package org.example.project.api

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
class ControlledMutableSet<T>(
	private val lock: () -> Boolean,
	private val onError: () -> Nothing = { throw IllegalStateException("Set is locked!") },
	private val innerSet: MutableSet<T> = mutableSetOf()
) : MutableSet<T> by innerSet {
	
	override fun iterator(): MutableIterator<T> =
		ControlledMutableIterator(innerSet.iterator())
	
	private inner class ControlledMutableIterator(
		private val delegate: MutableIterator<T>
	) : MutableIterator<T> by delegate {
		override fun remove() {
			if (lock()) onError()
			delegate.remove()
		}
	}
	
	override fun add(element: T): Boolean {
		if (lock()) onError()
		return innerSet.add(element)
	}
	
	override fun addAll(elements: Collection<T>): Boolean {
		if (lock()) onError()
		return innerSet.addAll(elements)
	}
	
	override fun remove(element: T): Boolean {
		if (lock()) onError()
		return innerSet.remove(element)
	}
	
	override fun removeAll(elements: Collection<T>): Boolean {
		if (lock()) onError()
		return innerSet.removeAll(elements)
	}
	
	override fun retainAll(elements: Collection<T>): Boolean {
		if (lock()) onError()
		return innerSet.retainAll(elements)
	}
	
	override fun clear() {
		if (lock()) onError()
		innerSet.clear()
	}
}