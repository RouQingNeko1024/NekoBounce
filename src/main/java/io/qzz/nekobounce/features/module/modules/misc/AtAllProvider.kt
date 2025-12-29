/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.misc

import kotlinx.coroutines.delay
import io.qzz.nekobounce.event.PacketEvent
import io.qzz.nekobounce.event.async.loopSequence
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import net.minecraft.network.play.client.C01PacketChatMessage
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object AtAllProvider :
    Module("AtAllProvider", Category.MISC, subjective = true, gameDetecting = false) {

    private val delay by intRange("Delay", 500..1000, 0..20000)

    private val retry by boolean("Retry", false)
    private val sendQueue = ArrayDeque<String>()
    private val retryQueue = ArrayDeque<String>()

    private val lock = ReentrantLock()

    override fun onDisable() {
        lock.withLock {
            sendQueue.clear()
            retryQueue.clear()
        }

        super.onDisable()
    }

    val onUpdate = loopSequence {
        lock.withLock {
            if (sendQueue.isEmpty()) {
                if (!retry || retryQueue.isEmpty())
                    return@loopSequence
                else
                    sendQueue += retryQueue
            }

            mc.thePlayer.sendChatMessage(sendQueue.removeFirst())
        }

        delay(delay.random().toLong())
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet !is C01PacketChatMessage)
            return@handler

        val message = event.packet.message

        if ("@a" !in message)
            return@handler

        lock.withLock {
            val selfName = mc.thePlayer.name
            for (playerInfo in mc.netHandler.playerInfoMap) {
                val playerName = playerInfo?.gameProfile?.name

                if (playerName == selfName)
                    continue

                // Replace out illegal characters
                val filteredName = playerName?.replace("[^a-zA-Z0-9_]", "")?.let {
                    message.replace("@a", it)
                } ?: continue

                sendQueue += filteredName
            }

            if (retry) {
                retryQueue.clear()
                retryQueue += sendQueue
            }
        }

        event.cancelEvent()
    }
}