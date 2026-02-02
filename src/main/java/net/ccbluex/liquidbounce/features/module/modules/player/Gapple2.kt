//Lizz 3.2
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.item.ItemAppleGold

object Gapple2 : Module("Gapple2", Category.PLAYER) {
    private val c by int("C03PacketPlayer", 32, 32..40)
    private val triggerHealth by int("Health", 12, 1..18)
    private val autoEat by boolean("AutoGapple", true)

    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var cancelMove = false
    private var r = false
    private var ticks = 0
    private var pauseTicks = 0
    private var yaw = 0f
    private var pitch = 0f
    private var shouldEat = false
    var isEating = false
    private var lastHealth = 20.0f

    private var slot = -1

    override fun onEnable() {
        shouldEat = false
        isEating = false
        ticks = 0
        pauseTicks = 0
        stopStuck()
    }

    override fun onDisable() {
        ticks = 0
        pauseTicks = 0
        shouldEat = false
        isEating = false
        stopStuck()
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is C03PacketPlayer && cancelMove && ticks < c) {
            if (packet is C05PacketPlayerLook) {
                yaw = packet.yaw
                pitch = packet.pitch
            }
            ticks++
            event.cancelEvent()
        }
    }

    val onMove = handler<MoveEvent> { event ->
        if (cancelMove) {
            event.cancelEvent()
        }
    }

    val onUpdate = handler<UpdateEvent> {

        slot = getGApple()

        val shouldContinueEating = checkHealthCondition()

        if (shouldContinueEating && slot >= 0) {
            if (pauseTicks == 0) {
                stuck()
            } else {
                if (pauseTicks > 0) {
                    stopStuck()
                    pauseTicks--
                }
            }

            if (ticks >= c) {
                sendPacket(C09PacketHeldItemChange(slot))
                sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(slot)))
                release()
                sendPacket(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem)))
                pauseTicks++
                ticks = 0

                if (mc.thePlayer.ticksExisted % 20 == 0) {
                    chat("§6Eating...")
                }
            }
        } else {
            stopStuck()
            ticks = 0
            pauseTicks = 0

            if (shouldContinueEating && slot < 0) {
                if (mc.thePlayer.ticksExisted % 40 == 0) {
                    chat("§4NoGapple!")
                }
            }
        }
    }

    private fun stuck() {
        if (!r) {
            x = mc.thePlayer.motionX
            y = mc.thePlayer.motionY
            z = mc.thePlayer.motionZ
            r = true
        }
        cancelMove = true
    }

    private fun stopStuck() {
        cancelMove = false
        if (r) {
            mc.thePlayer.motionX = x
            mc.thePlayer.motionY = y
            mc.thePlayer.motionZ = z
            r = false
        }
    }

    private fun release() {
        sendPacket(C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround))
        for (i in 1 until ticks) {
            sendPacket(C03PacketPlayer(mc.thePlayer.onGround))
        }
    }

    private fun getGApple(): Int {
        for (i in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            if (stack != null && stack.item is ItemAppleGold) {
                return i
            }
        }
        return -1
    }

    private fun checkHealthCondition(): Boolean {
        if (!autoEat) {
            shouldEat = false
            isEating = false
            return false
        }

        val currentHealth = mc.thePlayer.health
        val maxHealth = mc.thePlayer.maxHealth

        if (currentHealth <= triggerHealth) {
            if (!shouldEat) {
                chat("§aWorking")
            }
            shouldEat = true
            isEating = true
        }

        if (currentHealth >= maxHealth && shouldEat) {
            chat("§aDone!")
            shouldEat = false
            isEating = false
        }

        lastHealth = currentHealth
        return shouldEat
    }

    // 添加进度获取函数，供Island模块使用
    fun getEatingProgress(): Float {
        return if (isEating && state) {
            (ticks.toFloat() / c.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}