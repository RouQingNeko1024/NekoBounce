/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.renderer.GlStateManager

object ItemRotate : Module("ItemRotate", Category.RENDER) {

    private val speed by float("Speed", 2.0f, 0.1f..10.0f)
    private val ignoreBlocking by boolean("IgnoreBlocking", true)

    private var rotation = 0f
    private var isBlockingLastTick = false
    
    val onGameLoop = handler<GameLoopEvent> { event ->
        if (mc.thePlayer == null || mc.theWorld == null) return@handler
        
        val isBlockingNow = mc.thePlayer.isBlocking
        
        // 检查是否正在格挡
        if (ignoreBlocking && isBlockingNow) {
            if (!isBlockingLastTick) {
                // 刚开始格挡，重置旋转
                rotation = 0f
            }
            isBlockingLastTick = true
            return@handler
        }
        
        isBlockingLastTick = false
        
        // 更新旋转角度
        rotation += speed * 0.5f
        if (rotation > 360f) rotation -= 360f
    }

    override fun onEnable() {
        rotation = 0f
        isBlockingLastTick = false
    }

    override fun onDisable() {
        rotation = 0f
        isBlockingLastTick = false
    }

    override val tag: String
        get() = String.format("%.1f", speed)
    
    // 提供给Mixin使用的方法
    fun getRotation(): Float = if (state) rotation else 0f
    
    fun shouldRotate(): Boolean {
        if (!state) return false
        if (mc.thePlayer == null || mc.theWorld == null) return false
        if (ignoreBlocking && mc.thePlayer.isBlocking) return false
        return mc.gameSettings.thirdPersonView == 0  // 只在第一人称
    }

    // 公开getter，供Mixin访问
    fun getSpeedValue(): Float = speed
    fun getIgnoreBlockingSetting(): Boolean = ignoreBlocking
}