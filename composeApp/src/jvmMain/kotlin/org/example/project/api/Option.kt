package org.example.project.api

sealed interface Option<out T> {
	data object None : Option<Nothing>
	data class Some<T>(val value: T) : Option<T>
}

fun <T> Option<T>.orElse(default: T): T = when (this) {
	Option.None -> default
	is Option.Some -> this.value
}

inline fun <T> Option<T>.orElse(default: () -> T): T = when (this) {
	Option.None -> default()
	is Option.Some -> this.value
}

fun <T> Option<T>.orThrow(): T = when (this) {
	is Option.None -> throw IllegalStateException("Option is None")
	is Option.Some -> this.value
}

fun <T> Option<T>.orThrow(message: String): T = when (this) {
	is Option.None -> throw IllegalStateException("Option is None. $message")
	is Option.Some -> this.value
}

inline fun <T> Option<T>.map(transform: (T) -> T): Option<T> = when (this) {
	is Option.None -> Option.None
	is Option.Some -> Option.Some(transform(this.value))
}

fun <T> Option<T>.filter(predicate: (T) -> Boolean): Option<T> = when (this) {
	is Option.None -> Option.None
	is Option.Some -> if (predicate(this.value)) this else Option.None
}

fun <T> Option<T>.flatMap(transform: (T) -> Option<T>): Option<T> = when (this) {
	is Option.None -> Option.None
	is Option.Some -> transform(this.value)
}

fun <T> Option<T>.isEmpty(): Boolean = this is Option.None
fun <T> Option<T>.isNotEmpty(): Boolean = this is Option.Some

fun <T> Option<T>.isPresent(): Boolean = this is Option.Some
fun <T> Option<T>.isNone(): Boolean = this is Option.None

fun <T: Any> Option<T>.get(): T? = when (this) {
	is Option.None -> null
	is Option.Some -> this.value
}

fun <T> Option<T>.orNull(): T? = when (this) {
	is Option.None -> null
	is Option.Some -> this.value
}

fun <T : Any> T?.toOption(): Option<T> = if (this == null) Option.None else Option.Some(this)
fun <T : Any?> T.toSome(): Option<T> = Option.Some(this)

fun <T> Some(value: T): Option.Some<T> = Option.Some(value)
inline val None: Option.None get() = Option.None