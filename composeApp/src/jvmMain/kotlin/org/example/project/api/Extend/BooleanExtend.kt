package ru_lonya.api.util.Extend.Language


fun Boolean.Companion.fromString(value: String?): Boolean? {
  return when ((value ?: return null).lowercase().trim()) {
    // Русский
    "да" -> true
    "нет" -> false
    
    // Английский
    "yes", "true", "1" -> true
    "no", "false", "0" -> false
    
    // Специальные варианты
    "+", "✓", "v" -> true
    "-", "✗", "x" -> false
    
    // Пустые варианты
    "", " " -> null
    
    else -> null
  }
}