/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scale
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui.clamp
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts.fontSemibold35
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.blendColors
import net.ccbluex.liquidbounce.utils.render.ColorUtils.minecraftRed
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexture
import net.ccbluex.liquidbounce.utils.render.RenderUtils.updateTextureCache
import net.ccbluex.liquidbounce.utils.ui.EditableText
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.StringUtils
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color
import kotlin.math.abs
import kotlin.math.roundToInt

@SideOnly(Side.CLIENT)
object AugustusStyle : Style() {
    // 存储当前选中的模块
    private var selectedModuleElement: ModuleElement? = null
    
    // 分类按钮区域高度
    private val categoryBarHeight = 25
    
    // 模块列表区域宽度
    private val moduleListWidth = 120
    
    // 跟踪每个面板的模块索引
    private val moduleIndices = mutableMapOf<Panel, MutableMap<ModuleElement, Int>>()
    
    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        // 绘制主面板边框和背景
        drawBorderedRect(
            panel.x.toFloat(),
            panel.y.toFloat(),
            (panel.x + panel.width).toFloat(),
            (panel.y + panel.height).toFloat(),
            1.0f,
            Color.GRAY.rgb,
            Int.MIN_VALUE
        )
        
        // 绘制分类栏
        drawBorderedRect(
            panel.x.toFloat(),
            panel.y.toFloat(),
            (panel.x + panel.width).toFloat(),
            (panel.y + categoryBarHeight).toFloat(),
            1.0f,
            Color.GRAY.rgb,
            Color(20, 20, 20).rgb
        )
        
        // 绘制分类名称
        val xPos = panel.x - (fontSemibold35.getStringWidth(StringUtils.stripControlCodes(panel.name)) - 100) / 2
        fontSemibold35.drawString(panel.name, xPos.toFloat(), (panel.y + 6).toFloat(), Color.WHITE.rgb)
        
        // 绘制模块列表区域和设置区域的分隔线
        val moduleListEndX = panel.x + moduleListWidth
        drawRect(
            moduleListEndX.toFloat(),
            (panel.y + categoryBarHeight).toFloat(),
            (moduleListEndX + 1).toFloat(),
            (panel.y + panel.height).toFloat(),
            Color.GRAY.rgb
        )
        
        // 绘制模块列表区域背景
        drawRect(
            (panel.x + 1).toFloat(),
            (panel.y + categoryBarHeight + 1).toFloat(),
            moduleListEndX.toFloat(),
            (panel.y + panel.height - 1).toFloat(),
            Color(30, 30, 30).rgb
        )
        
        // 绘制设置区域背景
        drawRect(
            (moduleListEndX + 1).toFloat(),
            (panel.y + categoryBarHeight + 1).toFloat(),
            (panel.x + panel.width - 1).toFloat(),
            (panel.y + panel.height - 1).toFloat(),
            Color(25, 25, 25).rgb
        )
        
        // 绘制模块列表标题
        fontSemibold35.drawString(
            "Modules",
            (panel.x + 10).toFloat(),
            (panel.y + categoryBarHeight + 8).toFloat(),
            Color.WHITE.rgb
        )
        
