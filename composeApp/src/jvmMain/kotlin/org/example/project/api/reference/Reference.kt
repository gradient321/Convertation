package ru_lonya.util.reference

typealias MutBool = Mut<Boolean>
typealias RefBool = Ref<Boolean>

typealias MutInt = Mut<Int>
typealias RefInt = Ref<Int>

data class Mut<T>(override var value: T) : MutableReference<T> { companion object }
data class Ref<T>(override val value: T) : Reference<T> { companion object }

interface Reference<T> {
	companion object;
	val value: T
	fun get(): T = value
}

interface MutableReference<T> : Reference<T> {
	companion object;
	override var value: T
	fun set(value: T) { this.value = value }
}