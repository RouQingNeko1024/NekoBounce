/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
//Code By Lizz
//Skid Neko
package io.qzz.nekobounce.features.module.modules.world

import io.qzz.nekobounce.event.*
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.features.module.modules.combat.KillAura
import io.qzz.nekobounce.ui.font.Fonts
import io.qzz.nekobounce.utils.block.*
import io.qzz.nekobounce.utils.block.BlockUtils.getBlockName
import io.qzz.nekobounce.utils.block.BlockUtils.getCenterDistance
import io.qzz.nekobounce.utils.block.BlockUtils.isBlockBBValid
import io.qzz.nekobounce.utils.client.PacketUtils.sendPacket
import io.qzz.nekobounce.utils.extensions.eyes
import io.qzz.nekobounce.utils.extensions.onPlayerRightClick
import io.qzz.nekobounce.utils.extensions.rotation
import io.qzz.nekobounce.utils.render.RenderUtils
import io.qzz.nekobounce.utils.render.RenderUtils.drawBlockBox
import io.qzz.nekobounce.utils.render.RenderUtils.drawBlockDamageText
import io.qzz.nekobounce.utils.rotation.RotationSettings
import io.qzz.nekobounce.utils.rotation.RotationUtils.currentRotation
import io.qzz.nekobounce.utils.rotation.RotationUtils.faceBlock
import io.qzz.nekobounce.utils.rotation.RotationUtils.performRaytrace
import io.qzz.nekobounce.utils.rotation.RotationUtils.setTargetRotation
import io.qzz.nekobounce.utils.rotation.RotationUtils.toRotation
import io.qzz.nekobounce.utils.timing.MSTimer
import io.qzz.nekobounce.utils.timing.TickedActions.nextTick
import io.qzz.nekobounce.utils.GlowUtils
import net.minecraft.block.Block
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.*
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.awt.Color

object Fucker : Module("Fucker", Category.WORLD) {

    /**
     * SETTINGS
     */
    private val hypixel by boolean("Hypixel", false)

    private val block by block("Block", 26)
    private val throughWalls by choices("ThroughWalls", arrayOf("None", "Raycast", "Around"), "None") { !hypixel }
    private val range by float("Range", 5F, 1F..7F)

    private val action by choices("Action", arrayOf("Destroy", "Use"), "Destroy")
    private val surroundings by boolean("Surroundings", true) { !hypixel }
    private val instant by boolean("Instant", false) { (action == "Destroy" || surroundings) && !hypixel }

    // 添加 Matrix 选项
    private val matrix by boolean("Matrix", false)

    private val switch by int("SwitchDelay", 250, 0..1000)
    private val swing by boolean("Swing", true)
    val noHit by boolean("NoHit", false)

    private val options = RotationSettings(this).withoutKeepRotation()

    private val blockProgress2D by boolean("BlockProgress2D", false)
    private val progressBeginY by float("ProgressBeginY", 5F, 0F..40F)

    private val blockProgress by boolean("BlockProgress", true).subjective()

    private val scale by float("Scale", 2F, 1F..6F) { blockProgress }.subjective()
    private val font by font("Font", Fonts.fontSemibold40) { blockProgress }.subjective()
    private val fontShadow by boolean("Shadow", true) { blockProgress }.subjective()

    private val color by color("Color", Color(200, 100, 0)) { blockProgress }.subjective()

    private val ignoreOwnBed by boolean("IgnoreOwnBed", true)
    private val ownBedDist by int("MaxBedDistance", 16, 1..32) { ignoreOwnBed }

    /**
     * VALUES
     */
    var pos: BlockPos? = null
        private set
    private var obstructingPos: BlockPos? = null
    private var spawnLocation: Vec3? = null
    private var oldPos: BlockPos? = null
    private var blockHitDelay = 0
    private val switchTimer = MSTimer()
    var currentDamage = 0F
    var isOwnBed = false

