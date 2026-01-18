package ru_lonya.util.code

val Any?.str
  get() = this.toString()

inline fun <T : Any> T?.notNull(func: (any: T) -> Unit) {
  if (this != null) {
    func(this)
  }
}




