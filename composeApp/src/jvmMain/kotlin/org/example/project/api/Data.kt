@file:Suppress("UNCHECKED_CAST")

package org.example.project.api

import ru_lonya.util.codec.AnyCodec
import ru_lonya.util.extend.lang.filter
import java.util.Collections.emptyList
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

// &a =================================================
// &a Top-level utilities
// &a =================================================

val Map<String, Any>?.data: Data
  get() = Data(this)

/**
 * Нет конвертаций
 */
val Map<String, Any>.dataUnsafe: Data
  get() = Data(this, Data.Unsafe)

val Data.data: Data
  get() = Data(this)

val Map<String, Any>.asData: Data?
  get() = this as? Data

@get:JvmName("dataChecked")
val Map<*, *>?.data: Data?
  get() {
    if (this == null) return null
    if (this is Data) return this
    if (this.isEmpty()) return Data()
    if (this.keys.iterator().next() !is String) return null
    return Data(this as Map<String, Any>)
  }


fun dataOf() = Data()
fun dataOf(pair: Pair<String, Any>) = mapOf(pair).data
fun dataOf(map: Map<String, Any>?) = map.data
fun dataOf(vararg pairs: Pair<String, Any>) = pairs.toMap().data

fun dataOf(init: Data.() -> Unit) = Data().apply(init)

// &b =================================================
// &b Main Data Class
// &b =================================================
open class Data : MutableMap<String, Any> {
  
  constructor(map: Map<String, Any>? = mapOf()) {
//    this.map = deepConvertMap(isData(map)?.toMutableMap() ?: mutableMapOf())
    this.map = (if (map is Data) map.map else map)?.toMutableMap()?.let { deepConvertMap(it) } ?: mutableMapOf()
  }
  
  constructor(map: Map<String, Any>, additional: Map<String, Any>) {
    this.map = deepConvertMap(
      (if (map is Data) map.map else map).toMutableMap()
        .apply { putAll(additional.toMutableMap()) }
    )
  }
  
  data object Unsafe {} // Что бы для JVM сигнатура конструктора была другой
  constructor(map: Map<String, Any>, unsafe: Unsafe) {
    this.map = map.toMutableMap()
  }
  
//  constructor(data: Data) {
//    this.map = data.map.toMutableMap()
//  }
  
  // &d ================= Internal State =================
  var map: MutableMap<String, Any>
  
  // &d =============== Map Conversions ================
  // <editor-fold defaultstate="collapsed" desc="Map Conversion Utilities">
  private fun deepConvertMap(source: MutableMap<String, Any>): MutableMap<String, Any> {
    return source.mapValues { (_, value) ->
      when (value) {
        is DataList<*> -> value.clone()
        is Data -> value.clone()
        is Map<*, *> -> Data(value as Map<String, Any>)
        is Collection<*> -> value.map { deepConvertValue(it) }
        else -> value
      }
    }.toMutableMap()
  }
  
  private fun deepConvertValue(value: Any?): Any? = when (value) {
    is DataList<*> -> value.clone()
    is Data -> value.clone()
    is Map<*, *> -> Data(value as Map<String, Any>)
    is Collection<*> -> value.map { deepConvertValue(it) }
    else -> value
  }
  
  private fun wrapValue(value: Any): Any = when (value) {
    is Data -> value
    is DataList<*> -> value
    is Map<*, *> -> Data(value as Map<String, Any>)
    is Collection<*> -> value.map { it?.let(::wrapValue) ?: it }
    else -> value
  }
  // </editor-fold>
  
