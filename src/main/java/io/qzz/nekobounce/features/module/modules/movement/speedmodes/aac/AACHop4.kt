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

object AACHop4 : SpeedMode("AACHop4") {
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f

        if (!thePlayer.isMoving || thePlayer.isInLiquid || thePlayer.isOnLadder || thePlayer.isRiding)
            return

        if (thePlayer.onGround)
            thePlayer.tryJump()
        else {
            if (thePlayer.fallDistance <= 0.1)
                mc.timer.timerSpeed = 1.5f
            else if (thePlayer.fallDistance < 1.3)
                mc.timer.timerSpeed = 0.7f
            else
                mc.timer.timerSpeed = 1f
        }
    }

}