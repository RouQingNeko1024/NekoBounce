/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.player

import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.features.module.modules.exploit.Ghost
import net.minecraft.client.gui.GuiGameOver

object AutoRespawn : Module("AutoRespawn", Category.PLAYER, gameDetecting = false) {

    private val instant by boolean("Instant", true)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer

        if (thePlayer == null || Ghost.handleEvents())
            return@handler

        if (if (instant) mc.thePlayer.health == 0F || mc.thePlayer.isDead else mc.currentScreen is GuiGameOver && (mc.currentScreen as GuiGameOver).enableButtonsTimer >= 20) {
            thePlayer.respawnPlayer()
            mc.displayGuiScreen(null)
        }
    }
}