//By Neko
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.CameraPositionEvent
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object CameraView : Module("CameraView", Category.RENDER, gameDetecting = false) {

    private val customY by float("CustomY", 0f, -10f..10f)
    private val saveLastGroundY by boolean("SaveLastGroundY", true)
    private val onScaffold by boolean("OnScaffold", true)
    private val onF5 by boolean("OnF5", true)
    
    // 平滑效果设置
    private val smoothCamera by boolean("SmoothCamera", true)
    private val smoothSpeed by float("SmoothSpeed", 0.3f, 0.1f..1f) { smoothCamera }
    private val smoothTransition by float("SmoothTransition", 0.5f, 0.1f..2f) { smoothCamera }
    private val maxSmoothDistance by float("MaxSmoothDistance", 3f, 0.5f..10f) { smoothCamera }

    private var launchY: Double? = null
    private var currentCameraY: Double? = null
    private var targetCameraY: Double? = null
    private var lastUpdateTime = System.currentTimeMillis()

    override fun onEnable() {
        mc.thePlayer?.run {
            launchY = posY
            currentCameraY = posY + customY
            targetCameraY = posY + customY
        }
        lastUpdateTime = System.currentTimeMillis()
    }

    override fun onDisable() {
        currentCameraY = null
        targetCameraY = null
    }

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.POST) return@handler

        mc.thePlayer?.run {
            if (!saveLastGroundY || (onGround || ticksExisted == 1)) {
                launchY = posY
            }
            

            targetCameraY = (launchY ?: posY) + customY
        }
    }

    val onCameraUpdate = handler<CameraPositionEvent> { event ->
        mc.thePlayer?.run {
            val currentLaunchY = launchY ?: return@handler
            if (onScaffold && !Scaffold.handleEvents()) return@handler
            if (onF5 && mc.gameSettings.thirdPersonView == 0) return@handler


            val targetY = currentLaunchY + customY
            
            if (smoothCamera) {

                smoothCameraTransition(event, targetY)
            } else {

                event.withY(targetY)
                currentCameraY = targetY
                targetCameraY = targetY
            }
        }
    }


    private fun smoothCameraTransition(event: CameraPositionEvent, targetY: Double) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0f
        lastUpdateTime = currentTime


        val safeDeltaTime = if (deltaTime > 0.5f) 0.05f else deltaTime

        val currentY = currentCameraY ?: let {
            currentCameraY = targetY
            targetY
        }


        val distance = abs(targetY - currentY)
        

        val limitedTargetY = if (distance > maxSmoothDistance) {
            if (targetY > currentY) currentY + maxSmoothDistance
            else currentY - maxSmoothDistance
        } else {
            targetY
        }

        val smoothedY = when {

            distance > smoothTransition -> {
                val speedMultiplier = min(1.0, distance / smoothTransition)
                currentY + (limitedTargetY - currentY) * smoothSpeed * speedMultiplier * safeDeltaTime * 20
            }

            else -> {
                currentY + (limitedTargetY - currentY) * smoothSpeed * safeDeltaTime * 20
            }
        }


        event.withY(smoothedY)
        currentCameraY = smoothedY
        targetCameraY = targetY
    }
}