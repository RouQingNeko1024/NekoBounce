/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.aac

import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import io.qzz.nekobounce.utils.movement.MovementUtils

object AACv2 : LongJumpMode("AACv2") {
    override fun onUpdate() {
        mc.thePlayer.jumpMovementFactor = 0.09f
        mc.thePlayer.motionY += 0.01320999999999999
        mc.thePlayer.jumpMovementFactor = 0.08f
        MovementUtils.strafe()
    }
}