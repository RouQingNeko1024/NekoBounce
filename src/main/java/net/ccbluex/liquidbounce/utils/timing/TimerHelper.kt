/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timing

class TimerHelper {
    var lastMS = System.currentTimeMillis()

    fun hasTimeElapsed(requiredMS: Long, reset: Boolean): Boolean {
        val current = System.currentTimeMillis()
        if (current - lastMS >= requiredMS) {
            if (reset) {
                reset()
            }
            return true
        }
        return false
    }

    fun hasTimeElapsed(requiredMS: Long): Boolean {
        return hasTimeElapsed(requiredMS, false)
    }
    
    fun hasTimePassed(requiredMS: Long): Boolean {
        return System.currentTimeMillis() - lastMS >= requiredMS
    }

    fun reset() {
        lastMS = System.currentTimeMillis()
    }
}