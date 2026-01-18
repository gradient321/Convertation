package org.example.project.api

/**
 * Контейнер для хранения ошибки и результата выполнения функции.
 * Что бы можно было хранить и то и другое на всякий случай.
 */
data class Try<E : Any>(val value: E, val throwable: Throwable?)

class UResultException : Exception {
	constructor(message: String) : super(message)
	constructor(message: String, cause: Throwable?) : super(message, cause)
}

sealed interface UResult<O : Any?, E : Any> {
	
	fun get(): O? = if (this is Ok) value else null
	fun getOrNull(): O? = if (this is Ok) value else null
	
	fun getOrElse(other: O): O = if (this is Ok) value else other
	fun getOrThrow(): O = when (this) {
		is Ok -> value;
		is Err -> throw createThrowable() // Safe: Тут не может быть `Ok`
	}
	
	/**
	 * Создаёт исключение на основе текущего состояния.
	 * Если текущее состояние - `Ok`, то будет возвращено `null`.
	 */
	fun createThrowableOrNull(): Throwable? = when (this) {
		is Ok -> null
		is Err -> when (error) {
			is Throwable -> error
			is Try<*> -> {
				val throwable = error.throwable
				if (throwable != null) {
					if (error.value is Throwable) throwable.apply { addSuppressed(error.value) }
					else UResultException(error.toString(), throwable)
				} else {
					if (error.value is Throwable) error.value
					else UResultException(error.toString())
				}
			}
			else -> UResultException(error.toString())
		}
	}
	
	fun createThrowableOrNull(newMessage: String): Throwable? {
		val throwable = createThrowableOrNull() ?: return null
		return UResultException(newMessage, throwable)
	}
	
	fun createThrowable(): Throwable  {
		return createThrowableOrNull() ?: UResultException("UResult.Ok(value=${(this as Ok<*, *>).value})")
	}
	
	fun createThrowable(newMessage: String): Throwable {
		val throwable = createThrowable()
		return UResultException(newMessage, throwable)
	}
	
	/**
	 * При применении в контексте `return ...asReturn()` дженерики подставятся автоматически.
	 * Если функция, где это используется тоже, возвращает `UResult`.
	 */
	@Suppress("UNCHECKED_CAST")
	fun <RO : Any?, RE : Any> asReturn(): UResult<RO, RE> = when (this) {
		is Ok -> Ok(value as RO)
		is Err -> Err(error as RE)
	}
	
	fun isOk(): Boolean = this is Ok
	fun isErr(): Boolean = this is Err
	
	fun unwrap() = getOrThrow()
	
	data class Ok<O : Any?, E : Any>(val value: O) : UResult<O, E> {
		override fun toString(): String {
			return "UResult.Ok<${value?.let { it::class.simpleName }}>($value)"
		}
	}
	
	data class Err<O : Any?, E : Any>(val error: E) : UResult<O, E> {
		override fun toString(): String {
			return "UResult.Err<${error::class.simpleName}>($error)"
		}
	}
}

//inline fun <O : Any?, E : Any> UResult<O, E>.ret(action: (E) -> Nothing = { return Err(it) }): UResult<O, E> =
//	when (this) {
//		is UResult.Ok -> this
//		is UResult.Err -> action(error)
//	}

inline fun <O : Any?, E : Any> UResult<O, E>.getOrNothing(block: (E) -> Nothing): O = when (this) {
	is UResult.Ok -> value
	is UResult.Err -> block(error)
}

inline fun <O : Any?, E : Any> UResult<O, E>.orElse(f: () -> O): UResult<O, E> = when (this) {
	is UResult.Ok -> Ok(value)
	is UResult.Err -> Ok(f())
}

inline fun <O : Any?, E : Any> UResult<O, E>.ifErr(f: (E) -> Unit): UResult<O, E> =
	apply {
		if (this is UResult.Err) {
			f(error)
		}
	}

inline fun <O : Any?, E : Any> UResult<O, E>.ifOk(f: (O) -> Unit): UResult<O, E> =
	apply {
		if (this is UResult.Ok) {
			f(value)
		}
	}

inline fun <O : Any?, E : Any, R> UResult<O, E>.fold(
	onSuccess: (O) -> R,
	onFailure: (E) -> R,
): R = when (this) {
	is UResult.Ok -> onSuccess(value)
	is UResult.Err -> onFailure(error)
}

inline fun <O : Any?, E : Any> UResult<O, E>.recover(f: (E) -> O): UResult<O, E> = when (this) {
	is UResult.Ok -> this
	is UResult.Err -> Ok(f(error))
}

inline fun <O : Any?, E : Any> UResult<O, E>.recoverWith(f: (E) -> UResult<O, E>): UResult<O, E> = when (this) {
	is UResult.Ok -> this
	is UResult.Err -> f(error)
}

