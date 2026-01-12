package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.movement.MoveUtils
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.*

// 定义移动状态数据类
data class MovingState(val forward: Boolean, val back: Boolean, val left: Boolean, val right: Boolean)

object CollideBoost : Module("CollideBoost", Category.MOVEMENT) {
    private val mode by choices("Mode", arrayOf("Matrix2", "Matrix1", "Grim"), "Matrix2")
    private val speed by float("Speed", 0.16f, 0.01f..0.15f)
    
    // 添加触发距离选项
    private val triggerDistance by float("TriggerDistance", 1.0f, 0.1f..3.0f)

    private var offGroundTicks = 0
    private var isJumping = false
    private val timer = TickTimer()

    // 获取玩家当前移动状态
    private fun getMovingState(): MovingState {
        val gameSettings = mc.gameSettings
        return MovingState(
            forward = gameSettings.keyBindForward.isKeyDown,
            back = gameSettings.keyBindBack.isKeyDown,
            left = gameSettings.keyBindLeft.isKeyDown,
            right = gameSettings.keyBindRight.isKeyDown
        )
    }

    override fun onEnable() {
        timer.reset()
    }

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.PRE) return@handler

        val thePlayer = mc.thePlayer
        val targets = getTargets()

        // 检查玩家是否正在移动，如果没有移动则不触发加速
        // 使用MoveUtil.getSpeed()来获取实际移动速度，更准确
        val currentSpeed = sqrt(thePlayer.motionX * thePlayer.motionX + thePlayer.motionZ * thePlayer.motionZ)
        if (currentSpeed < 0.01) return@handler

        // 检查玩家是否在按键移动，避免静止时的误触发
        val movingState = getMovingState()
        if (!movingState.forward && !movingState.back && !movingState.left && !movingState.right) return@handler

        // 获取玩家当前朝向（根据键盘输入确定实际移动方向）
        val (xt, zt) = when {
            movingState.forward -> {
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                (-sin(yaw) to cos(yaw))
            }
            movingState.back -> {
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                (sin(yaw) to -cos(yaw))  // 倒退方向
            }
            movingState.left -> {
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                (-sin(yaw - Math.PI/2) to cos(yaw - Math.PI/2))  // 左移方向
            }
            movingState.right -> {
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                (-sin(yaw + Math.PI/2) to cos(yaw + Math.PI/2))  // 右移方向
            }
            else -> {
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                (-sin(yaw) to cos(yaw))
            }
        }

        when (mode) {
            "Matrix2" -> funtimeBoost(targets, xt, zt)
            "Matrix1" -> matrixBoost(targets, xt, zt)
            "Grim" -> grimBoost(targets, xt, zt)
        }
    }

    /* Funtime：直接给速度 */
    private fun funtimeBoost(targets: List<EntityLivingBase>, xt: Double, zt: Double) {
        for (ent in targets) {
            // 使用可配置的触发距离
            val expandedBox = mc.thePlayer.entityBoundingBox.expand(triggerDistance.toDouble(), triggerDistance.toDouble(), triggerDistance.toDouble())
            if (expandedBox.intersectsWith(ent.entityBoundingBox)) {
                mc.thePlayer.motionX += xt * speed
                mc.thePlayer.motionZ += zt * speed
                break // 只加速一次
            }
        }
    }

    /* Matrix：根据碰撞箱重叠体积计算"反弹"速度 */
    private fun matrixBoost(targets: List<EntityLivingBase>, xt: Double, zt: Double) {
        for (ent in targets) {
            // 使用可配置的触发距离
            val expandedBox = mc.thePlayer.entityBoundingBox.expand(triggerDistance.toDouble(), triggerDistance.toDouble(), triggerDistance.toDouble())
            if (expandedBox.intersectsWith(ent.entityBoundingBox)) {
                // 计算重叠体积 -> 速度倍率
                val deltaX = min(
                    mc.thePlayer.entityBoundingBox.maxX,
                    ent.entityBoundingBox.maxX
                ) - max(mc.thePlayer.entityBoundingBox.minX, ent.entityBoundingBox.minX)
                val deltaZ = min(
                    mc.thePlayer.entityBoundingBox.maxZ,
                    ent.entityBoundingBox.maxZ
                ) - max(mc.thePlayer.entityBoundingBox.minZ, ent.entityBoundingBox.minZ)

                val intersectionArea = deltaX * deltaZ
                val mult = intersectionArea * 0.045

                mc.thePlayer.motionX += xt * mult
                mc.thePlayer.motionZ += zt * mult
                break
            }
        }
    }

    /* Grim：统计重叠实体数量，按数量线性给速度 */
    private fun grimBoost(targets: List<EntityLivingBase>, xt: Double, zt: Double) {
        var hit = 0L
        for (ent in targets) {
            // 使用可配置的触发距离
            val expandedBox = mc.thePlayer.entityBoundingBox.expand(triggerDistance.toDouble(), triggerDistance.toDouble(), triggerDistance.toDouble())
            if (expandedBox.intersectsWith(ent.entityBoundingBox)) {
                hit++
            }
        }
        val base = 0.05 * hit

        mc.thePlayer.motionX += xt * base
        mc.thePlayer.motionZ += zt * base
    }

    /* 工具：获取可碰撞的 LivingEntity（非自己、非 NPC、非 Bot） */
    private fun getTargets(): List<EntityLivingBase> {
        val player = mc.thePlayer ?: return emptyList()
        val world = mc.theWorld ?: return emptyList()

        return world.loadedEntityList.filterIsInstance<EntityLivingBase>().filter { entity ->
            entity != player &&
                    entity !is EntityArmorStand &&
                    entity.entityId != -8 && // 排除某些特殊实体
                    entity.entityId != -1337 &&
                    entity.isEntityAlive &&
                    !isBot(entity)
        }
    }

    private fun isBot(entity: EntityLivingBase): Boolean {
        // 简单的机器人检测，可根据需要扩展
        return entity.name.contains("Bot", ignoreCase = true)
    }

    override fun onDisable() {
        // 重置状态
        isJumping = false
        offGroundTicks = 0
    }
    
    override val tag: String
        get() = mode
}