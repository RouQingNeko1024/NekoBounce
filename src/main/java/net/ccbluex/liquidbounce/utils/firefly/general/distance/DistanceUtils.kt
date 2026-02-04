package net.ccbluex.liquidbounce.utils.firefly.general.distance

import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.minecraft.entity.Entity
import kotlin.math.abs
import kotlin.math.sqrt

object DistanceUtils : MinecraftInstance {
    /**
     * 计算从玩家当前位置到距离敌人指定距离的最近点的距离
     * @param playerX 玩家X坐标
     * @param playerY 玩家Y坐标
     * @param playerZ 玩家Z坐标
     * @param target 目标敌人实体
     * @param targetDistance 期望的与敌人的距离（默认3.5格）
     * @param useTargetEyeHeight 是否使用敌人的眼睛高度
     * @return 到距离敌人指定距离的最近点的距离，如果无法计算则返回 Double.MAX_VALUE
     */
    fun distanceToTargetDistancePoint(
        playerX: Double,
        playerY: Double,
        playerZ: Double,
        target: Entity,
        targetDistance: Double = 3.5,
        useTargetEyeHeight: Boolean = true
    ): Double {
        // 敌人位置
        val targetX = target.posX
        val targetY = target.posY + if (useTargetEyeHeight) target.eyeHeight.toDouble() else 0.0
        val targetZ = target.posZ

        // 计算玩家到敌人的向量
        val dx = targetX - playerX
        val dy = targetY - playerY
        val dz = targetZ - playerZ

        // 玩家到敌人的当前距离
        val currentDistance = sqrt(dx * dx + dy * dy + dz * dz)

        // 如果当前距离已经小于等于目标距离，直接返回0（已经满足条件）
        if (currentDistance <= targetDistance) {
            return 0.0
        }

        // 归一化向量
        val normalizedDx = dx / currentDistance
        val normalizedDy = dy / currentDistance
        val normalizedDz = dz / currentDistance

        // 计算到敌人的目标距离点的坐标
        val targetPointX = targetX - normalizedDx * targetDistance
        val targetPointY = targetY - normalizedDy * targetDistance
        val targetPointZ = targetZ - normalizedDz * targetDistance

        // 计算玩家到目标点的距离
        val playerToPointX = targetPointX - playerX
        val playerToPointY = targetPointY - playerY
        val playerToPointZ = targetPointZ - playerZ

        return sqrt(playerToPointX * playerToPointX + playerToPointY * playerToPointY + playerToPointZ * playerToPointZ)
    }

    /**
     * 快捷方法：使用玩家当前位置计算
     */
    fun distanceToTargetDistancePoint(
        target: Entity,
        targetDistance: Double = 3.5,
        useEyeHeight: Boolean = true,
        useTargetEyeHeight: Boolean = true
    ): Double {
        val player = mc.thePlayer ?: return Double.MAX_VALUE
        val playerX = player.posX
        val playerY = player.posY + if (useEyeHeight) player.getEyeHeight().toDouble()else 0.0
        val playerZ = player.posZ

        return distanceToTargetDistancePoint(
            playerX, playerY, playerZ,
            target, targetDistance, useTargetEyeHeight
        )
    }

    /**
     * 获取距离敌人指定距离的点的坐标
     * @param playerX 玩家X坐标
     * @param playerY 玩家Y坐标
     * @param playerZ 玩家Z坐标
     * @param target 目标敌人
     * @param targetDistance 期望的与敌人的距离
     * @param useTargetEyeHeight 是否使用敌人的眼睛高度
     * @return 三维坐标 Point(x, y, z)
     */
    fun getPointAtTargetDistance(
        playerX: Double,
        playerY: Double,
        playerZ: Double,
        target: Entity,
        targetDistance: Double = 3.5,
        useTargetEyeHeight: Boolean = true
    ): Point {
        // 敌人位置
        val targetX = target.posX
        val targetY = target.posY + if (useTargetEyeHeight) target.eyeHeight.toDouble() else 0.0
        val targetZ = target.posZ

        // 计算玩家到敌人的向量
        val dx = targetX - playerX
        val dy = targetY - playerY
        val dz = targetZ - playerZ

        // 玩家到敌人的当前距离
        val currentDistance = sqrt(dx * dx + dy * dy + dz * dz)

        // 归一化向量
        val normalizedDx = if (currentDistance > 0) dx / currentDistance else 0.0
        val normalizedDy = if (currentDistance > 0) dy / currentDistance else 0.0
        val normalizedDz = if (currentDistance > 0) dz / currentDistance else 0.0

        // 计算到敌人的目标距离点的坐标
        val targetPointX = targetX - normalizedDx * targetDistance
        val targetPointY = targetY - normalizedDy * targetDistance
        val targetPointZ = targetZ - normalizedDz * targetDistance

        return Point(targetPointX, targetPointY, targetPointZ)
    }

