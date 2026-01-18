package org.example.project

import org.example.project.api.Data

abstract class Element {
  var data: Data? = null
}

open class Block(
  var text: String = "",
  var elements: Map<String, Element> = mapOf()
): Element()

open class BlockUnderText : Block()

open class TextElement(
  var text: String = ""
) : Element()

abstract class NumberElement : Element() {
  open var number: Number = 0.0
}

open class IntElement(
  var int: Int = 0
) : NumberElement() {
  override var number: Number
    get() = int
    set(value) { int = value.toInt() }
}

open class DoubleElement(
  var double: Double = 0.0
): NumberElement() {
  override var number: Number
    get() = double
    set(value) { double = value.toDouble() }
}

open class IntLimitElement(
  int: Int = 0,
  var limit: List<IntRange> = listOf(Int.MIN_VALUE..Int.MAX_VALUE)
) : IntElement(int)

open class DoubleLimitElement(
  double: Double = 0.0,
  var limit: List<DoubleRange> = listOf(Double.MIN_VALUE..Double.MAX_VALUE)
) : DoubleElement(double)

open class ChoiceElement(
  var text: String = "",
  var choices: List<String> = listOf(),
  var isOnlyChoices: Boolean = false
) : Element()






class DoubleRange(
  var from: Double, var to: Double
)

operator fun Double.rangeTo(to: Double) = DoubleRange(this, to)

fun a() {
}

/*
* Элемент:
Доп данные (если нужно будет хранить что-то кастом) )

fun get


Block наследуется от Element:
Текст: String

Элементы - Map<String, Element>

Варианты элементов

Блок с текстом на блоке выше от блока:



Текст от элемента:
Текст


Число от элемента:
Число: Number

Целое число от числа:
Число: Int

Целое число с ограничениями от целого числа:
Ограничения

Дробное число от числа:
Число: Double

Дробное число с ограничениями от дробного  числа:
Ограничения

Варианты текстов от элемента:
Выбранный вариант

Варианты













* */