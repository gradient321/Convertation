package ru_lonya.util.extend.lang

inline fun Boolean.then(block: () -> Unit) {
	if (this) block()
}