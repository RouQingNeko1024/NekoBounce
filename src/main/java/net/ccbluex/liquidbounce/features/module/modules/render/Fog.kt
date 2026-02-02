/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object Fog : Module("Fog", Category.RENDER) {
    
    // 雾效果开关
    val enabled by boolean("Enabled", true)
    
    // 雾距离参数
    val fogStart by float("FogStart", 0.0f, 0.0f..10.0f)
    val fogEnd by float("FogEnd", 10.0f, 0.0f..50.0f)
    
    // 雾密度
    val fogDensity by float("FogDensity", 0.005f, 0.0f..0.1f)
    
    // 雾颜色（RGB）
    val red by int("Red", 255, 0..255)
    val green by int("Green", 255, 0..255)
    val blue by int("Blue", 255, 0..255)
    
    // 雾类型
    val fogType by choices("FogType", arrayOf("LINEAR", "EXP", "EXP2"), "EXP")
    
    // 雾颜色作为Color对象
    fun getFogColor(): Int {
        return ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)
    }
    
    // 获取雾类型的OpenGL常量
    fun getGlFogType(): Int {
        return when (fogType) {
            "LINEAR" -> 9729  // GL11.GL_LINEAR
            "EXP" -> 2048    // GL11.GL_EXP
            "EXP2" -> 2049   // GL11.GL_EXP2
            else -> 2048
        }
    }
}