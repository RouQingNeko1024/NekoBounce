package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.matrix

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.features.module.modules.player.NoFall
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos
import kotlin.math.floor

object MatrixSpoof : NoFallMode("MatrixSpoof") {

    private var timered = false

    override fun onEnable() {
        timered = false
    }

    override fun onDisable() {
        try { 
            mc.timer.timerSpeed = 1f 
        } catch (_: Throwable) {}
        timered = false
    }

    /**
     * 与 loftily.NoFall.fallDamage() 行为一致：
     * return mc.player.fallDistance - mc.player.motionY > minFallDistance
     */
    private fun fallDamage(): Boolean {
        val player = mc.thePlayer ?: return false
        return (player.fallDistance - player.motionY) > NoFall.matrixMinFallDistance
    }

    /**
     * 与 loftily.NoFall.inVoidCheck() 行为一致：
     * if noVoid is false -> return true
     * else return !isInVoid()
     */
    private fun inVoidCheck(): Boolean {
        if (!NoFall.matrixNoVoid) return true
        return !isInVoid()
    }

    /**
     * 简单版 isInVoid 检测：从玩家脚下向下扫描到 Y=0，
     * 若发现任意非空气方块则判定不是 void（返回 false），
     * 否则判定为 inVoid（返回 true）。
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

    override fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
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
                    sendPacket(C04PacketPlayerPosition(px, py, pz, true), false)
                    sendPacket(C04PacketPlayerPosition(px, py, pz, false), false)

                    // reset fall distance
                    player.fallDistance = 0f

                    // apply legit timer if requested
                    if (NoFall.matrixLegitTimer) {
                        timered = true
                        try { 
                            mc.timer.timerSpeed = 0.2f 
                        } catch (_: Throwable) {}
                    }
                }
            } else if (timered) {
                // restore timer if we previously slowed it
                try { 
                    mc.timer.timerSpeed = 1f 
                } catch (_: Throwable) {}
                timered = false
            }
        } catch (_: Throwable) {
            // 忽略任何异常以保证客户端稳定
        }
    }
}