package ru_lonya.util.extend.lang

val Number.sub: Number
	get() = when (this) {
		is Int    -> -this
		is Long   -> -this
		is Byte   -> -this
		is Short  -> -this
		is Float  -> -this
		is Double -> -this
		else -> error("Неизвестный тип числа: '$this'")
	}