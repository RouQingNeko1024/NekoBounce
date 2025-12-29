/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.render

import io.qzz.nekobounce.event.PacketEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import net.minecraft.network.play.server.S3FPacketCustomPayload

object AntiBlind : Module("AntiBlind", Category.RENDER, gameDetecting = false) {
    val confusionEffect by boolean("Confusion", true)
    val pumpkinEffect by boolean("Pumpkin", true)
    val fireEffect by float("FireAlpha", 0.3f, 0f..1f)
    val bossHealth by boolean("BossHealth", true)
    private val bookPage by boolean("BookPage", true)
    val achievements by boolean("Achievements", true)

    val onPacket = handler<PacketEvent> { event ->
        if (!bookPage) return@handler

        val packet = event.packet
        if (packet is S3FPacketCustomPayload && packet.channelName == "MC|BOpen") event.cancelEvent()
    }
}