    // Matrix 模式相关变量
    private var matrixJumped = false
    private var matrixStartedBreaking = false
    private var matrixFreezeEnabled = false
    private var freezeX = 0.0
    private var freezeY = 0.0
    private var freezeZ = 0.0
    private var freezeMotionX = 0.0
    private var freezeMotionY = 0.0
    private var freezeMotionZ = 0.0
    private var canJumpAgain = true // 是否可以再次跳跃（防止模块重复触发跳跃）

    // Surroundings
    private var areSurroundings = false

    override fun onToggle(state: Boolean) {
        if (pos != null && !mc.thePlayer.capabilities.isCreativeMode) {
            sendPacket(C07PacketPlayerDigging(ABORT_DESTROY_BLOCK, pos, EnumFacing.DOWN))
        }

        currentDamage = 0F
        pos = null
        obstructingPos = null
        areSurroundings = false
        isOwnBed = false

        // 重置 Matrix 相关变量
        matrixJumped = false
        matrixStartedBreaking = false
        if (matrixFreezeEnabled) {
            disableMatrixFreeze()
        }
        canJumpAgain = true
    }

    val onPacket = handler<PacketEvent> { event ->
        if (mc.thePlayer == null || mc.theWorld == null) return@handler

        val packet = event.packet

        // Matrix 模式：处理玩家移动数据包
        if (matrixFreezeEnabled && packet is C03PacketPlayer) {
            event.cancelEvent()
        }

        // 处理服务器位置同步数据包
        if (packet is S08PacketPlayerPosLook) {
            spawnLocation = Vec3(packet.x, packet.y, packet.z)

            // Matrix 模式：如果处于冻结状态，更新冻结位置
            if (matrixFreezeEnabled) {
                freezeX = packet.x
                freezeY = packet.y
                freezeZ = packet.z
            }
        }
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        if (noHit && KillAura.handleEvents() && KillAura.target != null) return@handler

        val targetId = block

        if (pos == null || pos!!.block!!.id != targetId || getCenterDistance(pos!!) > range) {
            pos = find(targetId)
            obstructingPos = null
        }

        // Reset current breaking when there is no target block
        if (pos == null) {
            currentDamage = 0F
            areSurroundings = false
            isOwnBed = false
            obstructingPos = null
            // 没有目标时重置 Matrix 状态，但不解除冻结
            matrixJumped = false
            matrixStartedBreaking = false
            // 注意：这里不自动关闭冻结，只有按空格才能关闭
            return@handler
        }

        var currentPos = pos ?: return@handler

        // Check if it is the player's own bed
        isOwnBed = ignoreOwnBed && isBedNearSpawn(currentPos)
        if (isOwnBed) {
            obstructingPos = null
            matrixJumped = false
            matrixStartedBreaking = false
            // 如果是自己的床，也不解除冻结，只有按空格才能关闭
            return@handler
        }

        if (surroundings || hypixel) {
            if (hypixel && obstructingPos == null) {
                val abovePos = currentPos.up()
                if (abovePos.block != Blocks.air && isHittable(abovePos)) {
                    obstructingPos = abovePos
                    currentPos = obstructingPos!!
                }
            } else if (surroundings && obstructingPos == null) {
                val eyes = player.eyes
                val spotToBed = faceBlock(currentPos) ?: return@handler
                val blockPos = world.rayTraceBlocks(eyes, spotToBed.vec, false, false, true)?.blockPos
                if (blockPos != null && blockPos.block != Blocks.air && blockPos != currentPos) {
                    obstructingPos = blockPos
                    currentPos = obstructingPos!!
                }
            } else if (obstructingPos != null) {
                currentPos = obstructingPos!!
                if (surroundings) {
                    val eyes = player.eyes
                    val spotToObstruction = faceBlock(currentPos) ?: return@handler
                    val rayTraceResultToObstruction = world.rayTraceBlocks(eyes, spotToObstruction.vec, false, false, true)
                    // If a new block is blocking it, reset and re-evaluate next cycle.
                    if (rayTraceResultToObstruction?.blockPos != currentPos &&
                        rayTraceResultToObstruction?.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK
                    ) {
                        obstructingPos = null
                        return@handler
                    }
                    val spotToBed = faceBlock(pos!!) ?: return@handler
                    val rayTraceToBed = world.rayTraceBlocks(eyes, spotToBed.vec, false, false, true)
                    // Target bed if it's open
                    if (rayTraceToBed?.blockPos == pos &&
                        rayTraceToBed.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK
                    ) {
                        obstructingPos = null
                        currentPos = pos!!
                    }
                }
            }
        }

        val spot = faceBlock(currentPos) ?: return@handler

        // Reset switch timer when position changes
        if (oldPos != null && oldPos != currentPos) {
            currentDamage = 0F
            switchTimer.reset()
            // 目标变化时重置 Matrix 状态，但不解除冻结
            matrixJumped = false
            matrixStartedBreaking = false
            // 注意：这里不自动关闭冻结，只有按空格才能关闭
        }
        oldPos = currentPos

        if (!switchTimer.hasTimePassed(switch)) return@handler

        // Block hit delay
        if (blockHitDelay > 0) {
            blockHitDelay--
            return@handler
        }

        // Face block
        if (options.rotationsActive) {
            setTargetRotation(spot.rotation, options = options)
        }
    }

