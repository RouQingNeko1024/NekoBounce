/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes.other

import io.qzz.nekobounce.features.module.modules.movement.Speed.bmcDamageBoost
import io.qzz.nekobounce.features.module.modules.movement.Speed.bmcLowHop
import io.qzz.nekobounce.features.module.modules.movement.Speed.damageLowHop
import io.qzz.nekobounce.features.module.modules.movement.Speed.fullStrafe
import io.qzz.nekobounce.features.module.modules.movement.Speed.safeY
import io.qzz.nekobounce.features.module.modules.movement.speedmodes.SpeedMode
import io.qzz.nekobounce.utils.extensions.airTicks
import io.qzz.nekobounce.utils.extensions.isInLiquid
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.extensions.tryJump
import io.qzz.nekobounce.utils.movement.MovementUtils.speed
import io.qzz.nekobounce.utils.movement.MovementUtils.strafe
import net.minecraft.potion.Potion

object BlocksMCHop : SpeedMode("BlocksMCHop") {

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isInLiquid || player.isInWeb || player.isOnLadder) return

        if (player.isMoving) {
            if (player.onGround) {
                player.tryJump()
            } else {
                if (fullStrafe) {
                    strafe(speed - 0.004F)
                } else {
                    if (player.airTicks >= 6) {
                        strafe()
                    }
                }

                if ((player.getActivePotionEffect(Potion.moveSpeed)?.amplifier ?: 0) > 0 && player.airTicks == 3) {
                    player.motionX *= 1.12
                    player.motionZ *= 1.12
                }

                if (bmcLowHop && player.airTicks == 4) {
                    if (safeY) {
                        if (player.posY % 1.0 == 0.16610926093821377) {
                            player.motionY = -0.09800000190734863
                        }
                    } else {
                        player.motionY = -0.09800000190734863
                    }
                }

                if (player.hurtTime == 9 && bmcDamageBoost) {
                    strafe(speed.coerceAtLeast(0.7F))
                }

                if (damageLowHop && player.hurtTime >= 1) {
                    if (player.motionY > 0) {
                        player.motionY -= 0.15
                    }
                }
            }
        }
    }

}