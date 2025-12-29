/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.render

import io.qzz.nekobounce.event.Render3DEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

object EnchantColor : Module("EnchantColor", Category.RENDER) {

    private val red by int("Red", 255, 0..255)
    private val green by int("Green", 0, 0..255)
    private val blue by int("Blue", 255, 0..255)
    private val alpha by int("Alpha", 255, 0..255)

    private val rainbow by boolean("Rainbow", false)
    private val rainbowSpeed by int("RainbowSpeed", 5, 1..20) { rainbow }
    private val rainbowSaturation by float("RainbowSaturation", 0.8f, 0f..1f) { rainbow }
    private val rainbowBrightness by float("RainbowBrightness", 0.8f, 0f..1f) { rainbow }

    private var currentHue = 0f

    val onRender3D = handler<Render3DEvent> { event ->
        if (rainbow) {
            currentHue += rainbowSpeed.toFloat() / 1000f
            if (currentHue > 1f) currentHue = 0f
        }
    }

    fun getEnchantColor(): Int {
        return if (rainbow) {
            Color.HSBtoRGB(currentHue, rainbowSaturation, rainbowBrightness)
        } else {
            Color(red, green, blue, alpha).rgb
        }
    }

    // 添加一个函数来直接修改附魔颜色
    fun applyEnchantColor() {
        val color = getEnchantColor()
        
        val r = (color shr 16 and 0xFF) / 255.0f
        val g = (color shr 8 and 0xFF) / 255.0f
        val b = (color and 0xFF) / 255.0f
        val a = (color shr 24 and 0xFF) / 255.0f
        
        GlStateManager.color(r, g, b, a)
    }

    override val tag: String
        get() = if (rainbow) "Rainbow" else "$red,$green,$blue"
}