package ru_lonya.util.stringbuilder

fun kspStringBuilder(func: KspStringBuilderDsl.() -> Unit) = KspStringBuilderDsl().apply(func).build()

class KspStringBuilderDsl(builder: StringBuilder = StringBuilder()) : TabStringBuilderDsl(builder) {
	
	// <editor-fold defaultstate="collapsed" desc="Устаревшее">
	/**
	 * Добавляет переданную строку, после неё добавляя скобку(по умолчанию `{`) и отступ для последующих строк.
	 */
	fun open(string: String, objOpen: String = " {", isNewLine: Boolean = true) {
		+"$string$objOpen"
		+tab
		if (isNewLine) { +newline }
	}
	
	inline fun block(
		string: String,
		objOpen: String = " {",
		objClose: String = "}",
		isNewLineOpen: Boolean = false,
		isNewLineClose: Boolean = true,
		block: KspStringBuilderDsl.() -> Unit
	) {
		open(string, objOpen, isNewLine = isNewLineOpen)
		block()
		close(objClose, isNewLineClose)
	}
	
	/**
	 * Добавляет переданную строку, после неё добавляя скобку и отступ для последующих строк.
	 */
	inline fun openFun(string: String, block: KspStringBuilderDsl.() -> Unit = {}) {
		open(string, isNewLine = false)
		block()
		close()
	}
	
	/**
	 * Убирает один отступ и добавляет закрывающую скобку(по умолчанию `}`).
	 */
	fun close(objClose: String = "}", isNewLine: Boolean = true) {
		-tab
		+objClose
		if (isNewLine) { +newline }
	}
	// </editor-fold>
	
	// <editor-fold defaultstate="collapsed" desc="Новое">
	fun openClassDeclaration(
		declaration: String,
		name: String,
		modifiers: Set<ClassModifier> = setOf(),
		superTypes: List<String> = emptyList(),
		block: KspStringBuilderDsl.() -> Unit
	) {
		val mods = modifiers.joinToString(" ") { it.name.lowercase() }
		val superTypesStr = if (superTypes.isNotEmpty()) " : ${superTypes.joinToString(", ")}" else ""
		open("${if (modifiers.isEmpty()) "" else "$mods "}$declaration $name$superTypesStr")
		block()
		close()
	}
	
	fun openObject(
		name: String,
		modifiers: Set<ClassModifier> = setOf(),
		superTypes: List<String> = emptyList(),
		block: KspStringBuilderDsl.() -> Unit
	) = openClassDeclaration("object", name, modifiers, superTypes, block)
	
	fun openClass(
		name: String,
		modifiers: Set<ClassModifier> = setOf(),
		superTypes: List<String> = emptyList(),
		block: KspStringBuilderDsl.() -> Unit
	) = openClassDeclaration("class", name, modifiers, superTypes, block)
	
	fun openInterface(
		name: String,
		modifiers: Set<ClassModifier> = setOf(),
		superTypes: List<String> = emptyList(),
		block: KspStringBuilderDsl.() -> Unit
	) = openClassDeclaration("interface", name, modifiers, superTypes, block)
	
	fun openCompanion(
		name: String = "",
		modifiers: Set<ClassModifier> = setOf(),
		superTypes: List<String> = emptyList(),
		block: KspStringBuilderDsl.() -> Unit
	) = openObject(name, modifiers + ClassModifier.COMPANION, superTypes, block)
	
	fun openFunction(
		name: String,
		modifiers: Set<FunctionModifier> = setOf(),
		parameters: List<String> = emptyList(),
		returnType: String? = null,
		block: (KspStringBuilderDsl.() -> Unit)? = null
	) {
		val mods = modifiers.joinToString(" ") { it.name.lowercase() }
		val params = parameters.joinToString(", ")
		val returnTypeStr = returnType?.let { ": $it" } ?: ""
		val declaration = "$mods fun $name($params)$returnTypeStr"
		
		if (block != null) {
			open(declaration)
			block()
			close()
		} else {
			+"$declaration;"
			+newline
		}
	}
	
	fun property(
		name: String,
		modifiers: Set<PropertyModifier> = setOf(),
		type: String,
		initializer: String? = null,
		getter: String? = null,
		setter: String? = null
	) {
		val mods = modifiers.joinToString(" ") { it.name.lowercase() }
		val initStr = initializer?.let { " = $it" } ?: ""
		val getterStr = getter?.let { "\n${tab}get() = $it" } ?: ""
		val setterStr = setter?.let { "\n${tab}set(value) { $it }" } ?: ""
		
		+"$mods val $name: $type$initStr$getterStr$setterStr"
		+newline
	}
	// </editor-fold>
}

enum class ClassModifier {
	PRIVATE,
	INTERNAL,
	PROTECTED,
	
	OPEN,
	SEALED,
	DATA,
	INNER,
	
	COMPANION,
	ENUM,
	ABSTRACT
}

enum class FunctionModifier {
	PRIVATE,
	INTERNAL,
	PROTECTED,
	
	OPEN,
	OVERRIDE,
	
	SUSPEND,
	INLINE,
	INFIX,
	OPERATOR,
	TAILREC,
	EXTERNAL,
	
	ABSTRACT
}

enum class PropertyModifier {
	PRIVATE,
	INTERNAL,
	PROTECTED,
	
	OPEN,
	OVERRIDE,
	
	CONST,
	LATEINIT,
	
	ABSTRACT
}