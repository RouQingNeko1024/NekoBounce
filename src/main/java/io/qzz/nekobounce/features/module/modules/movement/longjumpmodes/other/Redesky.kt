/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.other

import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.LongJumpMode

object Redesky : LongJumpMode("Redesky") {
    override fun onUpdate() {
        mc.thePlayer.jumpMovementFactor = 0.15f
        mc.thePlayer.motionY += 0.05f
    }
}