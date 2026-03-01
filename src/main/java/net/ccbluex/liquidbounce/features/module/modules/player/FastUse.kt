/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.ItemUtils.isConsumingItem
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.serverOnGround
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.play.client.C03PacketPlayer

object FastUse : Module("FastUse", Category.PLAYER) {

    private val mode by choices("Mode", arrayOf("Instant", "NCP", "AAC", "Custom","OldIntave","Matrix"), "NCP")

    private val delay by int("CustomDelay", 0, 0..300) { mode == "Custom" }
    private val customSpeed by int("CustomSpeed", 2, 1..35) { mode == "Custom" }
    private val customTimer by float("CustomTimer", 1.1f, 0.5f..2f) { mode == "Custom" }
    // Intave
    private val lowTimer by float("LowTimer", 0.3f, 0.01f..10f) { mode == "Intave" }
    private val maxTimer by float("MaxTimer", 0.3f, 0.01f..10f) { mode == "Intave" }
    private val ticks by float("Ticks", 1f, 1f..20f) { mode == "Intave" }

    private val noMove by boolean("NoMove", false)

    private val msTimer = MSTimer()
    private var usedTimer = false

    // Intave
    private var isEating = 10
    private var reset = false

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (usedTimer) {
            mc.timer.timerSpeed = 1F
            usedTimer = false
        }

        if (mode == "OldIntave") {
            if (isConsumingItem()) {
                reset = false
                if (isEating >= 1) {
                    isEating--
                    mc.timer.timerSpeed = lowTimer
                    usedTimer = true
                } else {
                    isEating = ticks.toInt()
                    mc.timer.timerSpeed = maxTimer
                    usedTimer = true
                }
            } else {
                isEating = ticks.toInt()
                if (!reset) {
                    mc.timer.timerSpeed = 1F
                    usedTimer = false
                    reset = true
                }
            }

            return@handler
        }
        if (!isConsumingItem()) {
            msTimer.reset()
            return@handler
        }

        when (mode.lowercase()) {
            "instant" -> {
                repeat(35) {
                    sendPacket(C03PacketPlayer(serverOnGround))
                }

                mc.playerController.onStoppedUsingItem(thePlayer)
            }

            "ncp" -> if (thePlayer.itemInUseDuration > 14) {
                repeat(20) {
                    sendPacket(C03PacketPlayer(serverOnGround))
                }

                mc.playerController.onStoppedUsingItem(thePlayer)
            }
            "matrix" -> {
                mc.timer.timerSpeed = 0.5F
                usedTimer = true
                sendPacket(C03PacketPlayer(serverOnGround))
            }
            "aac" -> {
                mc.timer.timerSpeed = 1.22F
                usedTimer = true
            }

            "custom" -> {
                mc.timer.timerSpeed = customTimer
                usedTimer = true

                if (!msTimer.hasTimePassed(delay))
                    return@handler

                repeat(customSpeed) {
                    sendPacket(C03PacketPlayer(serverOnGround))
                }

                msTimer.reset()
            }
        }
    }

    val onMove = handler<MoveEvent> { event ->
        mc.thePlayer ?: return@handler

        if (!isConsumingItem() || !noMove)
            return@handler

        event.zero()
    }

    override fun onDisable() {
        if (usedTimer) {
            mc.timer.timerSpeed = 1F
            usedTimer = false
        }
    }

    override val tag
        get() = mode
}
