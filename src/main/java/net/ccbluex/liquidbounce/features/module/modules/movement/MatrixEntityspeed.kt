package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object MatrixEntityspeed : Module("MatrixEntityspeed", Category.MOVEMENT) {
    private val distance by float("Distance", 1f, 0.5f..2.5f)
    private val speed by float("Speed", 1f, 0.01f..0.1f)
    private val followTargetOnSpace by boolean("FollowTargetOnSpace", true)
    private val multiCount by boolean("MultiCount", true)
    private val antiVoid by boolean("AntiVoid", true)
    
    // 概率速度相关设置
    private val enableProbabilitySpeed by boolean("ProbabilitySpeed", false)
    private val probability by float("Probability", 50f, 0f..100f) { enableProbabilitySpeed }
    private val speedDuration by float("SpeedDuration", 0.5f, 0.1f..2f) { enableProbabilitySpeed }
    private val probabilityMultiplier by float("ProbabilityMultiplier", 1.5f, 1f..3f) { enableProbabilitySpeed }

    // Tick周期加速相关设置
    private val enableTickBoost by boolean("TickBoost", false)
    private val boostInterval by int("BoostInterval", 100, 1..1000) { enableTickBoost }
    private val boostDuration by int("BoostDuration", 50, 1..1000) { enableTickBoost }

    private var offGroundTicks = 0
    private var isJumping = false
    
    // 概率速度相关变量
    private var hasSpeedBoost = false
    private var speedBoostEndTime = 0L
    private val probabilityTimer = MSTimer()
    private var lastCheckTime = 0L

    // Tick周期加速相关变量
    private var tickCounter = 0
    private var isTickBoostActive = false
    private var boostRemainingTicks = 0
    private var wasInRange = false

    // 添加标签显示
    override val tag: String?
        get() {
            val tags = mutableListOf<String>()
            
            if (enableProbabilitySpeed) {
                val currentTime = System.currentTimeMillis()
                if (hasSpeedBoost) {
                    val remainingTime = (speedBoostEndTime - currentTime) / 1000.0
                    tags.add(String.format("P:%.1fs", remainingTime))
                } else {
                    val timeSinceLastCheck = (currentTime - lastCheckTime) / 1000.0
                    tags.add(String.format("P:%.1fs", 1.0 - timeSinceLastCheck))
                }
            }
            
            if (enableTickBoost) {
                if (isTickBoostActive) {
                    tags.add("T:$boostRemainingTicks")
                } else {
                    tags.add("T:$tickCounter/$boostInterval")
                }
            }
            
            return if (tags.isEmpty()) null else tags.joinToString(" ")
        }

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

        val thePlayer = mc.thePlayer ?: return@handler
        val playerBox = thePlayer.entityBoundingBox.expand(1.0, 1.0, 1.0)
        var count = 0
        var inRange = false

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityLivingBase &&
                entity !is EntityArmorStand &&
                entity.entityId != thePlayer.entityId &&
                playerBox.intersectsWith(entity.entityBoundingBox) &&
                entity.entityId != -8 &&
                entity.entityId != -1337) {

                count++
                inRange = true
                if (multiCount) {
                    break
                }
            }
        }

        // Tick周期加速逻辑
        if (enableTickBoost) {
            tickCounter++
            
            if (!isTickBoostActive) {
                // 检查是否应该开始加速
                if (inRange && !wasInRange && tickCounter >= boostInterval) {
                    isTickBoostActive = true
                    boostRemainingTicks = boostDuration
                    tickCounter = 0
                }
            } else {
                // 更新加速剩余时间
                boostRemainingTicks--
                if (boostRemainingTicks <= 0) {
                    isTickBoostActive = false
                }
            }
            
            wasInRange = inRange
        }

        // 概率速度逻辑
        val currentTime = System.currentTimeMillis()
        
        if (enableProbabilitySpeed) {
            // 每秒检查一次概率
            if (currentTime - lastCheckTime >= 1000) {
                lastCheckTime = currentTime
                val randomValue = Random.nextFloat() * 100f
                
                if (randomValue <= probability) {
                    // 触发速度提升
                    hasSpeedBoost = true
                    speedBoostEndTime = currentTime + (speedDuration * 1000).toLong()
                } else {
                    // 没有速度提升
                    hasSpeedBoost = false
                }
            }
            
            // 检查速度提升是否已过期
            if (hasSpeedBoost && currentTime > speedBoostEndTime) {
                hasSpeedBoost = false
            }
        }

        // 确定是否应该应用加速
        val shouldApplyBoost = when {
            enableTickBoost -> isTickBoostActive
            else -> count > 0
        }

        if (shouldApplyBoost) {
            // 基础速度计算
            val baseStrafeOffset = count * speed
            val baseSpeedOffset = count * speed
            
            // 应用概率速度乘数
            val strafeOffset = if (hasSpeedBoost) baseStrafeOffset * probabilityMultiplier else baseStrafeOffset
            val speedOffset = if (hasSpeedBoost) baseSpeedOffset * probabilityMultiplier else baseSpeedOffset

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
        val thePlayer = mc.thePlayer ?: return 0f
        
        if (followTargetOnSpace && mc.gameSettings.keyBindJump.isKeyDown) {
            val yaw = RotationUtils.targetRotation?.yaw ?: thePlayer.rotationYaw
            if (antiVoid && isVoid(yaw)) {
                return yaw + 180.0f
            }
            return yaw
        }
        return thePlayer.rotationYaw
    }

    private fun isVoid(yaw: Float): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        
        val mx = -sin(Math.toRadians(yaw.toDouble()))
        val mz = cos(Math.toRadians(yaw.toDouble()))
        val posX = thePlayer.posX + (1.5 * mx)
        val posZ = thePlayer.posZ + (1.5 * mz)

        for (i in 0..15) {
            if (!mc.theWorld.isAirBlock(
                    net.minecraft.util.BlockPos(
                        posX,
                        thePlayer.posY - i,
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
        
        // 重置概率速度状态
        hasSpeedBoost = false
        lastCheckTime = 0L
        probabilityTimer.reset()
        
        // 重置Tick周期加速状态
        tickCounter = 0
        isTickBoostActive = false
        boostRemainingTicks = 0
        wasInRange = false
    }
    
    override fun onEnable() {
        // 初始化概率速度计时器
        lastCheckTime = System.currentTimeMillis()
        probabilityTimer.reset()
        
        // 初始化Tick周期加速状态
        tickCounter = 0
        isTickBoostActive = false
        boostRemainingTicks = 0
        wasInRange = false
    }
}