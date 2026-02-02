/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.PlayerCapabilities
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C13PacketPlayerAbilities
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import kotlin.math.*

object TeleportAura : Module("TeleportAura", Category.COMBAT) {

    // 配置项 - 使用int/float/boolean代替Value
    private val packetChoices = arrayOf("PacketPosition", "PacketPosLook")
    private val packetMode by choices("PacketMode", packetChoices, "PacketPosition")
    
    private val packetBack by boolean("DoTeleportBackPacket", false)
    
    private val modeChoices = arrayOf("Aura", "Click")
    private val mode by choices("Mode", modeChoices, "Aura")
    
    private val targets by int("Targets", 3, 1..10) { mode == "Aura" }
    private val cps by int("CPS", 1, 1..20) // 改为20
    private val dist by int("Distance", 30, 20..100)
    private val moveDistance by float("MoveDistance", 5f, 2f..15f)
    private val noRegen by boolean("NoRegen", true)
    private val noLagBack by boolean("NoLagback", true)
    private val swing by boolean("Swing", true) { mode == "Aura" }
    private val pathRender by boolean("PathRender", true)
    
    // 新增预测选项
    private val predict by boolean("PredictMovement", false)
    private val predictTime by float("PredictTime", 0.5f, 0.1f..2.0f) { predict }

    // 使用数组代替列表以提高性能
    private val points = ArrayDeque<Vec3>()
    private val predictedPoints = ArrayDeque<Vec3>() // 新增：存储预测路径点
    private val attackTimer = MSTimer()
    private val delayMillis: Long get() = (1000L / cps).coerceAtLeast(50L)

    private var currentTarget: EntityLivingBase? = null

    // 重用Vec3对象减少GC压力
    private val pathBuffer = mutableListOf<Vec3>()
    private val predictedPathBuffer = mutableListOf<Vec3>() // 新增：预测路径缓冲区

    override fun onDisable() {
        points.clear()
        predictedPoints.clear()
        pathBuffer.clear()
        predictedPathBuffer.clear()
        currentTarget = null
        attackTimer.reset()
    }

    private val onWorld = handler<WorldEvent> {
        points.clear()
        predictedPoints.clear()
        pathBuffer.clear()
        predictedPathBuffer.clear()
        currentTarget = null
        attackTimer.reset()
    }

    private val onGameLoop = handler<GameLoopEvent> {
        if (!state || mc.thePlayer == null || mc.theWorld == null) return@handler
        
        // 检查延迟
        if (!attackTimer.hasTimePassed(delayMillis)) return@handler
        attackTimer.reset()
        
        when (mode) {
            "Aura" -> doAuraAttack()
            "Click" -> doClickAttack()
        }
    }

    private fun doAuraAttack() {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return
        
        // 使用while循环代替filter和sortedBy，减少对象创建
        val entities = world.loadedEntityList
        val targetList = ArrayList<EntityLivingBase>(entities.size)
        
        for (i in 0 until entities.size) {
            val entity = entities[i]
            if (entity is EntityLivingBase &&
                entity !== player &&
                entity.health > 0 &&
                EntityUtils.isSelected(entity, true)) {
                
                val dx = entity.posX - player.posX
                val dy = entity.posY - player.posY
                val dz = entity.posZ - player.posZ
                val distSq = dx * dx + dy * dy + dz * dz
                
                if (distSq < dist * dist) {
                    targetList.add(entity)
                }
            }
        }
        
        if (targetList.isEmpty()) return
        
        // 按距离排序（近到远）
        targetList.sortBy { 
            val dx = it.posX - player.posX
            val dy = it.posY - player.posY
            val dz = it.posZ - player.posZ
            dx * dx + dy * dy + dz * dz
        }
        
        var count = 0
        for (i in 0 until minOf(targetList.size, targets)) {
            if (hit(targetList[i])) {
                count++
                if (count >= targets) break
            }
        }
    }

    private fun doClickAttack() {
        val player = mc.thePlayer ?: return
        
        if (!mc.gameSettings.keyBindAttack.isKeyDown) return
        
        val entity = RaycastUtils.raycastEntity(dist.toDouble()) { target ->
            EntityUtils.isSelected(target, true) && 
            player.getDistanceToEntity(target) >= 3
        } ?: return
        
        if (entity is EntityLivingBase) {
            hit(entity, true)
        }
    }

