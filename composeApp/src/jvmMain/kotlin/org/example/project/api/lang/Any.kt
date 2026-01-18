package ru_lonya.util.extend.lang

/**
 * Удобная функция.
 *
 * Если это такой тип, то проверить условие иначе пропустить(true) *
 *
 * До:
 *
 * `(entity is Player && entity.gameMode in listOf(GameMode.SURVIVAL, GameMode.ADVENTURE)) || entity !is Player`
 *
 * После:
 *
 * `entity.checkCondition<Player> { it.gameMode in listOf(GameMode.SURVIVAL, GameMode.ADVENTURE) }`
 */
inline fun <reified T : Any> Any?.checkCondition(condition: (T) -> Boolean): Boolean {
	return (this is T && condition(this)) || this !is T
}