package net.ccbluex.liquidbounce.features.module.modules.skid

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.*
import net.minecraft.util.Vec3

object AugsVelocity : Module("AugsVelocity", Category.SKID) {

    private val mode by choices(
        "Mode",
        arrayOf("Basic", "Legit", "PushGround", "Push", "Intave", "Reverse", "Spoof", "Test"),
        "Basic"
    )

    // Basic mode settings
    private val xzValue by int("XZ-Velocity", 20, 0..100)
    private val yValue by int("Y-Velocity", 20, 0..100)
    private val ignoreExplosion by boolean("Explosion", true)

    // Intave mode settings
    private val xzValueIntave by float("Intave-XZ", 0.6f, -1f..1f)
    private val jumpIntave by boolean("Intave-Jump", false)

    // Push mode settings
    private val pushXZ by float("Push-XZ", 1.1f, 0.01f..20f)
    private val pushStart by int("Push-Start", 9, 1..10)
    private val pushEnd by int("Push-End", 2, 1..10)
    private val pushOnGround by boolean("Push-OnGround", false)

    // Reverse mode settings
    private val reverseStart by int("Reverse-Start", 9, 1..10)
    private val reverseStrafe by boolean("Reverse-Strafe", false)

    // Other settings
    private val hitBug by boolean("HitBug", false)

    private var counter = 0
    private val timeHelper = MSTimer()
    private val timeDelay = MSTimer()

    override val tag: String
        get() = mode

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val player = mc.thePlayer ?: return@handler

        when (mode) {
            "Basic" -> {
                when (packet) {
                    is S12PacketEntityVelocity -> {
                        if (packet.entityID == player.entityId) {
                            if (xzValue > 0 || yValue > 0) {
                                packet.motionX = (packet.motionX * xzValue / 100).toInt()
                                packet.motionY = (packet.motionY * yValue / 100).toInt()
                                packet.motionZ = (packet.motionZ * xzValue / 100).toInt()
                            } else {
                                event.cancelEvent()
                            }
                        }
                    }
                    is S27PacketExplosion -> {
                        if (ignoreExplosion) {
                            if (xzValue > 0 || yValue > 0) {
                                packet.field_149152_f = (packet.field_149152_f * xzValue / 100f).toFloat()
                                packet.field_149153_g = (packet.field_149152_f * yValue / 100f).toFloat()
                                packet.field_149159_h = (packet.field_149152_f * xzValue / 100f).toFloat()
                            } else {
                                event.cancelEvent()
                            }
                        }
                    }
                }
            }
            "Spoof" -> {
                if (packet is S12PacketEntityVelocity && packet.entityID == player.entityId) {
                    event.cancelEvent()
                    player.sendQueue.addToSendQueue(
                        C03PacketPlayer.C04PacketPlayerPosition(
                            player.posX + packet.motionX / 8000.0,
                            player.posY + packet.motionY / 8000.0,
                            player.posZ + packet.motionZ / 8000.0,
                            false
                        )
                    )
                }
            }
        }

        if (packet is S29PacketSoundEffect && hitBug) {
            if (packet.soundName.equals("game.player.hurt", ignoreCase = true) ||
                packet.soundName.equals("game.player.die", ignoreCase = true)
            ) {
                event.cancelEvent()
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        when (mode) {
            "PushGround" -> {
                pushGround()
            }
            "Push" -> {
                push()
            }
            "Reverse" -> {
                reverse()
            }
        }
    }

    val onJump = handler<JumpEvent> {
        if (mode == "Intave" && jumpIntave) {
            val player = mc.thePlayer ?: return@handler
            if (player.hurtTime == 9 && player.onGround && counter++ % 2 == 0) {
                // Let the jump happen naturally
            }
        }
    }

    val onMotion = handler<MotionEvent> {
        if (mode == "Test") {
            val player = mc.thePlayer ?: return@handler
            if (player.hurtTime > 2) {
                // Simulate strafing with very low speed
                if (player.isMoving) {
                    val speed = 0.01
                    val yaw = player.rotationYaw
                    val forward = player.moveForward
                    val strafe = player.moveStrafing
                    
                    player.motionX = -Math.sin(Math.toRadians(yaw.toDouble())) * strafe * speed
                    player.motionZ = Math.cos(Math.toRadians(yaw.toDouble())) * strafe * speed
                    
                    if (forward != 0f) {
                        player.motionX = -Math.sin(Math.toRadians((yaw + (if (forward > 0) 0 else 180)).toDouble())) * forward * speed
                        player.motionZ = Math.cos(Math.toRadians((yaw + (if (forward > 0) 0 else 180)).toDouble())) * forward * speed
                    }
                }
                
                if (player.hurtTime == 9 && player.onGround) {
                    player.jump()
                }
            }
        }

        if (mode == "Reverse" && reverseStrafe) {
            val player = mc.thePlayer ?: return@handler
            if (player.hurtTime in 1..reverseStart) {
                // Simple strafe implementation
                if (player.isMoving) {
                    val speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)
                    val yaw = player.rotationYaw
                    val forward = player.moveForward
                    val strafe = player.moveStrafing
                    
                    player.motionX = -Math.sin(Math.toRadians(yaw.toDouble())) * strafe * speed
                    player.motionZ = Math.cos(Math.toRadians(yaw.toDouble())) * strafe * speed
                    
                    if (forward != 0f) {
                        player.motionX = -Math.sin(Math.toRadians((yaw + (if (forward > 0) 0 else 180)).toDouble())) * forward * speed
                        player.motionZ = Math.cos(Math.toRadians((yaw + (if (forward > 0) 0 else 180)).toDouble())) * forward * speed
                    }
                }
            }
        }
    }

    private fun pushGround() {
        val player = mc.thePlayer ?: return
        if (player.hurtTime > 0) {
            player.onGround = true
        }
    }

    private fun push() {
        val player = mc.thePlayer ?: return
        val hurtTime = player.hurtTime
        
        val maxTime = maxOf(pushStart, pushEnd)
        val minTime = minOf(pushStart, pushEnd)
        
        if (hurtTime in minTime..maxTime) {
            player.moveFlying(0f, 0.98f, pushXZ / 100f)
            if (pushOnGround) {
                player.onGround = true
            }
        }
    }

    private fun reverse() {
        val player = mc.thePlayer ?: return
        if (player.hurtTime == reverseStart) {
            player.motionX *= -1.0
            player.motionZ *= -1.0
        }
    }

    // Extension property for Entity
    private val net.minecraft.entity.Entity.isMoving: Boolean
        get() = when (this) {
            is net.minecraft.entity.player.EntityPlayer -> {
                this.moveForward != 0f || this.moveStrafing != 0f
            }
            else -> false
        }

    // Note: The "Legit" mode logic from original code requires KillAura module
    // which is not available in this context, so it's omitted
}