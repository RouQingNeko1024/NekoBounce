/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes.hypixel

import io.qzz.nekobounce.features.module.modules.movement.speedmodes.SpeedMode
import io.qzz.nekobounce.utils.client.PacketUtils.sendPacket
import io.qzz.nekobounce.utils.extensions.*
import io.qzz.nekobounce.utils.movement.MovementUtils
import net.minecraft.block.BlockAir
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object WatchdogSpeed : SpeedMode("WatchdogSpeed") {
    
    private var angle = 0f
    private var lastAngle = 0f
    
    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        
        // 只在Full Strafe模式下处理地面状态
        if (isFullStrafeMode()) {
            val blockBelow = mc.theWorld.getBlockState(BlockPos(player.posX, player.posY - 1, player.posZ)).block
            val blockBelow2 = mc.theWorld.getBlockState(BlockPos(player.posX, player.posY - 1.1, player.posZ)).block
            
            // 发送破坏方块包
            sendPacket(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, 
                BlockPos(player.posX, player.posY, player.posZ), EnumFacing.UP))
        }
    }
    
    override fun onStrafe() {
        val player = mc.thePlayer ?: return
        
        if (player.isInLiquid || !player.isMoving)
            return
            
        val modeType = getModeType()
        
        when (modeType) {
            "Ground Strafe" -> {
                if (player.onGround && player.isMoving) {
                    MovementUtils.strafe(getAllowedHorizontalDistance() - Math.random().toFloat() / 100f)
                    player.jump()
                }
            }
            
            "Damage Strafe" -> {
                if (player.onGround && player.isMoving) {
                    MovementUtils.strafe()
                    MovementUtils.strafe(getAllowedHorizontalDistance() - Math.random().toFloat() / 100f)
                    player.jump()
                }
            }
            
            "Full Strafe" -> {
                if (!(player.isOnLadder || player.isInWeb || player.isInWater || player.isInLava)) {
                    if (player.onGround) {
                        MovementUtils.strafe(getAllowedHorizontalDistance() - Math.random().toFloat() / 1000f)
                        player.jump()
                    }
                }
            }
        }
    }
    
    override fun onEnable() {
        // 重置状态
        angle = 0f
        lastAngle = 0f
    }
    
    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
    }
    
    // 辅助函数：获取模式类型
    private fun getModeType(): String {
        // 这里需要从Speed模块获取设置，但由于是单例模式，我们无法直接访问
        // 简化处理：使用默认值或通过其他方式获取
        return "Full Strafe"
    }
    
    // 辅助函数：是否为Full Strafe模式
    private fun isFullStrafeMode(): Boolean {
        return getModeType() == "Full Strafe"
    }
    
    // 辅助函数：获取允许的水平移动距离
    private fun getAllowedHorizontalDistance(): Float {
        val player = mc.thePlayer ?: return 0.2f
        
        val baseSpeed = 0.2873f
        val speedEffect = player.getActivePotionEffect(Potion.moveSpeed)
        
        return if (speedEffect != null) {
            baseSpeed * (1.0f + 0.2f * (speedEffect.amplifier + 1))
        } else {
            baseSpeed
        }
    }
    
    // 辅助函数：移动飞行
    private fun moveFlying(speed: Float) {
        val player = mc.thePlayer ?: return
        
        var forward = player.moveForward
        var strafe = player.moveStrafing
        var yaw = player.rotationYaw
        
        if (forward != 0f) {
            if (strafe > 0f) {
                yaw += (if (forward > 0f) -45 else 45)
            } else if (strafe < 0f) {
                yaw += (if (forward > 0f) 45 else -45)
            }
            strafe = 0f
            forward = if (forward > 0f) 1f else -1f
        }
        
        if (strafe > 0f) {
            strafe = 1f
        } else if (strafe < 0f) {
            strafe = -1f
        }
        
        val yawRadians = Math.toRadians(yaw.toDouble())
        
        player.motionX = (forward * speed * sin(yawRadians) + 
                         strafe * speed * cos(yawRadians))
        player.motionZ = (forward * speed * cos(yawRadians) - 
                         strafe * speed * sin(yawRadians))
    }
    
    private fun simulationStrafeAngle(currentAngle: Float, increment: Float): Float {
        var newAngle = currentAngle + increment
        if (newAngle > 360f) newAngle -= 360f
        return newAngle
    }
}