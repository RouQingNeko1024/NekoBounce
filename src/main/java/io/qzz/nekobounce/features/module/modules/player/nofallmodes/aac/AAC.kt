/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.player.nofallmodes.aac

import io.qzz.nekobounce.features.module.modules.player.nofallmodes.NoFallMode
import io.qzz.nekobounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object AAC : NoFallMode("AAC") {

    private var currentState = 0

    override fun onUpdate() {
        val thePlayer = mc.thePlayer

        if (thePlayer.fallDistance > 2f) {
            sendPacket(C03PacketPlayer(true))
            currentState = 2
        } else if (currentState == 2 && thePlayer.fallDistance < 2) {
            thePlayer.motionY = 0.1
            currentState = 3
            return
        }

        when (currentState) {
            3 -> {
                thePlayer.motionY = 0.1
                currentState = 4
            }

            4 -> {
                thePlayer.motionY = 0.1
                currentState = 5
            }

            5 -> {
                thePlayer.motionY = 0.1
                currentState = 1
            }
        }
    }
}