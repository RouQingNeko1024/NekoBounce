/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes.other

import io.qzz.nekobounce.features.module.modules.movement.Speed
import io.qzz.nekobounce.features.module.modules.movement.Speed.customAirStrafe
import io.qzz.nekobounce.features.module.modules.movement.Speed.customAirTimer
import io.qzz.nekobounce.features.module.modules.movement.Speed.customAirTimerTick
import io.qzz.nekobounce.features.module.modules.movement.Speed.customGroundStrafe
import io.qzz.nekobounce.features.module.modules.movement.Speed.customGroundTimer
import io.qzz.nekobounce.features.module.modules.movement.Speed.customY
import io.qzz.nekobounce.features.module.modules.movement.Speed.notOnConsuming
import io.qzz.nekobounce.features.module.modules.movement.Speed.notOnFalling
import io.qzz.nekobounce.features.module.modules.movement.Speed.notOnVoid
import io.qzz.nekobounce.features.module.modules.movement.speedmodes.SpeedMode
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.extensions.stopXZ
import io.qzz.nekobounce.utils.extensions.stopY
import io.qzz.nekobounce.utils.extensions.tryJump
import io.qzz.nekobounce.utils.movement.FallingPlayer
import io.qzz.nekobounce.utils.movement.MovementUtils.strafe
import net.minecraft.item.ItemBucketMilk
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPotion

object CustomSpeed : SpeedMode("Custom") {

    override fun onMotion() {
        val player = mc.thePlayer ?: return
        val heldItem = player.heldItem

        val fallingPlayer = FallingPlayer()
        if (notOnVoid && fallingPlayer.findCollision(500) == null
            || notOnFalling && player.fallDistance > 2.5f
            || notOnConsuming && player.isUsingItem
            && (heldItem.item is ItemFood
                    || heldItem.item is ItemPotion
                    || heldItem.item is ItemBucketMilk)
        ) {

            if (player.onGround) player.tryJump()
            mc.timer.timerSpeed = 1f
            return
        }

        if (player.isMoving) {
            if (player.onGround) {
                if (customGroundStrafe > 0) {
                    strafe(customGroundStrafe)
                }

                mc.timer.timerSpeed = customGroundTimer
                player.motionY = customY.toDouble()
            } else {
                if (customAirStrafe > 0) {
                    strafe(customAirStrafe)
                }

                if (player.ticksExisted % customAirTimerTick == 0) {
                    mc.timer.timerSpeed = customAirTimer
                } else {
                    mc.timer.timerSpeed = 1f
                }
            }
        }
    }

    override fun onEnable() {
        val player = mc.thePlayer ?: return

        if (Speed.resetXZ) player.stopXZ()
        if (Speed.resetY) player.stopY()

        super.onEnable()
    }

}