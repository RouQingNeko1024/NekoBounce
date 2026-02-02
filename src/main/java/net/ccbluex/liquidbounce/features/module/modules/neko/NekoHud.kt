/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.client.ServerUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import java.awt.Color

object NekoHud : Module("NekoHud", Category.NEKO) {

    private val titleColor = Color(255, 105, 180) // 粉色
    
    private val customTitleColor by color("TitleColor", titleColor)
    private val bgColor by color("Background", Color(0, 0, 0, 100))
    private val borderRadius by float("BorderRadius", 5F, 0F..10F)
    private val offsetX by int("OffsetX", 5, 0..100)
    private val offsetY by int("OffsetY", 5, 0..100)
    private val scale by float("Scale", 1.0F, 0.5F..2.0F)
    private val barWidth by int("BarWidth", 6, 3..15)
    private val rainbowMode by boolean("Rainbow", false)
    private val padding by int("Padding", 6, 0..20)
    
    // 对于中文显示，强制使用默认字体以确保中文字符正确显示
    private val useDefaultFont by boolean("UseDefaultFont", true)
    
    private val text = StringBuilder()
    
    private fun updateText() {
        text.clear()
        text.append("欣欣公益客户端 | ")
            .append(LiquidBounce.clientVersionText)
            .append(" | ")
            .append(Minecraft.getDebugFPS())
            .append(" fps | ")
            .append(ServerUtils.remoteIp)
    }
    
    private fun getTitleColor(): Int {
        return if (rainbowMode) {
            val hue = (System.currentTimeMillis() % 10000L) / 10000F
            Color.getHSBColor(hue, 0.7F, 1F).rgb
        } else {
            customTitleColor.rgb
        }
    }
    
    private fun getBackgroundColor(): Int {
        return if (rainbowMode) {
            val hue = (System.currentTimeMillis() % 10000L) / 10000F
            Color.getHSBColor(hue, 0.7F, 0.5F).rgb
        } else {
            bgColor.rgb
        }
    }
    
    private fun getFontRenderer() = if (useDefaultFont) {
        mc.fontRendererObj // 使用默认字体确保中文正确显示
    } else {
        // 如果用户坚持使用自定义字体，尝试使用思源黑体
        Fonts.fontsiyuanback45 ?: mc.fontRendererObj
    }
    
    val onRender2D = handler<Render2DEvent> {
        updateText()
        val sr = ScaledResolution(mc)
        val fontRenderer = getFontRenderer()
        
        // 计算文本宽度和高度
        val textStr = text.toString()
        val textWidth = fontRenderer.getStringWidth(textStr)
        val textHeight = fontRenderer.FONT_HEIGHT
        
        // 计算背景位置和大小
        val scaledWidth = (textWidth * scale).toInt()
        val scaledHeight = (textHeight * scale).toInt()
        val x = offsetX
        val y = offsetY
        val paddingValue = padding
        val bgWidth = scaledWidth + paddingValue * 2 + barWidth
        val bgHeight = scaledHeight + paddingValue * 2
        
        // 绘制圆角背景
        drawRoundedRect(
            x.toFloat(), y.toFloat(), 
            (x + bgWidth).toFloat(), (y + bgHeight).toFloat(), 
            getBackgroundColor(), borderRadius
        )
        
        // 绘制左侧圆角长条
        val barPadding = 2
        drawRoundedRect(
            x.toFloat() + barPadding, y.toFloat() + barPadding,
            (x + barPadding + barWidth).toFloat(), (y + bgHeight - barPadding).toFloat(),
            getTitleColor(), borderRadius
        )
        
        // 保存当前变换状态
        mc.entityRenderer.setupOverlayRendering()
        
        // 应用缩放
        org.lwjgl.opengl.GL11.glPushMatrix()
        org.lwjgl.opengl.GL11.glTranslatef((x + barWidth + paddingValue).toFloat(), y.toFloat() + paddingValue, 0F)
        org.lwjgl.opengl.GL11.glScalef(scale, scale, 1.0F)
        
        // 查找"欣欣公益客户端"的结束位置
        val titleEndIndex = "欣欣公益客户端".length
        val titlePart = textStr.substring(0, titleEndIndex)
        val restPart = textStr.substring(titleEndIndex)
        
        // 计算标题部分的宽度
        val titleWidth = fontRenderer.getStringWidth(titlePart)
        
        // 绘制标题部分
        fontRenderer.drawString(titlePart, 0F, 0F, getTitleColor(), false)
        
        // 绘制剩余部分（白色）
        fontRenderer.drawString(restPart, titleWidth.toFloat(), 0F, Color.WHITE.rgb, false)
        
        // 恢复变换状态
        org.lwjgl.opengl.GL11.glPopMatrix()
    }
    
    override fun onEnable() {
        // 无需特殊处理
    }
    
    override fun onDisable() {
        // 无需特殊处理
    }
    
    override val tag: String?
        get() = if (rainbowMode) "Rainbow" else if (!useDefaultFont) "CustomFont" else null
}