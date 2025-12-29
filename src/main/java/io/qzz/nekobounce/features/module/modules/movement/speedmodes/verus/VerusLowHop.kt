/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes.verus

import io.qzz.nekobounce.features.module.modules.movement.speedmodes.SpeedMode
import io.qzz.nekobounce.utils.extensions.airTicks
import io.qzz.nekobounce.utils.extensions.isInLiquid
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.extensions.tryJump
import io.qzz.nekobounce.utils.movement.MovementUtils.strafe
import net.minecraft.potion.Potion

object VerusLowHop : SpeedMode("VerusLowHop") {

    private var speed = 0.0f

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isInLiquid || player.isInWeb || player.isOnLadder) return

        if (player.isMoving) {
            if (player.onGround) {
                speed = if (player.isPotionActive(Potion.moveSpeed)
                    && player.getActivePotionEffect(Potion.moveSpeed).amplifier >= 1
                )
                    0.5f else 0.36f

                player.tryJump()
            } else {
                if (player.airTicks <= 1) {
                    player.motionY = -0.09800000190734863
                }

                speed *= 0.98f
            }

            strafe(speed)
        }
    }
}
