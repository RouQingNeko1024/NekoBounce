/*
 * Matrix NoFall Mode
 * @author Alan
 * @since 3/02/2022
 * Adapted for LiquidBounce B100
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.init.Blocks
import kotlin.math.sqrt

object MatrixNew : NoFallMode("MatrixNew") {

    override fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return
        
        // 检查脚下是否有方块
        if (!isBlockUnder()) {
            return
        }

        val distance = player.fallDistance

        if (distance > 2) {
            // 设置水平速度为 0.19
            MovementUtils.strafe(0.19f)
        }

        // 使用自定义的 getSpeed 函数
        if (distance > 3 && getSpeed() < 0.2) {
            // 设置为地面状态
            event.onGround = true
            player.fallDistance = 0f
        }
    }

    override fun onPacket(event: PacketEvent) {
        // MatrixNew 主要在运动事件中处理逻辑
        // 这里可以留空或添加其他逻辑
    }

    /**
     * 检查玩家脚下是否有方块
     */
    private fun isBlockUnder(): Boolean {
        val player = mc.thePlayer ?: return false
        
        // 检查玩家下方1格范围内是否有非空气方块
        for (i in 0..1) {
            val blockPos = net.minecraft.util.BlockPos(player.posX, player.posY - i, player.posZ)
            if (BlockUtils.getBlock(blockPos) != Blocks.air) {
                return true
            }
        }
        return false
    }

    /**
     * 获取玩家当前水平移动速度
     */
    private fun getSpeed(): Double {
        val player = mc.thePlayer ?: return 0.0
        return sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)
    }
}