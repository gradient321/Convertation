package ru_lonya.util.file

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileLogger(
	logDirectory: File,
	private val logDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"),
	private val archiveDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"),
	private val isPrintingToConsole: Boolean = true,
) {
	private val logFolder = logDirectory.apply { mkdirs() }
	private val archivesFolder = File(logFolder, "archives").apply { mkdirs() }
	private val currentLogFile = File(logFolder, "latest.log")
	
	fun log(message: String) {
		val timestamp = logDateFormat.format(Date())
		val logEntry = "[$timestamp] $message\n"
		if (isPrintingToConsole) {
			println(logEntry.trim())
		}
		try {
			currentLogFile.appendText(logEntry)
		} catch (e: IOException) {
			System.err.println("Log write failed: ${e.message}")
		}
	}
	
	fun archive() {
		if (!currentLogFile.exists()) return
		try {
			val timestamp = archiveDateFormat.format(Date())
			val archiveFile = File(archivesFolder, "log_$timestamp.zip")
			ZipOutputStream(archiveFile.outputStream()).use { zos ->
				zos.putNextEntry(ZipEntry(currentLogFile.name))
				Files.copy(currentLogFile.toPath(), zos)
				zos.closeEntry()
			}
			currentLogFile.delete()
		} catch (e: Exception) {
			System.err.println("Log archiving failed: ${e.message}")
		}
	}
}