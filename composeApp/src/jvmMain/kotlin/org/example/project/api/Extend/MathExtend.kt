package ru_lonya.api.util.Extend.Language


//fun Double.toIntIfZero(places: Int = 2): Number {
//  // Обрезаем до 2 знаков после запятой
//  val roundedNumber = UMath.round(this, places)
//
//  // Проверяем, является ли число целым
//  return if (roundedNumber * 100.0 % 100 <= 0) {
//    roundedNumber.toInt()
//  } else {
//    roundedNumber
//  }
//}

//fun Number.toIntIfZero(places: Int = 2): Number = this.toDouble().toIntIfZero(places)

fun clamp(value: Int, min: Int, max: Int): Int {
  return when {
    value < min -> min
    value > max -> max
    else -> value
  }
}

fun clamp(value: Float, min: Float, max: Float): Float {
  return when {
    value < min -> min
    value > max -> max
    else -> value
  }
}

fun clamp(value: Double, min: Double, max: Double): Double {
  return when {
    value < min -> min
    value > max -> max
    else -> value
  }
}

fun clamp(value: Long, min: Long, max: Long): Long {
  return when {
    value < min -> min
    value > max -> max
    else -> value
  }
}

fun Int.pow(n: Int): Int {
  if (n < 0) throw IllegalArgumentException("Negative exponent not supported")
  if (n == 0) return 1
  
  var result = 1
  var base = this
  var exp = n
  
  while (exp > 0) {
    if (exp and 1 == 1) {
      result *= base
    }
    base *= base
    exp = exp shr 1
  }
  
  return result
}