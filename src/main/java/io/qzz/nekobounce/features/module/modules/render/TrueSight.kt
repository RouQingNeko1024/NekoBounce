/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.render

import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module

object TrueSight : Module("TrueSight", Category.RENDER) {
    val barriers by boolean("Barriers", true)
    val entities by boolean("Entities", true)

    val onUpdate = handler<UpdateEvent> {
        if (barriers && mc.gameSettings.particleSetting == 2) {
            mc.gameSettings.particleSetting = 1
        }
    }
}