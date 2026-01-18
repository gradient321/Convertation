package ru_lonya.util.stringbuilder

fun stringBuilder(function: StringBuilderDsl.() -> Unit) = StringBuilderDsl().apply(function).build()

open class StringBuilderDsl(private val builder: StringBuilder = StringBuilder()) {
	
	fun append(value: String) {
		builder.append(value)
	}
	
	fun append(value: Any) {
		builder.append(value)
	}
	
	fun append(value: CharSequence) {
		builder.append(value)
	}
	
	fun append(value: CharSequence, start: Int, end: Int) {
		builder.append(value, start, end)
	}
	
	fun append(value: CharArray) {
		builder.append(value)
	}
	
	fun append(value: CharArray, offset: Int, len: Int) {
		builder.appendRange(value, offset, offset + len)
	}
	
	operator fun set(index: Int, value: Char) {
		builder[index] = value
	}
	
	open operator fun String.unaryPlus() {
		append(this)
	}
	
	open operator fun Any.unaryPlus() {
		append(this)
	}
	
	/**
	 * При получении этого значения (getter) будет добавлен символ переноса строки.
	 */
	val new: Unit get() { +newline }
	
	/**
	 * При получении этого значения (getter) будут добавлены два символа переноса строки.
	 */
	val newX2: Unit get() { +newline; +newline }
	
	/**
	 * Символ переноса строки.
	 */
	inline val newline: Char get() = '\n'
	
	/**
	 * Символ пробела.
	 */
	inline val space: Char get() = ' '
	
	/**
	 * Пустая строка.
	 */
	inline val empty: String get() = ""
	
	/**
	 * Итоговая строка.
	 */
	override fun toString(): String = builder.toString()
	/**
	 * Итоговая строка.
	 */
	fun build() = builder.toString()
	
	override fun equals(other: Any?): Boolean = when (other) {
		is StringBuilderDsl -> this.builder == other.builder
		else -> builder == other
	}
	override fun hashCode(): Int = builder.hashCode()
}