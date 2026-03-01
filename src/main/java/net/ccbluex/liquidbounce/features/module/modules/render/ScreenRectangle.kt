/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.GlowUtils2
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.minecraft.client.gui.ScaledResolution
import java.awt.Color

/**
 * ScreenRectangle module
 * Draws a customizable rectangle in the center of the screen.
 * - Mica: Gaussian blur only.
 * - Blur: Gaussian blur + white shadows (top, bottom, surface).
 */
object ScreenRectangle : Module("ScreenRectangle", Category.RENDER) {

    // ========== 配置 ==========
    private val mode by choices("Mode", arrayOf("Mica", "Blur"), "Mica")
    private val rectColor = ColorSettingsInteger(this, "RectColor").with(255, 255, 255)

    // 固定尺寸与模糊半径
    private const val RECT_WIDTH = 400
    private const val RECT_HEIGHT = 200
    private const val BLUR_RADIUS = 12          // 高斯模糊半径，强度适中

    // ========== 渲染事件 ==========
    val onRender2D = handler<Render2DEvent> {
        val sr = ScaledResolution(mc)
        val x = (sr.scaledWidth - RECT_WIDTH) / 2.0
        val y = (sr.scaledHeight - RECT_HEIGHT) / 2.0
        val color = rectColor.color()

        // 1. 绘制高斯模糊背景（云母/毛玻璃共用）
        GlowUtils2.drawGlow(
            x.toFloat(), y.toFloat(),
            RECT_WIDTH.toFloat(), RECT_HEIGHT.toFloat(),
            BLUR_RADIUS, color
        )

        // 2. 毛玻璃模式添加白色阴影
        if (mode.equals("Blur", ignoreCase = true)) {
            drawWhiteShadows(x, y, x + RECT_WIDTH, y + RECT_HEIGHT)
        }
    }

    // ========== 白色阴影绘制（上/下/表面） ==========
    private fun drawWhiteShadows(left: Double, top: Double, right: Double, bottom: Double) {
        // 上阴影（向外扩展，半透白）
        drawRect(
            (left - 2).toFloat(), (top - 4).toFloat(),
            (right + 2).toFloat(), (top - 1).toFloat(),
            Color(255, 255, 255, 50).rgb
        )
        // 下阴影
        drawRect(
            (left - 2).toFloat(), (bottom + 1).toFloat(),
            (right + 2).toFloat(), (bottom + 4).toFloat(),
            Color(255, 255, 255, 50).rgb
        )
        // 表面阴影（整体泛白）
        drawRect(
            left.toFloat(), top.toFloat(),
            right.toFloat(), bottom.toFloat(),
            Color(255, 255, 255, 25).rgb
        )
    }
}