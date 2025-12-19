package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.minecraft.network.play.client.C03PacketPlayer

/**
 * Matrix New NoFall Mode
 * Thx To Zerolysimin#6403
 */
object Matrix : NoFallMode("Matrix") {

    override fun onEnable() {
        // 重置计时器速度
        mc.timer.timerSpeed = 1f
    }

    override fun onDisable() {
        // 恢复计时器速度
        mc.timer.timerSpeed = 1f
    }

    override fun onMotion(event: MotionEvent) {
        // MatrixNewNofall 主要在包处理器中处理逻辑
        // 这里可以留空或添加其他逻辑
    }

    override fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet

        if (packet is C03PacketPlayer) {
            if (!player.onGround) {
                if (player.fallDistance > 2.69f) {
                    mc.timer.timerSpeed = 0.3f
                    packet.onGround = true
                    player.fallDistance = 0f
                }
                if (player.fallDistance > 3.5) {
                    mc.timer.timerSpeed = 0.3f
                } else {
                    mc.timer.timerSpeed = 1f
                }
            }
            
            if (mc.theWorld.getCollidingBoundingBoxes(
                    player, 
                    player.entityBoundingBox.offset(0.0, player.motionY, 0.0)
                ).isNotEmpty()
            ) {
                if (!packet.onGround && player.motionY < -0.6) {
                    packet.onGround = true
                    player.onGround = true
                }
            }
        }
    }
}