inline fun <O : Any?, F : Any?, E : Any> UResult<O, E>.mapOk(f: (O) -> F): UResult<F, E> = when (this) {
	is UResult.Ok -> Ok(f(value))
	is UResult.Err -> Err(this.error)
}

inline fun <O : Any?, E : Any, NE : Any> UResult<O, E>.mapError(
	crossinline f: (E) -> NE,
): UResult<O, NE> = when (this) {
	is UResult.Ok -> Ok(value)
	is UResult.Err -> Err(f(error))
}

fun <O : Any, E : Any> UResult<O, E>.rotate(): UResult<E, O> = when (this) {
	is UResult.Ok -> Err(this.value)
	is UResult.Err -> Ok(this.error)
}

// <editor-fold defaultstate="collapsed" desc="Collection">
inline fun <O : Any?, R : Any?, E : Any> UResult<Collection<O>, E>.mapEach(
	transform: (O) -> R,
): UResult<Collection<R>, E> = when (this) {
	is UResult.Ok -> Ok(value.map(transform))
	is UResult.Err -> Err(error)
}

inline fun <O : Any?, E : Any, R : Any?, NE : Any> UResult<Collection<O>, E>.mapToResult(
	transform: (O) -> UResult<R, NE>,
): UResult<Collection<UResult<R, NE>>, E> = when (this) {
	is UResult.Ok -> Ok(value.map(transform))
	is UResult.Err -> Err(error)
}

inline fun <O : Any?, E : Any> UResult<Collection<O>, E>.filter(predicate: (O) -> Boolean): UResult<Collection<O>, E> =
	when (this) {
		is UResult.Ok -> Ok(value.filter(predicate))
		is UResult.Err -> Err(error)
	}

inline fun <O : Any?, E : Any> UResult<Collection<O>, E>.forEach(action: (O) -> Unit): UResult<Unit, E> = when (this) {
	is UResult.Ok -> Ok(value.forEach(action)) // Always returns Unit
	is UResult.Err -> Err(error)
}
// </editor-fold>

// <editor-fold defaultstate="collapsed" desc="Sequence">
@JvmName("mapEachSequence")
fun <O : Any?, R : Any?, E : Any> UResult<Sequence<O>, E>.mapEach(
	transform: (O) -> R,
): UResult<Sequence<R>, E> = when (this) {
	is UResult.Ok -> Ok(value.map(transform))
	is UResult.Err -> Err(error)
}

@JvmName("mapToResultSequence")
fun <O : Any?, E : Any, R : Any?, NE : Any> UResult<Sequence<O>, E>.mapToResult(
	transform: (O) -> UResult<R, NE>,
): UResult<Sequence<UResult<R, NE>>, E> = when (this) {
	is UResult.Ok -> Ok(value.map(transform))
	is UResult.Err -> Err(error)
}

@JvmName("filterSequence")
fun <O : Any?, E : Any> UResult<Sequence<O>, E>.filter(predicate: (O) -> Boolean): UResult<Sequence<O>, E> =
	when (this) {
		is UResult.Ok -> Ok(value.filter(predicate))
		is UResult.Err -> Err(error)
	}

@JvmName("forEachSequence")
fun <O : Any?, E : Any> UResult<Sequence<O>, E>.forEach(action: (O) -> Unit): UResult<Unit, E> = when (this) {
	is UResult.Ok -> Ok(value.forEach(action)) // Always returns Unit
	is UResult.Err -> Err(error)
}

fun <O : Any?, E : Any> UResult<Sequence<O>, E>.toList(): UResult<List<O>, E> = when (this) {
	is UResult.Ok -> Ok(value.toList())
	is UResult.Err -> Err(error)
}
// </editor-fold>

inline fun <O : Any?, E : Any, F : Any?, RE : Any> UResult<O, E>.flatMap(
	crossinline f: (O) -> UResult<F, RE>,
): UResult<UResult<F, RE>, E> = when (this) {
	is UResult.Ok -> Ok(f(value))
	is UResult.Err -> Err(error)
}

fun <O : Any?, E : Any> UResult<UResult<O, E>, E>.flatten(): UResult<O, E> =
	when (this) {
		is UResult.Ok -> value
		is UResult.Err -> Err(error)
	}

@Suppress("FunctionName")
fun <O : Any?, E : Any> Ok(data: O): UResult<O, E> = UResult.Ok(data)

@Suppress("FunctionName")
fun <O : Any?, E : Any> Err(error: E): UResult<O, E> = UResult.Err(error)

@Suppress("FunctionName")
fun <O : Any?, E : Any> Err(error: E, throwable: Throwable?): UResult<O, Try<E>> = UResult.Err(Try(error, throwable))

@Suppress("FunctionName")
fun <O : Any?, E : Any> ErrTry(error: E, throwable: Throwable? = null): UResult<O, Try<E>> = UResult.Err(Try(error, throwable))