  // &e ================= Delegates =================
  // <editor-fold defaultstate="collapsed" desc="Property Delegates">
  inline fun <reified T : Any?> delegate() = object : ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = getAs<T>(property.name)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) = set(property.name, value)
  }
  
  inline fun <reified T : Any?> delegate(default: T) = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getAs<T>(property.name) ?: default
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(property.name, value)
  }
  
  inline fun <reified T : Any?> delegate(key: String) = object : ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = getAs<T>(key)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) = set(key, value)
  }
  
  inline fun <reified T : Any?> delegate(key: String, default: T) = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = getAs<T>(key, default)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(key, value)
  }
  
  inline operator fun <reified T> getValue(nothing: Nothing?, property: KProperty<*>, default: T) =
    getAs<T>(property.name) ?: default
  
  inline operator fun <reified T> setValue(nothing: Nothing?, property: KProperty<*>, value: T) {
    this[property.name] = value
  }
  // </editor-fold>
  
  // &6 ================= Core Accessors =================
  // <editor-fold defaultstate="collapsed" desc="Basic Access Methods">
  override operator fun get(key: String): Any? = map[key]
  
  inline fun <reified T> getAs(key: String): T? = this[key]?.let { AnyParse.parse<T>(it) }
  inline fun <reified T> getAs(key: String, default: T): T = getAs<T>(key) ?: default
  
  operator fun <T> get(key: String, default: T): T = (this[key] as? T) ?: default
  fun <T> get(key: String, klass: Class<T>): T? = this[key] as? T
  inline fun <reified T : Any> get(key: String, klass: KClass<T>): T? = this[key] as? T
  
  fun getData(key: String): Data = getAs<Data>(key) ?: Data()
  fun getDataNull(key: String): Data? = getAs<Data>(key)
  fun getDataOrNull(key: String): Data? = get(key) as? Data?
  
  inline fun <reified T, R> getWithCodec(id: String, codec: AnyCodec<R, T>, default: R): R {
    return getAs<T>(id)?.let { codec.decode(it) } ?: default
  }
  // </editor-fold>
  
  // &9 ================= Nested Access =================
  // <editor-fold defaultstate="collapsed" desc="Nested Operations">
  
  operator fun get(parts: List<String>): Any? {
    var current: Any? = this
    for (part in parts) {
      current = when (current) {
        is Data -> current[part]
        is List<*> -> {
          val index = part.toIntOrNull()
          if (index != null) current.getOrNull(index) else null
        }
        else -> null
      }
      if (current == null) break
    }
    return current
  }
  operator fun get(vararg parts: String): Any? = this[parts.asList()]
  
  fun getNestedData(vararg parts: String): Data? = getNestedData(parts.asList())
  fun getNestedData(parts: List<String>): Data? {
    var current: Data = this
    for (part in parts) {
      current = current[part] as? Data ?: return null
    }
    return current
  }
  
  inline fun <reified T> getNestedAs(vararg parts: String): T? = getNestedAs(parts.asList())
  inline fun <reified T> getNestedAs(parts: List<String>): T? = AnyParse.parse<T>(this[parts])
  
  fun getOrCreateData(key: String): Data = when (val existing = map[key]) {
    is Data -> existing
    is Map<*, *> -> Data(existing as Map<String, Any>).also { map[key] = it }
    else -> Data().also { map[key] = it }
  }
  // </editor-fold>
  
  // &c ================= Modifiers =================
  // <editor-fold defaultstate="collapsed" desc="Modification Methods">
  operator fun set(key: String, value: Any?) {
    if (value == null) remove(key) else map[key] = wrapValue(value)
  }
  
  operator fun set(parts: List<String>, value: Any?) {
    var current = this
    for (i in 0 until parts.lastIndex) {
      current = current.getOrCreateData(parts[i])
    }
    current[parts.last()] = value
  }
  operator fun set(vararg parts: String, value: Any?) { this[parts.asList()] = value }
  
  fun removeNested(path: String): Any? {
    val parts = path.split('.')
    if (parts.size == 1) return remove(parts[0])
    
    return getNestedData(parts.dropLast(1))?.remove(parts.last())
  }
  
  fun <V> setOrRemove(key: String, value: V, defaultValue: V) {
    if (value != defaultValue) {
      this[key] = value
    } else {
      this.remove(key)
    }
  }
  
  fun <V> setOrRemove(key: String, value: V, shouldRemove: (V) -> Boolean) {
    if (shouldRemove(value)) {
      this.remove(key)
    } else {
      this[key] = value
    }
  }
  // </editor-fold>
  
  // &5 ================= Map Implementation =================
  // <editor-fold defaultstate="collapsed" desc="MutableMap Overrides">
  override val entries: MutableSet<MutableMap.MutableEntry<String, Any>> get() = map.entries
  override val keys: MutableSet<String> get() = map.keys
  override val size: Int get() = map.size
  override val values: MutableCollection<Any> get() = map.values
  
  override fun clear() = map.clear()
  override fun isEmpty(): Boolean = map.isEmpty()
  override fun putAll(from: Map<out String, Any>) = from.forEach { (k, v) -> set(k, v) }
  override fun containsValue(value: Any): Boolean = map.containsValue(value)
  override fun containsKey(key: String): Boolean = map.containsKey(key)
  override fun remove(key: String): Any? = map.remove(key)
  override fun put(key: String, value: Any): Any? = map.put(key, wrapValue(value))
  
  operator fun plusAssign(map: Map<String, Any>) = putAll(map)
  operator fun plusAssign(pair: Pair<String, Any>) = set(pair.first, pair.second)
  // </editor-fold>
  
  // &7 ================= Utilities =================
  // <editor-fold defaultstate="collapsed" desc="Utility Methods">
  
  /**
   * Сливает текущие данные с [other], сохраняя значения для существующих ключей.
   * Если ключ существует в обоих экземплярах Data и оба значения являются Data,
   * они рекурсивно сливаются по тем же правилам (исходные значения сохраняются).
   * Ключи, присутствующие в [other], но отсутствующие в текущих данных, добавляются.
   *
   * @param other Данные для слияния.
   * @return Текущий экземпляр Data со слиянием.
   */
  fun mergeKeepingOriginal(other: Data): Data {
    other.forEach { (key, otherValue) ->
      val currentValue = this[key]
      when {
        currentValue == null -> {
          // Добавляем ключ из other
          this[key] = otherValue
        }
        currentValue is Data && otherValue is Data -> {
          // Рекурсивно сливаем вложенные Data
          currentValue.mergeKeepingOriginal(otherValue)
        }
        // Иначе ничего не делаем (сохраняем текущее значение)
      }
    }
    return this
  }
  
  /**
   * Сливает текущие данные с [other], где значения из [other] имеют приоритет.
   * Если ключ существует в обоих экземплярах Data и оба значения являются Data,
   * они рекурсивно сливаются с приоритетом значений из [other].
   * Ключи, присутствующие в [other], добавляются или перезаписываются в текущих данных,
   * а ключи, присутствующие только в текущих данных, остаются неизменными.
   *
   * @param other Данные, чьи значения будут иметь приоритет.
   * @return Текущий экземпляр Data со слиянием.
   */
  fun mergeWithOverriding(other: Data): Data {
    other.forEach { (key, otherValue) ->
      val currentValue = this[key]
      when {
        currentValue is Data && otherValue is Data -> {
          // Рекурсивно сливаем вложенные Data с приоритетом other
          currentValue.mergeWithOverriding(otherValue)
        }
        else -> {
          // Заменяем текущее значение значением из other
          this[key] = otherValue
        }
      }
    }
    return this
  }
  
  operator fun plus(other: Data): Data {
    return Data(this).apply { mergeKeepingOriginal(other) }
  }
  
  operator fun plusAssign(other: Data) {
    mergeKeepingOriginal(other)
  }
  
  fun containsNested(vararg parts: String): Boolean = this[parts.asList()] != null
  fun getFromValue(value: Any): List<String> = map.filterValues { it == value }.map { it.key }
  override fun toString(): String = map.toString()
  
  override fun equals(other: Any?): Boolean {
    return if (this === other) true
    else when (other) {
      is Data -> map == other.map
      is Map<*, *> -> map == other
      else -> false
    }
  }
  
  override fun hashCode(): Int {
    return map.hashCode()
  }
  
  fun clone() = Data(this.map)
  // </editor-fold>
  
  companion object {
    private fun isData(value: Map<String, Any>?): Map<String, Any>? {
      if (value is Data) return value.map
      return value
    }
  }
}

