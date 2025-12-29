/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.misc

import io.qzz.nekobounce.NekoBounce.hud
import io.qzz.nekobounce.event.PacketEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.script.remapper.Remapper
import io.qzz.nekobounce.ui.client.hud.element.elements.Notification
import io.qzz.nekobounce.utils.client.chat
import io.qzz.nekobounce.utils.timing.MSTimer

object PacketDebugger : Module("PacketDebugger", Category.MISC, gameDetecting = false) {

    private val notify by choices("Notify", arrayOf("Chat", "Notification"), "Chat")
    val packetType by choices("PacketType", arrayOf("Both", "Server", "Client", "Custom"), "Both")
    private val delay by int("Delay", 100, 0..1000)
    private val notificationStayTime by float(
        "NotificationStayTime", 3f, 0.5f..60f, suffix = "Seconds"
    ) { notify == "Notification" }

    private val timer = MSTimer()
    val selectedPackets = mutableSetOf<String>()

    val onPacket = handler<PacketEvent> { event ->
        if (mc.thePlayer == null || mc.theWorld == null) {
            return@handler
        }

        val packet = event.packet

        val isServerPacket = packet.javaClass.name.startsWith("net.minecraft.network.play.server")
        val isClientPacket = packet.javaClass.name.startsWith("net.minecraft.network.play.client")

        if (timer.hasTimePassed(delay)) {
            when (packetType.lowercase()) {
                "both" -> logPacket(event)
                "server" -> if (isServerPacket) logPacket(event)
                "client" -> if (isClientPacket) logPacket(event)
                "custom" -> if (selectedPackets.contains(packet.javaClass.simpleName)) logPacket(event)
            }
            timer.reset()
        }
    }

    private fun logPacket(event: PacketEvent) {
        val packet = event.packet

        val packetEvent = if (event.isCancelled) "§7(§cCancelled§7)" else ""

        val packetInfo = buildString {
            append("\n")
            append("§aPacket: §b${packet.javaClass.simpleName} $packetEvent\n")
            append("§aEventType: §b${event.eventType}\n")

            var clazz: Class<*>? = packet.javaClass

            while (clazz != null) {
                clazz.declaredFields.forEach { field ->
                    field.isAccessible = true

                    append("§a${Remapper.remapField(clazz!!, field.name)}: §b${field.get(packet)}\n")
                }

                clazz = clazz.superclass
            }
        }

        if (notify == "Chat") {
            chat(packetInfo)
        } else {
            // Not a good idea...
            hud.addNotification(Notification.informative(this, packetInfo, (notificationStayTime * 1000).toLong()))
        }
    }
}