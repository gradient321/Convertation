package ru_lonya.util.extend.lang


@Suppress("NOTHING_TO_INLINE")
inline fun <T> Array<T?>.mergeInto(other: Array<T?>) {
	val limit = minOf(size, other.size)
	for (i in 0 until limit) {
		if (this[i] == null) {
			this[i] = other[i]
		}
	}
}