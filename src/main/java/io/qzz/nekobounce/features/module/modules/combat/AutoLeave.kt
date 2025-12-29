/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.combat

import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.client.PacketUtils.sendPacket
import io.qzz.nekobounce.utils.kotlin.RandomUtils.nextInt
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object AutoLeave : Module("AutoLeave", Category.COMBAT, subjective = true) {
    private val health by float("Health", 8f, 0f..20f)
    private val mode by choices("Mode", arrayOf("Quit", "InvalidPacket", "SelfHurt", "IllegalChat"), "Quit")

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (thePlayer.health <= health && !thePlayer.capabilities.isCreativeMode && !mc.isIntegratedServerRunning) {
            when (mode.lowercase()) {
                "quit" -> mc.theWorld.sendQuittingDisconnectingPacket()
                "invalidpacket" -> sendPacket(
                    C04PacketPlayerPosition(
                        Double.NaN,
                        Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY,
                        !mc.thePlayer.onGround
                    )
                )

                "selfhurt" -> sendPacket(C02PacketUseEntity(mc.thePlayer, ATTACK))
                "illegalchat" -> thePlayer.sendChatMessage(nextInt().toString() + "§§§" + nextInt())
            }

            state = false
        }
    }
}