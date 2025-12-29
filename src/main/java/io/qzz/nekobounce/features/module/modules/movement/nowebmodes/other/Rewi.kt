/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.nowebmodes.other

import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.NoWebMode
import io.qzz.nekobounce.utils.extensions.tryJump

object Rewi : NoWebMode("Rewi") {
    override fun onUpdate() {
        if (!mc.thePlayer.isInWeb) {
            return
        }
        mc.thePlayer.jumpMovementFactor = 0.42f

        if (mc.thePlayer.onGround)
            mc.thePlayer.tryJump()
    }
}
