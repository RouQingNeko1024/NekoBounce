/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.world

import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.WorldEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.extensions.isMoving

object Timer : Module("Timer", Category.WORLD, gameDetecting = false) {

    private val mode by choices("Mode", arrayOf("OnMove", "NoMove", "Always"), "OnMove")
    private val speed by float("Speed", 2F, 0.1F..10F)

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1F
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        if (mode == "Always" || mode == "OnMove" && player.isMoving || mode == "NoMove" && !player.isMoving) {
            mc.timer.timerSpeed = speed
            return@handler
        }

        mc.timer.timerSpeed = 1F
    }

    val onWorld = handler<WorldEvent> {
        if (it.worldClient == null)
            state = false
    }
}
