package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB
import kotlin.math.cos
import kotlin.math.sin

object Entityspeed : Module("Entityspeed", Category.MOVEMENT) {
    private val distance by float("Distance", 1f, 0.5f..2.5f)
    private val speed by float("Speed", 1f, 0.01f..0.1f)
    private val followTargetOnSpace by boolean("FollowTargetOnSpace", true)
    private val multiCount by boolean("MultiCount", true)
    private val antiVoid by boolean("AntiVoid", true)

    private var offGroundTicks = 0
    private var isJumping = false

    val onUpdate = handler<MotionEvent> { event ->
        // 检查空格键状态
        val currentlyJumping = mc.gameSettings.keyBindJump.isKeyDown

        // 如果之前没有跳跃但现在按下了空格，标记为跳跃状态
        if (!isJumping && currentlyJumping) {
            isJumping = true
        }
        // 如果之前是跳跃状态但现在没有按空格，重置跳跃状态
        else if (isJumping && !currentlyJumping) {
            isJumping = false
        }

        // Update off ground ticks
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0
            // 落地后重置跳跃状态
            isJumping = false
        } else {
            offGroundTicks++
        }
    }

    val onMotionEvent = handler<MotionEvent> { event ->
        if (event.eventState != EventState.PRE) return@handler

        // 只有在跳跃状态下才执行功能
        if (!isJumping) return@handler

        val thePlayer = mc.thePlayer
        val playerBox = thePlayer.entityBoundingBox.expand(1.0, 1.0, 1.0)
        var count = 0

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityLivingBase &&
                entity !is EntityArmorStand &&
                entity.entityId != thePlayer.entityId &&
                playerBox.intersectsWith(entity.entityBoundingBox) &&
                entity.entityId != -8 &&
                entity.entityId != -1337) {

                count++
                if (multiCount) {
                    break
                }
            }
        }

        if (count > 0) {
            val strafeOffset = count * speed
            val speedOffset = count * speed

            if (thePlayer.movementInput.moveForward == 0f && thePlayer.movementInput.moveStrafe == 0f) {
                // Slow down when not pressing movement keys
                if (thePlayer.motionX > strafeOffset) {
                    thePlayer.motionX -= strafeOffset
                } else if (thePlayer.motionX < -strafeOffset) {
                    thePlayer.motionX += strafeOffset
                } else {
                    thePlayer.motionX = 0.0
                }

                if (thePlayer.motionZ > strafeOffset) {
                    thePlayer.motionZ -= strafeOffset
                } else if (thePlayer.motionZ < -strafeOffset) {
                    thePlayer.motionZ += strafeOffset
                } else {
                    thePlayer.motionZ = 0.0
                }
            }

            val yaw = getYaw()
            val mx = -sin(Math.toRadians(yaw.toDouble()))
            val mz = cos(Math.toRadians(yaw.toDouble()))

            // Apply motion in X direction
            when {
                mx < 0.0 -> {
                    if (thePlayer.motionX > strafeOffset) {
                        thePlayer.motionX -= strafeOffset
                    } else {
                        thePlayer.motionX += mx * speedOffset
                    }
                }
                mx > 0.0 -> {
                    if (thePlayer.motionX < -strafeOffset) {
                        thePlayer.motionX += strafeOffset
                    } else {
                        thePlayer.motionX += mx * speedOffset
                    }
                }
            }

            // Apply motion in Z direction
            when {
                mz < 0.0 -> {
                    if (thePlayer.motionZ > strafeOffset) {
                        thePlayer.motionZ -= strafeOffset
                    } else {
                        thePlayer.motionZ += mz * speedOffset
                    }
                }
                mz > 0.0 -> {
                    if (thePlayer.motionZ < -strafeOffset) {
                        thePlayer.motionZ += strafeOffset
                    } else {
                        thePlayer.motionZ += mz * speedOffset
                    }
                }
            }
        }
    }

    private fun getYaw(): Float {
        if (followTargetOnSpace && mc.gameSettings.keyBindJump.isKeyDown) {
            val yaw = RotationUtils.targetRotation?.yaw ?: mc.thePlayer.rotationYaw
            if (antiVoid && isVoid(yaw)) {
                return yaw + 180.0f
            }
            return yaw
        }
        return mc.thePlayer.rotationYaw
    }

    private fun isVoid(yaw: Float): Boolean {
        val mx = -sin(Math.toRadians(yaw.toDouble()))
        val mz = cos(Math.toRadians(yaw.toDouble()))
        val posX = mc.thePlayer.posX + (1.5 * mx)
        val posZ = mc.thePlayer.posZ + (1.5 * mz)

        for (i in 0..15) {
            if (!mc.theWorld.isAirBlock(
                    net.minecraft.util.BlockPos(
                        posX,
                        mc.thePlayer.posY - i,
                        posZ
                    )
                )) {
                return false
            }
        }
        return true
    }

    override fun onDisable() {
        // 重置状态
        isJumping = false
        offGroundTicks = 0
    }
}