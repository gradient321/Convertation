package org.example.project.api

import java.time.*
import java.time.format.DateTimeFormatter

object DateTimeUtils {
	
	// Часовой пояс по умолчанию — Москва
	private val DEFAULT_ZONE: ZoneId = ZoneId.of("Europe/Moscow")
	
	/**
	 * Получить текущее время как строку.
	 *
	 * @param zoneId - часовой пояс (по умолчанию Москва)
	 * @param pattern - формат даты/времени (по умолчанию "yyyy-MM-dd HH:mm:ss z")
	 */
	fun getCurrentTime(
		zoneId: ZoneId = DEFAULT_ZONE,
		pattern: String = "yyyy-MM-dd HH:mm:ss z"
	): String {
		val formatter = DateTimeFormatter.ofPattern(pattern)
		return ZonedDateTime.now(zoneId).format(formatter)
	}
	
	/**
	 * Получить текущую дату.
	 */
	fun getCurrentDate(zoneId: ZoneId = DEFAULT_ZONE): LocalDate =
		LocalDate.now(zoneId)
	
	/**
	 * Получить текущее время суток.
	 */
	fun getCurrentTimeOfDay(zoneId: ZoneId = DEFAULT_ZONE): LocalTime =
		LocalTime.now(zoneId)
	
	/**
	 * Получить timestamp в секундах.
	 */
	fun getCurrentTimestamp(zoneId: ZoneId = DEFAULT_ZONE): Long =
		Instant.now().atZone(zoneId).toEpochSecond()
}