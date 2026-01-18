package org.example.project.api

import java.io.Closeable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface CloseableAs<T: Any> : Closeable {
	val obj: T
}

/**
 * Без try-finally
 */
@OptIn(ExperimentalContracts::class)
inline fun <T: Any, R> CloseableAs<T>.useUnsafe(block: (T) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(obj)
}

@OptIn(ExperimentalContracts::class)
inline fun <T: Any, R> CloseableAs<T>.use(block: (T) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	var exception: Throwable? = null
	try {
		return block(obj)
	} catch (e: Throwable) {
		exception = e
		throw e
	} finally {
		this.closeFinally(exception)
	}
}

@PublishedApi
internal fun Closeable?.closeFinally(cause: Throwable?): Unit = when {
	this == null -> {}
	cause == null -> close()
	else ->
		try {
			close()
		} catch (closeException: Throwable) {
			cause.addSuppressed(closeException)
		}
}