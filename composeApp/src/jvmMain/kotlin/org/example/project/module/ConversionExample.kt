package org.example.project.module

class ConversionExample(val add: Double, val multiplier: Double) : ConversionSettings<ConversionExample> {
  fun convertTo(value: Double) = value * multiplier + add
  
  
  override var value: ConversionExample = this
}