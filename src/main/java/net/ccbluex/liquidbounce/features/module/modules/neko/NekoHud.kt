//By Neko
package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

object NekoHud : Module("NekoHud", Category.NEKO) {

    private val displayMode by choices("Mode", arrayOf("Image", "Text", "Nya"), "Image")
    
    private val imageType by choices("Image-Type", arrayOf("Logo", "Amogus","Fire"), "Logo") { displayMode == "Image" }
    
    private val imageSize by int("Image-Size", 50, 10..200) { displayMode == "Image" }
    
    private val textContent by text("Text-Content", "NekoBounce") { displayMode == "Text" }
    private val textColor by color("Text-Color", Color(255, 105, 180)) { displayMode == "Text" }
    private val textShadow by boolean("Text-Shadow", true) { displayMode == "Text" }
    private val textX by int("Text-X", 10, 0..1000) { displayMode == "Text" }
    private val textY by int("Text-Y", 10, 0..1000) { displayMode == "Text" }
    private val textBackground by boolean("Text-Background", false) { displayMode == "Text" }
    
    // 添加背景位置设置
    private val backgroundX by int("Background-X", -2, -100..100) { 
        (displayMode == "Text" && textBackground) || (displayMode == "Nya" && nyaBackground) 
    }
    private val backgroundY by int("Background-Y", -2, -100..100) { 
        (displayMode == "Text" && textBackground) || (displayMode == "Nya" && nyaBackground) 
    }
    private val backgroundWidth by int("Background-Width", 4, -300..200) { 
        (displayMode == "Text" && textBackground) || (displayMode == "Nya" && nyaBackground) 
    }
    private val backgroundHeight by int("Background-Height", 4, -300..200) { 
        (displayMode == "Text" && textBackground) || (displayMode == "Nya" && nyaBackground) 
    }
    
    // 添加阴影设置
    private val shadowCheck by boolean("Shadow", false) { 
        (displayMode == "Text" && textBackground) || (displayMode == "Nya" && nyaBackground) 
    }
    private val shadowStrength by int("ShadowStrength", 1, 1..2) { 
        (displayMode == "Text" && textBackground && shadowCheck) || (displayMode == "Nya" && nyaBackground && shadowCheck) 
    }
    
    // 添加字体选项
    private val fontSelection by choices("Font", arrayOf("Default", "Fortalesia40", "Augs", "SiyuanBack45", "SiyuanBack35"), "Default") { 
        displayMode == "Text" || displayMode == "Nya" 
    }
    
    // 添加Nya文本自定义
    private val nyaCustomText by text("Neko-Text", "Neko") { displayMode == "Nya" }
    private val nyaCustomTextColor by color("Neko-Text-Color", Color(255, 105, 180)) { displayMode == "Nya" }
    private val nyaColor by color("Nya-Color", Color(255, 255, 255)) { displayMode == "Nya" }
    private val nyaShadow by boolean("Nya-Shadow", true) { displayMode == "Nya" }
    private val nyaX by int("Nya-X", 10, 0..1000) { displayMode == "Nya" }
    private val nyaY by int("Nya-Y", 10, 0..1000) { displayMode == "Nya" }
    private val nyaBackground by boolean("Nya-Background", true) { displayMode == "Nya" }
    private val nyaBackgroundRadius by float("Background-Radius", 5f, 0f..20f) { 
        (displayMode == "Text" && textBackground) || (displayMode == "Nya" && nyaBackground) 
    }

    private val logoImage = ResourceLocation("liquidbounce/logo/logo.png")
    private val amogusImage = ResourceLocation("liquidbounce/logo/amogus.png")
    private val fireImage = ResourceLocation("liquidbounce/logo/fire.png")  // 修复：改为小写开头以保持一致性
    
    private val timeFormat = SimpleDateFormat("HH:mm:ss")
    private var lastUpdateTime = 0L
    
    // 阴影绘制函数（从 WaterMark.kt 提取）
    private fun showShadow(startX: Float, startY: Float, width: Float, height: Float) {
        if (shadowCheck) {
            GlowUtils.drawGlow(
                startX, startY,
                width, height,
                (shadowStrength * 13F).toInt(),
                Color(0, 0, 0, 120)
            )
        }
    }
    