    private fun hit(entity: EntityLivingBase, force: Boolean = false): Boolean {
        val player = mc.thePlayer ?: return false
        val netHandler = mc.netHandler ?: return false
        
        // 计算原始目标位置
        val originalPos = Vec3(entity.posX, entity.posY, entity.posZ)
        
        // 计算预测位置
        val predictedPos = if (predict) {
            calculatePredictedPosition(entity, predictTime)
        } else {
            originalPos
        }
        
        // 计算原始路径
        val originalPath = calculateDirectPath(
            player.posX, player.posY, player.posZ,
            originalPos.xCoord, originalPos.yCoord, originalPos.zCoord,
            moveDistance.toDouble()
        )
        
        if (originalPath.isEmpty()) return false
        
        // 计算预测路径（如果启用预测且预测位置不同）
        val predictedPath = if (predict && predictedPos != originalPos) {
            calculateDirectPath(
                player.posX, player.posY, player.posZ,
                predictedPos.xCoord, predictedPos.yCoord, predictedPos.zCoord,
                moveDistance.toDouble()
            )
        } else {
            originalPath
        }
        
        // 使用原始路径或预测路径（优先预测路径）
        val finalPath = if (predictedPath.isNotEmpty()) predictedPath else originalPath
        val finalTargetPos = if (predictedPath.isNotEmpty() && predict) predictedPos else originalPos
        
        if (finalPath.isEmpty()) return false
        
        // 检查路径末端距离
        val lastPoint = finalPath.last()
        val dx = finalTargetPos.xCoord - lastPoint.xCoord
        val dy = finalTargetPos.yCoord - lastPoint.yCoord
        val dz = finalTargetPos.zCoord - lastPoint.zCoord
        val lastDistance = sqrt(dx * dx + dy * dy + dz * dz)
        
        if (!force && lastDistance > 10) return false
        
        // 发送前进包
        sendPathPackets(finalPath, player, netHandler, false)
        
        // 保存路径点用于渲染
        points.clear()
        predictedPoints.clear()
        points.addAll(originalPath)
        if (predict && predictedPath.isNotEmpty() && predictedPos != originalPos) {
            predictedPoints.addAll(predictedPath)
        }
        
        // 如果需要额外返回包
        if (lastDistance > 3 && packetBack) {
            netHandler.addToSendQueue(
                C04PacketPlayerPosition(
                    finalTargetPos.xCoord,
                    finalTargetPos.yCoord,
                    finalTargetPos.zCoord,
                    true
                )
            )
        }
        
        // 攻击
        if (swing) {
            player.swingItem()
        } else {
            netHandler.addToSendQueue(C0APacketAnimation())
        }
        
        mc.playerController?.attackEntity(player, entity)
        
        // 发送返回包（反向路径）
        sendPathPackets(finalPath, player, netHandler, true)
        
        // 返回原点
        if (packetBack) {
            netHandler.addToSendQueue(
                C04PacketPlayerPosition(
                    player.posX,
                    player.posY,
                    player.posZ,
                    true
                )
            )
        }
        
        currentTarget = entity
        return true
    }
    
