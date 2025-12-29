package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition

object Grim : FlyMode("Grim") {
    
    // 子模式选择 - 使用简单变量
    private var mode = "Block" // Block 或 TNT
    
    // 状态变量
    private var flightEnabled = false
    private var lastY = 0.0
    
    override fun onEnable() {
        flightEnabled = false
        lastY = mc.thePlayer?.posY ?: 0.0
        
        // 显示模式信息
        val player = mc.thePlayer ?: return
        player.addChatMessage(
            net.minecraft.util.ChatComponentText("§aGrim Flight §7- §eMode: $mode")
        )
        
        when (mode) {
            "Block" -> {
                player.addChatMessage(
                    net.minecraft.util.ChatComponentText("§7Warning: Any blocks you place will be ghost blocks. You have infinite range to place blocks.")
                )
            }
            "TNT" -> {
                player.addChatMessage(
                    net.minecraft.util.ChatComponentText("§7Get knock-back from a TNT to trigger the flight")
                )
            }
        }
    }
    
    override fun onDisable() {
        flightEnabled = false
    }
    
    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        
        when (mode) {
            "Block" -> {
                // Block模式：右键点击时传送并放置方块
                if (mc.gameSettings.keyBindUseItem.isKeyDown) {
                    // 获取玩家看向的方块
                    val movingObjectPosition = player.rayTrace(999.0, 1.0f) ?: return
                    
                    val pos = movingObjectPosition.blockPos
                    val side = movingObjectPosition.sideHit
                    val tpPos = pos.offset(side, 4)
                    
                    // 发送传送包
                    mc.netHandler.addToSendQueue(C03PacketPlayer.C06PacketPlayerPosLook(
                        tpPos.x + 0.5, tpPos.y.toDouble(), tpPos.z + 0.5,
                        player.rotationYaw, player.rotationPitch,
                        false
                    ))
                    
                    // 放置方块
                    if (mc.playerController != null) {
                        mc.playerController.onPlayerRightClick(
                            player,
                            mc.theWorld,
                            player.heldItem,
                            pos,
                            side,
                            movingObjectPosition.hitVec
                        )
                    }
                }
            }
            "TNT" -> {
                // TNT模式：受到TNT击退后飞行
                if (flightEnabled) {
                    // 计算移动方向
                    val yaw = Math.toRadians(player.rotationYaw.toDouble())
                    
                    // 设置玩家位置
                    player.motionX = -Math.sin(yaw) * 0.5
                    player.motionY = 0.0
                    player.motionZ = Math.cos(yaw) * 0.5
                    
                    // 保持飞行高度
                    if (player.posY < lastY) {
                        player.motionY = 0.42
                        player.tryJump()
                    }
                    lastY = player.posY
                    
                    // 防止落地
                    player.onGround = false
                }
            }
        }
    }
    
    // 监听包接收事件（用于TNT模式检测击退）
    override fun onPacket(event: PacketEvent) {
        if (mode == "TNT" && event.packet is S12PacketEntityVelocity) {
            val packet = event.packet as S12PacketEntityVelocity
            
            // 修正：使用 entityID 而不是 entityId
            if (packet.entityID == mc.thePlayer?.entityId) {
                flightEnabled = true
                // 显示提示信息
                mc.thePlayer?.addChatMessage(
                    net.minecraft.util.ChatComponentText("§aGrim Flight §7- §eTNT knock-back detected! Flight activated.")
                )
            }
        }
    }
    
    // 处理方块碰撞箱（防止方块阻挡）
    override fun onBB(event: BlockBBEvent) {
        val player = mc.thePlayer ?: return
        
        // 在飞行时，移除碰撞箱以避免阻挡
        if ((mode == "Block" && mc.gameSettings.keyBindUseItem.isKeyDown) || 
            (mode == "TNT" && flightEnabled)) {
            
            // 移除玩家下方一定高度内的方块碰撞箱
            if (event.y.toDouble() < player.posY + 2) {
                event.boundingBox = null
            }
        }
    }
    
    // 移动事件处理
    override fun onMove(event: MoveEvent) {
        if (mode == "TNT" && flightEnabled) {
            val player = mc.thePlayer ?: return
            
            // 控制TNT飞行模式下的移动
            event.x = player.motionX
            event.y = player.motionY
            event.z = player.motionZ
            
            // 防止掉落
            if (event.y < 0) {
                event.y = 0.0
                player.motionY = 0.0
            }
        }
    }
    
    // 跳跃处理
    override fun onJump(event: JumpEvent) {
        if (mode == "TNT" && flightEnabled) {
            // 在TNT飞行模式下禁用跳跃
            event.cancelEvent()
        }
    }
    
    // 切换模式的方法（如果需要外部切换）
    fun switchMode(newMode: String) {
        mode = newMode
        flightEnabled = false
        
        // 显示模式切换信息
        mc.thePlayer?.addChatMessage(
            net.minecraft.util.ChatComponentText("§aGrim Flight §7- §eSwitched to $newMode mode")
        )
    }
}