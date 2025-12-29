/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes.aac

import io.qzz.nekobounce.features.module.modules.movement.speedmodes.SpeedMode
import io.qzz.nekobounce.utils.extensions.isInLiquid
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.extensions.tryJump

object AACHop5 : SpeedMode("AACHop5") {
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isMoving || thePlayer.isInLiquid || thePlayer.isOnLadder || thePlayer.isRiding)
            return

        if (thePlayer.onGround) {
            thePlayer.tryJump()
            mc.timer.timerSpeed = 0.9385f
            thePlayer.speedInAir = 0.0201f
        }

        if (thePlayer.fallDistance < 2.5) {
            if (thePlayer.fallDistance > 0.7) {
                if (thePlayer.ticksExisted % 3 == 0) {
                    mc.timer.timerSpeed = 1.925f
                } else if (mc.thePlayer.fallDistance < 1.25) {
                    mc.timer.timerSpeed = 1.7975f
                }
            }
            thePlayer.speedInAir = 0.02f
        }
    }

}