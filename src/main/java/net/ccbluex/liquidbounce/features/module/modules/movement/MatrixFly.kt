/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MoveUtils
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object MatrixFly : Module("MatrixFly", Category.MOVEMENT) {

    private val bypassMethod by choices("BypassMethod", arrayOf("Fall", "NoGround"), "Fall")
    private val flySpeed by float("FlySpeed", 2.1f, 0.1f..8f)

    private var flagTicks = 0
    private var lastFlagTime = 0L
    private var canFly = false
    private var flying = false
    private var touchGround = false

    override fun onEnable() {
        flagTicks = 0
        lastFlagTime = 0L
        canFly = false
        flying = false
        touchGround = false

        if (bypassMethod.equals("NoGround", ignoreCase = true)) {
            mc.thePlayer?.tryJump()
            touchGround = true
        }
    }

    override fun onDisable() {
        flagTicks = 0
        lastFlagTime = 0L
        canFly = false
        flying = false
        touchGround = false
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        
        if (packet is S08PacketPlayerPosLook) {
            val now = System.currentTimeMillis()
            if (now - lastFlagTime > 50L) {
                flagTicks++
                lastFlagTime = now
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        // 检测地面接触状态
        if (!player.onGround && touchGround) {
            touchGround = false
        }

        if (player.onGround && !touchGround) {
            touchGround = true
            flying = false
            if (bypassMethod.equals("NoGround", ignoreCase = true)) {
                canFly = true
            }
        }

        // 坠落检测
        if (player.fallDistance >= 0.25 && !flying && bypassMethod.equals("Fall", ignoreCase = true)) {
            canFly = true
        }

        // 飞行逻辑
        if (canFly) {
            // 设置水平移动速度
            MoveUtils.setSpeed(flySpeed.toDouble(), false)
            
            // 固定垂直位置 - 不允许上下移动
            player.motionY = 0.0
            
            // 固定位置高度
            if (!player.onGround) {
                player.setPosition(player.posX, player.posY, player.posZ)
            }
            
            flying = true
        }

        // 防封禁检测
        if (flagTicks >= 1 && flying) {
            state = false
            canFly = false
            flagTicks = 0
            lastFlagTime = 0L
        }
    }
}