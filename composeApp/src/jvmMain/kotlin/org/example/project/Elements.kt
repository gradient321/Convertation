//package org.example.project
//
//// Определение классов элементов
//abstract class Element
//
//data class TextElement(
//    val text: String = ""
//) : Element()
//
//data class IntElement(
//    val int: Int = 0
//) : Element()
//
//data class DoubleElement(
//    val double: Double = 0.0
//) : Element()
//
//data class ChoiceElement(
//    val text: String = "",
//    val choices: List<String> = listOf(),
//    val isOnlyChoices: Boolean = false
//) : Element()
//
//
//
//
//class DoubleRange(
//  var from: Double, var to: Double
//)
//
//operator fun Double.rangeTo(to: Double) = DoubleRange(this, to)
//
//fun a() {
//}
//
///*
//* Элемент:
//Доп данные (если нужно будет хранить что-то кастом) )
//
//fun get
//
//
//Block наследуется от Element:
//Текст: String
//
//Элементы - Map<String, Element>
//
//Варианты элементов
//
//Блок с текстом на блоке выше от блока:
//
//
//
//Текст от элемента:
//Текст
//
//
//Число от элемента:
//Число: Number
//
//Целое число от числа:
//Число: Int
//
//Целое число с ограничениями от целого числа:
//Ограничения
//
//Дробное число от числа:
//Число: Double
//
//Дробное число с ограничениями от дробного  числа:
//Ограничения
//
//Варианты текстов от элемента:
//Выбранный вариант
//
//Варианты
//
//
//
//
//
//
//
//
//
//
//
//
//
//* */