package ru_lonya.util.classloader

fun printClassLoaderUrls(classLoader: ClassLoader?) {
	var cl = classLoader ?: Thread.currentThread().contextClassLoader
	
	while (cl != null) {
		if (cl is java.net.URLClassLoader) {
			println("ClassLoader: ${cl.javaClass.name}")
			cl.urLs.forEach { url ->
				println("  - $url")
			}
		} else {
			println("ClassLoader: ${cl.javaClass.name} (не URLClassLoader, URL'ы напрямую недоступны)")
		}
		cl = cl.parent
	}
}