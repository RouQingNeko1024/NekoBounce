package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.fontmanager.GuiFontManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.pow

class GuiMainMenu : AbstractScreen() {

    private val backgroundTexture = ResourceLocation("liquidbounce/background/background.jpg")

    // 图标资源
    private val iconLogo = ResourceLocation("liquidbounce/icons/logo.png")
    private val iconSingle = ResourceLocation("liquidbounce/icons/singleplayer.png")
    private val iconMulti = ResourceLocation("liquidbounce/icons/multiplayer.png")
    private val iconAlt = ResourceLocation("liquidbounce/icons/alt.png")
    private val iconProxy = ResourceLocation("liquidbounce/icons/proxy.png")
    private val iconOptions = ResourceLocation("liquidbounce/icons/options.png")
    private val iconExit = ResourceLocation("liquidbounce/icons/exit.png")

    private var initTime = System.currentTimeMillis()

    // 颜色定义
    // 基础强调色 (蓝紫色调的亮灰)
    private val accentColor = Color(110, 110, 135)
    // 选中时的胶囊背景色
    private val highlightColor = Color(83, 82, 101)

    companion object {
        private var popupOnce = false
    }

    init {
        if (!popupOnce) {
            if (FileManager.firstStart) popupOnce = true
        }
    }

    override fun initGui() {
        initTime = System.currentTimeMillis()

        val centerX = width / 2
        val centerY = height / 2

        val lineHeight = 26
        // 列表起始Y坐标
        val panelTopY = centerY - 40
        val listStartY = panelTopY + 12

        buttonList.clear()

        buttonList.add(IconListButton(1, centerX, listStartY,                  I18n.format("menu.singleplayer"), iconSingle))
        buttonList.add(IconListButton(2, centerX, listStartY + lineHeight,     I18n.format("menu.multiplayer"), iconMulti))
        buttonList.add(IconListButton(100, centerX, listStartY + lineHeight * 2, translationMenu("altManager"), iconAlt))
        buttonList.add(IconListButton(109, centerX, listStartY + lineHeight * 3, "Font Manager", iconProxy))
        buttonList.add(IconListButton(0, centerX, listStartY + lineHeight * 4, I18n.format("menu.options"), iconOptions))
        buttonList.add(IconListButton(4, centerX, listStartY + lineHeight * 5, I18n.format("menu.quit"), iconExit))
    }

    private fun easeOutExpo(x: Float): Float {
        return if (x == 1f) 1f else 1f - 2f.pow(-10f * x)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val time = System.currentTimeMillis() - initTime
        val animProgress = (time / 800f).coerceIn(0f, 1f)
        val easedProgress = easeOutExpo(animProgress)

        // 1. 绘制背景
        mc.textureManager.bindTexture(backgroundTexture)
        GlStateManager.color(1f, 1f, 1f, 1f)
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0f, 0f, width, height, width.toFloat(), height.toFloat())

        // 2. 绘制全屏暗色遮罩
        drawGradientRect(0, 0, width, height, 0x00000000, Color(0, 0, 0, (200 * easedProgress).toInt()).rgb)

        // --- 3. 绘制 Logo ---
        val logoAlpha = easedProgress
        val logoSize = 64
        val logoX = (width - logoSize) / 2
        val logoY = (height / 2) - 130

        GlStateManager.pushMatrix()
        GlStateManager.translate(0f, (1f - easedProgress) * -30f, 0f)

        val logoDrawColor = Color(255, 255, 255, (255 * logoAlpha).toInt())
        drawImage(iconLogo, logoX, logoY, logoSize, logoSize, logoDrawColor)

        GlStateManager.popMatrix()


        // --- 4. 绘制菜单背景板 ---
        GlStateManager.pushMatrix()

        val panelTranslateY = (1f - easedProgress) * 50f
        GlStateManager.translate(0f, panelTranslateY, 0f)

        // 背景板尺寸
        val panelW = 170f
        val panelH = 175f
        val panelX = (width - panelW) / 2f
        val panelY = (height / 2f) - 40f

        // 深色圆角矩形背景
        drawRoundedRect(
            panelX, panelY,
            panelX + panelW, panelY + panelH,
            15f,
            Color(20, 20, 20, (200 * easedProgress).toInt()).rgb
        )

        super.drawScreen(mouseX, mouseY, partialTicks)