//class DataList<T : Any> : MutableList<T> {
//
//  private val data: Data
//	constructor(list: List<T> = emptyList()) {
//		this.data = Data(
//			list.withIndex().associate { it.index.toString() to it.value },
//			mapOf("size" to list.size, "nbt:type" to "data_list")
//		)
//	}
//
//	private constructor(data: Data) {
//    this.data = data
//  }
//  fun clone(): DataList<T> = DataList(data.clone())
//
//  fun toList(): List<T> {
//    return (data["size"] as? Int ?: 0).let { size ->
//      (0 until size).mapTo(ArrayList(size)) { data[it.toString()] as T }
//    }
//  }
//
//}

fun <T : Any> dataListOf(): DataList<T> = DataList()
fun <T : Any> dataListOf(element: T): DataList<T> = DataList(listOf(element))
fun <T : Any> dataListOf(vararg elements: T): DataList<T> = DataList(elements.toList())
fun <T : Any> List<T>.dataList(): DataList<T> = DataList(this)
fun <T : Any> dataListOf(list: List<T>): DataList<T> = DataList(list)

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
class DataList<T : Any> private constructor(
  private val listRef: MutableList<T>,
  @Suppress("unused") dummy: Unit = Unit
) : MutableList<T> by listRef {
  
  constructor(list: List<T> = emptyList()) : this(list.toMutableList())
  
  constructor(data: Data) : this(
    (data["size"] as? Int ?: 0).let { size ->
	    if (size == 0) emptyList()
	    else data.map.filter { it.key != "size" && it.key != "nbt:type" }.toList().sortedBy { it.first.toIntOrNull() ?: Int.MAX_VALUE }.map { it.second as T }
    }
  )
  
  fun toData(): Data {
    return Data(
      listRef.withIndex().associate { it.index.toString() to it.value },
      mapOf("size" to listRef.size, "nbt:type" to "data_list")
    )
  }
  
  fun clone(): DataList<T> = DataList(listRef.toList())
  
  companion object {
    fun fromData(data: Data): DataList<Any> {
      return DataList(data)
    }
    
    fun fromMap(map: Map<String, Any>): DataList<Any> {
      return DataList(map.data)
    }
  }
}

//operator fun Data?.plus(data: Data): Data = if (this==null) data else this + data
//operator fun Data.plus(data: Data?): Data = if (data==null) this else this + data