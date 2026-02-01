package org.example.project.module

import org.example.project.api.UResult

//fun <T> invokeModules(input: T) {
//
//}

fun <T : Any, H : Any> createBundle(first: IConversionObject<T, *>, vararg args: IConversionObject<*, *>, final: IConversionObject<*, H>): UResult<Bundle<T, H>, String> {
  val list = mutableListOf(first) + args.toMutableList() + mutableListOf(final)
  
  var lastType = first.conversion().output
  var conversion: IConversion<*, *>?
  for (i in 1 until list.size) {
    conversion = list[i].conversion()
    
    if (!conversion.isTransfer(lastType)) return UResult.Err("Невозможно передать $lastType в ${conversion.input} в ${conversion::class}!")
    lastType = conversion.output
  }
  
  
  return UResult.Ok<Bundle<T, H>, String>(Bundle(list))
}

fun <T : Any, H : Any> createBundle(first: IConversionObject<T, *>, vararg args: IConversionObject<*, *>): UResult<Bundle<T, H>, String> {
  val list = mutableListOf(first) + args.toMutableList()
  
  var lastType = first.conversion().output
  var conversion: IConversion<*, *>?
  for (i in 1 until list.size) {
    conversion = list[i].conversion()
    
    if (!conversion.isTransfer(lastType)) return UResult.Err("Невозможно передать $lastType в ${conversion.input} в ${conversion::class}!")
    lastType = conversion.output
  }
  
  return UResult.Ok<Bundle<T, H>, String>(Bundle(list))
}

class Bundle<T : Any, H : Any>(
  val list: List<IConversionObject<*, *>>
) {
  private val privateList = list as MutableList<IConversionObject<Any, Any>>
  operator fun invoke(input: T): H {
    var value: Any = input
    
    for (i in privateList) value = i(value)
    
    return value as H
  }
}

fun main() {
  val bundle = createBundle(
    Mo.roundUp,
    Mo.toDouble,
    Mo.multiplyAndAdd.with(ConversionExample(3.0, 2.0)),
    final = Mo.roundUp
  ).unwrap()
  
  println(bundle(4.3))
  println(bundle(1.0f))
}