    /**
     * 重新设计预测算法：根据玩家转头方向和速度预测路径
     * 1. 平面移动：根据玩家水平速度和转头方向预测
     * 2. 竖向移动：根据水平移动速度预测
     */
    private fun calculatePredictedPosition(entity: EntityLivingBase, time: Float): Vec3 {
        // 获取当前速度向量
        val currentSpeedX = entity.motionX
        val currentSpeedY = entity.motionY
        val currentSpeedZ = entity.motionZ
        
        // 计算水平速度大小
        val horizontalSpeed = sqrt(currentSpeedX * currentSpeedX + currentSpeedZ * currentSpeedZ)
        
        // 计算玩家转头方向（yaw）
        val yaw = Math.toRadians(entity.rotationYaw.toDouble())
        
        // 获取玩家移动输入
        val moveForward = entity.moveForward.toDouble()
        val moveStrafing = entity.moveStrafing.toDouble()
        
        // 计算预测位置
        var predictedX = entity.posX
        var predictedY = entity.posY
        var predictedZ = entity.posZ
        
        // 计算预测ticks数（1秒=20ticks）
        val ticks = (time * 20).toInt()
        
        for (tick in 1..ticks) {
            // 计算当前tick的水平速度
            val currentHorizontalSpeed = horizontalSpeed * (1.0 - 0.05 * tick) // 逐渐减速
            
            // 根据玩家移动输入和转头方向计算移动方向
            var moveX = 0.0
            var moveZ = 0.0
            
            if (abs(moveForward) > 0.1 || abs(moveStrafing) > 0.1) {
                // 玩家有移动输入，根据转头方向计算移动向量
                val forward = if (abs(moveForward) > 0.1) moveForward / abs(moveForward) else 0.0
                val strafe = if (abs(moveStrafing) > 0.1) moveStrafing / abs(moveStrafing) else 0.0
                
                // 计算移动方向向量（考虑WASD控制）
                moveX = -sin(yaw) * forward - cos(yaw) * sin(Math.PI / 2) * strafe
                moveZ = cos(yaw) * forward - sin(yaw) * sin(Math.PI / 2) * strafe
                
                // 归一化
                val length = sqrt(moveX * moveX + moveZ * moveZ)
                if (length > 0.0) {
                    moveX /= length
                    moveZ /= length
                }
            } else {
                // 玩家没有移动输入，使用当前速度方向
                if (horizontalSpeed > 0.001) {
                    moveX = currentSpeedX / horizontalSpeed
                    moveZ = currentSpeedZ / horizontalSpeed
                }
            }
            
            // 计算水平位移
            val horizontalMove = currentHorizontalSpeed * 0.05 // 每个tick的时间是0.05秒
            predictedX += moveX * horizontalMove
            predictedZ += moveZ * horizontalMove
            
            // 计算竖向位移（根据水平移动速度预测）
            // 如果玩家在移动，假设会保持当前高度或根据地形调整
            if (horizontalSpeed > 0.1) {
                // 玩家在水平移动，检查下方是否有地面
                val world = mc.theWorld
                if (world != null) {
                    val groundPos = net.minecraft.util.BlockPos(predictedX, predictedY - 0.1, predictedZ)
                    val groundBlock = world.getBlockState(groundPos).block
                    
                    if (groundBlock.isPassable(world, groundPos)) {
                        // 脚下是空气，检查下方是否有支撑
                        val belowPos = net.minecraft.util.BlockPos(predictedX, predictedY - 1.1, predictedZ)
                        val belowBlock = world.getBlockState(belowPos).block
                        
                        if (belowBlock.isPassable(world, belowPos)) {
                            // 下方没有支撑，玩家可能会下落
                            predictedY -= 0.1 * horizontalSpeed // 下落速度与水平速度相关
                        }
                    } else {
                        // 脚下有地面，玩家可能会走上坡
                        predictedY += 0.05 * horizontalSpeed // 轻微上坡
                    }
                }
            }
            
            // 如果玩家不在移动，保持当前高度
        }
        
        return Vec3(predictedX, predictedY, predictedZ)
    }

    /**
     * 计算直接路径（直线插值）
     * 优化：避免复杂寻路算法，使用简单的直线插值
     */
    private fun calculateDirectPath(
        startX: Double, startY: Double, startZ: Double,
        targetX: Double, targetY: Double, targetZ: Double,
        stepSize: Double
    ): List<Vec3> {
        val dx = targetX - startX
        val dy = targetY - startY
        val dz = targetZ - startZ
        val totalDistance = sqrt(dx * dx + dy * dy + dz * dz)
        
        if (totalDistance <= stepSize) {
            // 距离小于步长，直接返回目标点
            return listOf(Vec3(targetX, targetY, targetZ))
        }
        
        val steps = ceil(totalDistance / stepSize).toInt()
        val stepX = dx / steps
        val stepY = dy / steps
        val stepZ = dz / steps
        
        // 重用pathBuffer避免创建新列表
        pathBuffer.clear()
        
        var lastValidPoint: Vec3? = null
        
        for (i in 1..steps) {
            val x = startX + stepX * i
            val y = startY + stepY * i
            val z = startZ + stepZ * i
            
            // 检查是否可通行
            if (isPositionPassable(x, y, z)) {
                lastValidPoint = Vec3(x, y, z)
                pathBuffer.add(lastValidPoint)
            } else {
                // 遇到障碍物，尝试简单调整
                if (lastValidPoint != null) {
                    // 尝试从上一个有效点向上调整
                    val adjustedY = lastValidPoint.yCoord + 1.0
                    if (isPositionPassable(lastValidPoint.xCoord, adjustedY, lastValidPoint.zCoord)) {
                        pathBuffer.add(Vec3(lastValidPoint.xCoord, adjustedY, lastValidPoint.zCoord))
                        
                        // 尝试继续向前
                        val nextX = x
                        val nextZ = z
                        if (isPositionPassable(nextX, adjustedY, nextZ)) {
                            pathBuffer.add(Vec3(nextX, adjustedY, nextZ))
                            continue
                        }
                    }
                }
                // 如果调整失败，返回当前已生成的路径
                break
            }
        }
        
        return if (pathBuffer.isNotEmpty()) pathBuffer.toList() else emptyList()
    }

