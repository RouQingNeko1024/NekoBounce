/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.skid

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.tryJump

object OldFDPNoWeb : Module("OldFDPNoWeb", Category.SKID) {

    private val mode by choices(
        "Mode", 
        arrayOf("None", "FastFall", "OldAAC", "LAAC", "Rewinside", "Spartan", "AAC4", "AAC5", "Matrix", "Intave14", "Verus"), 
        "None"
    )

    private var usedTimer = false

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        
        if (usedTimer) {
            mc.timer.timerSpeed = 1F
            usedTimer = false
        }
        if (!player.isInWeb) {
            return@handler
        }

        when (mode.lowercase()) {
            "none" -> player.isInWeb = false
            "fastfall" -> {
                //Bypass AAC(All) Vulcan Verus Matrix NCP3.17 HAWK Spartan
                if (player.onGround) player.tryJump()
                if (player.motionY > 0f) {
                    player.motionY -= player.motionY * 2
                }
            }
            "oldaac" -> {
                player.jumpMovementFactor = 0.59f

                if (!mc.gameSettings.keyBindSneak.isKeyDown) {
                    player.motionY = 0.0
                }
            }
            "laac" -> {
                player.jumpMovementFactor = if (player.movementInput.moveStrafe != 0f) 1.0f else 1.21f

                if (!mc.gameSettings.keyBindSneak.isKeyDown) {
                    player.motionY = 0.0
                }

                if (player.onGround) {
                    player.tryJump()
                }
            }
            "aac4" -> {
                mc.timer.timerSpeed = 0.99F
                player.jumpMovementFactor = 0.02958f
                player.motionY -= 0.00775
                if (player.onGround) {
                    player.motionY = 0.4050
                    mc.timer.timerSpeed = 1.35F
                }
            }
            "spartan" -> {
                // 替代 MovementUtils.strafe
                val yaw = Math.toRadians(player.rotationYaw.toDouble())
                val speed = 0.27F
                player.motionX = -Math.sin(yaw) * speed
                player.motionZ = Math.cos(yaw) * speed
                
                mc.timer.timerSpeed = 3.7F
                if (!mc.gameSettings.keyBindSneak.isKeyDown) {
                    player.motionY = 0.0
                }
                if (player.ticksExisted % 2 == 0) {
                    mc.timer.timerSpeed = 1.7F
                }
                if (player.ticksExisted % 40 == 0) {
                    mc.timer.timerSpeed = 3F
                }
                usedTimer = true
            }
            "matrix" -> {
                player.jumpMovementFactor = 0.12425f
                player.motionY = -0.0125
                if (mc.gameSettings.keyBindSneak.isKeyDown) player.motionY = -0.1625
                
                if (player.ticksExisted % 40 == 0) {
                    mc.timer.timerSpeed = 3.0F
                    usedTimer = true
                }
            }
            "aac5" -> {
                player.jumpMovementFactor = 0.42f

                if (player.onGround) {
                    player.tryJump()
                }
            }
            "intave14" -> {
                if (player.movementInput.moveStrafe == 0.0F && mc.gameSettings.keyBindForward.isKeyDown && player.isCollidedVertically) {
                    player.jumpMovementFactor = 0.74F
                } else {
                    player.jumpMovementFactor = 0.2F
                    player.onGround = true
                }
            }
            "rewinside" -> {
                player.jumpMovementFactor = 0.42f

                if (player.onGround) {
                    player.tryJump()
                }
            }
            "verus" -> {
                // 替代 MovementUtils.strafe
                val yaw = Math.toRadians(player.rotationYaw.toDouble())
                val speed = 1.00f
                player.motionX = -Math.sin(yaw) * speed
                player.motionZ = Math.cos(yaw) * speed
                
                if (!mc.gameSettings.keyBindJump.isKeyDown && !mc.gameSettings.keyBindSneak.isKeyDown) {
                    player.motionY = 0.00
                }
                if (mc.gameSettings.keyBindJump.isKeyDown) {
                    player.motionY = 4.42
                }
                if (mc.gameSettings.keyBindSneak.isKeyDown) {
                    player.motionY = -4.42
                }
            }
        }
    }

    val onJump = handler<JumpEvent> { event ->
        if (mode.lowercase() == "aac4") {
            event.cancelEvent()
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0F
    }

    override val tag
        get() = mode
}