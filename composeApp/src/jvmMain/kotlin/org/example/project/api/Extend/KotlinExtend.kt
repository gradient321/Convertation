package ru_lonya.api.util.Extend.Language

object KotlinExtend {
  
  fun <T, R> T.throwExceptionToNull(block: (T) -> R): R? {
    return try {
      block(this)
    } catch (_: Exception) {
      null
    }
  }
  
  fun <T, R> T.throwErrorToNull(block: (T) -> R): R? {
    return try {
      block(this)
    } catch (_: Error) {
      null
    }
  }
  
  fun <T, R> T.throwToNull(block: (T) -> R): R? {
    return try {
      block(this)
    } catch (_: Throwable) {
      null
    }
  }
  
}