    /**
     * 快捷方法：使用玩家当前位置
     */
    fun getPointAtTargetDistance(
        target: Entity,
        targetDistance: Double = 3.5,
        useTargetEyeHeight: Boolean = true
    ): Point {
        val player = mc.thePlayer ?: return Point.ZERO
        val playerX = player.posX
        val playerY = player.posY + player.getEyeHeight()
        val playerZ = player.posZ

        return getPointAtTargetDistance(
            playerX, playerY, playerZ,
            target, targetDistance, useTargetEyeHeight
        )
    }

    /**
     * 计算水平面上（忽略Y轴）到距离敌人指定距离的点的距离
     * @param playerX 玩家X坐标
     * @param playerZ 玩家Z坐标
     * @param target 目标敌人
     * @param targetDistance 期望的水平距离
     * @return 到目标点的水平距离
     */
    fun distanceToTargetHorizontalPoint(
        playerX: Double,
        playerZ: Double,
        target: Entity,
        targetDistance: Double = 3.5
    ): Double {
        // 敌人位置（忽略Y轴）
        val targetX = target.posX
        val targetZ = target.posZ

        // 计算玩家到敌人的水平向量
        val dx = targetX - playerX
        val dz = targetZ - playerZ

        // 玩家到敌人的当前水平距离
        val currentDistance = sqrt(dx * dx + dz * dz)

        // 如果当前距离已经小于等于目标距离，直接返回0
        if (currentDistance <= targetDistance) {
            return 0.0
        }

        // 归一化水平向量
        val normalizedDx = dx / currentDistance
        val normalizedDz = dz / currentDistance

        // 计算到敌人的目标水平距离点的坐标
        val targetPointX = targetX - normalizedDx * targetDistance
        val targetPointZ = targetZ - normalizedDz * targetDistance

        // 计算玩家到目标点的水平距离
        val playerToPointX = targetPointX - playerX
        val playerToPointZ = targetPointZ - playerZ

        return sqrt(playerToPointX * playerToPointX + playerToPointZ * playerToPointZ)
    }

    /**
     * 快捷方法：使用玩家当前位置
     */
    fun distanceToTargetHorizontalPoint(
        target: Entity,
        targetDistance: Double = 3.5
    ): Double {
        val player = mc.thePlayer ?: return Double.MAX_VALUE
        val playerX = player.posX
        val playerZ = player.posZ

        return distanceToTargetHorizontalPoint(playerX, playerZ, target, targetDistance)
    }

    /**
     * 计算点到实体的距离
     * @param pointX 点的X坐标
     * @param pointY 点的Y坐标
     * @param pointZ 点的Z坐标
     * @param entity 目标实体
     * @param useEyeHeight 是否使用实体的眼睛高度作为Y坐标，false则使用脚部Y坐标
     * @param squared 是否返回平方距离（性能更好，适合仅用于比较大小）
     * @return 点到实体的距离（或平方距离）
     */
    fun distanceToEntity(
        pointX: Double,
        pointY: Double,
        pointZ: Double,
        entity: Entity,
        useEyeHeight: Boolean = false,
        squared: Boolean = false
    ): Double {
        val entityY = if (useEyeHeight) entity.posY + entity.eyeHeight else entity.posY
        val entityPosX = entity.posX
        val entityPosY = entityY
        val entityPosZ = entity.posZ

        val dx = pointX - entityPosX
        val dy = pointY - entityPosY
        val dz = pointZ - entityPosZ

        val distanceSq = dx * dx + dy * dy + dz * dz

        return if (squared) distanceSq else sqrt(distanceSq)
    }

