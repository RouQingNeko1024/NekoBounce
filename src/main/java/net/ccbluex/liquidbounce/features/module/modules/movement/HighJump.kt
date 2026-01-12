/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.client.chat

import net.ccbluex.liquidbounce.utils.movement.MoveUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.block.BlockPane
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos

object HighJump : Module("HighJump", Category.MOVEMENT) {
    private val mode by choices("Mode", arrayOf("Vanilla", "Damage", "AACv3", "DAC", "Mineplex", "Matrix"), "Vanilla")
    private val height by float("Height", 2f, 1.1f..5f) { mode in arrayOf("Vanilla", "Damage") }

    // Matrix模式相关设置
    private val matrixMotion by float("Matrix-Motion", 0.5f, 0.1f..1.0f) { mode == "Matrix" }
    private val matrixDelay by int("Matrix-Delay", 500, 100..2000) { mode == "Matrix" }
    private val matrixOnlyOnGround by boolean("Matrix-OnlyOnGround", true) { mode == "Matrix" }
    private val matrixShowHeight by boolean("Matrix-ShowHeight", true) { mode == "Matrix" }

    private val glass by boolean("OnlyGlassPane", false)

    // Matrix模式相关变量
    private var matrixActive = false
    private var matrixFalling = false
    private var matrixMoving = false
    private var matrixTicksSinceJump = 0
    private var matrixStartY = 0.0
    private var matrixMaxHeight = 0.0

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer

        if (glass && BlockPos(thePlayer).block !is BlockPane)
            return@handler

        when (mode.lowercase()) {
            "damage" -> if (thePlayer.hurtTime > 0 && thePlayer.onGround) thePlayer.motionY += 0.42f * height
            "aacv3" -> if (!thePlayer.onGround) thePlayer.motionY += 0.059
            "dac" -> if (!thePlayer.onGround) thePlayer.motionY += 0.049999
            "mineplex" -> if (!thePlayer.onGround) strafe(0.35f)
            "matrix" -> {
                // 更新最大高度
                if (matrixActive) {
                    val currentHeight = thePlayer.posY - matrixStartY
                    if (currentHeight > matrixMaxHeight) {
                        matrixMaxHeight = currentHeight
                    }
                }
                
                if (!matrixMoving) {
                    MoveUtils.setSpeed(0.16, false)
                    matrixMoving = true
                }
                
                if (thePlayer.onGround) {
                    matrixActive = true
                }
                
                if (matrixTicksSinceJump == 1) {
                    thePlayer.onGround = false
                    thePlayer.motionY = 0.998
                }
                
                if (thePlayer.onGround && matrixTicksSinceJump > 4) {
                    state = false
                }
                
                if (!thePlayer.onGround && matrixTicksSinceJump >= 2) {
                    thePlayer.motionY += 0.0034999
                    
                    if (!matrixFalling && thePlayer.motionY < 0.0 && thePlayer.motionY > -0.05) {
                        thePlayer.motionY = 0.0029999
                        matrixFalling = true
                        state = false
                    }
                }
                
                if (matrixActive) {
                    ++matrixTicksSinceJump
                }
            }
        }
    }

    val onMove = handler<MoveEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (glass && BlockPos(thePlayer).block !is BlockPane)
            return@handler
        
        when (mode.lowercase()) {
            "mineplex" -> {
                if (!thePlayer.onGround) {
                    thePlayer.motionY += if (thePlayer.fallDistance == 0f) 0.0499 else 0.05
                }
            }
            "matrix" -> {
                if (matrixTicksSinceJump == 1) {
                    thePlayer.onGround = false
                }
            }
        }
    }

    val onJump = handler<JumpEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        if (glass && BlockPos(thePlayer).block !is BlockPane)
            return@handler
        
        when (mode.lowercase()) {
            "vanilla" -> event.motion *= height
            "mineplex" -> event.motion = 0.47f
            "matrix" -> {
                matrixTicksSinceJump = 0
                matrixActive = false
                matrixFalling = false
                matrixMoving = MoveUtils.isMoving()
                
                // 记录起始高度
                matrixStartY = thePlayer.posY
                matrixMaxHeight = 0.0
            }
        }
    }
    
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val thePlayer = mc.thePlayer ?: return@handler
        
        if (mode.lowercase() != "matrix") return@handler
        
        if (packet is S12PacketEntityVelocity) {
            if (packet.entityID == thePlayer.entityId && packet.motionY < -500) {
                event.cancelEvent()
            }
        }
        
        if (packet is S08PacketPlayerPosLook) {
            // 重置状态以应对反作弊检测
            matrixTicksSinceJump = 0
            matrixActive = false
            matrixFalling = false
        }
    }
    
    override fun onEnable() {
        // 重置Matrix模式变量
        if (mode.lowercase() == "matrix") {
            matrixTicksSinceJump = 0
            matrixActive = false
            matrixFalling = false
            matrixMoving = MoveUtils.isMoving()
            
            mc.thePlayer?.let {
                matrixStartY = it.posY
                matrixMaxHeight = 0.0
            }
        }
    }
    
    override fun onDisable() {
        // 显示最大跳跃高度
        if (mode.lowercase() == "matrix" && matrixShowHeight && matrixMaxHeight > 0) {
            chat("§a[MatrixHighJump] §f最大高度: §e${String.format("%.2f", matrixMaxHeight)} §fblocks")
        }
        
        // 重置Matrix模式变量
        if (mode.lowercase() == "matrix") {
            matrixActive = false
            matrixFalling = false
            matrixMoving = false
            matrixTicksSinceJump = 0
        }
    }

    override val tag
        get() = mode
}