package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.matrix

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.bypassMode
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.customYMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.jumpTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.speed
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.speedTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import kotlin.math.cos
import kotlin.math.sin

object Matrix : FlyMode("Matrix") {
    var boostMotion = 0
    override fun onUpdate() {
        if (boostMotion == 0) {
            val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
            mc.netHandler.addToSendQueue(
                C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ,
                    true
                )
            )
            if (bypassMode.equals("High")) {
                MovementUtils.strafe(5f)
                mc.thePlayer.motionY = 2.0
            } else {
                mc.netHandler.addToSendQueue(
                    C04PacketPlayerPosition(
                        mc.thePlayer.posX + -sin(yaw) * 1.5,
                        mc.thePlayer.posY + 1,
                        mc.thePlayer.posZ + cos(yaw) * 1.5,
                        false
                    )
                )
            }
            boostMotion = 1
            mc.timer.timerSpeed = jumpTimer.get()
        } else if (boostMotion == 1 && bypassMode.equals("High")) {
            MovementUtils.strafe(1.89f)
            mc.thePlayer.motionY = 2.0
        } else if (boostMotion == 2) {
            MovementUtils.strafe(speed.get())
            when (bypassMode.get().lowercase()) {
                "stable" -> mc.thePlayer.motionY = 0.8
                "new" -> mc.thePlayer.motionY = 0.48
                "high" -> {
                    val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
                    mc.netHandler.addToSendQueue(
                        C04PacketPlayerPosition(
                            mc.thePlayer.posX + -sin(yaw) * 2,
                            mc.thePlayer.posY + 2.0,
                            mc.thePlayer.posZ + cos(yaw) * 2,
                            true
                        )
                    )
                    mc.thePlayer.motionY = 2.0
                    MovementUtils.strafe(1.89f)
                }

                "custom" -> mc.thePlayer.motionY = customYMotion.get().toDouble()
            }
            boostMotion = 3
        } else if (boostMotion < 5) {
            boostMotion++
        } else if (boostMotion >= 5) {
            mc.timer.timerSpeed = speedTimer.get()
        }
    }

    override fun onEnable() {
        boostMotion = 0
    }

    override fun onDisable() {
        boostMotion = 0
    }
}