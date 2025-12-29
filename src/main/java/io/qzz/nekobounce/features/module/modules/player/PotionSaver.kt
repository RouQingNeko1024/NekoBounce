/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.player

import io.qzz.nekobounce.event.PacketEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import net.minecraft.network.play.client.C03PacketPlayer

object PotionSaver : Module("PotionSaver", Category.PLAYER) {

    val onPacket = handler<PacketEvent> {
        val packet = it.packet

        if (packet is C03PacketPlayer && mc.thePlayer?.isUsingItem == false && !packet.rotating &&
            (!packet.isMoving || (packet.x == mc.thePlayer.lastTickPosX && packet.y == mc.thePlayer.lastTickPosY && packet.z == mc.thePlayer.lastTickPosZ))
        )
            it.cancelEvent()
    }

}