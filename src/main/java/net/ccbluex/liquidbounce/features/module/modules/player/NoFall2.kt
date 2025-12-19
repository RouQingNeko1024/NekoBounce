package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.BlockPos
import kotlin.math.floor

/**
 * Matrix-style NoFall spoof (移植自 loftily)
 *
 * - 新增设置：
 *    MinFallDistance (默认 3f)
 *    NoVoid (默认 true)
 *    LegitTimer (默认 false)
 *
 * - 内部实现包含 loftily 风格的 fallDamage() 与 inVoidCheck() 方法，
 *   以便在没有全局 NoFall 监测时也能正确决定何时进行 spoof。
 */
object NoFall2 : Module("MatrixNoFall", Category.PLAYER) {

    private val legitTimer by boolean("LegitTimer", false)

    // --- 新增：NoFall 风格的本地检测配置 ---
    private val minFallDistance by float("MinFallDistance", 3.0f, 0.0f..8.0f)
    private val noVoid by boolean("NoVoid", true)
    // -------------------------------------------

    private var timered = false

    override fun onDisable() {
        try { mc.timer.timerSpeed = 1f } catch (_: Throwable) {}
        timered = false
    }

    /**
     * 与 loftily.NoFall.fallDamage() 行为一致：
     * return mc.player.fallDistance - mc.player.motionY > minFallDistance
     */
    private fun fallDamage(): Boolean {
        val player = mc.thePlayer ?: return false
        return (player.fallDistance - player.motionY) > minFallDistance
    }

    /**
     * 与 loftily.NoFall.inVoidCheck() 行为一致：
     * if noVoid is false -> return true
     * else return !isInVoid()
     */
    private fun inVoidCheck(): Boolean {
        if (!noVoid) return true
        return !isInVoid()
    }

    /**
     * 简单版 isInVoid 检测：从玩家脚下向下扫描到 Y=0，
     * 若发现任意非空气方块则判定不是 void（返回 false），
     * 否则判定为 inVoid（返回 true）。
     *
     * 注意：此方法每次调用会查询方块，若担心性能可以改成缓存或分帧检查。
     */
    private fun isInVoid(): Boolean {
        val player = mc.thePlayer ?: return false
        val world = mc.theWorld ?: return false

        val px = floor(player.posX).toInt()
        val pz = floor(player.posZ).toInt()
        val startY = floor(player.posY).toInt()

        for (y in startY downTo 0) {
            val block = world.getBlockState(BlockPos(px, y, pz)).block
            if (block != Blocks.air) {
                return false // 找到实体方块 -> 不在 void
            }
        }
        return true // 到底都没方块 -> 在 void
    }

    /**
     * PacketEvent handler: 拦截 outgoing C03 (movement) 包并在满足条件时 spoof。
     */
    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet

        try {
            // 根据 loftily 的逻辑：当 fallDamage() && inVoidCheck() 时才 spoof
            if (fallDamage() && inVoidCheck()) {
                if (packet is C03PacketPlayer) {
                    // Cancel original movement packet
                    event.cancelEvent()

                    // Use player's current coordinate (兼容性较好)
                    val px = player.posX
                    val py = player.posY
                    val pz = player.posZ

                    // Send spoofed position packets (onGround = true then false)
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(px, py, pz, true), false)
                    sendPacket(C03PacketPlayer.C04PacketPlayerPosition(px, py, pz, false), false)

                    // reset fall distance
                    player.fallDistance = 0f

                    // apply legit timer if requested
                    if (legitTimer) {
                        timered = true
                        try { mc.timer.timerSpeed = 0.2f } catch (_: Throwable) {}
                    }
                }
            } else if (timered) {
                // restore timer if we previously slowed it
                try { mc.timer.timerSpeed = 1f } catch (_: Throwable) {}
                timered = false
            }
        } catch (_: Throwable) {
            // 忽略任何异常以保证客户端稳定
        }
    }
}
