package org.example.project.api

import java.lang.reflect.Field

object ReflectionUtils {
	// Кэш для ускорения доступа к полям
	private val fieldCache = mutableMapOf<Class<*>, MutableMap<String, Field>>()
	
	/**
	 * Получить значение поля (в том числе приватного) из объекта
	 * @param target объект, из которого нужно получить значение
	 * @param fieldName имя поля
	 * @return значение поля
	 * @throws RuntimeException если поле не найдено или недоступно
	 */
	@Suppress("UNCHECKED_CAST")
	fun <T> getFieldValue(target: Any, fieldName: String): T {
		try {
			val field = findField(target.javaClass, fieldName)
			field.isAccessible = true
			return field.get(target) as T
		} catch (e: Exception) {
			throw RuntimeException("Failed to get field '$fieldName' from ${target.javaClass}", e)
		}
	}
	
	/**
	 * Установить значение поля (в том числе приватного) в объекте
	 * @param target объект, в который нужно установить значение
	 * @param fieldName имя поля
	 * @param value значение для установки
	 * @throws RuntimeException если поле не найдено или недоступно
	 */
	fun setFieldValue(target: Any, fieldName: String, value: Any?) {
		try {
			val field = findField(target.javaClass, fieldName)
			field.isAccessible = true
			field.set(target, value)
		} catch (e: Exception) {
			throw RuntimeException("Failed to set field '$fieldName' in ${target.javaClass}", e)
		}
	}
	
	/**
	 * Найти поле в классе (с учетом иерархии наследования)
	 * @param clazz класс для поиска
	 * @param fieldName имя поля
	 * @return найденное поле
	 * @throws NoSuchFieldException если поле не найдено
	 */
	private fun findField(clazz: Class<*>, fieldName: String): Field {
		// Проверяем кэш
		val cachedFields = fieldCache[clazz]
		if (cachedFields != null && cachedFields.containsKey(fieldName)) {
			return cachedFields[fieldName]!!
		}
		
		// Ищем поле в классе и всех его родителях
		var currentClass: Class<*>? = clazz
		while (currentClass != null) {
			try {
				val field = currentClass.getDeclaredField(fieldName)
				// Обновляем кэш
				val classFields = fieldCache.getOrPut(clazz) { mutableMapOf() }
				classFields[fieldName] = field
				return field
			} catch (e: NoSuchFieldException) {
				currentClass = currentClass.superclass
			}
		}
		
		throw NoSuchFieldException("Field '$fieldName' not found in class ${clazz.name} or its parents")
	}
}