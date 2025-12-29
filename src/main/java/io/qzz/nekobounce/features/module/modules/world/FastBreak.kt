/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.world

import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module

object FastBreak : Module("FastBreak", Category.WORLD) {

    private val breakDamage by float("BreakDamage", 0.8F, 0.1F..1F)

    val onUpdate = handler<UpdateEvent> {
        mc.playerController.blockHitDelay = 0

        if (mc.playerController.curBlockDamageMP > breakDamage)
            mc.playerController.curBlockDamageMP = 1F

        if (Fucker.currentDamage > breakDamage)
            Fucker.currentDamage = 1F

        if (Nuker.currentDamage > breakDamage)
            Nuker.currentDamage = 1F
    }
}