        // 如果有选中的模块，绘制模块名称在设置区域
        selectedModuleElement?.let { moduleElement ->
            // 检查选中的模块是否属于当前面板
            val belongsToPanel = panel.elements.contains(moduleElement)
            if (belongsToPanel) {
                fontSemibold35.drawString(
                    moduleElement.displayName,
                    (moduleListEndX + 15).toFloat(),
                    (panel.y + categoryBarHeight + 8).toFloat(),
                    if (moduleElement.module.state) guiColor else Color.WHITE.rgb
                )
            }
        }
    }
    
    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.lines()
        
        val width = lines.maxOfOrNull { fontSemibold35.getStringWidth(it) + 14 }
            ?: return // 如果没有文本则不渲染
        val height = fontSemibold35.fontHeight * lines.size + 3
        
        // 确保悬停文本在窗口边界内
        val (scaledWidth, scaledHeight) = ScaledResolution(mc)
        val x = mouseX.clamp(0, (scaledWidth / scale - width).roundToInt())
        val y = mouseY.clamp(0, (scaledHeight / scale - height).roundToInt())
        
        drawBorderedRect((x + 9).toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), 1.0f, Color.GRAY.rgb, Int.MIN_VALUE)
        lines.forEachIndexed { index, text ->
            fontSemibold35.drawString(text, (x + 12).toFloat(), (y + 3 + (fontSemibold35.fontHeight) * index).toFloat(), Int.MAX_VALUE)
        }
    }
    
    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
        val xPos = buttonElement.x - (fontSemibold35.getStringWidth(buttonElement.displayName) - 100) / 2
        fontSemibold35.drawString(buttonElement.displayName, xPos.toFloat(), (buttonElement.y + 6).toFloat(), buttonElement.color)
    }
    
    override fun drawModuleElementAndClick(
        mouseX: Int, mouseY: Int, moduleElement: ModuleElement, mouseButton: Int?
    ): Boolean {
        // 获取父面板（通过查找包含此模块的面板）
        val parentPanel = findParentPanel(moduleElement) ?: return false
        val moduleListEndX = parentPanel.x + moduleListWidth
        
        // 初始化模块索引
        if (!moduleIndices.containsKey(parentPanel)) {
            moduleIndices[parentPanel] = mutableMapOf()
        }
        val panelMap = moduleIndices[parentPanel]!!
        
        // 为模块分配索引（如果还没有）
        if (!panelMap.containsKey(moduleElement)) {
            panelMap[moduleElement] = panelMap.size
        }
        val moduleIndex = panelMap[moduleElement]!!
        
        // 模块元素现在绘制在左侧模块列表区域
        val moduleY = parentPanel.y + categoryBarHeight + 25 + (moduleIndex * 20)
        val moduleX = parentPanel.x + 10
        
        // 检查是否在模块列表区域内
        val inModuleList = mouseX in parentPanel.x..moduleListEndX && 
                          mouseY in parentPanel.y + categoryBarHeight..parentPanel.y + parentPanel.height
        
        // 绘制模块元素
        val isSelected = selectedModuleElement == moduleElement
        val isModuleActive = moduleElement.module.state
        
        // 绘制模块背景（如果被选中）
        if (isSelected && inModuleList) {
            drawRect(
                (moduleX - 2).toFloat(),
                (moduleY - 2).toFloat(),
                (moduleX + 110).toFloat(),
                (moduleY + 12).toFloat(),
                Color(50, 50, 50).rgb
            )
        }
        
        // 绘制模块名称
        fontSemibold35.drawString(
            moduleElement.displayName,
            moduleX.toFloat(),
            moduleY.toFloat(),
            if (isModuleActive) guiColor else Color.WHITE.rgb
        )
        
        // 处理模块点击
        if (inModuleList && mouseButton == 0 && 
            mouseX in moduleX..(moduleX + 110) && 
            mouseY in (moduleY - 2)..(moduleY + 12)) {
            selectedModuleElement = moduleElement
            return true
        }
        
        // 绘制模块设置（如果有选中的模块且该模块被选中）
        if (isSelected && selectedModuleElement == moduleElement) {
            return drawModuleSettings(mouseX, mouseY, moduleElement, mouseButton, parentPanel, moduleListEndX)
        }
        
        return false
    }
    
    private fun findParentPanel(moduleElement: ModuleElement): Panel? {
        // 由于ModuleElement没有直接引用其父Panel，我们需要通过其他方式查找
        // 这里我们假设通过ClickGui的panels列表来查找
        val clickGui = net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
        for (panel in clickGui.panels) {
            if (panel.elements.contains(moduleElement)) {
                return panel
            }
        }
        return null
    }
    
    private fun drawModuleSettings(
        mouseX: Int, 
        mouseY: Int, 
        moduleElement: ModuleElement, 
        mouseButton: Int?, 
        panel: Panel,
        settingsStartX: Int
    ): Boolean {
        val moduleValues = moduleElement.module.values.filter { it.shouldRender() }
        if (moduleValues.isEmpty()) {
            return false
        }
        
        val settingsAreaY = panel.y + categoryBarHeight + 25
        var yPos = settingsAreaY
        
        for (value in moduleValues) {
            assumeNonVolatile = value.get() is Number
            
            val suffix = value.suffix ?: ""
            
            when (value) {
                is BoolValue -> {
                    val text = value.name
                    
                    // 检查点击
                    val valueWidth = fontSemibold35.getStringWidth(text) + 20
                    val valueX = settingsStartX + 10
                    
                    if (mouseButton == 0 && 
                        mouseX in valueX..(valueX + valueWidth) && 
                        mouseY in yPos + 2..yPos + 14) {
                        value.toggle()
                        clickSound()
                        return true
                    }
                    
                    // 绘制复选框和文本
                    drawRect(
                        valueX.toFloat(),
                        (yPos + 4).toFloat(),
                        (valueX + 10).toFloat(),
                        (yPos + 14).toFloat(),
                        Color.DARK_GRAY.rgb
                    )
                    if (value.get()) {
                        drawRect(
                            (valueX + 2).toFloat(),
                            (yPos + 6).toFloat(),
                            (valueX + 8).toFloat(),
                            (yPos + 12).toFloat(),
                            guiColor
                        )
                    }
                    
                    fontSemibold35.drawString(
                        text, (valueX + 15).toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb
                    )
                    
                    yPos += 16
                }
                
                is ListValue -> {
                    val text = value.name
                    val valueX = settingsStartX + 10
                    val valueWidth = fontSemibold35.getStringWidth(text) + 20
                    
                    if (mouseButton == 0 && 
                        mouseX in valueX..(valueX + valueWidth) && 
                        mouseY in yPos + 2..yPos + 14) {
                        value.openList = !value.openList
                        clickSound()
                        return true
                    }
                    
                    fontSemibold35.drawString(text, valueX.toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    fontSemibold35.drawString(
                        if (value.openList) "-" else "+",
                        (valueX + valueWidth - 10).toFloat(),
                        (yPos + 4).toFloat(),
                        Color.WHITE.rgb
                    )
                    
                    yPos += 16
                    
                    // 绘制列表选项
                    if (value.openList) {
                        for (valueOfList in value.values) {
                            val optionX = settingsStartX + 20
                            val optionWidth = fontSemibold35.getStringWidth(valueOfList) + 10
                            
                            if (mouseButton == 0 && 
                                mouseX in optionX..(optionX + optionWidth) && 
                                mouseY in yPos + 2..yPos + 14) {
                                value.set(valueOfList)
                                clickSound()
                                return true
                            }
                            
                            val isSelected = value.get() == valueOfList
                            if (isSelected) {
                                drawRect(
                                    (optionX - 2).toFloat(),
                                    (yPos + 2).toFloat(),
                                    (optionX + optionWidth + 2).toFloat(),
                                    (yPos + 16).toFloat(),
                                    Color(50, 50, 50).rgb
                                )
                            }
                            
                            fontSemibold35.drawString(
                                valueOfList,
                                optionX.toFloat(),
                                (yPos + 4).toFloat(),
                                if (isSelected) guiColor else Color.LIGHT_GRAY.rgb
                            )
                            
                            yPos += 16
                        }
                    }
                }
                
                is FloatValue -> {
                    val text = value.name + "§f: §c" + round(value.get()) + " §8${suffix}§c"
                    val valueX = settingsStartX + 10
                    val sliderWidth = 150
                    
                    // 绘制滑块背景
                    drawRect(
                        valueX.toFloat(),
                        (yPos + 18).toFloat(),
                        (valueX + sliderWidth).toFloat(),
                        (yPos + 19).toFloat(),
                        Color.DARK_GRAY.rgb
                    )
                    
                    // 计算滑块位置
                    val displayValue = value.get().coerceIn(value.range)
                    val sliderPos = ((displayValue - value.minimum) / (value.maximum - value.minimum) * sliderWidth).toInt()
                    
                    // 绘制滑块
                    drawRect(
                        (valueX + sliderPos - 3).toFloat(),
                        (yPos + 15).toFloat(),
                        (valueX + sliderPos + 3).toFloat(),
                        (yPos + 21).toFloat(),
                        guiColor
                    )
                    
                    // 处理滑块拖动
                    if (mouseButton == 0 && 
                        mouseX in valueX..(valueX + sliderWidth) && 
                        mouseY in yPos + 15..yPos + 21) {
                        val percentage = (mouseX - valueX) / sliderWidth.toFloat()
                        value.set(value.minimum + (value.maximum - value.minimum) * percentage)
                        return true
                    }
                    
                    // 绘制文本
                    fontSemibold35.drawString(text, valueX.toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    
                    yPos += 25
                }
                
                is IntValue -> {
                    val text = value.name + "§f: §c" + value.get() + " §8${suffix}"
                    val valueX = settingsStartX + 10
                    val sliderWidth = 150
                    
                    // 绘制滑块背景
                    drawRect(
                        valueX.toFloat(),
                        (yPos + 18).toFloat(),
                        (valueX + sliderWidth).toFloat(),
                        (yPos + 19).toFloat(),
                        Color.DARK_GRAY.rgb
                    )
                    
                    // 计算滑块位置
                    val displayValue = value.get().coerceIn(value.range)
                    val sliderPos = ((displayValue - value.minimum).toFloat() / (value.maximum - value.minimum) * sliderWidth).toInt()
                    
                    // 绘制滑块
                    drawRect(
                        (valueX + sliderPos - 3).toFloat(),
                        (yPos + 15).toFloat(),
                        (valueX + sliderPos + 3).toFloat(),
                        (yPos + 21).toFloat(),
                        guiColor
                    )
                    
                    // 处理滑块拖动
                    if (mouseButton == 0 && 
                        mouseX in valueX..(valueX + sliderWidth) && 
                        mouseY in yPos + 15..yPos + 21) {
                        val percentage = (mouseX - valueX) / sliderWidth.toFloat()
                        value.set((value.minimum + (value.maximum - value.minimum) * percentage).toInt())
                        return true
                    }
                    
                    // 绘制文本
                    fontSemibold35.drawString(text, valueX.toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    
                    yPos += 25
                }
                
                is FloatRangeValue -> {
                    val slider1 = value.get().start
                    val slider2 = value.get().endInclusive
                    val text = value.name + "§f: §c" + round(slider1) + " §f- §c" + round(slider2) + " §8${suffix}"
                    val valueX = settingsStartX + 10
                    val sliderWidth = 150
                    
                    // 绘制滑块背景
                    drawRect(
                        valueX.toFloat(),
                        (yPos + 18).toFloat(),
                        (valueX + sliderWidth).toFloat(),
                        (yPos + 19).toFloat(),
                        Color.DARK_GRAY.rgb
                    )
                    
                    // 计算滑块位置
                    val displayValue1 = value.get().start
                    val displayValue2 = value.get().endInclusive
                    val sliderPos1 = ((displayValue1 - value.minimum) / (value.maximum - value.minimum) * sliderWidth).toInt()
                    val sliderPos2 = ((displayValue2 - value.minimum) / (value.maximum - value.minimum) * sliderWidth).toInt()
                    
                    // 绘制滑块
                    drawRect(
                        (valueX + sliderPos1 - 3).toFloat(),
                        (yPos + 15).toFloat(),
                        (valueX + sliderPos1 + 3).toFloat(),
                        (yPos + 21).toFloat(),
                        guiColor
                    )
                    drawRect(
                        (valueX + sliderPos2 - 3).toFloat(),
                        (yPos + 15).toFloat(),
                        (valueX + sliderPos2 + 3).toFloat(),
                        (yPos + 21).toFloat(),
                        guiColor
                    )
                    
                    // 处理滑块拖动（简化处理，只拖动第一个滑块）
                    if (mouseButton == 0 && 
                        mouseX in valueX..(valueX + sliderWidth) && 
                        mouseY in yPos + 15..yPos + 21) {
                        val percentage = (mouseX - valueX) / sliderWidth.toFloat()
                        val newValue = value.minimum + (value.maximum - value.minimum) * percentage
                        // 这里简化处理，实际应该根据靠近哪个滑块来决定修改哪个值
                        value.set(newValue..newValue)
                        return true
                    }
                    
                    // 绘制文本
                    fontSemibold35.drawString(text, valueX.toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    
                    yPos += 25
                }
                
                // 其他Value类型的处理
                is BlockValue -> {
                    val text = value.name + "§f: §c" + getBlockName(value.get()) + " (" + value.get() + ")" + " §8${suffix}"
                    val valueX = settingsStartX + 10
                    
                    fontSemibold35.drawString(text, valueX.toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    yPos += 16
                }
                
                is IntRangeValue -> {
                    val slider1 = value.get().first
                    val slider2 = value.get().last
                    val text = value.name + "§f: §c" + slider1 + " §f- §c" + slider2 + " §8${suffix}"
                    val valueX = settingsStartX + 10
                    
                    fontSemibold35.drawString(text, valueX.toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    yPos += 16
                }
                
                is FontValue -> {
                    val displayString = value.displayName
                    val valueX = settingsStartX + 10
                    
                    if (mouseButton != null && mouseX in valueX..(valueX + fontSemibold35.getStringWidth(displayString) + 20) && 
                        mouseY in yPos + 4..yPos + 12) {
                        // 循环切换字体
                        if (mouseButton == 0) value.next()
                        else value.previous()
                        clickSound()
                        return true
                    }
                    
                    fontSemibold35.drawString(displayString, valueX.toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    yPos += 16
                }
                
                is ColorValue -> {
                    val currentColor = value.selectedColor()
                    val text = value.name + ": #" + "%08X".format(currentColor.rgb)
                    val valueX = settingsStartX + 10
                    
                    // 绘制颜色预览
                    drawRect(
                        valueX.toFloat(),
                        (yPos + 4).toFloat(),
                        (valueX + 20).toFloat(),
                        (yPos + 14).toFloat(),
                        currentColor.rgb
                    )
                    
                    // 绘制文本
                    fontSemibold35.drawString(text, (valueX + 25).toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    
                    // 点击颜色预览可以打开/关闭颜色选择器
                    if (mouseButton == 0 && mouseX in valueX..(valueX + 20) && mouseY in yPos + 4..yPos + 14) {
                        value.showPicker = !value.showPicker
                        clickSound()
                        return true
                    }
                    
                    yPos += 16
                }
                
                is TextValue -> {
                    val startText = value.name + "§f: "
                    val valueText = value.get().toString()
                    val valueX = settingsStartX + 10
                    val textX = valueX + fontSemibold35.getStringWidth(startText)
                    
                    fontSemibold35.drawString(startText, valueX.toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    fontSemibold35.drawString(valueText, textX.toFloat(), (yPos + 4).toFloat(), Color.LIGHT_GRAY.rgb)
                    
                    yPos += 16
                }
                
                else -> {
                    val text = value.name + "§f: §c" + value.get().toString() + " §8${suffix}"
                    val valueX = settingsStartX + 10
                    
                    fontSemibold35.drawString(text, valueX.toFloat(), (yPos + 4).toFloat(), Color.WHITE.rgb)
                    yPos += 16
                }
            }
        }
        
        return false
    }
    
    // 重置选中模块（当切换分类时）
    fun resetSelection() {
        selectedModuleElement = null
    }
    
    // 清除模块索引缓存
    fun clearModuleIndices() {
        moduleIndices.clear()
    }
    
    // 辅助函数：格式化浮点数
    private fun formatFloat(value: Float): String {
        return String.format("%.2f", value)
    }
}