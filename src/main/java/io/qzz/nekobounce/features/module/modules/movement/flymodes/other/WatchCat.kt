/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.flymodes.other

import io.qzz.nekobounce.features.module.modules.movement.Fly.startY
import io.qzz.nekobounce.features.module.modules.movement.flymodes.FlyMode
import io.qzz.nekobounce.utils.extensions.stopXZ
import io.qzz.nekobounce.utils.kotlin.RandomUtils.nextDouble
import io.qzz.nekobounce.utils.movement.MovementUtils.strafe

object WatchCat : FlyMode("WatchCat") {
    override fun onUpdate() {
        strafe(0.15f)
        mc.thePlayer.isSprinting = true

        if (mc.thePlayer.posY < startY + 2) {
            mc.thePlayer.motionY = nextDouble(endInclusive = 0.5)
            return
        }

        if (startY > mc.thePlayer.posY) mc.thePlayer.stopXZ()
    }
}
