package org.example.project.api

import java.sql.DriverManager.println

interface UtilRef {
	
	fun info(message: String)
	fun warn(message: String)
	fun error(message: String)
	
	companion object : UtilRef {
		val default = object : UtilRef {
			override fun info(message: String) {
				println("[INFO] $message")
			}
			
			override fun warn(message: String) {
				println("[WARN] $message")
			}
			
			override fun error(message: String) {
				println("[ERROR] $message")
			}
		}
		var inst: UtilRef = default
		
		fun init(instance: UtilRef) {
			UtilRef.inst = instance
		}
		override fun info(message: String) {
			inst.info(message)
		}
		override fun warn(message: String) {
			inst.warn(message)
		}
		override fun error(message: String) {
			inst.error(message)
		}
		
	}
	
	abstract class DelegateUtilRef : UtilRef {
		abstract val delegate: UtilRef
		override fun info(message: String) {
			delegate.info(message)
		}
		override fun warn(message: String) {
			delegate.warn(message)
		}
		override fun error(message: String) {
			delegate.error(message)
		}
	}
	
}