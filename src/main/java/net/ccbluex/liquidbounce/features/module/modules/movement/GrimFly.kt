package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.*  // 扩展函数
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.ChatComponentText
import java.lang.reflect.Constructor
import java.lang.reflect.Field

object GrimFly : Module("GrimFly", Category.MOVEMENT) {
    
    private val mode by choices("Mode", arrayOf("Packet", "Reflection", "Position"), "Packet")
    
    private val delayedPackets = mutableListOf<Packet<*>>()
    private var intercepting = false
    private var shouldStartFallFlying = false
    private var ticksUntilFallFlying = 0
    
    private val msTimer = MSTimer()
    
    override fun onEnable() {
        resetState(false)
        
        when (mode) {
            "Packet" -> {
                trySendStartFallFlying()
            }
            "Reflection" -> {
                sendViaReflection()
            }
            "Position" -> {
                sendPositionPacket()
            }
        }
    }
    
    override fun onDisable() {
        flushDelayedPackets()
        resetState(false)
    }
    
    private fun resetState(clearQueue: Boolean) {
        intercepting = false
        shouldStartFallFlying = false
        ticksUntilFallFlying = 0
        if (clearQueue) {
            delayedPackets.clear()
        }
    }
    
    private fun flushDelayedPackets() {
        delayedPackets.forEach { packet ->
            mc.netHandler.addToSendQueue(packet)
        }
        delayedPackets.clear()
    }
    
    private fun displayMessage(message: String) {
        try {
            // 尝试多种发送消息的方法
            mc.thePlayer?.addChatMessage(ChatComponentText("§7[§c§lGrimFly§7] §r$message"))
        } catch (e: Exception) {
            // 如果失败，尝试直接输出到控制台
            println("[GrimFly] $message")
        }
    }
    
    // 发送数据包的方法
    private fun sendPacket(packet: Packet<*>) {
        mc.netHandler.addToSendQueue(packet)
    }
    
    // 方法1: 尝试发送 START_FALL_FLYING 包
    private fun trySendStartFallFlying() {
        val player = mc.thePlayer ?: return
        
        try {
            // 首先尝试创建 Action 枚举
            val actionClass = Class.forName("net.minecraft.network.play.client.C0BPacketEntityAction\$Action")
            
            // 尝试获取 START_FALL_FLYING（如果存在）
            val existingAction = try {
                actionClass.getField("START_FALL_FLYING").get(null) as? C0BPacketEntityAction.Action
            } catch (e: NoSuchFieldException) {
                null
            }
            
            if (existingAction != null) {
                // 直接使用现有枚举
                val packet = C0BPacketEntityAction(player, existingAction)
                sendPacket(packet)
                displayMessage("§aSent START_FALL_FLYING packet")
            } else {
                // 尝试通过反射创建
                val action = createCustomAction()
                if (action != null) {
                    val packet = C0BPacketEntityAction(player, action)
                    sendPacket(packet)
                    displayMessage("§aSent custom fall flying packet")
                } else {
                    // 回退到位置包
                    sendPositionPacket()
                }
            }
        } catch (e: Exception) {
            displayMessage("§cError: ${e.message}")
        }
    }
    
