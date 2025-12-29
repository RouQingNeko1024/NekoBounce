/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes.verus

import io.qzz.nekobounce.features.module.modules.movement.speedmodes.SpeedMode
import io.qzz.nekobounce.utils.extensions.tryJump
import io.qzz.nekobounce.utils.movement.MovementUtils.strafe

object VerusFHop : SpeedMode("VerusFHop") {
    override fun onMotion() {
        val player = mc.thePlayer ?: return

        if (player.onGround) {
            if (player.movementInput.moveForward != 0f && player.movementInput.moveStrafe != 0f) {
                strafe(0.4825f)
            } else {
                strafe(0.535f)
            }

            player.tryJump()
        } else {
            if (player.movementInput.moveForward != 0f && player.movementInput.moveStrafe != 0f) {
                strafe(0.334f)
            } else {
                strafe(0.3345f)
            }
        }
    }
}
