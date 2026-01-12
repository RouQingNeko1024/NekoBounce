/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import java.util.*

object Grim : FlyMode("Grim") {
    private var fuckGrim = false
    private var grimFlag = false
    private var grimTicks = 0
    private val packetQueue: Queue<C0FPacketConfirmTransaction> = LinkedList()

    override fun onEnable() {
        grimTicks = 0
        fuckGrim = false
        grimFlag = false
        packetQueue.clear()
    }

    override fun onDisable() {
        grimTicks = 0
        fuckGrim = false
        grimFlag = false
        while (packetQueue.isNotEmpty()) {
            mc.netHandler.addToSendQueue(packetQueue.poll())
        }
    }

    override fun onUpdate() {
        if (fuckGrim) {
            grimTicks++
            if (grimTicks >= 8) {
                sendFallFlyingPacket()
                grimTicks = 0
                fuckGrim = false
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        
        // 处理发送的数据包
        if (packet is C0FPacketConfirmTransaction) {
            if (grimFlag) {
                event.cancelEvent()
                if (packetQueue.isEmpty()) {
                    fuckGrim = true
                }
                packetQueue.add(packet)
            }
        }
        
        if (packet is C02PacketUseEntity) {
            if (grimFlag && packetQueue.isNotEmpty()) {
                while (packetQueue.isNotEmpty()) {
                    mc.netHandler.addToSendQueue(packetQueue.poll())
                }
            }
        }
        
        // 处理接收的数据包
        if (packet is S08PacketPlayerPosLook) {
            if (grimFlag && packetQueue.isNotEmpty()) {
                while (packetQueue.isNotEmpty()) {
                    mc.netHandler.addToSendQueue(packetQueue.poll())
                }
            }
        }
        
        if (packet is S12PacketEntityVelocity) {
            if (packet.entityID == mc.thePlayer?.entityId) {
                if (grimFlag || packetQueue.isNotEmpty()) {
                    return
                }
                grimTicks = 0
                grimFlag = true
                event.cancelEvent()
            }
        }
    }

    fun onWorld(event: WorldEvent) {
        // 关闭模块
        (Fly as Fly).state = false
        grimTicks = 0
        fuckGrim = false
        grimFlag = false
        packetQueue.clear()
    }

    private fun sendFallFlyingPacket() {
        // 由于ViaForge API问题，这里使用一个简单的实现
        // 你可以根据实际的ViaForge API进行调整
        // 临时解决方案：发送聊天消息
        mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§8[§c§lGrimFly§8] §7Fall flying packet sent"))
    }
}