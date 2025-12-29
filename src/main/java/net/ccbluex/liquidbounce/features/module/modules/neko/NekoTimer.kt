package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S18PacketEntityTeleport
import java.util.concurrent.ConcurrentLinkedQueue

object NekoTimer : Module("NekoTimer", Category.NEKO) {

    private val speed by float("Speed", 2.0f, 0.1f..10.0f)
    private val onMove by boolean("OnMove", true)
    private val grimTimer by boolean("Grim-Timer[Balance]", false)
    private val spartanBypass by boolean("Spartan-Grim-Timer[Balance-Bypass]", false)
    private val timerDebug by boolean("Grim-Timer[Balance-Debug]", false) { grimTimer }
    private val chatNotify by boolean("Chat Notify", true)

    private var balance = 0
    private val stopWatch = MSTimer()
    private var blinkStart = false
    private var lastStoredCount = 0
    
    // 仅存储移动数据包
    private val incomingClientPackets = ConcurrentLinkedQueue<C03PacketPlayer>()

    override fun onEnable() {
        reset()
        if (chatNotify) {
            mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§7[NekoTimer] §aModule enabled"))
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
        balance = 0
        
        // 发送存储的移动数据包
        val storedCount = incomingClientPackets.size
        while (!incomingClientPackets.isEmpty()) {
            val packet = incomingClientPackets.poll()
            if (packet != null) {
                mc.netHandler.addToSendQueue(packet)
            }
        }
        
        if (storedCount > 0 && chatNotify) {
            mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§7[NekoTimer] §cReleased §6$storedCount §cstored movement packets"))
        }
        
        if (blinkStart) {
            if (spartanBypass) {
                mc.timer.timerSpeed = 0.1f
            }
            if (spartanBypass) {
                mc.timer.timerSpeed = 1.0f
            }
        }
        
        if (chatNotify) {
            mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§7[NekoTimer] §cModule disabled"))
        }
    }

    val onUpdate = handler<UpdateEvent> { 
        val thePlayer = mc.thePlayer ?: return@handler
        
        if (onMove) {
            mc.timer.timerSpeed = if (thePlayer.isMoving) speed else 1.0f
        } else {
            mc.timer.timerSpeed = if (!spartanBypass) speed else kotlin.random.Random.nextFloat()
        }
        
        if (balance > 0 && grimTimer && timerDebug && thePlayer.ticksExisted % 20 == 0) {
            println("[GrimTimer-Balance]:$balance")
        }
        
        if (!thePlayer.isMoving) {
            blinkStart = true
        }
        
        if (grimTimer && balance < 0) {
            toggleAll()
        }
        
        // 检查存储的移动包数量变化
        val currentStoredCount = incomingClientPackets.size
        if (currentStoredCount != lastStoredCount) {
            if (currentStoredCount == 0 && lastStoredCount > 0 && chatNotify) {
                mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§7[NekoTimer] §aAll movement packets released"))
            }
            lastStoredCount = currentStoredCount
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val thePlayer = mc.thePlayer ?: return@handler
        
        // 接收数据包
        if (grimTimer) {
            when (packet) {
                is S18PacketEntityTeleport -> {
                    if (packet.entityId == thePlayer.entityId) {
                        toggleAll()
                    }
                }
                is S08PacketPlayerPosLook -> {
                    toggleAll()
                }
            }
        }
        
        if (packet is S08PacketPlayerPosLook && grimTimer && balance != 0) {
            balance = 0
            if (blinkStart) {
                if (spartanBypass) {
                    mc.timer.timerSpeed = 0.1f
                }
                if (spartanBypass) {
                    mc.timer.timerSpeed = 1.0f
                }
            }
        }
        
        // 发送数据包 - 仅处理移动数据包
        if (packet is C03PacketPlayer && grimTimer) {
            val moving = packet.isMoving || packet.rotating
            val badPackets = thePlayer.posX == thePlayer.lastTickPosX && 
                           thePlayer.posY == thePlayer.lastTickPosY && 
                           thePlayer.posZ == thePlayer.lastTickPosZ
            
            if (!moving && !badPackets) {
                // 存储移动数据包而不是立即发送
                val oldSize = incomingClientPackets.size
                incomingClientPackets.add(packet)
                event.cancelEvent()
                balance -= 50
                
                val newSize = incomingClientPackets.size
                if (chatNotify && newSize > oldSize) {
                    if (oldSize == 0) {
                        mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§7[NekoTimer] §eStarted storing movement packets"))
                    }
                    if (newSize % 10 == 0) {
                        mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§7[NekoTimer] §6Currently storing §e$newSize §6movement packets"))
                    }
                }
            }
            
            if (!event.isCancelled) {
                balance = (balance + if (stopWatch.hasTimePassed(50)) 50 else 0).coerceAtLeast(0)
                stopWatch.reset()
            }
        }
        
        // 不再处理C0FPacketConfirmTransaction数据包
    }

    val onWorld = handler<WorldEvent> {
        reset()
        blinkStart = false
        if (chatNotify && incomingClientPackets.size > 0) {
            mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§7[NekoTimer] §cWorld changed, cleared stored movement packets"))
        }
    }

    private fun reset() {
        while (!incomingClientPackets.isEmpty()) {
            val packet = incomingClientPackets.poll()
            if (packet != null) {
                mc.netHandler.addToSendQueue(packet)
            }
        }
        
        balance = 0
        stopWatch.reset()
        lastStoredCount = 0
    }

    private fun toggleAll() {
        state = false
        if (chatNotify) {
            mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§7[NekoTimer] §cBalance negative, disabling module"))
        }
    }
}