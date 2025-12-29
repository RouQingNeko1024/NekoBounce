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
import io.qzz.nekobounce.utils.client.PacketUtils.sendPacket
import io.qzz.nekobounce.utils.movement.MovementUtils.serverOnGround
import net.minecraft.network.play.client.C03PacketPlayer

object Zoot : Module("Zoot", Category.PLAYER) {

    private val badEffects by boolean("BadEffects", true)
    private val fire by boolean("Fire", true)
    private val noAir by boolean("NoAir", false)

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (noAir && !serverOnGround)
            return@handler

        if (badEffects) {

            val effect = thePlayer.activePotionEffects
                .filter { it.potionID in NEGATIVE_EFFECT_IDS }
                .maxByOrNull { it.duration }

            if (effect != null) {
                repeat(effect.duration / 20) {
                    sendPacket(C03PacketPlayer(serverOnGround))
                }
            }
        }


        if (fire && mc.playerController.gameIsSurvivalOrAdventure() && thePlayer.isBurning) {
            repeat(9) {
                sendPacket(C03PacketPlayer(serverOnGround))
            }
        }
    }
}