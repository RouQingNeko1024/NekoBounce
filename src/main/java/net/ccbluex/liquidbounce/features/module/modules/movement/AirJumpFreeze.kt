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
import net.minecraft.network.play.client.C03PacketPlayer

object AirJumpFreeze : Module("AirJumpFreeze", Category.MOVEMENT) {
    private var motionX = 0.0
    private var motionY = 0.0
    private var motionZ = 0.0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var shouldFreeze = false

    override fun onEnable() {
        mc.thePlayer ?: return

        // 初始化位置和动量
        x = mc.thePlayer.posX
        y = mc.thePlayer.posY
        z = mc.thePlayer.posZ
        motionX = mc.thePlayer.motionX
        motionY = mc.thePlayer.motionY
        motionZ = mc.thePlayer.motionZ
        shouldFreeze = false

        // 如果在地面上则跳跃
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump()
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (!shouldFreeze) {
            // 检测是否到达跳跃最高点(垂直速度接近0)
            if (mc.thePlayer.motionY < 0.01 && mc.thePlayer.motionY > -0.01) {
                // 到达最高点，开始冻结
                x = mc.thePlayer.posX
                y = mc.thePlayer.posY
                z = mc.thePlayer.posZ
                motionX = 0.0
                motionY = 0.0
                motionZ = 0.0
                shouldFreeze = true
            }
        } else {
            // 冻结状态下保持位置不变
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionY = 0.0
            mc.thePlayer.motionZ = 0.0
            mc.thePlayer.setPositionAndRotation(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        // 在冻结状态下取消位置更新包
        if (shouldFreeze && event.packet is C03PacketPlayer) {
            event.cancelEvent()
        }
    }

    override fun onDisable() {
        // 恢复原有的动量
        mc.thePlayer.motionX = motionX
        mc.thePlayer.motionY = motionY
        mc.thePlayer.motionZ = motionZ
    }
}
