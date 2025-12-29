/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.nowebmodes.other

import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.NoWebMode

object None : NoWebMode("None") {
    override fun onUpdate() {
        mc.thePlayer.isInWeb = false
    }
}
