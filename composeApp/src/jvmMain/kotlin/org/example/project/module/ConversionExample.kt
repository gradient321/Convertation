package org.example.project.module

class ConversionExample(val add: Double, val multiplier: Double) : ConversionSettingsUseThis<ConversionExample>() {
  fun convertTo(value: Double) = value * multiplier + add
}