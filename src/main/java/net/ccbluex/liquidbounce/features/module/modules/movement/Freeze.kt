/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extras.StuckUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity

object Freeze : Module("Freeze", Category.MOVEMENT) {
    val isStuck by boolean("stuck", true)
    
    // 添加新选项：是否启用自动禁用计时器
    private val enableAutoDisable by boolean("EnableAutoDisable", false) { isStuck }
    // 当启用自动禁用时，显示并启用AutoDisableTicks选项
    private val autoDisableTicks by int("AutoDisableTicks", 20, 1..100) { enableAutoDisable && isStuck }
    
    private var stuckTicks = 0

    private var motionX = 0.0
    private var motionY = 0.0
    private var motionZ = 0.0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var yaw = 0f
    private var pitch = 0f

    override fun onEnable() {
        if (isStuck) {
            StuckUtils.stuck()
            StuckUtils.moveTicks = 0
            stuckTicks = 0
            
            // 保存当前位置和视角
            val player = mc.thePlayer ?: return
            x = player.posX
            y = player.posY
            z = player.posZ
            yaw = player.rotationYaw
            pitch = player.rotationPitch
        } else {
            val player = mc.thePlayer ?: return
            x = player.posX
            y = player.posY
            z = player.posZ
            yaw = player.rotationYaw
            pitch = player.rotationPitch
            motionX = player.motionX
            motionY = player.motionY
            motionZ = player.motionZ
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        
        if (isStuck) {
            // 保持玩家位置和视角不变
            player.setPositionAndRotation(x, y, z, yaw, pitch)
            player.motionX = 0.0
            player.motionY = 0.0
            player.motionZ = 0.0
            
            // 增加stuck计时
            stuckTicks++
            
            // 如果检测到移动或超过自动禁用时间（如果启用），则禁用模块
            if (StuckUtils.moveTicks > 0 || (enableAutoDisable && stuckTicks >= autoDisableTicks)) {
                state = false
            }
        } else {
            // 传统冻结模式
            player.motionX = 0.0
            player.motionY = 0.0
            player.motionZ = 0.0
            player.setPositionAndRotation(x, y, z, player.rotationYaw, player.rotationPitch)
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (isStuck) {
            // stuck模式下，只允许发送位置包，不取消任何包
            if (event.packet is C03PacketPlayer) {
                // 修改包的位置为冻结位置
                event.packet.x = x
                event.packet.y = y
                event.packet.z = z
                event.packet.yaw = yaw
                event.packet.pitch = pitch
                event.packet.onGround = false
            }
        } else {
            // 传统冻结模式：取消玩家的移动包
            if (event.packet is C03PacketPlayer) {
                event.cancelEvent()
            }
            if (event.packet is S08PacketPlayerPosLook) {
                x = event.packet.x
                y = event.packet.y
                z = event.packet.z
                motionX = 0.0
                motionY = 0.0
                motionZ = 0.0
            }
        }
    }

    override fun onDisable() {
        if (isStuck) {
            StuckUtils.stopStuck()
            StuckUtils.moveTicks = 0
            stuckTicks = 0
        } else {
            val player = mc.thePlayer ?: return
            player.motionX = motionX
            player.motionY = motionY
            player.motionZ = motionZ
            player.setPositionAndRotation(x, y, z, player.rotationYaw, player.rotationPitch)
        }
    }
}