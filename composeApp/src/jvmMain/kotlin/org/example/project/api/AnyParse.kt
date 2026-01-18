package org.example.project.api

import org.jetbrains.annotations.ApiStatus.Experimental
import ru_lonya.util.code.notNull
import java.util.UUID
import kotlin.reflect.KClass

object AnyParse {
  val strToBoolMap = mapOf(
    "false" to false,
    "0" to false,
    "f" to false,
    
    "true" to true,
    "1" to true,
    "t" to true,
  )
  
  inline fun <reified T> parse(any: Any?): T? = parseKlass(any, T::class)
  
  
  @Suppress("UNCHECKED_CAST")
  fun <T : Any> parseKlass(any: Any?, klass: KClass<*>): T? {
    if (any is Number) {
      when (klass) {
        Number::class -> any
        Int::class -> any.toInt()
        Double::class -> any.toDouble()
        Float::class -> any.toFloat()
        Long::class -> any.toLong()
        Short::class -> any.toShort()
        Byte::class -> any.toByte()
        else -> null
      }.notNull { return it as T }
    }
    
    if (any is DataList<*>) {
      when (klass) {
        DataList::class -> any
        Collection::class -> any
        else -> null
      }.notNull { return it as T }
    }
    
    if (any is Collection<*>) {
      when (klass) {
  //        InputObject::class -> InputObject(any)
        List::class -> any.toList()
        MutableList::class -> any.toMutableList()
        Collection::class -> any
        else -> null
      }.notNull { return it as T }
    }
    if (any is Map<*, *>) {
      when (klass) {
        Map::class -> any
        Data::class -> if (any is Data) any else Data(any as Map<String, Any>?)
  //          if (any.entries)
        
        
        else -> null
      }.notNull { return it as T }
    }
    
    if (klass == String::class) return any.toString() as? T
    if (klass == Boolean::class) return getBoolean(any) as? T
    if (klass == UUID::class) {
      when (any) {
        is String -> UUID.fromString(any)
        else -> null
      }.notNull { return it as T }
    }
    
    
    
    return any as? T
  }
  
  fun getBoolean(any: Any?): Boolean? {
    any ?: return null
    
    val a = when (any) {
      is Number -> any.toInt() % 2 == 1
      is Boolean -> any
      is String -> strToBoolMap[any]
      else -> null // TODO null или false // Ну вроде null нужен для дефолтного...
    }
    return a
  }
  
  
  fun stringForAny(string: String): Any? {
    if (string == "") return null
    if (string[0].digitToIntOrNull() == null) {
    
    }
    string
    return null
  }
  
  
  @Experimental
  inline fun <reified T> parseToMap(any: Any?): Data? {
    val data = Data()

//    if (any is SpellDataMap)
    
    return data
  }
}