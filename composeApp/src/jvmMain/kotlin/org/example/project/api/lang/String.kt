package ru_lonya.util.extend.lang

/**
 * Преобразует строку из формата snake_case в PascalCase.
 *
 * Пример:
 * "hello_world" -> "HelloWorld"
 */
fun String.snakeToPascalCase(): String {
	return split('_', '-')
		.joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
}

/**
 * Преобразует строку из формата PascalCase в snake_case.
 *
 * Пример:
 * "HelloWorld" -> "hello_world"
 */
fun String.pascalToSnakeCase(): String {
	return fold(StringBuilder()) { acc, char ->
		if (char.isUpperCase()) {
			if (acc.isNotEmpty()) acc.append('_')
			acc.append(char.lowercase())
		} else {
			acc.append(char)
		}
	}.toString()
}
