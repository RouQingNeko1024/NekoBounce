package net.ccbluex.liquidbounce.features.module.modules.player
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object Hub : Module("Hub", Category.PLAYER, gameDetecting = false) {

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        thePlayer.sendChatMessage("/hub")

        state = false
    }
}