    /**
     * 计算点到实体包围盒的距离（考虑实体的碰撞箱）
     * @param pointX 点的X坐标
     * @param pointY 点的Y坐标
     * @param pointZ 点的Z坐标
     * @param entity 目标实体
     * @return 点到实体碰撞箱表面的最近距离
     */
    fun distanceToEntityBoundingBox(
        pointX: Double,
        pointY: Double,
        pointZ: Double,
        entity: Entity
    ): Double {
        val boundingBox = entity.entityBoundingBox ?: return Double.MAX_VALUE

        // 计算点到包围盒各面的最近点
        val closestX = pointX.coerceIn(boundingBox.minX, boundingBox.maxX)
        val closestY = pointY.coerceIn(boundingBox.minY, boundingBox.maxY)
        val closestZ = pointZ.coerceIn(boundingBox.minZ, boundingBox.maxZ)

        // 计算最近点到原点的距离
        val dx = pointX - closestX
        val dy = pointY - closestY
        val dz = pointZ - closestZ

        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * 计算两个实体之间的距离
     * @param entity1 第一个实体
     * @param entity2 第二个实体
     * @param useEyeHeight1 是否使用第一个实体的眼睛高度
     * @param useEyeHeight2 是否使用第二个实体的眼睛高度
     * @param squared 是否返回平方距离
     * @return 实体之间的距离
     */
    fun distanceBetweenEntities(
        entity1: Entity,
        entity2: Entity,
        useEyeHeight1: Boolean = false,
        useEyeHeight2: Boolean = false,
        squared: Boolean = false
    ): Double {
        val y1 = if (useEyeHeight1) entity1.posY + entity1.eyeHeight else entity1.posY
        val y2 = if (useEyeHeight2) entity2.posY + entity2.eyeHeight else entity2.posY

        val dx = entity1.posX - entity2.posX
        val dy = y1 - y2
        val dz = entity1.posZ - entity2.posZ

        val distanceSq = dx * dx + dy * dy + dz * dz

        return if (squared) distanceSq else sqrt(distanceSq)
    }

    /**
     * 计算需要移动的距离来达到距离敌人指定距离
     * @param playerX 玩家X坐标
     * @param playerY 玩家Y坐标
     * @param playerZ 玩家Z坐标
     * @param target 目标敌人
     * @param targetDistance 期望的与敌人的距离
     * @param useEyeHeight 是否使用眼睛高度
     * @return 需要移动的距离，负数表示需要后退，正数表示需要前进
     */
    fun getMovementDistanceToTargetDistance(
        playerX: Double,
        playerY: Double,
        playerZ: Double,
        target: Entity,
        targetDistance: Double = 3.5,
        useEyeHeight: Boolean = true
    ): Double {
        // 当前距离
        val currentDistance = distanceToEntity(
            playerX, playerY, playerZ,
            target, useEyeHeight
        )

        // 需要移动的距离
        return currentDistance - targetDistance
    }

    /**
     * 快捷方法：使用玩家当前位置
     */
    fun getMovementDistanceToTargetDistance(
        target: Entity,
        targetDistance: Double = 3.5,
        useEyeHeight: Boolean = true
    ): Double {
        val player = mc.thePlayer ?: return 0.0
        val playerX = player.posX
        val playerY = player.posY + if (useEyeHeight) player.getEyeHeight().toDouble()else 0.0
        val playerZ = player.posZ

        return getMovementDistanceToTargetDistance(
            playerX, playerY, playerZ,
            target, targetDistance, useEyeHeight
        )
    }

    /**
     * 判断点是否在实体的攻击范围内
     * @param pointX 点X坐标
     * @param pointY 点Y坐标
     * @param pointZ 点Z坐标
     * @param entity 实体
     * @param attackRange 攻击范围（默认3.0）
     * @param useEyeHeight 是否使用眼睛高度
     * @param considerVertical 是否考虑垂直距离
     * @param verticalTolerance 垂直容差（当considerVertical为true时有效）
     * @return 是否在攻击范围内
     */
    fun isInAttackRange(
        pointX: Double,
        pointY: Double,
        pointZ: Double,
        entity: Entity,
        attackRange: Double = 3.0,
        useEyeHeight: Boolean = true,
        considerVertical: Boolean = false,
        verticalTolerance: Double = 2.0
    ): Boolean {
        val distance = distanceToEntity(pointX, pointY, pointZ, entity, useEyeHeight, squared = true)

        if (!considerVertical) {
            return distance <= attackRange * attackRange
        }

        // 检查水平距离
        if (distance > attackRange * attackRange) {
            return false
        }

        // 检查垂直距离
        val entityY = if (useEyeHeight) entity.posY + entity.eyeHeight else entity.posY
        val verticalDistance = abs(pointY - entityY)
        return verticalDistance <= verticalTolerance
    }

    /**
     * 计算点到线段的最近距离
     * @param point 点的坐标
     * @param lineStart 线段起点
     * @param lineEnd 线段终点
     * @return 点到线段的最近距离
     */
    fun distanceToLineSegment(point: Point, lineStart: Point, lineEnd: Point): Double {
        val lineVector = lineEnd - lineStart
        val pointVector = point - lineStart

        val lineLengthSquared = lineVector.lengthSquared()
        if (lineLengthSquared == 0.0) {
            return point.distanceTo(lineStart)
        }

        // 计算投影参数 t
        val t = (pointVector dot lineVector) / lineLengthSquared

        return when {
            t < 0.0 -> point.distanceTo(lineStart)
            t > 1.0 -> point.distanceTo(lineEnd)
            else -> {
                val projection = lineStart + (lineVector * t)
                point.distanceTo(projection)
            }
        }
    }
}