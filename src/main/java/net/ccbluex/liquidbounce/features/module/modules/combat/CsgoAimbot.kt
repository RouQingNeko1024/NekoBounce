package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isFaced
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.performAngleChange
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import java.util.*
import kotlin.math.atan

object CsgoAimbot : Module("CsgoAimbot", Category.COMBAT) {
    private val range by float("Range", 4.4f, 1f..8f)
    private val fov by float("FOV", 180f, 1f..180f)
    private val clickDelay by int("ClickDelay", 100, 0..500)
    private val autoClick by boolean("AutoClick", true)
    private val lock by boolean("Lock", true)
    private val center by boolean("Center", false)
    private val headLock by boolean("Headlock", false) { center && lock }
    private val headLockBlockHeight by float("HeadBlockHeight", -1f, -2f..0f) { headLock && center && lock }
    private val breakBlocks by boolean("BreakBlocks", true)
    private val maxAngleChange by float("MaxAngleChange", 10f, 1f..180f)
    private val inViewMaxAngleChange by float("InViewMaxAngleChange", 35f, 1f..180f)

    private var target: EntityPlayer? = null
    private val clickTimer = MSTimer()

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.PRE) return@handler
        
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        // 获取本玩家名字
        val playerName = player.name
        
        // 确定目标名字应包含的字符
        val targetChar = when {
            playerName.contains("銑") -> "銐"
            playerName.contains("銐") -> "銑"
            else -> {
                // 都不包含，不自瞄
                mc.gameSettings.keyBindUseItem.pressed = false
                target = null
                return@handler
            }
        }

        // 搜索符合条件的玩家目标
        target = world.loadedEntityList.filter {
            it is EntityPlayer && it != player && !it.isDead && it.name.contains(targetChar)
        }.minByOrNull { player.getDistanceToEntityBox(it) } as? EntityPlayer

        if (target == null || player.getDistanceToEntityBox(target!!) > range || 
            rotationDifference(target!!) > fov || !player.canEntityBeSeen(target!!)) {
            mc.gameSettings.keyBindUseItem.pressed = false
            return@handler
        }

        // 锁定目标
        val entity = target!!
        if (!lock && isFaced(entity, range.toDouble())) return@handler

        // 计算旋转
        val random = Random()
        if (!findRotation(entity, random)) return@handler

        // 长按右键
        if (autoClick && clickTimer.hasTimePassed(clickDelay)) {
            mc.gameSettings.keyBindUseItem.pressed = true
            clickTimer.reset()
        }
    }

    private fun findRotation(entity: Entity, random: Random): Boolean {
        val player = mc.thePlayer ?: return false

        if (mc.playerController.isHittingBlock && breakBlocks) {
            return false
        }

        val boundingBox = entity.hitBox
        val (currPos, oldPos) = player.currPos to player.prevPos

        val playerRotation = player.rotation

        val destinationRotation = if (center) {
            toRotation(boundingBox.center, true)
        } else {
            searchCenter(
                boundingBox,
                false,
                outborder = false,
                predict = true,
                lookRange = range,
                attackRange = 3f,
                bodyPoints = listOf("Head", "Head"), // 只瞄准头部
                horizontalSearch = 0f..0f
            )
        }

        if (destinationRotation == null) {
            player.setPosAndPrevPos(currPos, oldPos)
            return false
        }

        // 头部锁定
        if (headLock && center && lock) {
            val distance = player.getDistanceToEntityBox(entity)
            val playerEyeHeight = player.eyeHeight
            val blockHeight = headLockBlockHeight

            val pitchOffset = Math.toDegrees(atan((blockHeight + playerEyeHeight) / distance)).toFloat()
            destinationRotation.pitch -= pitchOffset
        }

        // 计算旋转差
        val rotationDiff = rotationDifference(playerRotation, destinationRotation)

        val supposedTurnSpeed = if (rotationDiff < mc.gameSettings.fovSetting) {
            inViewMaxAngleChange
        } else {
            maxAngleChange
        }

        val gaussian = random.nextGaussian()
        val realisticTurnSpeed = rotationDiff * ((supposedTurnSpeed + (gaussian - 0.5)) / 180)

        // 应用旋转
        val rotation = performAngleChange(
            player.rotation,
            destinationRotation,
            realisticTurnSpeed.toFloat(),
            legitimize = true,
            minRotationDiff = 0f,
            minRotationDiffResetTiming = "OnStart",
        )

        rotation.toPlayer(player, true, true)

        player.setPosAndPrevPos(currPos, oldPos)

        return true
    }

    override fun onDisable() {
        mc.gameSettings.keyBindUseItem.pressed = false
        target = null
    }

    override val tag: String?
        get() = target?.name
}