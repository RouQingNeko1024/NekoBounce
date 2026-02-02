package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.matrix

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.canBoost1
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.height
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.jumpCounter
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.jumpDamage
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.lastMotionX
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.lastMotionY
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.lastMotionZ
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.receivedFlag
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.speedF
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object MatrixBoost : FlyMode("MatrixBoost") {

    override fun onEnable() {
        jumpCounter = 0
        canBoost1 = false
        receivedFlag = false
    }

    override fun onUpdate() {
        if (canBoost1) {
            MovementUtils.setSpeed(speedF.get().toDouble(), false)
            mc.thePlayer.motionY = height.get().toDouble()
        }

        if (mc.thePlayer.hurtTime >= 1 && mc.thePlayer.hurtTime <= 8) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.tryJump()
            } else {
                if (mc.thePlayer.motionY < 0.2) {
                    canBoost1 = true
                }
            }
        }
        if (jumpCounter < 4 && jumpDamage.get()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.tryJump()
                jumpCounter += 1
            }
        }
    }

    override fun onMotion(event: MotionEvent) {
        if (jumpCounter < 4 && jumpDamage.get()) {
            mc.thePlayer.onGround = true
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook) {
            receivedFlag = true
            lastMotionX = mc.thePlayer.motionX
            lastMotionY = mc.thePlayer.motionY
            lastMotionZ = mc.thePlayer.motionZ
            canBoost1 = false
        }
        if (event.packet is C03PacketPlayer.C06PacketPlayerPosLook && receivedFlag) {
            receivedFlag = false
            mc.thePlayer.motionX = lastMotionX
            mc.thePlayer.motionY = lastMotionY
            mc.thePlayer.motionZ = lastMotionZ
        }
    }
}