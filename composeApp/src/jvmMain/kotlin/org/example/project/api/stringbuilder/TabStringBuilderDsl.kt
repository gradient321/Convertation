package ru_lonya.util.stringbuilder

fun tabStringBuilder(function: TabStringBuilderDsl.() -> Unit) = TabStringBuilderDsl().apply(function).toString()

open class TabStringBuilderDsl(builder: StringBuilder = StringBuilder()) : StringBuilderDsl(builder) {
	var currentIndent: Int = 0
	var autoNewLineBoolean: Boolean = true
	
	fun tab() {
		currentIndent++
	}
	
	fun untab() {
		if (currentIndent > 0) currentIndent--
	}
	
	override fun String.unaryPlus() {
		processString(this)
	}
	
	override fun Any.unaryPlus() {
		processString(this.toString())
	}
	
	
	private fun processString(str: String) {
		if (str.length == 1 && str[0] == newline) {
			append(newline)
			return
		}
		val lines = str.split('\n')
		lines.forEachIndexed { index, line ->
			appendIndent()
			append(line)
			appendNewLineIfNeeded(index, lines.lastIndex)
		}
	}
	
	private fun appendIndent() {
		if (currentIndent > 0) {
			append("\t".repeat(currentIndent))
		}
	}
	
	private fun appendNewLineIfNeeded(currentIndex: Int, lastIndex: Int) {
		if (!autoNewLineBoolean && currentIndex == lastIndex) return
		append('\n')
	}
	
	object TabIndent { override fun toString() = "\t" }
	
	val tab get() = TabIndent
	
	operator fun TabIndent.unaryPlus() = tab()
	
	operator fun TabIndent.unaryMinus() = untab()
	
	
	// <editor-fold defaultstate="collapsed" desc="autoNewLine">
	object AutoNewLine
	
	val autoNewLine get() = AutoNewLine
	
	operator fun AutoNewLine.unaryPlus() {
		autoNewLineBoolean = true
	}
	
	operator fun AutoNewLine.unaryMinus() {
		autoNewLineBoolean = false
	}
	
	operator fun AutoNewLine.not() {
		autoNewLineBoolean = !autoNewLineBoolean
	}
	
	fun notNewLine(block: TabStringBuilderDsl.() -> Unit) {
		if (!autoNewLineBoolean) { // Если autoNewLine уже false, то просто выполняем блок
			apply(block)
		} else {
			autoNewLineBoolean = false
			apply(block)
			autoNewLineBoolean = true
		}
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="KDoc Builder">
	/**
	 * Создаёт KDoc комментарий с поддержкой Markdown и тегов
	 * @param description Основное описание (можно использовать markdown)
	 * @param tags Список тегов KDoc (@param, @return и т.д.)
	 *
	 * Пример:
	 * kdoc("Вычисляет сумму двух чисел") {
	 *   param("a", "Первое число")
	 *   param("b", "Второе число")
	 *   returns("Сумма a и b")
	 * }
	 */
	fun kdoc(description: String = "", block: (KDocBuilder.() -> Unit)? = null) {
		notNewLine {
			+"/**"
			new
			
			// Добавляем основное описание с разбивкой на строки
			if (description.isNotEmpty()) {
				description.split('\n').forEach { line ->
					+" * $line"
					new
				}
			}
			
			// Добавляем теги
			val kdocBuilder = KDocBuilder()
			block?.invoke(kdocBuilder)
			
			kdocBuilder.strings.forEach { string ->
				+" * $string"
				new
			}
			+" */"
			new
		}
	}
	
	/**
	 * Вложенный билдер для KDoc тегов
	 */
	inner class KDocBuilder {
		val strings: MutableList<String> = mutableListOf()
		
		/**
		 * Добавляет пустую строку
		 */
		fun skip() {
			strings.add("")
		}
		
		/**
		 * Добавляет текст (markdown)
		 */
		operator fun String.unaryPlus() {
			strings.add(this)
		}
		
		/**
		 * Добавляет параметр функции
		 * @param name Имя параметра
		 * @param description Описание параметра
		 */
		fun param(name: String, description: String = "") {
			strings.add("@param $name $description")
		}
		
		/**
		 * Добавляет описание возвращаемого значения
		 * @param description Описание возвращаемого значения
		 */
		fun returns(description: String) {
			strings.add("@returns $description")
		}
		
		/**
		 * Добавляет тег исключения
		 * @param exception Тип исключения
		 * @param description Описание когда выбрасывается
		 */
		fun throws(exception: String, description: String) {
			strings.add("@throws $exception $description")
		}
		
		/**
		 * Добавляет пример использования
		 * @param example Пример кода
		 */
		fun example(example: String) {
			strings.add("@example $example")
		}
		
		/**
		 * Добавляет ссылку на другую сущность
		 * @param reference Ссылка в формате [Class] или [Class.method]
		 */
		fun see(reference: String) {
			strings.add("@see $reference")
		}
		
		/**
		 * Добавляет автора
		 * @param author Имя автора
		 */
		fun author(author: String) {
			strings.add("@author $author")
		}
		
		/**
		 * Добавляет кастомный тег
		 * @param name Имя тега
		 * @param value Значение тега
		 */
		fun tag(name: String, value: String = "") {
			strings.add("@$name $value")
		}
		
	}
	// </editor-fold>
}