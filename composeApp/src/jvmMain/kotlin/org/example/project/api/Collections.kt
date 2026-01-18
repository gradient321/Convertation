package org.example.project.api

import kotlin.collections.LinkedHashMap

/**
 * Данный класс представляет элемент с приоритетом.
 * @param priority Приоритет элемента (целое число).
 * @param item Сам элемент любого типа T.
 */
data class PriorityItem<T>(val priority: Int, val item: T)

typealias PriorityMutableCollection<T> = MutableCollection<PriorityItem<T>>
typealias PriorityCollection<T> = Collection<PriorityItem<T>>

typealias PriorityMutableIterable<T> = MutableIterable<PriorityItem<T>>
typealias PriorityIterable<T> = Iterable<PriorityItem<T>>

typealias PriorityMutableList<T> = MutableList<PriorityItem<T>>
typealias PriorityList<T> = List<PriorityItem<T>>

typealias PriorityMutableSet<T> = MutableSet<PriorityItem<T>>
typealias PrioritySet<T> = Set<PriorityItem<T>>

typealias PriorityMutableMap<K, V> = MutableMap<K, PriorityItem<V>>
typealias PriorityMap<K, V> = Map<K, PriorityItem<V>>

/**
 * Преобразует список элементов с приоритетом в Map, где ключ - это приоритет, а значение - сам элемент.
 * @return Map<Int, T>, где ключ - приоритет, значение - элемент.
 */
fun <T> PriorityCollection<T>.toPriorityMap(): Map<Int, T> = associate { it.priority to it.item }

/**
 * Преобразует список элементов с приоритетом в MutableMap, где ключ - это приоритет, а значение - сам элемент.
 * Используется LinkedHashMap, чтобы сохранить порядок добавления.
 * @return MutableMap<Int, T>, где ключ - приоритет, значение - элемент.
 */
fun <T> PriorityCollection<T>.toPriorityMutableMap(): MutableMap<Int, T>
	= associateTo(LinkedHashMap(size)) { it.priority to it.item }

fun <T> PriorityMutableCollection<T>.add(element: T, priority: Int) = add(PriorityItem(priority, element))

/**
 * Сортирует список элементов с приоритетом по приоритету.
 * В случае равенства приоритетов, сохраняется исходный порядок.
 * @return Отсортированный список элементов с приоритетом.
 */
fun <T> PriorityCollection<T>.sortedByPriority(): PriorityList<T> {
	return withIndex().sortedWith(compareBy({ it.value.priority }, { it.index })).map { it.value }
}
