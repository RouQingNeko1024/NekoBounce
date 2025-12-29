package io.qzz.nekobounce.features.module.modules.movement.flymodes.matrix

/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */

import io.qzz.nekobounce.features.module.modules.movement.flymodes.FlyMode
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.extensions.isInLiquid

object Matrix : FlyMode("Matrix") {
    override fun onUpdate() {
        mc.timer.timerSpeed = 1.0f

        if (!mc.thePlayer.isMoving || mc.thePlayer.isInLiquid ||  mc.thePlayer.isRiding)
            return

        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump()
            mc.timer.timerSpeed = 0.9f
        } else {
            if (mc.thePlayer.fallDistance <= 0.1) {
                mc.timer.timerSpeed = 1.5f
            } else if (mc.thePlayer.fallDistance < 1.3) {
                mc.timer.timerSpeed = 0.7f
            } else {
                mc.timer.timerSpeed = 1.0f
            }
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
    }
}