    private val onRender2D = handler<Render2DEvent> { event ->
        when (displayMode) {
            "Image" -> {
                try {
                    val imageToDraw = when (imageType) {
                        "Logo" -> logoImage
                        "Amogus" -> amogusImage
                        "Fire" -> fireImage  // 修复：使用小写变量名
                        else -> logoImage
                    }
                    
                    RenderUtils.drawImage(imageToDraw, 0, 0, imageSize, imageSize)
                } catch (e: Exception) {
                    // 忽略异常
                }
            }
            "Text" -> {
                val text = textContent
                val x = textX.toFloat()
                val y = textY.toFloat()
                
                // 绘制背景
                if (textBackground) {
                    val textWidth = when (fontSelection) {
                        "Default" -> mc.fontRendererObj.getStringWidth(text)
                        "Fortalesia40" -> Fonts.fontFortalesia40.getStringWidth(text)
                        "Augs" -> Fonts.fontAugs40.getStringWidth(text)
                        "SiyuanBack45" -> Fonts.fontsiyuanback45.getStringWidth(text)
                        "SiyuanBack35" -> Fonts.fontsiyuanback35.getStringWidth(text)
                        else -> mc.fontRendererObj.getStringWidth(text)
                    }
                    
                    val textHeight = when (fontSelection) {
                        "Default" -> mc.fontRendererObj.FONT_HEIGHT
                        "Fortalesia40" -> Fonts.fontFortalesia40.fontHeight
                        "Augs" -> Fonts.fontAugs40.fontHeight
                        "SiyuanBack45" -> Fonts.fontsiyuanback45.fontHeight
                        "SiyuanBack35" -> Fonts.fontsiyuanback35.fontHeight
                        else -> mc.fontRendererObj.FONT_HEIGHT
                    }
                    
                    val backgroundX = x + backgroundX
                    val backgroundY = y + backgroundY
                    val backgroundWidth = x + textWidth + backgroundWidth - backgroundX
                    val backgroundHeight = y + textHeight + backgroundHeight - backgroundY
                    
                    // 绘制阴影
                    showShadow(backgroundX, backgroundY, backgroundWidth, backgroundHeight)
                    
                    // 使用 Text.kt 中的方法绘制圆角矩形
                    RenderUtils.drawRoundedRect(
                        backgroundX, 
                        backgroundY, 
                        x + textWidth + backgroundWidth, 
                        y + textHeight + backgroundHeight, 
                        Color(0, 0, 0, 128).rgb,
                        nyaBackgroundRadius
                    )
                }
                
                // 绘制文字
                when (fontSelection) {
                    "Default" -> {
                        mc.fontRendererObj.drawString(
                            text,
                            x,
                            y,
                            textColor.rgb,
                            textShadow
                        )
                    }
                    "Fortalesia40" -> {
                        Fonts.fontFortalesia40.drawString(
                            text,
                            x,
                            y,
                            textColor.rgb,
                            textShadow
                        )
                    }
                    "Augs" -> {
                        Fonts.fontAugs40.drawString(
                            text,
                            x,
                            y,
                            textColor.rgb,
                            textShadow
                        )
                    }
                    "SiyuanBack45" -> {
                        Fonts.fontsiyuanback45.drawString(
                            text,
                            x,
                            y,
                            textColor.rgb,
                            textShadow
                        )
                    }
                    "SiyuanBack35" -> {
                        Fonts.fontsiyuanback35.drawString(
                            text,
                            x,
                            y,
                            textColor.rgb,
                            textShadow
                        )
                    }
                }
            }
            "Nya" -> {
                // 每秒更新一次时间
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime >= 1000) {
                    lastUpdateTime = currentTime
                }
                
                // 使用Minecraft.getDebugFPS()获取FPS
                val fps = Minecraft.getDebugFPS().toString()
                val serverIP = if (mc.isSingleplayer) "SinglePlayer" 
                              else mc.currentServerData?.serverIP ?: "Unknown"
                val time = timeFormat.format(Date())
                
                // 使用自定义的Neko文本
                val nyaText = "$nyaCustomText | ${fps}FPS | $serverIP | $time"
                val x = nyaX.toFloat()
                val y = nyaY.toFloat()
                
                // 绘制背景
                if (nyaBackground) {
                    val textWidth = when (fontSelection) {
                        "Default" -> mc.fontRendererObj.getStringWidth(nyaText)
                        "Fortalesia40" -> Fonts.fontFortalesia40.getStringWidth(nyaText)
                        "Augs" -> Fonts.fontAugs40.getStringWidth(nyaText)
                        "SiyuanBack45" -> Fonts.fontsiyuanback45.getStringWidth(nyaText)
                        "SiyuanBack35" -> Fonts.fontsiyuanback35.getStringWidth(nyaText)
                        else -> mc.fontRendererObj.getStringWidth(nyaText)
                    }
                    
                    val textHeight = when (fontSelection) {
                        "Default" -> mc.fontRendererObj.FONT_HEIGHT
                        "Fortalesia40" -> Fonts.fontFortalesia40.fontHeight
                        "Augs" -> Fonts.fontAugs40.fontHeight
                        "SiyuanBack45" -> Fonts.fontsiyuanback45.fontHeight
                        "SiyuanBack35" -> Fonts.fontsiyuanback35.fontHeight
                        else -> mc.fontRendererObj.FONT_HEIGHT
                    }
                    
                    val backgroundX = x + backgroundX
                    val backgroundY = y + backgroundY
                    val backgroundWidth = x + textWidth + backgroundWidth - backgroundX
                    val backgroundHeight = y + textHeight + backgroundHeight - backgroundY
                    
                    // 绘制阴影
                    showShadow(backgroundX, backgroundY, backgroundWidth, backgroundHeight)
                    
                    // 使用 Text.kt 中的方法绘制圆角矩形
                    RenderUtils.drawRoundedRect(
                        backgroundX, 
                        backgroundY, 
                        x + textWidth + backgroundWidth, 
                        y + textHeight + backgroundHeight, 
                        Color(0, 0, 0, 128).rgb,
                        nyaBackgroundRadius
                    )
                }
                
                // 绘制文字 - 分开绘制自定义文本和其余部分
                val customTextWidth = when (fontSelection) {
                    "Default" -> mc.fontRendererObj.getStringWidth(nyaCustomText)
                    "Fortalesia40" -> Fonts.fontFortalesia40.getStringWidth(nyaCustomText)
                    "Augs" -> Fonts.fontAugs40.getStringWidth(nyaCustomText)
                    "SiyuanBack45" -> Fonts.fontsiyuanback45.getStringWidth(nyaCustomText)
                    "SiyuanBack35" -> Fonts.fontsiyuanback35.getStringWidth(nyaCustomText)
                    else -> mc.fontRendererObj.getStringWidth(nyaCustomText)
                }
                
                val restText = " | ${fps}FPS | $serverIP | $time"
                
                when (fontSelection) {
                    "Default" -> {
                        // 先绘制自定义文本
                        mc.fontRendererObj.drawString(
                            nyaCustomText,
                            x,
                            y,
                            nyaCustomTextColor.rgb,
                            nyaShadow
                        )
                        // 再绘制其余文本
                        mc.fontRendererObj.drawString(
                            restText,
                            x + customTextWidth,
                            y,
                            nyaColor.rgb,
                            nyaShadow
                        )
                    }
                    "Fortalesia40" -> {
                        Fonts.fontFortalesia40.drawString(
                            nyaCustomText,
                            x,
                            y,
                            nyaCustomTextColor.rgb,
                            nyaShadow
                        )
                        Fonts.fontFortalesia40.drawString(
                            restText,
                            x + customTextWidth,
                            y,
                            nyaColor.rgb,
                            nyaShadow
                        )
                    }
                    "Augs" -> {
                        Fonts.fontAugs40.drawString(
                            nyaCustomText,
                            x,
                            y,
                            nyaCustomTextColor.rgb,
                            nyaShadow
                        )
                        Fonts.fontAugs40.drawString(
                            restText,
                            x + customTextWidth,
                            y,
                            nyaColor.rgb,
                            nyaShadow
                        )
                    }
                    "SiyuanBack45" -> {
                        Fonts.fontsiyuanback45.drawString(
                            nyaCustomText,
                            x,
                            y,
                            nyaCustomTextColor.rgb,
                            nyaShadow
                        )
                        Fonts.fontsiyuanback45.drawString(
                            restText,
                            x + customTextWidth,
                            y,
                            nyaColor.rgb,
                            nyaShadow
                        )
                    }
                    "SiyuanBack35" -> {
                        Fonts.fontsiyuanback35.drawString(
                            nyaCustomText,
                            x,
                            y,
                            nyaCustomTextColor.rgb,
                            nyaShadow
                        )
                        Fonts.fontsiyuanback35.drawString(
                            restText,
                            x + customTextWidth,
                            y,
                            nyaColor.rgb,
                            nyaShadow
                        )
                    }
                }
            }
        }
    }
    
    override val tag: String?
        get() = when (displayMode) {
            "Image" -> "$displayMode-$imageType"
            "Text" -> "$displayMode-$fontSelection"
            "Nya" -> "$displayMode-$fontSelection"
            else -> displayMode
        }
}