    /**
     * Check if the bed at the given position is near the spawn location
     */
    private fun isBedNearSpawn(currentPos: BlockPos): Boolean {
        if (currentPos.block != Block.getBlockById(block) || spawnLocation == null) {
            return false
        }
        return spawnLocation!!.squareDistanceTo(currentPos.center) < ownBedDist * ownBedDist
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler
        val controller = mc.playerController ?: return@handler

        // 检查玩家是否按下了空格（跳跃键）
        val jumpPressed = mc.gameSettings.keyBindJump.isKeyDown

        // Matrix 模式：如果处于冻结状态，检查是否按下空格
        if (matrixFreezeEnabled && jumpPressed && canJumpAgain) {
            disableMatrixFreeze()
            // 按下空格后，允许玩家正常跳跃
            player.jump()
            return@handler
        }

        // 如果刚刚解除冻结，重置canJumpAgain状态
        if (!matrixFreezeEnabled && !canJumpAgain) {
            canJumpAgain = true
        }

        // Matrix 模式：如果处于冻结状态，固定玩家位置
        if (matrixFreezeEnabled) {
            player.motionX = 0.0
            player.motionY = 0.0
            player.motionZ = 0.0
            player.setPositionAndRotation(freezeX, freezeY, freezeZ, player.rotationYaw, player.rotationPitch)
        }

        var currentPos = pos ?: return@handler
        if (obstructingPos != null) {
            currentPos = obstructingPos!!
        }

        // Matrix 模式：检查是否需要跳跃和冻结
        if (matrix && action == "Destroy" && !isOwnBed && !matrixFreezeEnabled && canJumpAgain) {
            // 如果刚开始拆床（currentDamage 为 0），触发跳跃
            if (currentDamage == 0F && !matrixJumped && !matrixStartedBreaking) {
                // 触发跳跃
                player.jump()
                matrixJumped = true
                matrixStartedBreaking = true
                canJumpAgain = false // 禁止再次跳跃，直到按空格解除冻结
            }

            // 检查是否到达跳跃最高点（motionY <= 0 表示开始下落）
            if (matrixJumped && player.motionY <= 0) {
                enableMatrixFreeze()
            }
        }

        val targetRotation = if (options.rotationsActive) {
            currentRotation ?: player.rotation
        } else {
            toRotation(currentPos.center, false).fixedSensitivity()
        }

        val raytrace = performRaytrace(currentPos, targetRotation, range) ?: return@handler

        when {
            // Destroy block
            action == "Destroy" || areSurroundings -> {
                isOwnBed = ignoreOwnBed && isBedNearSpawn(currentPos)
                if (isOwnBed) {
                    obstructingPos = null
                    // 如果是自己的床，不解除冻结（只有按空格才能关闭）
                    matrixJumped = false
                    matrixStartedBreaking = false
                    return@handler
                }

                EventManager.call(ClickBlockEvent(currentPos, raytrace.sideHit))

                if (instant && !hypixel) {
                    // CivBreak style block breaking
                    sendPacket(C07PacketPlayerDigging(START_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    if (swing) player.swingItem()
                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    clearTarget(currentPos)
                    // 注意：方块被破坏后，不自动解除冻结，只有按空格才能关闭
                    return@handler
                }

                val block = currentPos.block ?: return@handler

                if (currentDamage == 0F) {
                    // Prevent flagging FastBreak
                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    nextTick {
                        sendPacket(C07PacketPlayerDigging(START_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    }
                    if (player.capabilities.isCreativeMode ||
                        block.getPlayerRelativeBlockHardness(player, world, currentPos) >= 1f
                    ) {
                        if (swing) player.swingItem()
                        controller.onPlayerDestroyBlock(currentPos, raytrace.sideHit)
                        clearTarget(currentPos)
                        // 注意：方块被破坏后，不自动解除冻结，只有按空格才能关闭
                        return@handler
                    }
                }

                if (swing) player.swingItem()
                currentDamage += block.getPlayerRelativeBlockHardness(player, world, currentPos)
                world.sendBlockBreakProgress(player.entityId, currentPos, (currentDamage * 10F).toInt() - 1)

                if (currentDamage >= 1F) {
                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    controller.onPlayerDestroyBlock(currentPos, raytrace.sideHit)
                    blockHitDelay = 4
                    clearTarget(currentPos)
                    // 注意：方块被破坏后，不自动解除冻结，只有按空格才能关闭
                }
            }
            // Use block
            action == "Use" -> {
                if (player.onPlayerRightClick(currentPos, raytrace.sideHit, raytrace.hitVec, player.heldItem)) {
                    if (swing) player.swingItem() else sendPacket(C0APacketAnimation())
                    blockHitDelay = 4
                    clearTarget(currentPos)
                }
            }
        }
    }

    val onRender3D = handler<Render3DEvent> {
        val renderPos = obstructingPos ?: pos
        val posToDraw = renderPos ?: return@handler

        isOwnBed = ignoreOwnBed && isBedNearSpawn(posToDraw)
        if (mc.thePlayer == null || isOwnBed) return@handler

        if (block.blockById == Blocks.air) return@handler

        if (blockProgress) {
            posToDraw.drawBlockDamageText(
                currentDamage,
                font,
                fontShadow,
                color.rgb,
                scale
            )
        }

        // 修改ESP颜色为红色且不透明度为230
        drawBlockBox(posToDraw, Color(255, 0, 0, 230), outline = false)
    }

    val onRender2D = handler<Render2DEvent> {
        if (mc.thePlayer == null) return@handler
        if (block.blockById == Blocks.air) return@handler
        if (!blockProgress2D) return@handler

        val scaledScreen = ScaledResolution(mc)

        val progress = currentDamage.coerceIn(0F, 1F)
        val percentage = (progress * 100).toInt()

        if (progress == 0F) return@handler

        val screenWidth = scaledScreen.scaledWidth.toFloat()
        val screenHeight  = scaledScreen.scaledHeight.toFloat()

        val barWidth = 200F
        val barHeight = 10F
        val barX = screenWidth/2 - barWidth/2
        val barY = screenHeight / 2 + progressBeginY

        // 添加阴影效果
        ShowShadow(barX - 2, barY - 2, barWidth + 4, barHeight + 4, 0.3F)

        RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, Color(0, 0, 0, 100).rgb, 3F)

        if (progress > 0F) RenderUtils.drawRoundedGradientRectCorner(
            barX,
            barY,
            barX + (barWidth * progress) + 5,
            barY + barHeight,
            5f,
            Color(150, 200, 230).rgb,
            Color(100, 250, 200).rgb
        )


        val text = "$percentage%"
        val GoogleSans35 = Fonts.fontGoogleSans35
        val textWidth = GoogleSans35.getStringWidth(text)
        val textX = barX + barWidth - textWidth - 2F
        val textY = barY + (barHeight - GoogleSans35.FONT_HEIGHT) / 2F +1f

        GoogleSans35.drawString(text, textX, textY, Color.WHITE.rgb, false)
    }

    /**
     * 添加阴影效果函数
     */
    private fun ShowShadow(startX: Float, startY: Float, width: Float, height: Float, shadowStrength: Float) {
        GlowUtils.drawGlow(
            startX, startY,
            width, height,
            (shadowStrength * 13F).toInt(),
            Color(0, 0, 0, 120)
        )
    }

    /**
     * Finds a new target block by [targetID]
     */
    private fun find(targetID: Int): BlockPos? {
        val eyes = mc.thePlayer?.eyes ?: return null
        var nearestBlockDistanceSq = Double.MAX_VALUE
        val nearestBlock = BlockPos.MutableBlockPos()
        val rangeSq = range * range

        eyes.getAllInBoxMutable(range + 1.0).forEach {
            val distSq = it.distanceSqToCenter(eyes.xCoord, eyes.yCoord, eyes.zCoord)
            if (it.block?.id != targetID || distSq > rangeSq || distSq > nearestBlockDistanceSq ||
                !isHittable(it) && !surroundings && !hypixel
            ) return@forEach

            nearestBlockDistanceSq = distSq
            nearestBlock.set(it)
        }

        return nearestBlock.takeIf { nearestBlockDistanceSq != Double.MAX_VALUE }
    }

    /**
     * Checks if the block is hittable (or allowed to be hit through walls)
     */
    private fun isHittable(blockPos: BlockPos): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        return when (throughWalls.lowercase()) {
            "raycast" -> {
                val eyesPos = thePlayer.eyes
                val movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos, blockPos.center, false, true, false)
                movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
            }
            "around" -> EnumFacing.entries.any { !isBlockBBValid(blockPos.offset(it)) }
            else -> true
        }
    }

    /**
     * Clears the current target if it matches [currentPos] and resets相关值.
     * 注意：这里不清除冻结状态，只有按空格才能关闭冻结
     */
    private fun clearTarget(currentPos: BlockPos) {
        if (currentPos == obstructingPos) {
            obstructingPos = null
        }
        if (currentPos == pos) {
            pos = null
        }
        areSurroundings = false
        currentDamage = 0F
        // 清除目标时重置 Matrix 状态，但不解除冻结
        matrixJumped = false
        matrixStartedBreaking = false
        // 注意：这里不调用 disableMatrixFreeze()，冻结只能通过按空格解除
    }

    /**
     * 启用 Matrix 模式的冻结效果
     */
    private fun enableMatrixFreeze() {
        val player = mc.thePlayer ?: return

        // 记录当前位置和运动
        freezeX = player.posX
        freezeY = player.posY
        freezeZ = player.posZ
        freezeMotionX = player.motionX
        freezeMotionY = player.motionY
        freezeMotionZ = player.motionZ

        matrixFreezeEnabled = true
    }

    /**
     * 禁用 Matrix 模式的冻结效果
     */
    private fun disableMatrixFreeze() {
        val player = mc.thePlayer ?: return

        // 恢复运动
        if (matrixFreezeEnabled) {
            player.motionX = freezeMotionX
            player.motionY = freezeMotionY
            player.motionZ = freezeMotionZ
            player.setPositionAndRotation(freezeX, freezeY, freezeZ, player.rotationYaw, player.rotationPitch)
        }

        matrixFreezeEnabled = false
        matrixJumped = false
        matrixStartedBreaking = false
    }

    override val tag
        get() = getBlockName(block)
}