/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes.spectre

import io.qzz.nekobounce.features.module.modules.movement.speedmodes.SpeedMode
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.movement.MovementUtils.strafe

object SpectreLowHop : SpeedMode("SpectreLowHop") {
    override fun onMotion() {
        if (!mc.thePlayer.isMoving || mc.thePlayer.movementInput.jump) return
        if (mc.thePlayer.onGround) {
            strafe(1.1f)
            mc.thePlayer.motionY = 0.15
            return
        }
        strafe()
    }

}