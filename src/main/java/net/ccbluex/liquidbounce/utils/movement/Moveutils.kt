package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extras.Vec3d
import net.minecraft.entity.Entity
import kotlin.math.*

/**
 * 移植自 loftily 的 MoveUtils（适配 LiquidBounce）
 */
object MoveUtils : MinecraftInstance, Listenable {

    fun getSpeed(motionX: Double, motionZ: Double): Double {
        return sqrt(motionX * motionX + motionZ * motionZ)
    }

    fun getSpeed(): Double {
        val p = mc.thePlayer ?: return 0.0
        return getSpeed(p.motionX, p.motionZ)
    }

    fun getHorizontalSpeed(): Double {
        return getHorizontalSpeed(mc.thePlayer)
    }

    fun getHorizontalSpeed(entity: Entity?): Double {
        if (entity == null) return 0.0
        return sqrt(entity.motionX * entity.motionX + entity.motionZ * entity.motionZ)
    }

    fun getMovingYaw(): Float {
        return (getDirection() * 180.0 / Math.PI).toFloat()
    }

    fun getDirection(rotationYaw: Float, moveForward: Double, moveStrafe: Double): Double {
        return if (moveForward == 0.0 && moveStrafe == 0.0) {
            Math.toRadians(rotationYaw.toDouble())
        } else {
            val angle = atan2(-moveStrafe, moveForward)
            angle + Math.toRadians(rotationYaw.toDouble())
        }
    }

    fun getDirection(): Double {
        val p = mc.thePlayer ?: return 0.0
        return getDirection(p.rotationYaw, p.movementInput.moveForward.toDouble(), p.movementInput.moveStrafe.toDouble())
    }

    fun isMoving(): Boolean {
        val p = mc.thePlayer ?: return false
        return p.movementInput.moveForward != 0f || p.movementInput.moveStrafe != 0f
    }

    fun setSpeed(speed: Double, movingCheck: Boolean) {
        val p = mc.thePlayer ?: return
        if (isMoving() || !movingCheck) {
            val yaw = getDirection()
            p.motionX = -sin(yaw) * speed
            p.motionZ = cos(yaw) * speed
        }
    }

    fun getSpeedAmplifier(): Int {
        val p = mc.thePlayer ?: return 0
        return if (p.isPotionActive(1)) 1 else 0
    }

    fun strafe() {
        setSpeed(getSpeed(), true)
    }

    fun stop(y: Boolean) {
        val p = mc.thePlayer ?: return
        p.motionX = 0.0
        p.motionZ = 0.0
        if (y) p.motionY = 0.0
    }

    fun getBPS(): Double {
        val p = mc.thePlayer ?: return 0.0
        val lastPos = Vec3d(p.lastTickPosX, 0.0, p.lastTickPosZ)
        val nowPos = Vec3d(p.posX, 0.0, p.posZ)
        val dist = nowPos.distanceTo(lastPos)
        val raw = dist * 20.0 * mc.timer.timerSpeed.toDouble()
        return round(raw * 100.0) / 100.0
    }

    fun getMovementAngle(): Double {
        val p = mc.thePlayer ?: return 0.0
        val angle = Math.toDegrees(atan2(-p.moveStrafing.toDouble(), p.moveForward.toDouble()))
        return if (angle == 0.0) 0.0 else angle
    }

    fun getMotionYaw(): Float {
        val p = mc.thePlayer ?: return 0f
        val angle = Math.toDegrees(atan2(p.motionZ, p.motionX))
        return (angle.toFloat() - 90.0f)
    }
}
