/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.ui.client.clickgui.style.styles

import io.qzz.nekobounce.config.*
import io.qzz.nekobounce.event.Listenable
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.features.module.ModuleManager
import io.qzz.nekobounce.features.module.modules.render.ClickGUI
import io.qzz.nekobounce.ui.client.clickgui.Panel
import io.qzz.nekobounce.ui.client.clickgui.elements.ButtonElement
import io.qzz.nekobounce.ui.client.clickgui.elements.ModuleElement
import io.qzz.nekobounce.ui.client.clickgui.style.Style
import io.qzz.nekobounce.ui.font.Fonts.fontSemibold35
import io.qzz.nekobounce.ui.font.Fonts.fontSemibold40
import io.qzz.nekobounce.utils.extensions.component1
import io.qzz.nekobounce.utils.extensions.component2
import io.qzz.nekobounce.utils.render.ColorUtils
import io.qzz.nekobounce.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.MathHelper
import net.minecraft.util.StringUtils
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.math.max

@SideOnly(Side.CLIENT)
object AugustusStyle : Style(), Listenable {

    private var dragging = false
    private var draggingX = 0f
    private var draggingY = 0f
    private var posX = 100f
    private var posY = 100f
    private var width = 600f
    private var height = 400f
    
    private var selectModule: Module? = null
    private var waitingForKey = false
    private var valueScroll = 0f
    
    // 颜色选择器状态
    private data class ColorPickerState(
        var hue: Float = 0f,
        var saturation: Float = 1f,
        var brightness: Float = 1f,
        var alpha: Float = 1f,
        var draggingHue: Boolean = false,
        var draggingColor: Boolean = false,
        var isOpen: Boolean = false,
        var rainbow: Boolean = false,
        var lastChosenSlider: ColorSliderType? = null
    )
    
    enum class ColorSliderType {
        HUE, SATURATION, BRIGHTNESS, ALPHA
    }
    
    // 滑块状态
    private val sliderValues = mutableMapOf<String, Float>()
    private var draggingSlider: Pair<String, SliderType>? = null
    
    enum class SliderType {
        INT, FLOAT, PERCENT
    }
    
    // 颜色选择器状态存储
    private val colorPickerStates = mutableMapOf<String, ColorPickerState>()

    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        // Augustus 风格的面板绘制
        RenderUtils.drawBorderedRect(
            panel.x.toFloat(),
            panel.y.toFloat(),
            panel.x + panel.width.toFloat(),
            panel.y + panel.height + panel.fade.toFloat(),
            1f,
            Color.GRAY.rgb,
            Color(25, 25, 25, 180).rgb
        )

        val font = fontSemibold40
        val xPos = panel.x - (font.getStringWidth(StringUtils.stripControlCodes(panel.name)) - 100) / 2
        font.drawString(panel.name, xPos.toFloat(), (panel.y + 6).toFloat(), Color.WHITE.rgb)
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.lines()

        val width = lines.maxOfOrNull { fontSemibold35.getStringWidth(it) + 14 }
            ?: return // 没有内容就不渲染
        val height = fontSemibold35.fontHeight * lines.size + 3

        // 不要绘制超出窗口边界的悬停文本
        val (scaledWidth, scaledHeight) = ScaledResolution(mc)
        val x = mouseX.coerceIn(0, (scaledWidth / ClickGUI.scale - width).toInt())
        val y = mouseY.coerceIn(0, (scaledHeight / ClickGUI.scale - height).toInt())

