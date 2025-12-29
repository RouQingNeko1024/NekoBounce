/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.other

import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import io.qzz.nekobounce.utils.movement.MovementUtils

object Buzz : LongJumpMode("Buzz") {
    override fun onUpdate() {
        mc.thePlayer.motionY += 0.4679942989799998
        MovementUtils.speed *= 0.7578698f
    }
}