    // 创建自定义 Action 枚举
    private fun createCustomAction(): C0BPacketEntityAction.Action? {
        return try {
            val actionClass = C0BPacketEntityAction.Action::class.java
            
            // 尝试使用现有的 Action 枚举值
            // 在 1.8.9 中，使用 START_SPRINTING 或其他现有值
            // 这里我们尝试通过反射创建新值
            val values = actionClass.enumConstants ?: return null
            
            // 方法1: 使用字符串创建枚举（可能无效）
            try {
                val method = actionClass.getMethod("valueOf", String::class.java)
                method.invoke(null, "START_FALL_FLYING") as? C0BPacketEntityAction.Action
            } catch (e: Exception) {
                // 方法2: 修改 $VALUES 数组
                modifyEnumValues(actionClass, values)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // 修改枚举数组
    private fun modifyEnumValues(
        actionClass: Class<C0BPacketEntityAction.Action>, 
        oldValues: Array<C0BPacketEntityAction.Action>
    ): C0BPacketEntityAction.Action? {
        return try {
            // 获取 $VALUES 字段
            val valuesField: Field = actionClass.getDeclaredField("\$VALUES")
            valuesField.isAccessible = true
            
            // 创建新数组
            val newValues = java.lang.reflect.Array.newInstance(
                actionClass, 
                oldValues.size + 1
            ) as Array<C0BPacketEntityAction.Action>
            
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.size)
            
            // 尝试创建新枚举实例
            val constructor: Constructor<C0BPacketEntityAction.Action> = 
                actionClass.getDeclaredConstructor(String::class.java, Int::class.javaPrimitiveType)
            constructor.isAccessible = true
            
            val newAction = constructor.newInstance("START_FALL_FLYING", oldValues.size)
            newValues[oldValues.size] = newAction
            
            // 更新 $VALUES
            valuesField.set(null, newValues)
            
            newAction
        } catch (e: Exception) {
            null
        }
    }
    
    // 发送位置包替代方案
    private fun sendPositionPacket() {
        val player = mc.thePlayer ?: return
        val packet = C03PacketPlayer.C04PacketPlayerPosition(
            player.posX,
            player.posY + 0.42,
            player.posZ,
            false
        )
        sendPacket(packet)
        displayMessage("§eUsing position packet as fallback")
    }
    
    // 方法2: 反射方法
    private fun sendViaReflection() {
        try {
            val player = mc.thePlayer ?: return
            
            // 直接通过反射创建包实例
            val packetClass = Class.forName("net.minecraft.network.play.client.C0BPacketEntityAction")
            
            // 查找构造函数
            val constructors = packetClass.constructors
            var targetConstructor: Constructor<*>? = null
            
            for (constructor in constructors) {
                val params = constructor.parameterTypes
                if (params.size == 2 && 
                    params[0] == net.minecraft.entity.Entity::class.java && 
                    params[1] == C0BPacketEntityAction.Action::class.java) {
                    targetConstructor = constructor
                    break
                }
            }
            
            if (targetConstructor != null) {
                // 尝试使用现有的 Action
                val sprintingAction = C0BPacketEntityAction.Action.START_SPRINTING
                
                // 创建包实例
                val packet = targetConstructor.newInstance(player, sprintingAction) as Packet<*>
                sendPacket(packet)
                
                displayMessage("§aSent via reflection")
            }
        } catch (e: Exception) {
            displayMessage("§cReflection failed: ${e.message}")
        }
    }
    
    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState == EventState.PRE) {
            val player = mc.thePlayer ?: return@handler
            
            if (shouldStartFallFlying) {
                ticksUntilFallFlying++
                if (ticksUntilFallFlying >= 8) {
                    // 尝试发送飞行包
                    trySendStartFallFlying()
                    shouldStartFallFlying = false
                    ticksUntilFallFlying = 0
                }
            }
        }
    }
    
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val player = mc.thePlayer ?: return@handler
        
        // 检查是否是发送包
        val isClientPacket = packet.javaClass.name.startsWith("net.minecraft.network.play.client")
        
        when {
            // 接收包
            packet is S12PacketEntityVelocity && packet.entityID == player.entityId -> {
                if (!intercepting && delayedPackets.isEmpty()) {
                    intercepting = true
                    shouldStartFallFlying = false
                    ticksUntilFallFlying = 0
                    event.cancelEvent()
                    displayMessage("§aIntercepted velocity packet")
                }
            }
            
            packet is S08PacketPlayerPosLook -> {
                if (intercepting && delayedPackets.isNotEmpty()) {
                    flushAndStopIntercepting()
                }
            }
            
            // 发送包
            isClientPacket -> {
                when {
                    intercepting && packet.javaClass.simpleName == "C16PacketClientStatus" -> {
                        event.cancelEvent()
                        if (delayedPackets.isEmpty()) {
                            shouldStartFallFlying = true
                            ticksUntilFallFlying = 0
                        }
                        delayedPackets.add(packet)
                        
                        if (delayedPackets.size > 200) {
                            flushAndStopIntercepting()
                        }
                    }
                    
                    packet is C03PacketPlayer -> {
                        if (intercepting && delayedPackets.isNotEmpty()) {
                            flushAndStopIntercepting()
                        }
                    }
                }
            }
        }
    }
    
    private fun flushAndStopIntercepting() {
        flushDelayedPackets()
        intercepting = false
        shouldStartFallFlying = false
        ticksUntilFallFlying = 0
        displayMessage("§eFlushed delayed packets")
    }
    
    override val tag: String?
        get() = mode
}