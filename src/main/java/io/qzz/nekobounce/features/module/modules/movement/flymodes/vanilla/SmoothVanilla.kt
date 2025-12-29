/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.flymodes.vanilla

import io.qzz.nekobounce.features.module.modules.movement.Fly.handleVanillaKickBypass
import io.qzz.nekobounce.features.module.modules.movement.flymodes.FlyMode

object SmoothVanilla : FlyMode("SmoothVanilla") {
    override fun onUpdate() {
        mc.thePlayer.capabilities.isFlying = true
        handleVanillaKickBypass()
    }
}
