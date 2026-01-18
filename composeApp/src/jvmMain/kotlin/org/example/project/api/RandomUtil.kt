package org.example.project.api

import kotlin.random.Random

object RandomUtil {
	
	fun bool(): Boolean {
		return Random.nextBoolean()
	}
	
	fun bool(probability: Number): Boolean {
		return Random.nextDouble() < probability.toDouble()
	}
	
	fun int(min: Int, max: Int): Int {
		return Random.nextInt(min, max)
	}
	
	operator fun rem(probability: Number): Boolean {
		return Random.nextDouble() < probability.toDouble()
	}
	
	operator fun Number.rem(t: RandomUtil): Boolean {
		return Random.nextDouble() < this.toDouble()
	}
	
}