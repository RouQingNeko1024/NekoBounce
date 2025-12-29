package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.opengl.GL11
import java.awt.Color

object CustomCrosshair : Module("CustomCrosshair", Category.RENDER) {
    // 隐藏原版准星的标志
    var hideVanillaCrosshair = false

    private val thickness by float("Thickness", 2f, 1f..10f)
    private val red by int("Red", 0, 0..255)
    private val green by int("Green", 0, 0..255)
    private val blue by int("Blue", 0, 0..255)
    private val alpha by int("Alpha", 255, 0..255)

    override fun onEnable() {
        hideVanillaCrosshair = true
    }

    override fun onDisable() {
        hideVanillaCrosshair = false
    }

    val render = handler<Render2DEvent> {
        if (mc.gameSettings.showDebugInfo) return@handler
        
        val sr = ScaledResolution(mc)
        val width = sr.scaledWidth
        val height = sr.scaledHeight
        val centerX = width / 2f
        val centerY = height / 2f
        
        val color = Color(red, green, blue, alpha)
        
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(thickness)
        
        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        
        // 绘制横线（贯穿屏幕）
        GL11.glBegin(GL11.GL_LINES)
        GL11.glVertex2f(0f, centerY)
        GL11.glVertex2f(width.toFloat(), centerY)
        GL11.glEnd()
        
        // 绘制竖线（贯穿屏幕）
        GL11.glBegin(GL11.GL_LINES)
        GL11.glVertex2f(centerX, 0f)
        GL11.glVertex2f(centerX, height.toFloat())
        GL11.glEnd()
        
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }
}