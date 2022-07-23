package com.nixac.z.ui

import java.util.*

/**
 * Utils for [Random].
 * @author LuoLiangchen
 * @since 2016/12/14
 * Courtesy & Credit to the original author : LuaLiangChen : https://github.com/Luolc/EmojiRain
 *
 */
object NZRandom {
    private val random = Random()
    fun setSeed(seed: Long) {
        random.setSeed(seed)
    }

    fun floatStandard(): Float {
        return random.nextFloat()
    }

    fun floatAround(mean: Float, delta: Float): Float {
        return floatInRange(mean - delta, mean + delta)
    }

    fun floatInRange(left: Float, right: Float): Float {
        return left + (right - left) * random.nextFloat()
    }

    fun positiveGaussian(): Double {
        return Math.abs(random.nextGaussian())
    }
}