        GlStateManager.popMatrix()
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            1 -> mc.displayGuiScreen(GuiSelectWorld(this))
            2 -> mc.displayGuiScreen(GuiMultiplayer(this))
            4 -> mc.shutdown()
            100 -> mc.displayGuiScreen(GuiAltManager(this))
            109 -> mc.displayGuiScreen(GuiFontManager(this))
        }
    }

    // --- 绘图工具 ---

    fun drawImage(image: ResourceLocation, x: Int, y: Int, width: Int, height: Int, color: Color) {
        mc.textureManager.bindTexture(image)
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, width, height, width.toFloat(), height.toFloat())
        GlStateManager.resetColor()
    }

    private fun drawRoundedRect(x: Float, y: Float, x2: Float, y2: Float, radius: Float, color: Int) {
        val r = (color shr 16 and 0xFF) / 255.0f
        val g = (color shr 8 and 0xFF) / 255.0f
        val b = (color and 0xFF) / 255.0f
        val a = (color shr 24 and 0xFF) / 255.0f
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(r, g, b, a)
        GL11.glBegin(GL11.GL_POLYGON)
        for (i in 0..360 step 15) {
            val rad = Math.toRadians(i.toDouble())
            val px = if (i in 0..180) x2 - radius else x + radius
            val py = if (i in 0..90 || i in 270..360) y + radius else y2 - radius
            GL11.glVertex2d(px + Math.sin(rad) * radius, py - Math.cos(rad) * radius)
        }
        GL11.glEnd()
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    inner class IconListButton(
        id: Int, centerX: Int, y: Int, text: String, private val iconLoc: ResourceLocation
    ) : GuiButton(id, centerX - 80, y, 160, 24, text) {

        private var hoverProgress = 0f
        private var lastMs = System.currentTimeMillis()
        private val buttonIndex = buttonList.indexOf(this)

        override fun drawButton(mc: Minecraft, mouseX: Int, mouseY: Int) {
            if (!this.visible) return

            val time = System.currentTimeMillis() - initTime
            val delay = buttonIndex * 60L
            if (time < delay) return

            val animProgress = ((time - delay) / 500f).coerceIn(0f, 1f)
            val easedAnim = 1f - (1f - animProgress).pow(3)
            val offsetY = (1f - easedAnim) * 15f
            val alpha = easedAnim

            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                    mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height

            val now = System.currentTimeMillis()
            val delta = now - lastMs
            lastMs = now

            if (this.hovered) {
                hoverProgress = (hoverProgress + 0.005f * delta).coerceAtMost(1f)
            } else {
                hoverProgress = (hoverProgress - 0.005f * delta).coerceAtLeast(0f)
            }

            GlStateManager.pushMatrix()
            GlStateManager.translate(0f, offsetY, 0f)

            val baseContentAlpha = (255 * alpha).toInt()

            // 1. 绘制胶囊背景
            // 颜色 83, 82, 101, 透明度跟随悬停动画
            val bgAlpha = (255 * hoverProgress * alpha).toInt()
            if (bgAlpha > 1) {
                val radius = this.height / 2f
                val bgX = this.xPosition + 25f
                val bgWidth = 110f

                drawRoundedRect(
                    bgX,
                    this.yPosition.toFloat(),
                    bgX + bgWidth,
                    this.yPosition + this.height.toFloat(),
                    radius,
                    Color(highlightColor.red, highlightColor.green, highlightColor.blue, bgAlpha).rgb
                )
            }

            // 2. 绘制图标
            // [逻辑] 基础强调色 -> 白色，基于 hoverProgress 进行插值
            val startColor = accentColor
            val targetColor = Color.WHITE

            val iconR = (startColor.red   + (targetColor.red   - startColor.red)   * hoverProgress).toInt()
            val iconG = (startColor.green + (targetColor.green - startColor.green) * hoverProgress).toInt()
            val iconB = (startColor.blue  + (targetColor.blue  - startColor.blue)  * hoverProgress).toInt()

            val iconColor = Color(iconR, iconG, iconB, baseContentAlpha)
            val iconSize = 14
            val iconY = (this.yPosition + (this.height - iconSize) / 2)

            drawImage(iconLoc, this.xPosition + 30, iconY, iconSize, iconSize, iconColor)

            // 3. 绘制文字
            // 文字保持强调色，无描边，保持简洁
            GlStateManager.resetColor()
            val textColor = Color(accentColor.red, accentColor.green, accentColor.blue, baseContentAlpha).rgb

            Fonts.fontsiyuanback40.drawString(
                this.displayString,
                (this.xPosition + 55).toFloat(),
                (this.yPosition + (this.height / 2f) - 3f),
                textColor,
                false // 不开启默认黑色阴影
            )

            GlStateManager.popMatrix()
        }
    }
}