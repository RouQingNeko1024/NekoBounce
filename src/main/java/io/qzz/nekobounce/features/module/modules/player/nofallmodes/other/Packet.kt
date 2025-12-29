/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.player.nofallmodes.other

import io.qzz.nekobounce.features.module.modules.player.nofallmodes.NoFallMode
import io.qzz.nekobounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object Packet : NoFallMode("Packet") {
    override fun onUpdate() {
        if (mc.thePlayer.fallDistance > 2f)
            sendPacket(C03PacketPlayer(true))
    }
}