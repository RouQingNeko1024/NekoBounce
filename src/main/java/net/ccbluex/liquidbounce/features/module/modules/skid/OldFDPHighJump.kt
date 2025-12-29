/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
//Neko
package net.ccbluex.liquidbounce.features.module.modules.skid

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.block.Block
import net.minecraft.block.BlockPane
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object OldFDPHighJump : Module("OldFDPHighJump", Category.SKID) {

    private val height by float("Height", 2f, 1.1f..7f)
    private val mode by choices(
        "Mode", 
        arrayOf("Vanilla", "StableMotion", "Damage", "AACv3", "DAC", "Mineplex", "Matrix", "MatrixWater"), 
        "Vanilla"
    )
    private val onlyGlassPane by boolean("OnlyGlassPane", false)
    private val stableMotion by float("StableMotion", 0.42f, 0.1f..1f) { mode == "StableMotion" }
    
    private var jumpY = 114514.0
    private var matrixStatus = 0
    private var matrixWasTimer = false
    private val timer = MSTimer()

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        
        if (onlyGlassPane && getBlock(BlockPos(player.posX, player.posY, player.posZ)) !is BlockPane) return@handler

        when (mode.lowercase()) {
            "damage" -> {
                if (player.hurtTime > 0 && player.onGround) {
                    player.motionY += (0.42f * height).toDouble()
                }
            }
            "aacv3" -> {
                if (!player.onGround) {
                    player.motionY += 0.059
                }
            }
            "dac" -> {
                if (!player.onGround) {
                    player.motionY += 0.049999
                }
            }
            "mineplex" -> {
                if (!player.onGround) {
                    // 替代 MovementUtils.strafe
                    val yaw = Math.toRadians(player.rotationYaw.toDouble())
                    val speed = 0.35f
                    player.motionX = -Math.sin(yaw) * speed
                    player.motionZ = Math.cos(yaw) * speed
                }
            }
            "stablemotion" -> {
                if (jumpY != 114514.0) {
                    if (jumpY + height - 1 > player.posY) {
                        player.motionY = stableMotion.toDouble()
                    } else {
                        jumpY = 114514.0
                    }
                }
            }
            "matrixwater" -> {
                if (player.isInWater) {
                    if (mc.theWorld.getBlockState(BlockPos(player.posX, player.posY + 1, player.posZ)).block == Block.getBlockById(9)) {
                        player.motionY = 0.18
                    } else if (mc.theWorld.getBlockState(BlockPos(player.posX, player.posY, player.posZ)).block == Block.getBlockById(9)) {
                        player.motionY = height.toDouble()
                        player.onGround = true
                    }
                }
            }
            "matrix" -> {
                if (matrixWasTimer) {
                    mc.timer.timerSpeed = 1.00f
                    matrixWasTimer = false
                }
                if ((mc.theWorld.getCollidingBoundingBoxes(player, player.entityBoundingBox.offset(0.0, player.motionY, 0.0).expand(0.0, 0.0, 0.0)).isNotEmpty() ||
                            mc.theWorld.getCollidingBoundingBoxes(player, player.entityBoundingBox.offset(0.0, -4.0, 0.0).expand(0.0, 0.0, 0.0)).isNotEmpty()) &&
                    player.fallDistance > 10) {
                    if (!player.onGround) {
                        mc.timer.timerSpeed = 0.1f
                        matrixWasTimer = true
                    }
                }
                if (timer.hasTimePassed(1000) && matrixStatus == 1) {
                    mc.timer.timerSpeed = 1.0f
                    player.motionX = 0.0
                    player.motionZ = 0.0
                    matrixStatus = 0
                }
                if (matrixStatus == 1 && player.hurtTime > 0) {
                    mc.timer.timerSpeed = 1.0f
                    player.motionY = 3.0
                    player.motionX = 0.0
                    player.motionZ = 0.0
                    player.jumpMovementFactor = 0.00f
                    matrixStatus = 0
                }
                if (matrixStatus == 2) {
                    player.sendQueue.addToSendQueue(C0APacketAnimation())
                    player.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(player.posX, player.posY, player.posZ, false))
                    repeat(8) {
                        player.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(player.posX, player.posY + 0.3990, player.posZ, false))
                        player.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(player.posX, player.posY, player.posZ, false))
                    }
                    player.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(player.posX, player.posY, player.posZ, true))
                    player.sendQueue.addToSendQueue(C03PacketPlayer.C04PacketPlayerPosition(player.posX, player.posY, player.posZ, true))
                    mc.timer.timerSpeed = 0.6f
                    matrixStatus = 1
                    timer.reset()
                    player.sendQueue.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, BlockPos(player.posX, player.posY - 1, player.posZ), EnumFacing.UP))
                    player.sendQueue.addToSendQueue(C0APacketAnimation())
                }
                if (player.isCollidedHorizontally && matrixStatus == 0 && player.onGround) {
                    player.sendQueue.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, BlockPos(player.posX, player.posY - 1, player.posZ), EnumFacing.UP))
                    player.sendQueue.addToSendQueue(C0APacketAnimation())
                    matrixStatus = 2
                    mc.timer.timerSpeed = 0.05f
                }
                if (player.isCollidedHorizontally && player.onGround) {
                    player.motionX = 0.0
                    player.motionZ = 0.0
                    player.onGround = false
                }
            }
        }
    }

    val onMove = handler<MoveEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        
        if (onlyGlassPane && getBlock(BlockPos(player.posX, player.posY, player.posZ)) !is BlockPane) return@handler

        if (!player.onGround && mode.lowercase() == "mineplex") {
            event.y += if (player.fallDistance == 0f) 0.0499 else 0.05
        }
    }

    val onJump = handler<JumpEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        
        if (onlyGlassPane && getBlock(BlockPos(player.posX, player.posY, player.posZ)) !is BlockPane) return@handler

        when (mode.lowercase()) {
            "vanilla" -> {
                event.motion = event.motion * height
            }
            "mineplex" -> {
                event.motion = 0.47f
            }
            "stablemotion" -> {
                jumpY = player.posY
            }
        }
    }

    override fun onEnable() {
        jumpY = 114514.0
        matrixStatus = 0
        matrixWasTimer = false
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    override val tag
        get() = mode
}