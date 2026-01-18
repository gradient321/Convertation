package ru_lonya.api.util.Extend.Language

import java.security.MessageDigest
import java.util.*

fun String.replaceUnderscores(): String {
  return this.replace(Regex("(?<!\\\\)__"), " ")
    .replace("\\_", "_")
}

val String.uuid: UUID?
  get() = try {
    UUID.fromString(this)
  } catch (e: Exception) {
    null
  }

private val digest = MessageDigest.getInstance("SHA-256")

val String.hashUUID: UUID
  get() {
    val hash = digest.digest(this.toByteArray())
    val bytes = hash.copyOfRange(0, 16) // Берем первые 16 байт
    return UUID.nameUUIDFromBytes(bytes)
  }

//val String.number: Number?
//	get() {
//		this.toInt()
//	}