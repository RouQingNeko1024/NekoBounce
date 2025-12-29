/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.player

import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.extensions.fixedSensitivityPitch
import io.qzz.nekobounce.utils.extensions.fixedSensitivityYaw
import io.qzz.nekobounce.utils.extensions.tryJump
import io.qzz.nekobounce.utils.kotlin.RandomUtils.nextFloat
import io.qzz.nekobounce.utils.kotlin.RandomUtils.nextInt
import io.qzz.nekobounce.utils.timing.MSTimer
import net.minecraft.client.settings.GameSettings

object AntiAFK : Module("AntiAFK", Category.PLAYER, gameDetecting = false) {

    private val mode by choices("Mode", arrayOf("Old", "Random", "Custom"), "Random")

    private val rotateValue = boolean("Rotate", true) { mode == "Custom" }
    private val rotationDelay by int("RotationDelay", 100, 0..1000) { rotateValue.isActive() }
    private val rotationAngle by float("RotationAngle", 1f, -180F..180F) { rotateValue.isActive() }

    private val swingValue = boolean("Swing", true) { mode == "Custom" }
    private val swingDelay by int("SwingDelay", 100, 0..1000) { swingValue.isActive() }

    private val jump by boolean("Jump", true) { mode == "Custom" }
    private val move by boolean("Move", true) { mode == "Custom" }

    private var shouldMove = false
    private var randomTimerDelay = 500L

    private val swingDelayTimer = MSTimer()
    private val delayTimer = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        when (mode.lowercase()) {
            "old" -> {
                mc.gameSettings.keyBindForward.pressed = true

                if (delayTimer.hasTimePassed(500)) {
                    thePlayer.fixedSensitivityYaw += 180F
                    delayTimer.reset()
                }
            }

            "random" -> {
                getRandomMoveKeyBind().pressed = shouldMove

                if (!delayTimer.hasTimePassed(randomTimerDelay)) return@handler
                shouldMove = false
                randomTimerDelay = 500L
                when (nextInt(0, 6)) {
                    0 -> {
                        if (thePlayer.onGround) thePlayer.tryJump()
                        delayTimer.reset()
                    }

                    1 -> {
                        if (!thePlayer.isSwingInProgress) thePlayer.swingItem()
                        delayTimer.reset()
                    }

                    2 -> {
                        randomTimerDelay = nextInt(0, 1000).toLong()
                        shouldMove = true
                        delayTimer.reset()
                    }

                    3 -> {
                        thePlayer.inventory.currentItem = nextInt(0, 9)
                        mc.playerController.syncCurrentPlayItem()
                        delayTimer.reset()
                    }

                    4 -> {
                        thePlayer.fixedSensitivityYaw += nextFloat(-180f, 180f)
                        delayTimer.reset()
                    }

                    5 -> {
                        thePlayer.fixedSensitivityPitch += nextFloat(-10f, 10f)
                        delayTimer.reset()
                    }
                }
            }

            "custom" -> {
                if (move)
                    mc.gameSettings.keyBindForward.pressed = true

                if (jump && thePlayer.onGround)
                    thePlayer.tryJump()

                if (rotateValue.get() && delayTimer.hasTimePassed(rotationDelay)) {
                    thePlayer.fixedSensitivityYaw += rotationAngle
                    thePlayer.fixedSensitivityPitch += nextFloat(0F, 1F) * 2 - 1
                    delayTimer.reset()
                }

                if (swingValue.get() && !thePlayer.isSwingInProgress && swingDelayTimer.hasTimePassed(swingDelay)) {
                    thePlayer.swingItem()
                    swingDelayTimer.reset()
                }
            }
        }
    }

    private val moveKeyBindings =
        arrayOf(
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindRight
        )

    private fun getRandomMoveKeyBind() = moveKeyBindings.random()

    override fun onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindForward))
            mc.gameSettings.keyBindForward.pressed = false
    }
}