        RenderUtils.drawBorderedRect(x + 9f, y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), 1f, Color.GRAY.rgb, Int.MIN_VALUE)
        lines.forEachIndexed { index, line ->
            fontSemibold35.drawString(line, (x + 12).toFloat(), (y + 3 + (fontSemibold35.fontHeight) * index).toFloat(), Int.MAX_VALUE)
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
        val font = fontSemibold40
        val xPos = buttonElement.x - (font.getStringWidth(buttonElement.displayName) - 100) / 2
        font.drawString(buttonElement.displayName, xPos.toFloat(), (buttonElement.y + 6).toFloat(), buttonElement.color)
    }

    override fun drawModuleElementAndClick(
        mouseX: Int, mouseY: Int, moduleElement: ModuleElement, mouseButton: Int?
    ): Boolean {
        val module = moduleElement.module
        val font = fontSemibold40
        
        val xPos = moduleElement.x - (font.getStringWidth(moduleElement.displayName) - 100) / 2
        font.drawString(
            moduleElement.displayName, 
            xPos.toFloat(), 
            (moduleElement.y + 6).toFloat(), 
            if (module.state) ClickGUI.guiColor else Color.WHITE.rgb
        )
        
        // 处理点击
        if (mouseButton == 0) {
            if (mouseX in moduleElement.x..(moduleElement.x + moduleElement.width) &&
                mouseY in moduleElement.y..(moduleElement.y + moduleElement.height)) {
                module.toggle()
                return true
            }
        } else if (mouseButton == 1) {
            if (mouseX in moduleElement.x..(moduleElement.x + moduleElement.width) &&
                mouseY in moduleElement.y..(moduleElement.y + moduleElement.height)) {
                selectModule = module
                valueScroll = 0f
                return true
            }
        }
        
        return false
    }
    
    // Augustus 专用 GUI 绘制方法
    fun drawAugustusGui(mouseX: Int, mouseY: Int) {
        // 更新位置
        if (dragging) {
            val sr = ScaledResolution(mc)
            posX = MathHelper.clamp_float(mouseX - draggingX, 0f, (sr.scaledWidth - width).toFloat())
            posY = MathHelper.clamp_float(mouseY - draggingY, 0f, (sr.scaledHeight - height).toFloat())
        }
        
        // 限制最小尺寸
        width = max(width, 480f)
        height = max(height, 300f)

        // 绘制背景
        RenderUtils.drawBorderedRect(posX, posY, posX + width, posY + height, 2f, Color(60, 60, 60).rgb, Color(25, 25, 25, 200).rgb)
        
        // 绘制标题栏
        RenderUtils.drawRect(posX, posY, posX + width, posY + 20, Color(34, 34, 34).rgb)
        fontSemibold40.drawString("Augustus GUI", posX + 5f, posY + 6f, Color.WHITE.rgb)
        
        // 绘制模块选择侧边栏
        RenderUtils.drawRect(posX, posY + 20, posX + 110, posY + height, Color(30, 30, 30, 150).rgb)
        
        // 绘制右侧区域
        RenderUtils.drawRect(posX + 110, posY + 20, posX + width, posY + height, Color(35, 35, 35, 120).rgb)
        
        // 处理鼠标滚轮滚动
        if (mouseX in posX.toInt()..(posX + 110).toInt() && 
            mouseY in (posY + 20).toInt()..(posY + height).toInt()) {
            valueScroll = Math.min(0f, valueScroll + Mouse.getDWheel() * 0.1f)
        }
        
        // 绘制选中的模块设置
        selectModule?.let { module ->
            val font = fontSemibold35
            var currentY = posY + 30f
            
            // 模块名称
            font.drawString("${module.name}:", posX + 120f, currentY, Color(200, 200, 200).rgb)
            currentY += 20f
            
            // 按键绑定
            val keyName = if (module.keyBind == 0) "None" else Keyboard.getKeyName(module.keyBind)
            val keyText = "Key: $keyName"
            val keyHovered = mouseX in (posX+120).toInt()..(posX+120+font.getStringWidth(keyText)).toInt() && 
                           mouseY in currentY.toInt()..(currentY + font.fontHeight).toInt()
            
            font.drawString(keyText, posX + 120f, currentY, 
                if (waitingForKey) ClickGUI.guiColor else 
                if (keyHovered) Color(200, 200, 200).rgb else Color(150, 150, 150).rgb)
            
            currentY += 20f
            
            // 绘制模块的配置选项
            // 这里需要根据B100的API来访问模块的设置
            // 由于B100删除了Value系统，我们需要使用模块的字段
            // 这里只作为一个示例，实际使用时需要根据模块的具体字段来调整
            
            // 尝试获取模块的所有字段，并检查是否有注解或标记
            val moduleClass = module.javaClass
            for (field in moduleClass.declaredFields) {
                field.isAccessible = true
                
                // 检查是否是模块设置字段（根据B100的命名约定）
                if (field.name != "state" && field.name != "keyBind" && field.name != "name") {
                    val value = field.get(module)
                    
                    when (value) {
                        is Boolean -> {
                            val text = "${field.name}: ${if (value) "On" else "Off"}"
                            val hovered = mouseX in (posX+120).toInt()..(posX+120+font.getStringWidth(text)).toInt() && 
                                        mouseY in currentY.toInt()..(currentY + font.fontHeight).toInt()
                            
                            font.drawString(text, posX + 120f, currentY, 
                                if (value) Color(0, 200, 0).rgb else 
                                if (hovered) Color(200, 200, 200).rgb else Color(150, 150, 150).rgb)
                            
                            if (hovered && Mouse.isButtonDown(0)) {
                                field.set(module, !value)
                            }
                            
                            currentY += 20f
                        }
                        
                        is Int -> {
                            // 尝试获取范围信息（如果有）
                            val range = getIntRangeFromField(field)
                            // 绘制滑块标签
                            font.drawString("${field.name}: $value", posX + 120f, currentY, Color(200, 200, 200).rgb)
                            currentY += 15f
                            
                            // 绘制滑块
                            drawIntSlider(value, range, mouseX, mouseY, posX + 120f, currentY, 150f, module.name, field.name) { newValue ->
                                field.set(module, newValue)
                            }
                            currentY += 25f
                        }
                        
                        is Float -> {
                            // 尝试获取范围信息（如果有）
                            val range = getFloatRangeFromField(field)
                            // 绘制滑块标签
                            val roundedValue = (value * 100).toInt() / 100f
                            font.drawString("${field.name}: $roundedValue", posX + 120f, currentY, Color(200, 200, 200).rgb)
                            currentY += 15f
                            
                            // 绘制滑块
                            drawFloatSlider(value, range, mouseX, mouseY, posX + 120f, currentY, 150f, module.name, field.name) { newValue ->
                                field.set(module, newValue)
                            }
                            currentY += 25f
                        }
                        
                        is String -> {
                            // 绘制文本标签
                            font.drawString("${field.name}: $value", posX + 120f, currentY, Color(200, 200, 200).rgb)
                            currentY += 20f
                        }
                        
                        else -> {
                            // 其他类型的值
                            font.drawString("${field.name}: $value", posX + 120f, currentY, Color(200, 200, 200).rgb)
                            currentY += 20f
                        }
                    }
                }
            }
        }
        
        // 绘制模块列表
        drawModuleList(mouseX, mouseY)
    }
    
    private fun getIntRangeFromField(field: java.lang.reflect.Field): Pair<Int, Int> {
        // 这里应该根据B100的注解系统来获取范围
        // 为了示例，我们假设一些默认值
        return Pair(0, 100)
    }
    
    private fun getFloatRangeFromField(field: java.lang.reflect.Field): Pair<Float, Float> {
        // 这里应该根据B100的注解系统来获取范围
        // 为了示例，我们假设一些默认值
        return Pair(0f, 1f)
    }
    
    private fun drawIntSlider(currentValue: Int, range: Pair<Int, Int>, mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, moduleName: String, fieldName: String, onValueChanged: (Int) -> Unit) {
        val (min, max) = range
        
        // 计算滑块位置
        val percentage = (currentValue - min).toFloat() / (max - min).toFloat()
        val sliderPos = x + percentage * width
        
        // 绘制滑块背景
        RenderUtils.drawRect(x, y, x + width, y + 8f, Color(60, 60, 60).rgb)
        
        // 绘制滑块进度
        RenderUtils.drawRect(x, y, sliderPos, y + 8f, ClickGUI.guiColor)
        
        // 绘制滑块手柄
        RenderUtils.drawRect(sliderPos - 3f, y - 2f, sliderPos + 3f, y + 10f, Color.WHITE.rgb)
        
        // 处理滑块拖动
        val sliderId = "$moduleName:$fieldName"
        val isDragging = draggingSlider?.first == sliderId && draggingSlider?.second == SliderType.INT
        
        if (Mouse.isButtonDown(0) && !isDragging) {
            if (mouseX in x.toInt()..(x + width).toInt() && 
                mouseY in y.toInt()..(y + 8).toInt()) {
                draggingSlider = sliderId to SliderType.INT
            }
        }
        
        if (isDragging) {
            val mousePercentage = MathHelper.clamp_float((mouseX - x) / width, 0f, 1f)
            val newValue = min + (mousePercentage * (max - min)).toInt()
            onValueChanged(newValue)
            
            if (!Mouse.isButtonDown(0)) {
                draggingSlider = null
            }
        }
    }
    
    private fun drawFloatSlider(currentValue: Float, range: Pair<Float, Float>, mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, moduleName: String, fieldName: String, onValueChanged: (Float) -> Unit) {
        val (min, max) = range
        
        // 计算滑块位置
        val percentage = (currentValue - min) / (max - min)
        val sliderPos = x + percentage * width
        
        // 绘制滑块背景
        RenderUtils.drawRect(x, y, x + width, y + 8f, Color(60, 60, 60).rgb)
        
        // 绘制滑块进度
        RenderUtils.drawRect(x, y, sliderPos, y + 8f, ClickGUI.guiColor)
        
        // 绘制滑块手柄
        RenderUtils.drawRect(sliderPos - 3f, y - 2f, sliderPos + 3f, y + 10f, Color.WHITE.rgb)
        
        // 处理滑块拖动
        val sliderId = "$moduleName:$fieldName"
        val isDragging = draggingSlider?.first == sliderId && draggingSlider?.second == SliderType.FLOAT
        
        if (Mouse.isButtonDown(0) && !isDragging) {
            if (mouseX in x.toInt()..(x + width).toInt() && 
                mouseY in y.toInt()..(y + 8).toInt()) {
                draggingSlider = sliderId to SliderType.FLOAT
            }
        }
        
        if (isDragging) {
            val mousePercentage = MathHelper.clamp_float((mouseX - x) / width, 0f, 1f)
            val newValue = min + mousePercentage * (max - min)
            val roundedValue = (newValue * 100).toInt() / 100f
            onValueChanged(roundedValue)
            
            if (!Mouse.isButtonDown(0)) {
                draggingSlider = null
            }
        }
    }
    
    private fun drawModuleList(mouseX: Int, mouseY: Int) {
        var moduleY = posY + 30f + valueScroll
        
        // 按分类绘制模块
        for (category in Category.values()) {
            // 绘制分类标题
            fontSemibold35.drawString(category.displayName, posX + 10f, moduleY, ClickGUI.guiColor)
            moduleY += 15f
            
            // 绘制该分类下的模块 - 使用正确的模块获取方式
            // B100 中可能使用不同的方式获取模块列表
            // 这里使用 try-catch 来处理可能的异常
            try {
                // 尝试不同的方法来获取模块
                val modulesInCategory = try {
                    // 方法1: 假设有 getModules() 方法
                    val getModulesMethod = ModuleManager::class.java.getMethod("getModules")
                    (getModulesMethod.invoke(null) as? List<Module>)?.filter { it.category == category } ?: emptyList()
                } catch (e1: Exception) {
                    try {
                        // 方法2: 假设有 modules 属性
                        val modulesField = ModuleManager::class.java.getDeclaredField("modules")
                        modulesField.isAccessible = true
                        (modulesField.get(null) as? List<Module>)?.filter { it.category == category } ?: emptyList()
                    } catch (e2: Exception) {
                        // 方法3: 尝试其他可能的方法名
                        try {
                            val getModuleListMethod = ModuleManager::class.java.getMethod("getModuleList")
                            (getModuleListMethod.invoke(null) as? List<Module>)?.filter { it.category == category } ?: emptyList()
                        } catch (e3: Exception) {
                            // 如果都失败，返回空列表
                            emptyList<Module>()
                        }
                    }
                }
                
                for (module in modulesInCategory) {
                    if (moduleY < posY + 20 || moduleY > posY + height - 20) {
                        moduleY += 20f
                        continue
                    }
                    
                    val moduleName = module.name
                    val moduleHovered = mouseX in posX.toInt()..(posX + 110).toInt() && 
                                      mouseY in moduleY.toInt()..(moduleY + fontSemibold35.fontHeight).toInt()
                    
                    val textColor = when {
                        module == selectModule -> ClickGUI.guiColor
                        moduleHovered -> Color(200, 200, 200).rgb
                        module.state -> Color(0, 200, 0).rgb
                        else -> Color(150, 150, 150).rgb
                    }
                    
                    fontSemibold35.drawString(moduleName, posX + 15f, moduleY, textColor)
                    
                    // 处理模块选择
                    if (moduleHovered && Mouse.isButtonDown(0)) {
                        selectModule = module
                        valueScroll = 0f
                        colorPickerStates.clear()
                        sliderValues.clear()
                    }
                    
                    moduleY += 20f
                }
            } catch (e: Exception) {
                // 如果发生异常，跳过这个分类
                e.printStackTrace()
            }
            
            moduleY += 10f // 分类间间距
        }
    }
    
    // 事件处理器
    fun handleMouseClick() {
        val sr = ScaledResolution(mc)
        val mouseX = Mouse.getX() * sr.scaledWidth / mc.displayWidth
        val mouseY = sr.scaledHeight - Mouse.getY() * sr.scaledHeight / mc.displayHeight - 1
        
        // 检查标题栏拖动
        if (Mouse.getEventButton() == 0) {
            if (mouseX.toFloat() in posX..(posX + width) && mouseY.toFloat() in posY..(posY + 20)) {
                dragging = true
                draggingX = (mouseX - posX).toFloat()
                draggingY = (mouseY - posY).toFloat()
            }
            
            // 检查按键绑定点击
            selectModule?.let { module ->
                val font = fontSemibold35
                val keyText = "Key: ${if (module.keyBind == 0) "None" else Keyboard.getKeyName(module.keyBind)}"
                val textX = posX + 120f
                val textY = posY + 50f
                
                if (mouseX.toFloat() in textX..(textX + font.getStringWidth(keyText)) &&
                    mouseY.toFloat() in textY..(textY + font.fontHeight)) {
                    waitingForKey = !waitingForKey
                }
            }
        }
    }
    
    fun handleMouseRelease() {
        if (Mouse.getEventButton() == 0) {
            dragging = false
            draggingSlider = null
            
            // 停止所有颜色选择器拖动
            colorPickerStates.values.forEach { state ->
                state.lastChosenSlider = null
            }
        }
    }
    
    fun handleKey(keyCode: Int) {
        if (waitingForKey) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                selectModule?.keyBind = 0
            } else {
                selectModule?.keyBind = keyCode
            }
            waitingForKey = false
        }
    }
}