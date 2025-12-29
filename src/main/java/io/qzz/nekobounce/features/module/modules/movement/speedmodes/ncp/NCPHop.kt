/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes.ncp

import io.qzz.nekobounce.features.module.modules.movement.speedmodes.SpeedMode
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.extensions.tryJump
import io.qzz.nekobounce.utils.movement.MovementUtils.strafe

object NCPHop : SpeedMode("NCPHop") {
    override fun onEnable() {
        mc.timer.timerSpeed = 1.0865f
        super.onEnable()
    }

    override fun onDisable() {
        mc.thePlayer.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
        super.onDisable()
    }

    override fun onUpdate() {
        if (mc.thePlayer.isMoving) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.tryJump()
                mc.thePlayer.speedInAir = 0.0223f
            }
            strafe()
        } else {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
        }
    }

}