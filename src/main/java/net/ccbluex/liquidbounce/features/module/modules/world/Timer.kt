/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.MathHelper
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object Timer : Module("Timer", Category.WORLD, gameDetecting = false) {

    private val mode by choices("Mode", arrayOf("OnMove", "NoMove", "Always", "Balance"), "OnMove")
    private val speed by float("Speed", 2F, 0.1F..10F)
    
    // Balance mode specific values
    private val timerSpeed by float("Balance-TimerSpeed", 4F, 0.1F..10F) { mode == "Balance" } // Timer speed
    private val timerMaxSet by int("Balance-TimerMaxSet", 5, 0..30) { mode == "Balance" } // 等待积攒时间
    private val timerSet by int("Balance-TimerSet", 5, 0..30) { mode == "Balance" } // 释放时间

    private val counter by boolean("Balance-Counter", true) { mode == "Balance" } // 渲染框架
    private val startXMode by choices("Balance-StartXMode", arrayOf("-","+"), "-", { mode == "Balance" })
    private val startXValue by int("Balance-StartX", 75, 0..450) { mode == "Balance" }
    private val startYMode by choices("Balance-StartYMode", arrayOf("-","+"), "+", { mode == "Balance" })
    private val startYValue by int("Balance-StartY", 20, 0..450) { mode == "Balance" }

    private val fontR by int("Balance-FontR", 255, 0..255) { mode == "Balance" }
    private val fontG by int("Balance-FontG", 255, 0..255) { mode == "Balance" }
    private val fontB by int("Balance-FontB", 255, 0..255) { mode == "Balance" }
    private val fontA by int("Balance-FontA", 255, 0..255) { mode == "Balance" }

    private val rectangleR by int("Balance-RectangleR", 0, 0..255) { mode == "Balance" }
    private val rectangleG by int("Balance-RectangleG", 0, 0..255) { mode == "Balance" }
    private val rectangleB by int("Balance-RectangleB", 0, 0..255) { mode == "Balance" }
    private val rectangleA by int("Balance-RectangleA", 125, 0..255) { mode == "Balance" }

    private val frameMode by choices("Balance-FrameMode", arrayOf("<","^"), "^", { mode == "Balance" })
    private val frameR by int("Balance-FrameR", 0, 0..255) { mode == "Balance" }
    private val frameG by int("Balance-FrameG", 255, 0..255) { mode == "Balance" }
    private val frameB by int("Balance-FrameB", 255, 0..255) { mode == "Balance" }
    private val frameA by int("Balance-FrameA", 255, 0..255) { mode == "Balance" }

    // Balance mode variables
    private var lastMS = 0L
    private var s = 0
    private var m = 0
    private var timerActive = false
    private var progress = 0f

    override fun onEnable() {
        progress = 0f
        s = 0
        m = 0
        timerActive = false
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1F
        s = 0
        m = 0
        timerActive = false
    }
    
    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        when (mode) {
            "Always" -> mc.timer.timerSpeed = speed
            "OnMove" -> if (player.isMoving) mc.timer.timerSpeed = speed else mc.timer.timerSpeed = 1F
            "NoMove" -> if (!player.isMoving) mc.timer.timerSpeed = speed else mc.timer.timerSpeed = 1F
            "Balance" -> {
                if (timerActive) {
                    s++
                } else {
                    m++
                }
                if (m == timerMaxSet) {
                    timerActive = true
                    mc.timer.timerSpeed = timerSpeed
                    m = 0
                    s = 0
                }
                if (s == timerSet) {
                    timerActive = false
                    mc.timer.timerSpeed = 1F
                    m = 0
                    s = 0
                }
            }
        }
    }

    val onWorld = handler<WorldEvent> {
        if (it.worldClient == null)
            state = false
    }
    
    val onRender2D = handler<Render2DEvent> {
        if (mode == "Balance" && counter) {
            progress = (System.currentTimeMillis() - lastMS).toFloat() / 100f
            if (progress >= 1) progress = 1f
            val scaledResolution = ScaledResolution(mc)
            val counterMode = counter
            val startX = if (startXMode == "-") scaledResolution.scaledWidth / 2 - startXValue else scaledResolution.scaledWidth / 2 + startXValue
            val startY = if (startYMode == "-") scaledResolution.scaledHeight / 2 - startYValue else scaledResolution.scaledHeight / 2 + startYValue

            if (counterMode) {
                // Render the timer balance UI
                drawShadow(startX.toFloat(), startY.toFloat(), 160f, 21f)
                glPushMatrix()
                glPushAttrib(GL_ALL_ATTRIB_BITS)

                // Draw background rectangle
                drawRoundedRect(startX.toFloat(), startY.toFloat(), startX.toFloat() + 160f, startY.toFloat() + 22f, Color(rectangleR, rectangleG, rectangleB, rectangleA).rgb)
                
                // Draw frame based on mode
                when (frameMode.toLowerCase()) {
                    "^" -> {
                        drawRoundedRect(startX.toFloat(), startY.toFloat(), startX.toFloat() + 160f, startY.toFloat() + 3f, Color(frameR, frameG, frameB, frameA).rgb)
                        mc.fontRendererObj.drawString("Timer：$m ReleaseTime：$s", (startX + 36).toInt(), (startY + 7.5).toInt(), Color(fontR, fontG, fontB, fontA).rgb)
                    }
                    "<" -> {
                        drawRoundedRect(startX.toFloat(), startY.toFloat(), startX.toFloat() + 3f, startY.toFloat() + 22f, Color(frameR, frameG, frameB, frameA).rgb)
                        mc.fontRendererObj.drawString("Timer：$m ReleaseTime：$s", (startX + 36).toInt(), (startY + 6.5).toInt(), Color(fontR, fontG, fontB, fontA).rgb)
                    }
                }

                glPopAttrib()
                glPopMatrix()
            }
        }
    }

    // Simple shadow drawing function
    private fun drawShadow(x: Float, y: Float, width: Float, height: Float) {
        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)

        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // Draw shadow with rounded corners
        val shadowColor = Color(0, 0, 0, 70)
        drawRoundedRect(x - 2f, y - 2f, x + width + 2f, y + height + 2f, shadowColor.rgb)

        glPopAttrib()
        glPopMatrix()
    }

    // Simple rounded rectangle drawing function
    private fun drawRoundedRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        val alpha = (color shr 24 and 0xff) / 255.0f
        val red = (color shr 16 and 0xff) / 255.0f
        val green = (color shr 8 and 0xff) / 255.0f
        val blue = (color and 0xff) / 255.0f

        glColor4f(red, green, blue, alpha)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glShadeModel(GL_SMOOTH)

        glBegin(GL_QUADS)
        glVertex2f(x1, y1)
        glVertex2f(x1, y2)
        glVertex2f(x2, y2)
        glVertex2f(x2, y1)
        glEnd()

        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
    }
}