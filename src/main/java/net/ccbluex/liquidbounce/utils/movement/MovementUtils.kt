/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion
import net.minecraft.util.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MovementUtils : MinecraftInstance, Listenable {

    var affectSprintOnAttack: Boolean? = null

    var speed
        get() = mc.thePlayer?.run { sqrt(motionX * motionX + motionZ * motionZ).toFloat() } ?: .0f
        set(value) {
            strafe(value)
        }

    val hasMotion
        get() = mc.thePlayer?.run { motionX != .0 || motionY != .0 || motionZ != .0 } == true

    var airTicks = 0
    var groundTicks = 0

    // 添加 isMoving 函数
    fun isMoving(): Boolean {
        return mc.thePlayer?.let { 
            it.movementInput.moveForward != 0f || it.movementInput.moveStrafe != 0f 
        } ?: false
    }

    @JvmOverloads
    fun strafe(
        speed: Float = MovementUtils.speed, stopWhenNoInput: Boolean = false, moveEvent: MoveEvent? = null,
        strength: Double = 1.0,
    ) =
        mc.thePlayer?.run {
            if (!isMoving()) {
                if (stopWhenNoInput) {
                    moveEvent?.zeroXZ()
                    stopXZ()
                }

                return@run
            }

            val prevX = motionX * (1.0 - strength)
            val prevZ = motionZ * (1.0 - strength)
            val useSpeed = speed * strength

            val yaw = direction
            val x = (-sin(yaw) * useSpeed) + prevX
            val z = (cos(yaw) * useSpeed) + prevZ

            if (moveEvent != null) {
                moveEvent.x = x
                moveEvent.z = z
            }

            motionX = x
            motionZ = z
        }

    fun getBaseMoveSpeed(): Double {
        var baseSpeed = mc.thePlayer.capabilities.walkSpeed * 2.873

        mc.thePlayer.getActivePotionEffect(Potion.moveSlowdown)?.let { effect ->
            baseSpeed /= 1.0 + 0.2 * (effect.amplifier + 1)
        }

        mc.thePlayer.getActivePotionEffect(Potion.moveSpeed)?.let { effect ->
            baseSpeed *= 1.0 + 0.2 * (effect.amplifier + 1)
        }

        return baseSpeed
    }
    
    fun move(speed: Float) {
        if (isMoving()) {
            var rotationYaw = mc.thePlayer.rotationYaw

            // 处理后退时的角度调整
            if (mc.thePlayer.movementInput.moveForward < 0.0F) {
                rotationYaw += 180.0F
            }

            // 根据移动方向调整角度
            var forward = 1.0F
            when {
                mc.thePlayer.movementInput.moveForward < 0.0F -> forward = -0.5F
                mc.thePlayer.movementInput.moveForward > 0.0F -> forward = 0.5F
            }

            // 处理左右平移时的角度调整
            when {
                mc.thePlayer.movementInput.moveStrafe > 0.0F -> rotationYaw -= 90.0F * forward
                mc.thePlayer.movementInput.moveStrafe < 0.0F -> rotationYaw += 90.0F * forward
            }

            // 计算移动向量并应用
            val yaw = Math.toRadians(rotationYaw.toDouble())
            mc.thePlayer.motionX += -sin(yaw) * speed
            mc.thePlayer.motionZ += cos(yaw) * speed
        }
    }
    
    fun resetMotion(y: Boolean) {
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        if (y) mc.thePlayer.motionY = 0.0
    }

    fun Vec3.strafe(
        yaw: Float = direction.toDegreesF(), speed: Double = sqrt(xCoord * xCoord + zCoord * zCoord),
        strength: Double = 1.0,
        moveCheck: Boolean = false,
    ): Vec3 {
        if (moveCheck) {
            xCoord = 0.0
            zCoord = 0.0
            return this
        }

        val prevX = xCoord * (1.0 - strength)
        val prevZ = zCoord * (1.0 - strength)
        val useSpeed = speed * strength

        val angle = Math.toRadians(yaw.toDouble())
        xCoord = (-sin(angle) * useSpeed) + prevX
        zCoord = (cos(angle) * useSpeed) + prevZ
        return this
    }

    fun forward(distance: Double) =
        mc.thePlayer?.run {
            val yaw = rotationYaw.toRadiansD()
            setPosition(posX - sin(yaw) * distance, posY, posZ + cos(yaw) * distance)
        }

    val direction
        get() = mc.thePlayer?.run {
            var yaw = rotationYaw
            var forward = 1f

            if (movementInput.moveForward < 0f) {
                yaw += 180f
                forward = -0.5f
            } else if (movementInput.moveForward > 0f)
                forward = 0.5f

            if (movementInput.moveStrafe < 0f) yaw += 90f * forward
            else if (movementInput.moveStrafe > 0f) yaw -= 90f * forward

            yaw.toRadiansD()
        } ?: 0.0

    fun isOnGround(height: Double) =
        mc.theWorld != null && mc.thePlayer != null &&
            mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer,
                mc.thePlayer.entityBoundingBox.offset(Vec3_ZERO.withY(-height))
            ).isNotEmpty()

    fun getPlayerDirection(rotationYaw: Float,moveForward: Double,moveStrafe: Double): Double {
        if (moveForward != 0.0 && moveStrafe != 0.0) {
            return Math.toRadians(rotationYaw.toDouble())
        }
        val angle = atan2(-moveStrafe, moveForward)
        return angle + Math.toRadians(rotationYaw.toDouble())
    }
    
    fun getPlayerDirection(): Double {
        return getPlayerDirection(mc.thePlayer.rotationYaw, 
            mc.thePlayer.movementInput.moveForward.toDouble(),
            mc.thePlayer.movementInput.moveStrafe.toDouble()
        )
    }

    fun setSpeed(speed: Double, movingCheck: Boolean) {
        if (!isMoving() && movingCheck) return

        val yaw = getPlayerDirection()
        mc.thePlayer.motionX = -sin(yaw) * speed
        mc.thePlayer.motionZ = cos(yaw) * speed
    }
    
    var serverOnGround = false
    var serverX = .0
    var serverY = .0
    var serverZ = .0

    val onPacket = handler<PacketEvent> { event ->
        if (event.isCancelled)
            return@handler

        val packet = event.packet

        if (packet is C03PacketPlayer) {
            serverOnGround = packet.onGround

            if (packet.isMoving) {
                serverX = packet.x
                serverY = packet.y
                serverZ = packet.z
            }
        }
    }

}