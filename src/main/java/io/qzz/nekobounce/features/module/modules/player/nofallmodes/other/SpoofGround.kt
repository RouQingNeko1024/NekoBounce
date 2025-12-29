/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.player.nofallmodes.other

import io.qzz.nekobounce.event.PacketEvent
import io.qzz.nekobounce.features.module.modules.player.nofallmodes.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

object SpoofGround : NoFallMode("SpoofGround") {
    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer)
            event.packet.onGround = true
    }
}