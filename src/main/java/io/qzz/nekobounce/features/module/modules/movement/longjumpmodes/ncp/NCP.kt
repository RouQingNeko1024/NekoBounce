/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.ncp

import io.qzz.nekobounce.event.MoveEvent
import io.qzz.nekobounce.features.module.modules.movement.LongJump.canBoost
import io.qzz.nekobounce.features.module.modules.movement.LongJump.jumped
import io.qzz.nekobounce.features.module.modules.movement.LongJump.ncpBoost
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.movement.MovementUtils.speed

object NCP : LongJumpMode("NCP") {
    override fun onUpdate() {
        speed *= if (canBoost) ncpBoost else 1f
        canBoost = false
    }

    override fun onMove(event: MoveEvent) {
        if (!mc.thePlayer.isMoving && jumped) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            event.zeroXZ()
        }
    }
}