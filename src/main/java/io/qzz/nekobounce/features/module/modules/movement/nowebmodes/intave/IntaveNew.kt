/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.nowebmodes.intave

import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.NoWebMode
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.extensions.tryJump
import io.qzz.nekobounce.utils.movement.MovementUtils.strafe

object IntaveNew : NoWebMode("IntaveNew") {
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isInWeb) {
            return
        }

        if (thePlayer.isMoving && thePlayer.moveStrafing == 0.0f) {
            if (thePlayer.onGround) {
                if (mc.thePlayer.ticksExisted % 3 == 0) {
                    strafe(0.734f)
                } else {
                    mc.thePlayer.tryJump()
                    strafe(0.346f)
                }
            }
        }
    }
}