    /**
     * 检查位置是否可通行（简化版本）
     */
    private fun isPositionPassable(x: Double, y: Double, z: Double): Boolean {
        val world = mc.theWorld ?: return false
        
        // 检查脚下方块是否有支撑
        val footPos = net.minecraft.util.BlockPos(x, y - 0.1, z)
        val footBlock = world.getBlockState(footPos).block
        if (footBlock.isPassable(world, footPos)) {
            // 脚下是空气，检查下方是否有支撑
            val belowPos = net.minecraft.util.BlockPos(x, y - 1.1, z)
            val belowBlock = world.getBlockState(belowPos).block
            if (belowBlock.isPassable(world, belowPos)) {
                return false // 下方没有支撑
            }
        }
        
        // 检查玩家位置是否有阻挡
        val bodyPos = net.minecraft.util.BlockPos(x, y + 0.9, z)
        val bodyBlock = world.getBlockState(bodyPos).block
        if (!bodyBlock.isPassable(world, bodyPos)) {
            return false // 身体位置有阻挡
        }
        
        // 检查头部位置是否有阻挡
        val headPos = net.minecraft.util.BlockPos(x, y + 1.8, z)
        val headBlock = world.getBlockState(headPos).block
        if (!headBlock.isPassable(world, headPos)) {
            return false // 头部位置有阻挡
        }
        
        return true
    }

    /**
     * 发送路径包
     */
    private fun sendPathPackets(
        path: List<Vec3>, 
        player: net.minecraft.entity.player.EntityPlayer, 
        netHandler: net.minecraft.client.network.NetHandlerPlayClient,
        reverse: Boolean
    ) {
        if (path.isEmpty()) return
        
        val indices = if (reverse) {
            path.indices.reversed()
        } else {
            path.indices
        }
        
        for (i in indices) {
            val point = path[i]
            
            when (packetMode) {
                "PacketPosition" -> {
                    netHandler.addToSendQueue(
                        C04PacketPlayerPosition(
                            point.xCoord,
                            point.yCoord,
                            point.zCoord,
                            true
                        )
                    )
                }
                "PacketPosLook" -> {
                    netHandler.addToSendQueue(
                        C03PacketPlayer.C06PacketPlayerPosLook(
                            point.xCoord,
                            point.yCoord,
                            point.zCoord,
                            player.rotationYaw,
                            player.rotationPitch,
                            true
                        )
                    )
                }
            }
        }
    }

    private val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val netHandler = mc.netHandler ?: return@handler
        
        when (event.packet) {
            is S08PacketPlayerPosLook -> {
                // 服务器强制回退时清空路径
                points.clear()
                predictedPoints.clear()
                currentTarget = null
            }
        }
        
        // 检查是否是移动包
        val isMovePacket = (event.packet is C04PacketPlayerPosition || 
                          event.packet is C03PacketPlayer.C06PacketPlayerPosLook)
        
        // NoRegen：取消非移动的C03包
        if (noRegen && event.packet is C03PacketPlayer && !isMovePacket) {
            event.cancelEvent()
        }
        
