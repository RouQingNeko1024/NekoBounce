//全AI写的
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.sqrt

object BypassChest : Module("BypassChest", Category.WORLD) {

    private val mode by choices("Mode", arrayOf("Instant", "Normal"), "Instant")
    private val range by int("Range", 5, 1..6)
    private val rotations by boolean("Rotations", true)
    private val swing by boolean("Swing", true)
    private val esp by boolean("ESP", true)
    
    private var targetPos: BlockPos? = null
    private var placedPositions = mutableListOf<BlockPos>()

    // 替代RotationUtils的简单旋转计算
    private fun getRotations(pos: BlockPos): FloatArray {
        val player = mc.thePlayer ?: return floatArrayOf(0f, 0f)
        
        val eyesPos = Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ)
        val posVec = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        
        val diffX = posVec.xCoord - eyesPos.xCoord
        val diffY = posVec.yCoord - eyesPos.yCoord
        val diffZ = posVec.zCoord - eyesPos.zCoord
        
        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
        
        val yaw = (Math.toDegrees(atan2(diffZ, diffX)) - 90.0).toFloat()
        val pitch = (-Math.toDegrees(atan2(diffY, diffXZ))).toFloat()
        
        return floatArrayOf(yaw, pitch)
    }

    // 检查方块是否为全立方体
    private fun isSolidFullCube(blockPos: BlockPos): Boolean {
        val world = mc.theWorld ?: return false
        val blockState = world.getBlockState(blockPos)
        val block = blockState.block
        
        return block.isFullCube && block.isOpaqueCube
    }

    private val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        
        // 寻找可放置箱子的位置
        targetPos = findChestPlacePosition()
        
        targetPos?.let { pos ->
            if (rotations) {
                val rotations = getRotations(pos)
                player.rotationYaw = rotations[0]
                player.rotationPitch = rotations[1]
            }
            
            when (mode.lowercase()) {
                "instant" -> {
                    // 绕过限制强制放置
                    placeChestBypass(pos)
                }
                "normal" -> {
                    // 正常放置
                    placeChest(pos)
                }
            }
            
            if (swing) {
                player.swingItem()
            }
        }
    }
    
    private val onRender3D = handler<Render3DEvent> { event ->
        if (esp) {
            placedPositions.forEach { pos ->
                RenderUtils.drawBlockBox(pos, Color(255, 165, 0, 100), true)
            }
            
            targetPos?.let { pos ->
                RenderUtils.drawBlockBox(pos, Color(0, 255, 0, 100), true)
            }
        }
    }
    
    private fun findChestPlacePosition(): BlockPos? {
        val player = mc.thePlayer ?: return null
        
        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    val pos = BlockPos(player.posX + x, player.posY + y, player.posZ + z)
                    
                    // 检查是否可以放置箱子
                    if (canPlaceChest(pos)) {
                        return pos
                    }
                }
            }
        }
        return null
    }
    
    private fun canPlaceChest(pos: BlockPos): Boolean {
        val world = mc.theWorld ?: return false
        
        // 检查位置是否空气
        if (!world.isAirBlock(pos)) return false
        
        // 检查相邻方块是否可以支撑箱子
        for (facing in EnumFacing.values()) {
            val neighborPos = pos.offset(facing)
            
            if (isSolidFullCube(neighborPos)) {
                return true
            }
        }
        
        return false
    }
    
    private fun placeChest(pos: BlockPos) {
        val world = mc.theWorld ?: return
        
        // 找到支撑面
        val facing = findSupportFacing(pos) ?: return
        
        // 发送放置包
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(
            pos.offset(facing.opposite),
            facing.opposite.index,
            mc.thePlayer.heldItem,
            0.5f, 0.5f, 0.5f
        ))
        
        // 记录已放置的位置
        if (!placedPositions.contains(pos)) {
            placedPositions.add(pos)
        }
    }
    
    private fun placeChestBypass(pos: BlockPos) {
        val world = mc.theWorld ?: return
        
        // 绕过限制的强制放置方法
        val facing = findSupportFacing(pos) ?: EnumFacing.UP
        
        // 先破坏可能存在的障碍物
        if (!world.isAirBlock(pos)) {
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                pos,
                facing
            ))
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                pos,
                facing
            ))
        }
        
        // 强制放置箱子
        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(
            pos,
            facing.index,
            mc.thePlayer.heldItem,
            0.0f, 0.0f, 0.0f
        ))
        
        // 记录已放置的位置
        if (!placedPositions.contains(pos)) {
            placedPositions.add(pos)
        }
    }
    
    private fun findSupportFacing(pos: BlockPos): EnumFacing? {
        val world = mc.theWorld ?: return null
        
        for (facing in EnumFacing.values()) {
            val neighborPos = pos.offset(facing)
            
            if (isSolidFullCube(neighborPos)) {
                return facing
            }
        }
        
        return null
    }
    
    override fun onEnable() {
        placedPositions.clear()
    }
    
    override fun onDisable() {
        targetPos = null
    }
    
    override val tag: String
        get() = mode
}