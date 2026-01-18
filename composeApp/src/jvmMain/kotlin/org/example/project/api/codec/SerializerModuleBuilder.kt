package ru_lonya.util.codec

import kotlinx.serialization.modules.SerializersModule

class SerializerModuleBuilder {
	private val modules: MutableList<SerializersModule> = mutableListOf()
	private var cache: SerializersModule? = null
	private fun markDirty() {
		cache = null
	}
	fun add(module: SerializersModule) {
		modules.add(module)
		markDirty()
	}
	fun add(builder: SerializerModuleBuilder) {
		modules.add(builder.build())
		markDirty()
	}
	fun edit(actions: SerializerModuleBuilder.() -> Unit) {
		actions()
	}
	fun build(): SerializersModule {
		if (cache == null) {
			cache = SerializersModule {
				modules.forEach {
					include(it)
				}
			}
		}
		return cache!!
	}
}