        // NoLagBack：处理服务器回退
        if (noLagBack && event.packet is S08PacketPlayerPosLook) {
            event.cancelEvent()
            
            val packet = event.packet as S08PacketPlayerPosLook
            
            // 发送允许飞行包
            val capabilities = PlayerCapabilities()
            capabilities.allowFlying = true
            netHandler.addToSendQueue(C13PacketPlayerAbilities(capabilities))
            
            // 重新发送位置包
            PacketUtils.sendPacket(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    packet.x,
                    packet.y,
                    packet.z,
                    packet.yaw,
                    packet.pitch,
                    true
                )
            )
        }
    }

    private val onRender3D = handler<Render3DEvent> {
        if ((points.isEmpty() && predictedPoints.isEmpty()) || !pathRender) return@handler
        
        val renderManager = mc.renderManager ?: return@handler
        val player = mc.thePlayer ?: return@handler
        
        val renderPosX = renderManager.viewerPosX
        val renderPosY = renderManager.viewerPosY
        val renderPosZ = renderManager.viewerPosZ
        
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glShadeModel(GL11.GL_SMOOTH)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDepthMask(false)
        
        // 设置相机变换
        mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 2)
        
        // 1. 绘制原始路径（白色线）
        if (points.isNotEmpty()) {
            GL11.glColor4f(1f, 1f, 1f, 1f) // 白色
            
            // 绘制连接线条
            GL11.glLineWidth(2f)
            GL11.glBegin(GL11.GL_LINE_STRIP)
            for (vec in points) {
                val x = vec.xCoord - renderPosX
                val y = vec.yCoord - renderPosY + player.eyeHeight.toDouble() / 2
                val z = vec.zCoord - renderPosZ
                GL11.glVertex3d(x, y, z)
            }
            GL11.glEnd()
            
            // 绘制立方体线框
            GL11.glLineWidth(1.5f)
            for (vec in points) {
                drawCubeFrame(vec, renderPosX, renderPosY, renderPosZ, player.eyeHeight.toDouble())
            }
        }
        
        // 2. 绘制预测路径（金色线）
        if (predictedPoints.isNotEmpty()) {
            GL11.glColor4f(1f, 0.843f, 0f, 1f) // 金色 (255, 215, 0)
            
            // 绘制连接线条
            GL11.glLineWidth(2f)
            GL11.glBegin(GL11.GL_LINE_STRIP)
            for (vec in predictedPoints) {
                val x = vec.xCoord - renderPosX
                val y = vec.yCoord - renderPosY + player.eyeHeight.toDouble() / 2
                val z = vec.zCoord - renderPosZ
                GL11.glVertex3d(x, y, z)
            }
            GL11.glEnd()
            
            // 绘制立方体线框
            GL11.glLineWidth(1.5f)
            for (vec in predictedPoints) {
                drawCubeFrame(vec, renderPosX, renderPosY, renderPosZ, player.eyeHeight.toDouble())
            }
        }
        
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
        GL11.glColor4f(1f, 1f, 1f, 1f)
    }
    
    /**
     * 绘制立方体线框
     */
    private fun drawCubeFrame(vec: Vec3, renderPosX: Double, renderPosY: Double, renderPosZ: Double, height: Double) {
        val x = vec.xCoord - renderPosX
        val y = vec.yCoord - renderPosY
        val z = vec.zCoord - renderPosZ
        val width = 0.2 // 稍小的立方体，避免与线条重叠
        
        // 绘制立方体线框
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(x - width, y, z - width)
        GL11.glVertex3d(x - width, y, z - width)
        GL11.glVertex3d(x - width, y + height, z - width)
        GL11.glVertex3d(x + width, y + height, z - width)
        GL11.glVertex3d(x + width, y, z - width)
        GL11.glVertex3d(x - width, y, z - width)
        GL11.glVertex3d(x - width, y, z + width)
        GL11.glEnd()
        
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(x + width, y, z + width)
        GL11.glVertex3d(x + width, y + height, z + width)
        GL11.glVertex3d(x - width, y + height, z + width)
        GL11.glVertex3d(x - width, y, z + width)
        GL11.glVertex3d(x + width, y, z + width)
        GL11.glVertex3d(x + width, y, z - width)
        GL11.glEnd()
        
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(x + width, y + height, z + width)
        GL11.glVertex3d(x + width, y + height, z - width)
        GL11.glEnd()
        
        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glVertex3d(x - width, y + height, z + width)
        GL11.glVertex3d(x - width, y + height, z - width)
        GL11.glEnd()
    }

    override val tag: String
        get() = if (predict) "$mode+P" else mode
}