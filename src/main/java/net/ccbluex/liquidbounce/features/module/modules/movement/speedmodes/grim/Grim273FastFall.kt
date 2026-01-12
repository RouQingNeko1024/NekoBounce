package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object Grim273FastFall : SpeedMode("Grim2.3.73 1.9+ FastFall") {

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.isMoving) {
            // 持续跳跃
            if (Speed.constantJump && player.onGround) {
                player.tryJump()
            }

            // 快速下落
            if (!player.onGround && player.motionY > 0) {
                player.motionY -= Speed.fastFallStrength
            }

            // 保持速度
            MovementUtils.strafe()

            // 发送PostPlace模式的数据包
            if (Speed.postPlacePackets && player.heldItem != null) {
                // 发送释放使用物品的包
                PacketUtils.sendPacket(
                    C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        EnumFacing.DOWN
                    )
                )

                // 重复发送BlockPlacement包（模拟PostPlace模式）
                repeat(5) {
                    PacketUtils.sendPacket(
                        C08PacketPlayerBlockPlacement(
                            BlockPos(-1, -1, -1),
                            255,
                            player.heldItem,
                            0f,
                            0f,
                            0f
                        )
                    )
                }
            }
        }
    }

    override fun onMotion() {
        val player = mc.thePlayer ?: return

        // 在Motion事件中也发送包以确保同步
        if (Speed.postPlacePackets && player.isMoving && player.heldItem != null) {
            PacketUtils.sendPacket(
                C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
                )
            )
            repeat(3) {
                PacketUtils.sendPacket(
                    C08PacketPlayerBlockPlacement(
                        BlockPos(-1, -1, -1),
                        255,
                        player.heldItem,
                        0f,
                        0f,
                        0f
                    )
                )
            }
        }
    }

    override fun onMove(event: MoveEvent) {
        // 保持移动速度
        if (mc.thePlayer.isMoving) {
            // 直接使用 MovementUtils.strafe() 计算速度
            MovementUtils.strafe()
        }
    }
}