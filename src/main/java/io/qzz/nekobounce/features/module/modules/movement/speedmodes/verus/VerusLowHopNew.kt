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

object VerusLowHopNew : SpeedMode("VerusLowHopNew") {

    private var speed = 0.0f

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isInLiquid || player.isInWeb || player.isOnLadder) return

        if (player.isMoving) {
            if (player.onGround) {
                player.tryJump()

                // Checks the presence of Speed potion effect 1 & 2+
                if (player.isPotionActive(Potion.moveSpeed)) {
                    val amplifier = player.getActivePotionEffect(Potion.moveSpeed).amplifier

                    speed = when {
                        amplifier == 1 -> 0.55f
                        amplifier >= 2 -> 0.7f
                        else -> 0.33f
                    }
                }

                // Checks the presence of Slowness potion effect.
                speed = if (player.isPotionActive(Potion.moveSlowdown)
                    && player.getActivePotionEffect(Potion.moveSlowdown).amplifier == 1
                ) {
                    0.3f
                } else {
                    0.33f
                }
            } else {
                if (player.airTicks <= 1) {
                    mc.thePlayer.motionY = -0.09800000190734863
                }

                speed *= 0.99f
            }

            strafe(speed, false)
        }
    }
}
