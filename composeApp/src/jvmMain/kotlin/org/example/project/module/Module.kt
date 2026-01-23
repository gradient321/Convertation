package org.example.project.module

import com.sun.jdi.Value
import kotlin.reflect.KClass

abstract class Module(val name: String) {
  val conversions = mutableListOf<IConversion<*, *>>()
  
//  abstract fun registration()
  
  inline fun <reified T : Any, reified H : Any> reg(noinline func: (T) -> H)
                      = Conversion(func, T::class, H::class).also { conversions.add(it) }
  inline fun <reified T : Any, reified H : Any, reified Settings : ConversionSettings<Value>, reified Value : Any>
      regws(noinline func: Value.(T) -> H) = ConversionWithSettings(func, T::class,
                                          H::class, Settings::class).also { conversions.add(it) }
  
//  var list = mutableListOf<>()
  
  
  
}

object Mo : Module("Mo") {
  val a1 = reg {a: Number -> a.toInt() + 1}
  
  val a2 = regws(ConversionExample::convertTo)
  
  val toInt = reg {a: Number -> a.toInt()}
  val toDouble = reg {a: Number -> a.toDouble()}
}

fun main() {
  Mo.a1
}

abstract class IConversion<T : Any, H : Any>(val input: KClass<T>, val output: KClass<H>) {
  val inputJava by lazy { input.java }
  
  fun isTransfer(clazz: KClass<*>) = input == clazz || isTransferPrivate(clazz.java)
  fun isTransfer(clazz: Class<*>) = inputJava == clazz || isTransferPrivate(clazz)
  private fun isTransferPrivate(clazz: Class<*>) = inputJava.isAssignableFrom(clazz)
}
interface IConversionObject<T : Any, H : Any> {
  fun convert(t: T): H
  operator fun invoke(t: T): H = convert(t)
  fun conversion(): IConversion<T, H>
}

open class Conversion<T : Any, H : Any>(val func: (T) -> H, input: KClass<T>, output: KClass<H>)
                                                          : IConversion<T, H>(input, output), IConversionObject<T, H> {
  
  override fun convert(t: T): H = func(t)
  override fun conversion(): IConversion<T, H> = this
}

class ConversionWithSettings<T : Any, H : Any, Settings : ConversionSettings<Value>, Value : Any>
  (val func: Value.(T) -> H, input: KClass<T>, output: KClass<H>, val settingsType: KClass<Settings>)
                                        : IConversion<T, H>(input, output) {
  
  fun with(settings: Settings) = ConversionObjectWithSettings(settings)
  
  inner class ConversionObjectWithSettings(val settings: ConversionSettings<Value>) : IConversionObject<T, H> {
    override fun convert(t: T): H = settings.value.func(t)
    override fun conversion(): IConversion<T, H> = this@ConversionWithSettings
  }
}


interface ConversionSettings<V> {
  var value: V
}
interface ClassWithInformation {
  fun getDescription(): String
}



open class A1
class B : A1()


fun main1() {
//  val a = A1::class
//  val b = B::class
//  println(a.s(b))
  println(A1::class.java.isAssignableFrom(B::class.java))
  
}

